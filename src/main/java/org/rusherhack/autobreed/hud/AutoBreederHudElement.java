package org.rusherhack.autobreed.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.Animal;
import org.rusherhack.client.api.feature.hud.HudElement;
import org.rusherhack.client.api.render.RenderContext;
import org.rusherhack.client.api.render.font.IFontRenderer;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.EnumSetting;
import org.rusherhack.autobreed.modules.AutoBreedModule;

import java.util.Map;
import java.util.UUID;

/**
 * HUD element for showing the breeding cooldown of the targeted animal.
 */
public class AutoBreederHudElement extends HudElement {

    private final Minecraft mc = Minecraft.getInstance();
    private final AutoBreedModule breedModule;

    // Fixed breeding cooldown (5 minutes)
    private static final long BREED_COOLDOWN_MS = 5 * 60 * 1000L;

    // Enum for cooldown display format
    private enum TimeFormat { SECONDS, MINUTES, BOTH }
    private final EnumSetting<TimeFormat> timeFormatSetting = new EnumSetting<>("Cooldown Format", TimeFormat.SECONDS);

    // Settings for message display
    private final BooleanSetting showAnimalName = new BooleanSetting("Show Animal Name", true);
    private final BooleanSetting showNoCooldownMessage = new BooleanSetting("Show No Cooldown Message", true);
    private final BooleanSetting showNotTargetingMessage = new BooleanSetting("Show Not Targeting Message", true);

    /**
     * Constructor for the AutoBreederHudElement.
     *
     * @param moduleRef Reference to the AutoBreedModule.
     */
    public AutoBreederHudElement(AutoBreedModule moduleRef) {
        super("AutoBreederHudElement");
        this.breedModule = moduleRef;

        // Set the description for the HUD element
        this.setDescription("Displays the breeding cooldown for targeted animals.");

        // Register settings
        this.registerSettings(timeFormatSetting, showAnimalName, showNoCooldownMessage, showNotTargetingMessage);
    }

    @Override
    public void renderContent(RenderContext context, double mouseX, double mouseY) {
        IFontRenderer font = this.getFontRenderer();
        font.begin();

        if (mc.crosshairPickEntity instanceof Animal target) {
            Map<UUID, Long> timestamps = breedModule.getInteractionTimestamps();
            if (timestamps != null) {
                Long lastFed = timestamps.get(target.getUUID());
                if (lastFed != null) {
                    long elapsed = System.currentTimeMillis() - lastFed;
                    long timeLeft = BREED_COOLDOWN_MS - elapsed;

                    if (timeLeft > 0) {
                        String timeText = formatCooldown(timeLeft);
                        String name = target.getName().getString();
                        String text = showAnimalName.getValue()
                                ? String.format("%s: %s", name, timeText)
                                : timeText;

                        font.drawString(text, 0.0F, 0.0F, 0xFFFFFF, false);
                        font.end();
                        return;
                    }
                }
            }

            // If no cooldown is active
            if (showNoCooldownMessage.getValue()) {
                String noCooldownText = showAnimalName.getValue()
                        ? "No cooldown for " + target.getName().getString()
                        : "No active cooldown";
                font.drawString(noCooldownText, 0.0F, 0.0F, 0xAAAAAA, false);
            }
        } else {
            // Not targeting an animal
            if (showNotTargetingMessage.getValue()) {
                font.drawString("Not targeting a breedable mob", 0.0F, 0.0F, 0xAAAAAA, false);
            }
        }

        font.end();
    }

    /**
     * Converts cooldown time into selected format (Seconds, Minutes, or Both).
     *
     * @param timeLeft Time left in milliseconds
     * @return Formatted string based on selected format
     */
    private String formatCooldown(long timeLeft) {
        TimeFormat format = timeFormatSetting.getValue();
        long seconds = timeLeft / 1000;
        long minutes = seconds / 60;
        seconds %= 60;

        return switch (format) {
            case SECONDS -> String.format("%.1f s", timeLeft / 1000.0);
            case MINUTES -> String.format("%d min %d sec", minutes, seconds);
            case BOTH    -> String.format("%d min %d sec (%.1f s)", minutes, seconds, timeLeft / 1000.0);
        };
    }

    @Override
    public double getWidth() {
        IFontRenderer font = this.getFontRenderer();
        return font.getStringWidth("No cooldown for a breedable mob");
    }

    @Override
    public double getHeight() {
        return this.getFontRenderer().getFontHeight();
    }
}
