package org.rusherhack.autobreed.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.*;
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

    // General Settings
    private final NumberSetting<Integer> breedRadius = new NumberSetting<>("Breed Radius", 5, 1, 10);
    private final BooleanSetting prioritizePairs = new BooleanSetting("Prioritize Pairs", true);

    // Mob-Specific Settings
    private final BooleanSetting breedCows = new BooleanSetting("Breed Cows", true);
    private final BooleanSetting breedSheep = new BooleanSetting("Breed Sheep", true);
    private final BooleanSetting breedPigs = new BooleanSetting("Breed Pigs", true);
    private final BooleanSetting breedChickens = new BooleanSetting("Breed Chickens", true);
    private final BooleanSetting breedWolves = new BooleanSetting("Breed Wolves", true);
    private final BooleanSetting breedCats = new BooleanSetting("Breed Cats", true);

    private int previousHotbarSlot = -1;
    private int swappedInventorySlot = -1;
    private boolean hasSwitchedItem = false;
    private final Set<UUID> fedAnimals = new HashSet<>();
    private final Map<UUID, Long> interactionTimestamps = new HashMap<>();

    private static final Map<Class<? extends Animal>, Item> BREEDING_ITEMS = Map.of(
            Cow.class, Items.WHEAT,
            Sheep.class, Items.WHEAT,
            Pig.class, Items.CARROT,
            Chicken.class, Items.WHEAT_SEEDS,
            Wolf.class, Items.BEEF,
            Cat.class, Items.COD
    );

    public AutoBreedModule() {
        super("AutoBreed", "Automatically switches to mob food type when in range", ModuleCategory.MISC);

        // Create subcategories
        BooleanSetting generalSettings = new BooleanSetting("General Settings", true);
        generalSettings.addSubSettings(breedRadius, prioritizePairs);

        BooleanSetting mobSettings = new BooleanSetting("Mobs", true);
        mobSettings.addSubSettings(breedCows, breedSheep, breedPigs, breedChickens, breedWolves, breedCats);

        this.registerSettings(generalSettings, mobSettings);
    }

    @Override
    public void onEnable() {
        RusherHackAPI.getEventBus().subscribe(this);
        ChatUtils.print("AutoBreed enabled!");
        fedAnimals.clear(); // Reset fed animals on enable
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

        List<Animal> nearbyAnimals = mc.level.getEntitiesOfClass(Animal.class,
                mc.player.getBoundingBox().inflate(breedRadius.getValue()));

        if (nearbyAnimals.isEmpty()) {
            revertSlot();
            return;
        }

        Set<UUID> processedAnimals = new HashSet<>();

        for (Animal animal : nearbyAnimals) {
            if (!shouldBreed(animal) || processedAnimals.contains(animal.getUUID())) continue;

            // Delay check to avoid instant failure messages
            if (interactionTimestamps.containsKey(animal.getUUID()) &&
                System.currentTimeMillis() - interactionTimestamps.get(animal.getUUID()) < 1000) {
                continue;
            }

            Animal partner = prioritizePairs.getValue() ? nearbyAnimals.stream()
                    .filter(other -> other.getClass() == animal.getClass() &&
                                     !other.isBaby() && !other.isInLove() &&
                                     other.canFallInLove() && !fedAnimals.contains(other.getUUID()) &&
                                     !other.getUUID().equals(animal.getUUID()))
                    .findFirst()
                    .orElse(null) : null;

            if (prioritizePairs.getValue() && partner == null) {
                continue;
            }

            Item foodItem = BREEDING_ITEMS.get(animal.getClass());
            if (foodItem != null) {
                int foodSlot = InventoryUtils.findItem(foodItem, true, false);

                if ((foodSlot >= 0 && foodSlot <= 8) || (foodSlot >= 9 && foodSlot <= 35)) {
                    if (!hasSwitchedItem) {
                        previousHotbarSlot = mc.player.getInventory().selected;
                    }
                    switchToItem(foodSlot);
                    interactWithMob(animal);
                    interactionTimestamps.put(animal.getUUID(), System.currentTimeMillis());

                    if (partner != null) {
                        interactWithMob(partner);
                        interactionTimestamps.put(partner.getUUID(), System.currentTimeMillis());
                    }
                }
            }
        }
        revertSlot();
    }


    private boolean shouldBreed(Animal animal) {
        if (fedAnimals.contains(animal.getUUID()) || animal.isBaby() || animal.isInLove() || !animal.canFallInLove()) {
            return false;
        }
        return (animal instanceof Cow && breedCows.getValue()) ||
               (animal instanceof Sheep && breedSheep.getValue()) ||
               (animal instanceof Pig && breedPigs.getValue()) ||
               (animal instanceof Chicken && breedChickens.getValue()) ||
               (animal instanceof Wolf && breedWolves.getValue()) ||
               (animal instanceof Cat && breedCats.getValue());
    }

    private void revertSlot() {
        if (!hasSwitchedItem) return;
        if (swappedInventorySlot != -1) {
            InventoryUtils.swapSlots(swappedInventorySlot, previousHotbarSlot);
            swappedInventorySlot = -1;
        }
        if (previousHotbarSlot >= 0 && previousHotbarSlot <= 8) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(previousHotbarSlot));
            mc.player.getInventory().selected = previousHotbarSlot;
            ChatUtils.print("Successfully switched back to slot: " + previousHotbarSlot);
        }
        hasSwitchedItem = false;
        previousHotbarSlot = -1;
    }

    private void switchToItem(int slot) {
        if (slot < 9) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        } else if (slot >= 9 && slot <= 35) {
            if (previousHotbarSlot != -1) {
                InventoryUtils.swapSlots(slot, previousHotbarSlot);
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(previousHotbarSlot));
                swappedInventorySlot = slot;
            }
        }
        hasSwitchedItem = true;
    }

    private void interactWithMob(Animal animal) {
        if (mc.player == null || animal == null) return;
        mc.player.connection.send(ServerboundInteractPacket.createInteractionPacket(animal, false, InteractionHand.MAIN_HAND));
    }
}
