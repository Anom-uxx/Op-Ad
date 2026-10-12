package org.anomuxx.core.hud;

import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.texture.ThemeTextureLocation;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.gfx.pipeline.renderer.text.TextRenderingOptions;
import net.labymod.api.client.gui.hud.binding.category.HudWidgetCategory;
import net.labymod.api.client.gui.hud.hudwidget.ResizeableHudWidget;
import net.labymod.api.client.gui.mouse.MutableMouse;
import net.labymod.api.client.gui.screen.ScreenContext;
import net.labymod.api.client.gui.screen.key.MouseButton;
import net.labymod.api.client.render.font.RenderableComponent;
import org.anomuxx.core.AdSlot;
import org.anomuxx.core.AdAddon;
import org.anomuxx.core.listener.ChatListener;

/**
 * Skalierbares HUD-Widget pro Werbung.
 * Ecken: beide Seiten ändern sich → dynamisch proportional skalieren.
 * Ränder: eine Seite → Größe folgt dieser Seite (Ring bleibt rund).
 *
 * <p>Hinweis zu LabyMod Guideline 1.1: RenderableComponents werden gecacht und nur
 * neu erzeugt, wenn sich Anzeigetext oder Restzeit-String ändert – nicht jedes Frame.
 */
public class AdHudWidget extends ResizeableHudWidget<AdHudWidgetConfig> {

  private static final float RING_THICKNESS_RATIO = 0.12F;
  private static final float ANGLE_TOP = -90.0F;

  private final AdAddon addon;
  private final int index;
  private final ChatListener chatListener;

  /** Cache, damit nicht jedes Frame neue Text-Objekte entstehen (Guideline 1.1). */
  private String cachedTitleKey = "";
  private String cachedTimeKey = "";
  private RenderableComponent cachedNameRenderable;
  private RenderableComponent cachedTimeRenderable;

  public AdHudWidget(AdAddon addon, ChatListener chatListener, int index) {
    super("ad" + index, AdHudWidgetConfig.class);
    this.addon = addon;
    this.chatListener = chatListener;
    this.index = index;
    this.bindCategory(HudWidgetCategory.MISCELLANEOUS);
    // Icon vor dem HUD-Widget in der Widget-Liste
    ThemeTextureLocation texture = ThemeTextureLocation.of("opwerbung", "icon", 128, 128);
    this.setIcon(Icon.sprite(texture, 0, 0, 128));
  }

  /**
   * Ecken-Zug: width und height ändern sich gemeinsam → Mittelwert (proportionale Skalierung).
   * Rand-Zug: nur eine Seite ändert sich → diese Seite bestimmt die Quadrat-Größe.
   */
  @Override
  public void setSize(float width, float height) {
    float prevW = this.getWidth() > 0.0F ? this.getWidth() : width;
    float prevH = this.getHeight() > 0.0F ? this.getHeight() : height;

    boolean widthChanged = Math.abs(width - prevW) > 0.5F;
    boolean heightChanged = Math.abs(height - prevH) > 0.5F;

    float size;
    if (widthChanged && heightChanged) {
      size = (width + height) / 2.0F;
    } else if (widthChanged) {
      size = width;
    } else if (heightChanged) {
      size = height;
    } else {
      size = Math.min(width, height);
    }

    AdHudWidgetConfig config = this.getConfig();
    if (config != null) {
      float min = Math.min(config.getMinWidth(), config.getMinHeight());
      float max = Math.min(config.getMaxWidth(), config.getMaxHeight());
      if (size < min) {
        size = min;
      } else if (size > max) {
        size = max;
      }
      config.width().set(size);
      config.height().set(size);
    }

    super.setSize(size, size);
  }

  @Override
  public boolean isVisibleInGame() {
    if (!Boolean.TRUE.equals(this.addon.configuration().enabled().get())) {
      return false;
    }
    AdSlot slot = this.getSlot();
    if (slot == null) {
      return false;
    }
    if (!Boolean.TRUE.equals(slot.enabled().get())) {
      return false;
    }
    String message = slot.message().get();
    return message != null && !message.isBlank();
  }

  @Override
  public void render(ScreenContext context, boolean isEditorContext, float width, float height) {
    AdSlot slot = this.getSlot();
    if (slot == null && !isEditorContext) {
      return;
    }

    float size = Math.min(width, height);
    float centerX = width / 2.0F;
    float centerY = height / 2.0F;
    float outer = size * 0.42F;
    float thickness = Math.max(1.0F, outer * RING_THICKNESS_RATIO * 2.5F);
    float inner = Math.max(0.5F, outer - thickness);

    long cooldownMillis = this.getCooldownMillis();
    long lastSent = this.addon.timeconfig().getTime(this.index);
    long elapsed = lastSent <= 0L ? cooldownMillis : System.currentTimeMillis() - lastSent;
    long remaining = Math.max(0L, cooldownMillis - elapsed);
    boolean ready = remaining <= 0L;

    float remainingRatio = cooldownMillis <= 0
        ? 0.0F
        : Math.min(1.0F, Math.max(0.0F, (float) remaining / (float) cooldownMillis));

    int ringColor;
    if (ready) {
      ringColor = 0xFF22C55E;
    } else if (remaining <= 5L * 60_000L) {
      ringColor = 0xFFEAB308;
    } else {
      ringColor = 0xFFEF4444;
    }

    context.canvas().submitCircle(
        centerX, centerY, inner, outer, ANGLE_TOP, ANGLE_TOP + 360.0F, 0xFF3F3F46
    );

    if (remainingRatio > 0.001F) {
      float sweep = remainingRatio * 360.0F;
      context.canvas().submitCircle(
          centerX, centerY, inner, outer, ANGLE_TOP, ANGLE_TOP + sweep, ringColor
      );
    }

    String title;
    if (slot != null) {
      title = slot.title().get();
      if (title == null || title.isBlank()) {
        title = null;
      }
    } else {
      title = null;
    }

    String titleKey = title != null ? title : ("#" + (this.index + 1));
    String timeKey = ready ? "ready" : this.formatRemaining(remaining);

    if (!titleKey.equals(this.cachedTitleKey) || this.cachedNameRenderable == null) {
      this.cachedTitleKey = titleKey;
      Component nameComponent = title != null
          ? Component.text(title, NamedTextColor.WHITE)
          : Component.translatable(
              "opwerbung.hud.defaultTitle",
              NamedTextColor.WHITE,
              Component.text(String.valueOf(this.index + 1))
          );
      this.cachedNameRenderable = RenderableComponent.of(nameComponent);
    }

    if (!timeKey.equals(this.cachedTimeKey) || this.cachedTimeRenderable == null) {
      this.cachedTimeKey = timeKey;
      Component timeComponent = ready
          ? Component.translatable("opwerbung.hud.ready", NamedTextColor.GREEN)
          : Component.text(timeKey, NamedTextColor.WHITE);
      this.cachedTimeRenderable = RenderableComponent.of(timeComponent);
    }

    float nameW = this.cachedNameRenderable.getWidth();
    float nameH = this.cachedNameRenderable.getHeight();
    float timeW = this.cachedTimeRenderable.getWidth();
    float timeH = this.cachedTimeRenderable.getHeight();
    float gap = 1.0F;
    float totalH = nameH + gap + timeH;

    float nameX = centerX - nameW / 2.0F;
    float nameY = centerY - totalH / 2.0F;
    float timeX = centerX - timeW / 2.0F;
    float timeY = nameY + nameH + gap;

    context.canvas().submitRenderableComponent(
        this.cachedNameRenderable, nameX, nameY, -1, TextRenderingOptions.SHADOW
    );
    context.canvas().submitRenderableComponent(
        this.cachedTimeRenderable, timeX, timeY, -1, TextRenderingOptions.SHADOW
    );
  }

  @Override
  public boolean mouseClicked(MutableMouse mouse, MouseButton mouseButton) {
    if (mouseButton == null || !mouseButton.isLeft()) {
      return false;
    }
    long cooldownMillis = this.getCooldownMillis();
    long lastSent = this.addon.timeconfig().getTime(this.index);
    long elapsed = lastSent <= 0L ? cooldownMillis : System.currentTimeMillis() - lastSent;
    if (elapsed >= cooldownMillis) {
      this.chatListener.sendReady(this.index);
    } else {
      this.chatListener.requestConfirmation(this.index);
    }
    return true;
  }

  private AdSlot getSlot() {
    var slots = this.addon.adManager().slots();
    if (this.index < 0 || this.index >= slots.size()) {
      return null;
    }
    return slots.get(this.index);
  }

  private long getCooldownMillis() {
    int minutes = this.addon.configuration().getAdDuration().get();
    if (minutes < 3) {
      minutes = 3;
    } else if (minutes > 20) {
      minutes = 20;
    }
    return minutes * 60_000L;
  }

  private String formatRemaining(long remainingMillis) {
    long totalSeconds = (remainingMillis + 999L) / 1000L;
    long minutes = totalSeconds / 60L;
    long seconds = totalSeconds % 60L;
    if (minutes > 0) {
      return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }
    return seconds + "s";
  }
}
