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
    private final BooleanSetting feedBabies = new BooleanSetting("Feed Babies", false);

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
        super("AutoBreed", "Automatically breeds and feeds animals", ModuleCategory.MISC);
        
        BooleanSetting generalSettings = new BooleanSetting("General Settings", true);
        generalSettings.addSubSettings(breedRadius, prioritizePairs);

        BooleanSetting mobSettings = new BooleanSetting("Mobs", true);
        mobSettings.addSubSettings(breedCows, breedSheep, breedPigs, breedChickens, breedWolves, breedCats, feedBabies);

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

        List<Animal> nearbyAnimals = mc.level.getEntitiesOfClass(Animal.class,
                mc.player.getBoundingBox().inflate(breedRadius.getValue()));

        if (nearbyAnimals.isEmpty()) {
            revertSlot();
            return;
        }

        for (Animal animal : nearbyAnimals) {
            if (!shouldBreed(animal)) continue;
            
            if (!animal.isBaby() && interactionTimestamps.containsKey(animal.getUUID()) &&
                System.currentTimeMillis() - interactionTimestamps.get(animal.getUUID()) < 1000) {
                continue;
            }

            Item foodItem = BREEDING_ITEMS.get(animal.getClass());
            if (foodItem == null) continue;
            
            int foodSlot = InventoryUtils.findItem(foodItem, true, false);
            if (foodSlot < 0) continue;
            
            if (!hasSwitchedItem) {
                previousHotbarSlot = mc.player.getInventory().selected;
            }
            switchToItem(foodSlot);

            if (animal.isBaby() && feedBabies.getValue()) {
                for (int i = 0; i < 5; i++) {
                    interactWithMob(animal);
                }
            } else {
                interactWithMob(animal);
            }
            
            interactionTimestamps.put(animal.getUUID(), System.currentTimeMillis());
        }
        revertSlot();
    }

    private boolean shouldBreed(Animal animal) {
        if (fedAnimals.contains(animal.getUUID()) || animal.isInLove() || !animal.canFallInLove()) {
            return false;
        }
        if (animal.isBaby()) {
            return feedBabies.getValue() && BREEDING_ITEMS.containsKey(animal.getClass());
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
