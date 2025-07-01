# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/), and this project adheres to [Semantic Versioning](https://semver.org/).

---

## [1.0.5] - 2025-06-30  
[1.0.5]: https://github.com/GarlicRot/GarlicBreeder/releases/tag/v1.0.5  

### Allay Duplication Support  
- 🧚 Added support for duplicating **Allays** using Amethyst Shards while dancing near a Jukebox.  
- Introduced internal **cooldown tracking** for Allay duplication (2.5 minutes).  
- Ensured shard is forcefully moved to the correct slot for interaction.  
- Prevented unnecessary arm swinging during interaction.  
- Added new HUD setting: **Show Allay Cooldown**.  
- Updated HUD element to show Allay cooldowns and duplication readiness.  
- Cleaned up spammy chat messages related to failed interactions.  

> ℹ️ **Note**: The Jukebox must be playing music for the Allay to be eligible for duplication.

---

## [1.0.4] - 2025-06-29  
[1.0.4]: https://github.com/GarlicRot/GarlicBreeder/releases/tag/v1.0.4  

### Bug Fix – Turtle Crash  
- Fixed crash when feeding baby **Turtles** with "Feed Babies" enabled.  
- Added proper checks to avoid feeding items to entities that don't grow the same way as other animals.  

---

## [1.0.3] - 2025-06-24  
[1.0.3]: https://github.com/GarlicRot/GarlicBreeder/releases/tag/v1.0.3  

### Improved Feedback and Arm Swing Behavior  
- Improved visual feedback messages when attempting to breed mobs.  
- Prevented unnecessary **arm swings** when interaction packets are sent, to avoid visual clutter.  
- Added message when mobs are not breedable due to missing partner or cooldown.  

---

## [1.0.2] - 2025-06-21  
[1.0.2]: https://github.com/GarlicRot/GarlicBreeder/releases/tag/v1.0.2  

### UX and Code Enhancements  
- Optimized cooldown tracking logic internally.  
- Refactored how cooldown timestamps are stored and accessed.  
- Updated HUD rendering to better handle entities with no active cooldown.  
- Improved support for HUD message toggles and formatting options.

---

## [1.0.1] - 2025-06-12  
[1.0.1]: https://github.com/GarlicRot/GarlicBreeder/releases/tag/v1.0.1  

### Compatibility Update  
- Added `2b` branch support for:
  - **Minecraft 1.21.4**
  - **Java 21**
- No changes were made to the plugin logic.
- Artifact: `GarlicBreeder-1.21.4.jar`

---

## [1.0.0] - 2025-02-02  
[1.0.0]: https://github.com/GarlicRot/GarlicBreeder/releases/tag/v1.0.0  

### Initial Release  

### **AutoBreed Module**  
- **Automatic Breeding**:  
  - Detects nearby breedable mobs and switches to the correct food type.  
  - Breeds mobs automatically when they can fall in love.  
  - Supports **all** breedable mobs, including:
    - Cows, Sheep, Pigs, Chickens, Wolves, Cats, Foxes, Pandas, Turtles, Bees, Frogs, Goats, Hoglins, Striders, Mooshrooms, Rabbits, Sniffers, Camels, Axolotls.  

- **Follow Mode**:  
  - Holds the correct food item when near a mob, making it easier to lead animals.  
  - **Disables breeding** while enabled.  

- **Prioritize Pairs**:  
  - Ensures mobs find a proper partner before interacting.  

- **Feed Babies**:  
  - Optionally allows feeding baby mobs for faster growth.  

- **Taming Support**:  
  - Automatically tames Wolves and Cats if enabled.  

- **Breeding Radius Setting**:  
  - Adjustable detection range for finding breedable mobs.  

### **AutoBreeder HUD Element**  
- **Displays Breeding Cooldown**:  
  - Shows time left before an animal can breed again.  
  - Supports **multiple time formats**: Seconds, Minutes, or Both.  

- **Customizable Display Options**:  
  - Show/hide animal names, cooldown messages, and non-targeting messages.  

---

This marks the **first release** of GarlicBreeder! Enjoy automatic breeding and taming in Minecraft!
