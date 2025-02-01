package org.rusherhack.autobreed.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.animal.*;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.client.api.utils.InventoryUtils;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.NumberSetting;

import java.util.List;
import java.util.Map;

public class AutoBreedModule extends ToggleableModule {
    private final Minecraft mc = Minecraft.getInstance();

    private final NumberSetting<Integer> breedRadius = new NumberSetting<>(
            "Breed Radius", 5, 1, 10);

    private int previousHotbarSlot = -1;
    private int swappedInventorySlot = -1;
    private boolean hasSwitchedItem = false;

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
        this.registerSettings(breedRadius);
    }

    @Override
    public void onEnable() {
        RusherHackAPI.getEventBus().subscribe(this);
        ChatUtils.print("AutoBreed enabled!");
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

        // Find nearby animals
        List<Animal> nearbyAnimals = mc.level.getEntitiesOfClass(Animal.class,
                mc.player.getBoundingBox().inflate(breedRadius.getValue()));

        if (nearbyAnimals.isEmpty()) {
            revertSlot(); // Revert slot if no mobs are nearby
            return;
        }

        for (Animal animal : nearbyAnimals) {
            if (!animal.isBaby() && !animal.isInLove() && animal.canFallInLove()) {
                Item foodItem = BREEDING_ITEMS.get(animal.getClass());
                if (foodItem != null) {
                    int foodSlot = InventoryUtils.findItem(foodItem, true, false); // Find the food item using RusherHack API

                    // Validate the slot
                    if ((foodSlot >= 0 && foodSlot <= 8) || (foodSlot >= 9 && foodSlot <= 35)) { // Hotbar or main inventory only
                        if (!hasSwitchedItem) {
                            previousHotbarSlot = mc.player.getInventory().selected; // Save current hotbar slot
                        }
                        switchToItem(foodSlot);
                        ChatUtils.print("Switched to food: " + foodItem);
                        return;
                    } else {
                        ChatUtils.print("No valid food slot found.");
                    }
                }
            }
        }
    }

    private void switchToItem(int slot) {
        if (slot < 9) {
            // The item is already in the hotbar; just switch to it
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        } else if (slot >= 9 && slot <= 35) {
            // Move the item from the inventory to the hotbar
            if (previousHotbarSlot != -1) {
                ChatUtils.print("Swapping inventory slot " + slot + " with hotbar slot " + previousHotbarSlot);
                InventoryUtils.swapSlots(slot, previousHotbarSlot); // Use RusherHack API for swapping
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(previousHotbarSlot)); // Switch to the new hotbar slot
                swappedInventorySlot = slot; // Track the swapped inventory slot
            }
        }
        hasSwitchedItem = true; // Mark that we switched to an item
    }

    private void revertSlot() {
        if (!hasSwitchedItem) return;

        ChatUtils.print("Reverting to previous slot: " + previousHotbarSlot);

        if (swappedInventorySlot != -1) {
            // Move the item back to its original inventory slot
            if (swappedInventorySlot >= 9 && swappedInventorySlot <= 35) { // Ensure it's a valid inventory slot
                ChatUtils.print("Moving item from inventory slot " + swappedInventorySlot + " back to hotbar slot " + previousHotbarSlot);
                InventoryUtils.swapSlots(swappedInventorySlot, previousHotbarSlot); // Use RusherHack API to swap
            }
            swappedInventorySlot = -1; // Clear the swapped slot tracking
        }

        if (previousHotbarSlot >= 0 && previousHotbarSlot <= 8) { // Ensure valid hotbar slot
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(previousHotbarSlot)); // Switch back to original hotbar slot
            mc.player.getInventory().selected = previousHotbarSlot; // Update the local inventory state
            ChatUtils.print("Successfully switched back to slot: " + previousHotbarSlot);
        }

        hasSwitchedItem = false;
        previousHotbarSlot = -1; // Reset slot tracking
    }



}
