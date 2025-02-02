package org.rusherhack.autobreed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;
import org.rusherhack.client.api.system.IHudManager;
import org.rusherhack.autobreed.hud.AutoBreederHudElement;
import org.rusherhack.autobreed.modules.AutoBreedModule;

/**
 * Main plugin class for the AutoBreed mod.
 * Responsible for initializing and registering the module and HUD element.
 */
public class AutoBreedPlugin extends Plugin {
    private static final Logger LOGGER = LogManager.getLogger("AutoBreedPlugin");

    // Manager for handling HUD elements in the RusherHacks framework
    private final IHudManager hudManager = RusherHackAPI.getHudManager();

    // The module and HUD element for AutoBreed
    private final AutoBreedModule autoBreedModule = new AutoBreedModule();
    private final AutoBreederHudElement autoBreederHudElement = new AutoBreederHudElement(autoBreedModule);


    /**
     * Called when the plugin is loaded. Registers the module and HUD element.
     */
    @Override
    public void onLoad() {
        LOGGER.info("Loading AutoBreed Plugin...");

        // Register the AutoBreed module
        RusherHackAPI.getModuleManager().registerFeature(autoBreedModule);

        // Register the AutoBreeder HUD element
        this.hudManager.registerFeature(autoBreederHudElement);

        LOGGER.info("AutoBreed Plugin loaded successfully.");
    }

    /**
     * Called when the plugin is unloaded. Deregisters the module and HUD element.
     */
    @Override
    public void onUnload() {
        LOGGER.info("Unloading AutoBreed Plugin...");

        // Deregister the AutoBreeder HUD element
        this.autoBreederHudElement.setToggled(false);

        LOGGER.info("AutoBreed Plugin unloaded.");
    }
}
