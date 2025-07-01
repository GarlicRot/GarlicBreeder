package org.rusherhack.autobreed.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.allay.Allay;
import org.rusherhack.client.api.feature.hud.HudElement;
import org.rusherhack.client.api.render.RenderContext;
import org.rusherhack.client.api.render.font.IFontRenderer;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.EnumSetting;
import org.rusherhack.autobreed.modules.AutoBreedModule;

import java.util.Map;
import java.util.UUID;

/**
 * HUD element for showing the breeding cooldown of the targeted animal or Allay.
 */
public class AutoBreederHudElement extends HudElement {

    private final Minecraft mc = Minecraft.getInstance();
    private final AutoBreedModule breedModule;

    // Fixed breeding cooldown (5 minutes for animals, 2.5 minutes for Allay)
    private static final long BREED_COOLDOWN_MS = 5 * 60 * 1000L;
    private static final long ALLAY_COOLDOWN_MS = 150_000L;

    private enum TimeFormat { SECONDS, MINUTES, BOTH }
    private final EnumSetting<TimeFormat> timeFormatSetting = new EnumSetting<>("Cooldown Format", TimeFormat.SECONDS);

    private final BooleanSetting showAnimalName = new BooleanSetting("Show Animal Name", true);
    private final BooleanSetting showNoCooldownMessage = new BooleanSetting("Show No Cooldown Message", true);
    private final BooleanSetting showNotTargetingMessage = new BooleanSetting("Show Not Targeting Message", true);
    private final BooleanSetting showAllayCooldown = new BooleanSetting("Show Allay Cooldown", true);

    public AutoBreederHudElement(AutoBreedModule moduleRef) {
        super("AutoBreederHudElement");
        this.breedModule = moduleRef;
        this.setDescription("Displays the breeding cooldown for targeted animals or Allays.");
        this.registerSettings(timeFormatSetting, showAnimalName, showNoCooldownMessage, showNotTargetingMessage, showAllayCooldown);
    }

    @Override
    public void renderContent(RenderContext context, double mouseX, double mouseY) {
        IFontRenderer font = this.getFontRenderer();
        font.begin();

        Entity target = mc.crosshairPickEntity;

        if (target instanceof Allay allay && allay.isDancing() && showAllayCooldown.getValue()) {
            Map<UUID, Long> cooldowns = breedModule.getAllayCooldowns();
            Long lastDuped = cooldowns.get(allay.getUUID());

            if (lastDuped != null) {
                long elapsed = System.currentTimeMillis() - lastDuped;
                long timeLeft = ALLAY_COOLDOWN_MS - elapsed;

                if (timeLeft > 0) {
                    String timeText = formatCooldown(timeLeft);
                    String name = allay.getName().getString();
                    String text = showAnimalName.getValue()
                            ? String.format("%s: %s", name, timeText)
                            : timeText;

                    font.drawString(text, 0.0F, 0.0F, 0x55FFFF, false);
                    font.end();
                    return;
                }
            }

            if (showNoCooldownMessage.getValue()) {
                font.drawString("Allay is ready to duplicate", 0.0F, 0.0F, 0x55FF55, false);
            }

        } else if (target instanceof Animal animal) {
            Map<UUID, Long> timestamps = breedModule.getInteractionTimestamps();
            Long lastFed = timestamps.get(animal.getUUID());

            if (lastFed != null) {
                long elapsed = System.currentTimeMillis() - lastFed;
                long timeLeft = BREED_COOLDOWN_MS - elapsed;

                if (timeLeft > 0) {
                    String timeText = formatCooldown(timeLeft);
                    String name = animal.getName().getString();
                    String text = showAnimalName.getValue()
                            ? String.format("%s: %s", name, timeText)
                            : timeText;

                    font.drawString(text, 0.0F, 0.0F, 0xFFFFFF, false);
                    font.end();
                    return;
                }
            }

            if (showNoCooldownMessage.getValue()) {
                String noCooldownText = showAnimalName.getValue()
                        ? "No cooldown for " + animal.getName().getString()
                        : "No active cooldown";
                font.drawString(noCooldownText, 0.0F, 0.0F, 0xAAAAAA, false);
            }

        } else if (showNotTargetingMessage.getValue()) {
            font.drawString("Not targeting a breedable mob", 0.0F, 0.0F, 0xAAAAAA, false);
        }
        font.end();
    }

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
        return this.getFontRenderer().getStringWidth("Allay is ready to duplicate");
    }

    @Override
    public double getHeight() {
        return this.getFontRenderer().getFontHeight();
    }
}
