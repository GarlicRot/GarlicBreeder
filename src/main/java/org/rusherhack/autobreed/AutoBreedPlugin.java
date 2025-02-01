package org.rusherhack.autobreed;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;
import org.rusherhack.core.event.IEventBus;
import org.rusherhack.autobreed.modules.AutoBreedModule;

public class AutoBreedPlugin extends Plugin {
    private static final Logger LOGGER = LogManager.getLogger("AutoBreedPlugin");

    private final AutoBreedModule autoBreedModule = new AutoBreedModule();

    @Override
    public void onLoad() {
        LOGGER.info("Loading AutoBreed Plugin...");

        // Register the AutoBreedModule
        RusherHackAPI.getModuleManager().registerFeature(autoBreedModule);

        // Subscribe the module to the event bus
        IEventBus eventBus = RusherHackAPI.getEventBus();
        eventBus.subscribe(autoBreedModule);

        LOGGER.info("AutoBreed Plugin loaded successfully.");
    }

    @Override
    public void onUnload() {
        LOGGER.info("Unloading AutoBreed Plugin...");

        // Unsubscribe the module from the event bus
        IEventBus eventBus = RusherHackAPI.getEventBus();
        eventBus.unsubscribe(autoBreedModule);

        LOGGER.info("AutoBreed Plugin unloaded.");
    }
}
