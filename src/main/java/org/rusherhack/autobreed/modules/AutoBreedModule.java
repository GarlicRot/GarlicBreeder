package org.rusherhack.autobreed.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.client.api.utils.InventoryUtils;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.NumberSetting;

import java.util.*;

public class AutoBreedModule extends ToggleableModule {
    private final Minecraft mc = Minecraft.getInstance();

    private final NumberSetting<Integer> breedRadius = new NumberSetting<>("Breed Radius", 5, 1, 10);
    private final BooleanSetting prioritizePairs = new BooleanSetting("Prioritize Pairs", true);
    
    private final BooleanSetting followMode = new BooleanSetting("Follow Mode", false);

    private final BooleanSetting breedCows = new BooleanSetting("Cows", true);
    private final BooleanSetting breedSheep = new BooleanSetting("Sheep", true);
    private final BooleanSetting breedPigs = new BooleanSetting("Pigs", true);
    private final BooleanSetting breedChickens = new BooleanSetting("Chickens", true);
    private final BooleanSetting breedWolves = new BooleanSetting("Wolves", true);
    private final BooleanSetting breedCats = new BooleanSetting("Cats", true);
    private final BooleanSetting breedHorses = new BooleanSetting("Horses", true);
    private final BooleanSetting breedLlamas = new BooleanSetting("Llamas", true);
    private final BooleanSetting breedFoxes = new BooleanSetting("Foxes", true);
    private final BooleanSetting breedPandas = new BooleanSetting("Pandas", true);
    private final BooleanSetting breedTurtles = new BooleanSetting("Turtles", true);
    private final BooleanSetting feedBabies = new BooleanSetting("Feed Babies", false);

    // We mark animals that have been fed to avoid repeatedly feeding them
    private final Set<UUID> fedAnimals = new HashSet<>();
    // We also track interaction timestamps so we don't spam clicks
    private final Map<UUID, Long> interactionTimestamps = new HashMap<>();

    private int previousHotbarSlot = -1;
    private int swappedInventorySlot = -1;
    private boolean hasSwitchedItem = false;

    private static final Map<Class<? extends Animal>, List<Item>> BREEDING_ITEMS = new HashMap<>();

    static {
        BREEDING_ITEMS.put(Cow.class, Collections.singletonList(Items.WHEAT));
        BREEDING_ITEMS.put(Sheep.class, Collections.singletonList(Items.WHEAT));
        BREEDING_ITEMS.put(Pig.class, Arrays.asList(Items.CARROT, Items.POTATO, Items.BEETROOT));
        BREEDING_ITEMS.put(Chicken.class, Arrays.asList(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.PUMPKIN_SEEDS, Items.MELON_SEEDS));
        BREEDING_ITEMS.put(Wolf.class, Arrays.asList(Items.BEEF, Items.CHICKEN, Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.PORKCHOP, Items.COOKED_PORKCHOP));
        BREEDING_ITEMS.put(Cat.class, Arrays.asList(Items.COD, Items.SALMON));
        BREEDING_ITEMS.put(Horse.class, Arrays.asList(Items.GOLDEN_CARROT, Items.GOLDEN_APPLE));
        BREEDING_ITEMS.put(Llama.class, Collections.singletonList(Items.HAY_BLOCK));
        BREEDING_ITEMS.put(Fox.class, Arrays.asList(Items.SWEET_BERRIES, Items.GLOW_BERRIES));
        BREEDING_ITEMS.put(Panda.class, Collections.singletonList(Items.BAMBOO));
        BREEDING_ITEMS.put(Turtle.class, Collections.singletonList(Items.SEAGRASS));
    }

    public AutoBreedModule() {
        super("AutoBreed", "Automatically breeds and feeds animals", ModuleCategory.MISC);

        BooleanSetting generalSettings = new BooleanSetting("General Settings", true);
        generalSettings.addSubSettings(breedRadius, prioritizePairs, followMode);

        BooleanSetting mobSettings = new BooleanSetting("Mobs", true);
        mobSettings.addSubSettings(
                breedCows, breedSheep, breedPigs, breedChickens, breedWolves,
                breedCats, breedHorses, breedLlamas, breedFoxes, breedPandas,
                breedTurtles, feedBabies
        );

        this.registerSettings(generalSettings, mobSettings);
    }

    @Override
    public void onEnable() {
        RusherHackAPI.getEventBus().subscribe(this);
        ChatUtils.print("AutoBreed enabled!");
        fedAnimals.clear();
        interactionTimestamps.clear();
    }

    @Override
    public void onDisable() {
        RusherHackAPI.getEventBus().unsubscribe(this);
        revertSlot();
        ChatUtils.print("AutoBreed disabled!");
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.level == null) return;

        // Get all nearby animals within radius
        List<Animal> nearbyAnimals = mc.level.getEntitiesOfClass(
            Animal.class,
            mc.player.getBoundingBox().inflate(breedRadius.getValue())
        );

        // If none around, revert and do nothing
        if (nearbyAnimals.isEmpty()) {
            revertSlot();
            return;
        }

        // FOLLOW MODE - just hold the correct item without feeding
        if (followMode.getValue()) {
            handleFollowMode(nearbyAnimals);
            return;
        }

        // NORMAL BREEDING LOGIC
        for (Animal animal : nearbyAnimals) {
            if (!shouldBreed(animal, nearbyAnimals)) continue;

            // Only feed each adult once every second
            if (!animal.isBaby() && interactionTimestamps.containsKey(animal.getUUID())) {
                long lastInteraction = interactionTimestamps.get(animal.getUUID());
                if (System.currentTimeMillis() - lastInteraction < 1000) {
                    continue;
                }
            }

            // Lookup the correct item to feed
            List<Item> foodItems = BREEDING_ITEMS.get(animal.getClass());
            if (foodItems == null || foodItems.isEmpty()) continue;
            Item foodItem = foodItems.get(0);
            if (foodItem == null) continue;

            int foodSlot = InventoryUtils.findItem(foodItem, true, false);
            if (foodSlot < 0) continue;

            // Switch to item
            if (!hasSwitchedItem) {
                previousHotbarSlot = mc.player.getInventory().selected;
            }
            switchToItem(foodSlot);

            // Feed logic
            if (animal.isBaby() && feedBabies.getValue()) {
                // Spam feed a bit
                for (int i = 0; i < 5; i++) {
                    interactWithMob(animal);
                }
            } else {
                // Normal single feed
                interactWithMob(animal);
            }

            // Mark the time we fed, so we don't spam
            interactionTimestamps.put(animal.getUUID(), System.currentTimeMillis());
            // Mark this animal as fed so we won't feed it again
            fedAnimals.add(animal.getUUID());
        }

        revertSlot();
    }

    /**
     * If Follow Mode is enabled, we skip breeding. We just hold a suitable item to lure
     * any animal that can be lured within range.
     */
    private void handleFollowMode(List<Animal> nearbyAnimals) {
        Animal lurable = null;
        for (Animal animal : nearbyAnimals) {
            List<Item> items = BREEDING_ITEMS.get(animal.getClass());
            if (items != null && !items.isEmpty()) {
                lurable = animal;
                break; // Just pick the first valid
            }
        }

        if (lurable != null) {
            // Switch to the luring item
            List<Item> items = BREEDING_ITEMS.get(lurable.getClass());
            Item item = items.get(0);
            int foodSlot = InventoryUtils.findItem(item, true, false);

            if (foodSlot >= 0) {
                if (!hasSwitchedItem) {
                    previousHotbarSlot = mc.player.getInventory().selected;
                }
                switchToItem(foodSlot);
            }
        } else {
            // No lurable animals found, revert slot
            revertSlot();
        }
    }

    /**
     * Decide if an Animal should be bred/fed right now.
     */
    private boolean shouldBreed(Animal animal, List<Animal> nearbyAnimals) {
        // Already fed or can’t breed
        if (fedAnimals.contains(animal.getUUID())) return false;
        if (animal.isInLove() || !animal.canFallInLove()) return false;

        // Babies ignore Prioritize Pairs logic if feedBabies is on
        if (animal.isBaby()) {
            return feedBabies.getValue() && BREEDING_ITEMS.containsKey(animal.getClass());
        }

        // For an adult, if prioritizePairs is on, ensure there's at least one adult partner
        if (prioritizePairs.getValue()) {
            boolean foundPartner = false;
            for (Animal other : nearbyAnimals) {
                if (other == animal) continue;
                if (other.getClass() != animal.getClass()) continue;
                if (other.isBaby()) continue;  // partner must be adult
                if (fedAnimals.contains(other.getUUID())) continue;
                if (!other.canFallInLove() || other.isInLove()) continue;
                // If we get here, 'other' is a valid partner
                foundPartner = true;
                break;
            }
            if (!foundPartner) {
                return false; // no partner in range
            }
        }

        // Check which mobs we breed
        return (animal instanceof Cow && breedCows.getValue())
                || (animal instanceof Sheep && breedSheep.getValue())
                || (animal instanceof Pig && breedPigs.getValue())
                || (animal instanceof Chicken && breedChickens.getValue())
                || (animal instanceof Wolf && breedWolves.getValue())
                || (animal instanceof Cat && breedCats.getValue())
                || (animal instanceof Horse && breedHorses.getValue())
                || (animal instanceof Llama && breedLlamas.getValue())
                || (animal instanceof Fox && breedFoxes.getValue())
                || (animal instanceof Panda && breedPandas.getValue())
                || (animal instanceof Turtle && breedTurtles.getValue());
    }

    private void interactWithMob(Animal animal) {
        if (mc.player == null || animal == null) return;
        mc.player.connection.send(
            ServerboundInteractPacket.createInteractionPacket(animal, false, InteractionHand.MAIN_HAND)
        );
    }

    private void switchToItem(int slot) {
        if (slot < 9) {
            // Directly switch hotbar
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        } else if (slot >= 9 && slot <= 35) {
            // We found the item in the main inventory, so swap into our hotbar slot
            if (previousHotbarSlot != -1) {
                InventoryUtils.swapSlots(slot, previousHotbarSlot);
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(previousHotbarSlot));
                swappedInventorySlot = slot;
            }
        }
        hasSwitchedItem = true;
    }

    private void revertSlot() {
        if (!hasSwitchedItem) return;

        // If we swapped something from our main hotbar to an inventory slot, swap it back
        if (swappedInventorySlot != -1) {
            InventoryUtils.swapSlots(swappedInventorySlot, previousHotbarSlot);
            swappedInventorySlot = -1;
        }

        // Restore original selected hotbar slot
        if (previousHotbarSlot >= 0 && previousHotbarSlot <= 8) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(previousHotbarSlot));
            mc.player.getInventory().selected = previousHotbarSlot;
        }

        hasSwitchedItem = false;
        previousHotbarSlot = -1;
    }
}
