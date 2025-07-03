package org.rusherhack.autobreed.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Rabbit;
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
    private final BooleanSetting followMode = new BooleanSetting("Follow Mode", false);

    // Allay Settings
    private final NumberSetting<Integer> shardCountRequired = new NumberSetting<>("Min Shard Count", 1, 1, 16);


    // If true, we feed baby animals as well (instantly, repeatedly)
    private final BooleanSetting feedBabies = new BooleanSetting("Feed Babies", false);

    // Mob Settings
    private final BooleanSetting breedCows       = new BooleanSetting("Cows", true);
    private final BooleanSetting breedSheep      = new BooleanSetting("Sheep", true);
    private final BooleanSetting breedPigs       = new BooleanSetting("Pigs", true);
    private final BooleanSetting breedChickens   = new BooleanSetting("Chickens", true);
    private final BooleanSetting breedWolves     = new BooleanSetting("Wolves", false);
    private final BooleanSetting breedCats       = new BooleanSetting("Cats", false);
    private final BooleanSetting breedFoxes      = new BooleanSetting("Foxes", true);
    private final BooleanSetting breedPandas     = new BooleanSetting("Pandas", true);
    private final BooleanSetting breedTurtles    = new BooleanSetting("Turtles", true);
    private final BooleanSetting breedBees       = new BooleanSetting("Bees", true);
    private final BooleanSetting breedFrogs      = new BooleanSetting("Frogs", true);
    private final BooleanSetting breedGoats      = new BooleanSetting("Goats", true);
    private final BooleanSetting breedHoglins    = new BooleanSetting("Hoglins", true);
    private final BooleanSetting breedStriders   = new BooleanSetting("Striders", true);
    private final BooleanSetting breedMooshrooms = new BooleanSetting("Mooshrooms", true);
    private final BooleanSetting breedRabbits    = new BooleanSetting("Rabbits", true);
    private final BooleanSetting breedSniffers   = new BooleanSetting("Sniffers", true);
    private final BooleanSetting breedCamels     = new BooleanSetting("Camels", true);
    private final BooleanSetting breedAxolotls   = new BooleanSetting("Axolotls", true);

    // Taming toggles
    private final BooleanSetting autoTameWolves = new BooleanSetting("Wolves", false);
    private final BooleanSetting autoTameCats   = new BooleanSetting("Cats", false);

    // Track which animals are fed & cooldown
    private final Set<UUID> fedAnimals = new HashSet<>();
    private final Map<UUID, Long> interactionTimestamps = new HashMap<>();

    private int veryOriginalSlot   = -1;
    private int previousHotbarSlot = -1;
    private int swappedInventorySlot = -1;
    private boolean hasSwitchedItem = false;

    // Flowers for Bees
    private static final List<Item> BEE_FLOWERS = Arrays.asList(
            Items.DANDELION, Items.POPPY, Items.BLUE_ORCHID, Items.ALLIUM, Items.AZURE_BLUET,
            Items.RED_TULIP, Items.ORANGE_TULIP, Items.WHITE_TULIP, Items.PINK_TULIP,
            Items.OXEYE_DAISY, Items.CORNFLOWER, Items.LILY_OF_THE_VALLEY, Items.WITHER_ROSE,
            Items.SUNFLOWER, Items.LILAC, Items.ROSE_BUSH, Items.PEONY, Items.TORCHFLOWER
    );

    private final BooleanSetting breedAllays = new BooleanSetting("Allays", true);

    private static final Map<Class<? extends Animal>, List<Item>> BREEDING_ITEMS = new HashMap<>();
    static {
        BREEDING_ITEMS.put(Cow.class,       Collections.singletonList(Items.WHEAT));
        BREEDING_ITEMS.put(Sheep.class,     Collections.singletonList(Items.WHEAT));
        BREEDING_ITEMS.put(Pig.class,       Arrays.asList(Items.CARROT, Items.POTATO, Items.BEETROOT));
        BREEDING_ITEMS.put(Chicken.class,   Arrays.asList(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS,
                Items.PUMPKIN_SEEDS, Items.MELON_SEEDS));

        BREEDING_ITEMS.put(Wolf.class, Arrays.asList(
                Items.BEEF, Items.CHICKEN, Items.PORKCHOP, Items.MUTTON, Items.RABBIT,
                Items.ROTTEN_FLESH, Items.COOKED_BEEF, Items.COOKED_CHICKEN,
                Items.COOKED_PORKCHOP, Items.COOKED_MUTTON, Items.COOKED_RABBIT
        ));

        BREEDING_ITEMS.put(Cat.class, Arrays.asList(Items.COD, Items.SALMON));
        BREEDING_ITEMS.put(Fox.class, Arrays.asList(Items.SWEET_BERRIES, Items.GLOW_BERRIES));
        BREEDING_ITEMS.put(Panda.class, Collections.singletonList(Items.BAMBOO));
        BREEDING_ITEMS.put(Turtle.class, Collections.singletonList(Items.SEAGRASS));
        BREEDING_ITEMS.put(Bee.class, BEE_FLOWERS);
        BREEDING_ITEMS.put(Frog.class, Collections.singletonList(Items.SLIME_BALL));
        BREEDING_ITEMS.put(Goat.class, Collections.singletonList(Items.WHEAT));
        BREEDING_ITEMS.put(Hoglin.class, Collections.singletonList(Items.CRIMSON_FUNGUS));
        BREEDING_ITEMS.put(Strider.class, Collections.singletonList(Items.WARPED_FUNGUS));
        BREEDING_ITEMS.put(MushroomCow.class, Collections.singletonList(Items.WHEAT));
        BREEDING_ITEMS.put(Rabbit.class, Arrays.asList(Items.CARROT, Items.GOLDEN_CARROT, Items.DANDELION));
        BREEDING_ITEMS.put(Sniffer.class, Collections.singletonList(Items.TORCHFLOWER_SEEDS));
        BREEDING_ITEMS.put(Camel.class, Collections.singletonList(Items.CACTUS));
        BREEDING_ITEMS.put(Axolotl.class, Collections.singletonList(Items.TROPICAL_FISH_BUCKET));

    }

    // For taming
    private static final Map<Class<? extends Animal>, List<Item>> TAME_ITEMS = new HashMap<>();
    static {
        TAME_ITEMS.put(Wolf.class, Collections.singletonList(Items.BONE));
        TAME_ITEMS.put(Cat.class, Arrays.asList(Items.COD, Items.SALMON));
    }

    public AutoBreedModule() {
        super("AutoBreed", "Automatically tames and feeds animals (babies instantly)", ModuleCategory.MISC);

        BooleanSetting generalSettings = new BooleanSetting("General Settings", true);
        generalSettings.addSubSettings(breedRadius, prioritizePairs, followMode);

        BooleanSetting allaySettings = new BooleanSetting("Allay Settings", true);
        allaySettings.addSubSettings(
                breedAllays,
                shardCountRequired
        );

        BooleanSetting mobSettings = new BooleanSetting("Mobs", true);
        mobSettings.addSubSettings(
                feedBabies,
                breedCows, breedSheep, breedPigs, breedChickens, breedWolves,
                breedCats, breedFoxes, breedPandas, breedTurtles, breedBees,
                breedFrogs, breedGoats, breedHoglins, breedStriders,
                breedMooshrooms, breedRabbits, breedSniffers, breedCamels, breedAxolotls
        );

        BooleanSetting tamingSettings = new BooleanSetting("Taming", true);
        tamingSettings.addSubSettings(autoTameWolves, autoTameCats);

        this.registerSettings(generalSettings, allaySettings, mobSettings, tamingSettings);
    }


    @Override
    public void onEnable() {
        RusherHackAPI.getEventBus().subscribe(this);
        ChatUtils.print("AutoBreed enabled!");
        fedAnimals.clear();
        interactionTimestamps.clear();
        veryOriginalSlot = -1;
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

        List<Entity> nearbyEntities = mc.level.getEntities(
                mc.player,
                mc.player.getBoundingBox().inflate(breedRadius.getValue()),
                e -> e instanceof Animal || e instanceof Allay
        );

        List<Animal> allAnimals = new ArrayList<>();
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Allay) {
                if (tryDuplicateAllay(entity)) continue;
            }

            if (entity instanceof Animal animal) {
                allAnimals.add(animal);
            }
        }
        if (allAnimals.isEmpty()) {
            revertSlot();
            return;
        }

        // 1) Taming
        handleTaming(allAnimals);

        // 2) Follow Mode => just hold item, skip breeding
        if (followMode.getValue()) {
            handleFollowMode(allAnimals);
            return;
        }

        // 3) Normal Breed Logic
        // We'll keep "adults" separate from "babies" because we do something special for babies.
        List<Animal> adults = new ArrayList<>();
        List<Animal> babies = new ArrayList<>();

        for (Animal animal : allAnimals) {
            if (!shouldBreed(animal)) continue;
            if (animal.isBaby()) {
                babies.add(animal);
            } else {
                adults.add(animal);
            }
        }

        // Babies => feed instantly & repeatedly if feedBabies is on
        for (Animal baby : babies) {
            feedBabyContinuously(baby);
        }

        // Adults => feed normally
        if (prioritizePairs.getValue()) {
            feedAdultsInPairs(adults);
        } else {
            for (Animal adult : adults) {
                // 1-sec cooldown for adults only
                if (!checkCooldown(adult)) continue;
                feedSingleAnimal(adult);
            }
        }

        revertSlot();
    }

    // ========= BABY FEEDING =========
    /**
     * No cooldown. We'll feed the baby up to 20 times in one tick, or until we run out of items.
     */
    private void feedBabyContinuously(Animal baby) {
        if (!feedBabies.getValue()) return;

        // For other babies: feed repeatedly
        for (int i = 0; i < 20; i++) {
            if (!tryFeedAnimal(baby)) break;
        }
    }

    // ========= TAMING (Wolves & Cats) =========
    private void handleTaming(List<Animal> animals) {
        for (Animal animal : animals) {
            if (!shouldTame(animal)) continue;
            // If already tamed => skip
            if (animal instanceof TamableAnimal t && t.isTame()) continue;

            tameStandard(animal);
        }
    }

    private boolean shouldTame(Animal animal) {
        if (animal instanceof Wolf) return autoTameWolves.getValue();
        if (animal instanceof Cat)  return autoTameCats.getValue();
        return false;
    }

    private void tameStandard(Animal animal) {
        UUID id = animal.getUUID();
        // 500ms spam check
        if (interactionTimestamps.containsKey(id)) {
            long last = interactionTimestamps.get(id);
            if (System.currentTimeMillis() - last < 500) return;
        }

        // Attempt multiple feeds
        List<Item> items = TAME_ITEMS.get(animal.getClass());
        if (items != null) {
            feedMobMultipleTimes(animal, items);
        }
        interactionTimestamps.put(id, System.currentTimeMillis());
    }

    private void feedMobMultipleTimes(Animal animal, List<Item> possibleFoods) {
        int feedsDone = 0;
        for (Item item : possibleFoods) {
            while (feedsDone < 5) {
                int slot = InventoryUtils.findItem(item, true, false);
                if (slot < 0) break;
                switchToItem(slot);

                if (mc.player != null) {
                    mc.player.connection.send(
                            ServerboundInteractPacket.createInteractionPacket(animal, false, InteractionHand.MAIN_HAND)
                    );
                }
                feedsDone++;
            }
            if (feedsDone >= 5) break;
        }
    }

    // ========= BREEDING ADULTS =========
    private void feedAdultsInPairs(List<Animal> adults) {
        List<Animal> toFeed = new ArrayList<>(adults);
        toFeed.sort(Comparator.comparing(a -> a.getClass().getName()));

        List<Animal> processed = new ArrayList<>();
        for (int i = 0; i < toFeed.size(); i++) {
            Animal a1 = toFeed.get(i);
            if (processed.contains(a1)) continue;

            Animal partner = null;
            for (int j = i + 1; j < toFeed.size(); j++) {
                Animal a2 = toFeed.get(j);
                if (!processed.contains(a2) && a2.getClass() == a1.getClass() && !fedAnimals.contains(a2.getUUID())) {
                    partner = a2;
                    break;
                }
            }

            if (partner != null) {
                feedPair(a1, partner);
                processed.add(a1);
                processed.add(partner);
            }
        }
    }

    private void feedPair(Animal a1, Animal a2) {
        if (tryFeedAnimal(a1)) {
            // 1s cooldown
            interactionTimestamps.put(a1.getUUID(), System.currentTimeMillis());
            fedAnimals.add(a1.getUUID());
        }
        if (tryFeedAnimal(a2)) {
            interactionTimestamps.put(a2.getUUID(), System.currentTimeMillis());
            fedAnimals.add(a2.getUUID());
        }
    }

    private void feedSingleAnimal(Animal animal) {
        if (!tryFeedAnimal(animal)) return;
        interactionTimestamps.put(animal.getUUID(), System.currentTimeMillis());
        fedAnimals.add(animal.getUUID());
    }

    /**
     * Attempt feeding once with the first matching breeding item found.
     * If feeding an Axolotl, ensure it's released after being picked up.
     * If feeding an Allay, track giving and receiving of the shard.
     * Returns true if the animal was successfully fed.
     */
    private boolean tryFeedAnimal(Animal animal) {
        List<Item> items = BREEDING_ITEMS.get(animal.getClass());
        if (items == null || items.isEmpty()) {
            return false;
        }

        for (Item food : items) {
            int slot = InventoryUtils.findItem(food, true, false);
            if (slot >= 0) {
                switchToItem(slot);

                try {
                    if (mc.player != null) {
                        mc.player.connection.send(
                                ServerboundInteractPacket.createInteractionPacket(animal, false, InteractionHand.MAIN_HAND)
                        );
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                } catch (Exception e) {
                    ChatUtils.print("Error feeding " + animal.getClass().getSimpleName() + ": " + e.getMessage());
                    return false;
                }

                if (animal instanceof Axolotl) {
                    handleAxolotlRelease();
                }

                return true;
            }
        }

        return false;
    }

    private static final long ALLAY_COOLDOWN_MS = 150_000L;
    private final Map<UUID, Long> allayDuplicationCooldowns = new HashMap<>();

    private boolean tryDuplicateAllay(Entity entity) {
        if (!(entity instanceof Allay allay)) return false;
        if (!allay.isDancing()) return false;
        if (!breedAllays.getValue()) return false;

        long now = System.currentTimeMillis();
        Long last = allayDuplicationCooldowns.get(allay.getUUID());
        if (last != null && now - last < ALLAY_COOLDOWN_MS) return false;

        // Need shards
        if (InventoryUtils.getItemCount(Items.AMETHYST_SHARD, false, true) < shardCountRequired.getValue())
            return false;

        // Find the shard slot (0–35, excluding crafting slots)
        int shardSlot = InventoryUtils.findItem(Items.AMETHYST_SHARD, true, false);
        if (shardSlot < 0) return false;

        // Switch to the amethyst shard using existing slot-switching logic
        switchToItem(shardSlot);

        // Verify the main hand has the shard
        if (mc.player == null || !mc.player.getMainHandItem().is(Items.AMETHYST_SHARD)) {
            revertSlot(); // Revert if the switch failed
            return false;
        }

        // Interact with the Allay to give the shard
        mc.player.connection.send(
                ServerboundInteractPacket.createInteractionPacket(allay, false, InteractionHand.MAIN_HAND)
        );
        mc.player.swing(InteractionHand.MAIN_HAND);

        // Revert to the original item
        revertSlot();

        // Mark the Allay as duplicated and set cooldown
        allayDuplicationCooldowns.put(allay.getUUID(), now);
        return true;
    }


    /**
     * Releases an Axolotl from a bucket and puts the empty bucket back into the original slot.
     */
    private void handleAxolotlRelease() {
        int bucketSlot = InventoryUtils.findItem(Items.AXOLOTL_BUCKET, true, false);
        if (bucketSlot >= 0) {
            switchToItem(bucketSlot);

            // Use the bucket (right-click to release Axolotl)
            if (mc.gameMode != null) {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
            if (mc.player != null) {
                mc.player.swing(InteractionHand.MAIN_HAND);
            }

            // Wait a tick for the item to update in inventory
            mc.execute(() -> {
                int emptyBucketSlot = InventoryUtils.findItem(Items.BUCKET, true, false);
                if (emptyBucketSlot >= 0) {
                    // Switch back to the empty bucket to put it in the slot
                    switchToItem(emptyBucketSlot);
                }
            });
        }
    }

    /**
     * Only applies a 1-second cooldown to adults (since we want babies fed instantly).
     * If they’re not a baby, we do the usual 1s spam check.
     */
    private boolean checkCooldown(Animal animal) {
        if (!animal.isBaby()) {
            Long last = interactionTimestamps.get(animal.getUUID());
            return last == null || (System.currentTimeMillis() - last) >= 1000;
        }
        return true;
    }

    private boolean shouldBreed(Animal animal) {
        if (fedAnimals.contains(animal.getUUID())) return false;
        if (animal.isInLove() || !animal.canFallInLove()) return false;

        // Skip tamed animals not owned
        if (animal instanceof TamableAnimal tamed) {
            if (!tamed.isTame() || !tamed.isOwnedBy(mc.player)) {
                return false;
            }
        }

        // Keep baby turtles in the breeding logic
        if (animal.isBaby()) {
            return feedBabies.getValue() && isBreedingEnabled(animal);
        }

        return isBreedingEnabled(animal);
    }

    private final Map<Class<? extends Entity>, BooleanSetting> breedingSettings = new HashMap<>();
    {
        breedingSettings.put(Allay.class, breedAllays);
        breedingSettings.put(Cow.class, breedCows);
        breedingSettings.put(Sheep.class, breedSheep);
        breedingSettings.put(Pig.class, breedPigs);
        breedingSettings.put(Chicken.class, breedChickens);
        breedingSettings.put(Wolf.class, breedWolves);
        breedingSettings.put(Cat.class, breedCats);
        breedingSettings.put(Fox.class, breedFoxes);
        breedingSettings.put(Panda.class, breedPandas);
        breedingSettings.put(Turtle.class, breedTurtles);
        breedingSettings.put(Bee.class, breedBees);
        breedingSettings.put(Frog.class, breedFrogs);
        breedingSettings.put(Goat.class, breedGoats);
        breedingSettings.put(Hoglin.class, breedHoglins);
        breedingSettings.put(Strider.class, breedStriders);
        breedingSettings.put(MushroomCow.class, breedMooshrooms);
        breedingSettings.put(Rabbit.class, breedRabbits);
        breedingSettings.put(Sniffer.class, breedSniffers);
        breedingSettings.put(Camel.class, breedCamels);
        breedingSettings.put(Axolotl.class, breedAxolotls);
    }

    private boolean isBreedingEnabled(Entity entity) {
        BooleanSetting setting = breedingSettings.get(entity.getClass());
        return setting != null && setting.getValue();
    }

    // ========== FOLLOW MODE (just hold the lure item) ==========
    private void handleFollowMode(List<Animal> animals) {
        Animal lurable = null;
        for (Animal a : animals) {
            List<Item> items = BREEDING_ITEMS.get(a.getClass());
            if (items != null && !items.isEmpty()) {
                lurable = a;
                break;
            }
        }
        if (lurable != null) {
            for (Item lure : BREEDING_ITEMS.get(lurable.getClass())) {
                int slot = InventoryUtils.findItem(lure, true, false);
                if (slot >= 0) {
                    switchToItem(slot);
                    break;
                }
            }
        } else {
            revertSlot();
        }
    }

    public Map<UUID, Long> getInteractionTimestamps() {
        return this.interactionTimestamps;
    }

    public Map<UUID, Long> getAllayCooldowns() {
        return this.allayDuplicationCooldowns;
    }

    // ========== SLOT SWITCHING ==========
    private void switchToItem(int slot) {
        if (veryOriginalSlot == -1) {
            if (mc.player != null) {
                veryOriginalSlot = mc.player.getInventory().selected;
            }
        }
        if (previousHotbarSlot == -1) {
            if (mc.player != null) {
                previousHotbarSlot = mc.player.getInventory().selected;
            }
        }

        if (slot < 9) {
            // switch directly if it’s hotbar
            if (mc.player != null) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            }
        } else if (slot <= 35) {
            // from main inventory to hotbar
            if (previousHotbarSlot != -1) {
                InventoryUtils.swapSlots(slot, previousHotbarSlot);
                if (mc.player != null) {
                    mc.player.connection.send(new ServerboundSetCarriedItemPacket(previousHotbarSlot));
                }
                swappedInventorySlot = slot;
            }
        }
        hasSwitchedItem = true;
    }

    private void revertSlot() {
        if (!hasSwitchedItem) return;

        if (swappedInventorySlot != -1) {
            InventoryUtils.swapSlots(swappedInventorySlot, previousHotbarSlot);
            swappedInventorySlot = -1;
        }

        if (veryOriginalSlot >= 0 && veryOriginalSlot <= 8) {
            if (mc.player != null) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(veryOriginalSlot));
            }
            if (mc.player != null) {
                mc.player.getInventory().selected = veryOriginalSlot;
            }
        }

        hasSwitchedItem = false;
        previousHotbarSlot = -1;
        veryOriginalSlot   = -1;
    }
}