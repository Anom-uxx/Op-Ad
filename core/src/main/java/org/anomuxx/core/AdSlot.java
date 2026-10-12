package org.anomuxx.core;

import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.annotation.ShowSettingInParent;
import net.labymod.api.configuration.loader.annotation.SpriteSlot;
import net.labymod.api.configuration.loader.annotation.SpriteTexture;
import net.labymod.api.configuration.loader.property.ConfigProperty;

/** Eine einzelne Werbung: Name, Schalter, Nachricht, Sound und letzte Sendezeit. */
@SpriteTexture("icon.png")
public class AdSlot extends Config {

  @SpriteSlot(x = 0, y = 0, size = 128)
  @ShowSettingInParent
  @TextFieldSetting
  private final ConfigProperty<String> title = new ConfigProperty<>("");

  @SpriteSlot(x = 0, y = 0, size = 128)
  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(false);

  @SpriteSlot(x = 0, y = 0, size = 128)
  @TextFieldSetting
  private final ConfigProperty<String> message = new ConfigProperty<>("");

  @SpriteSlot(x = 0, y = 0, size = 128)
  @TextFieldSetting
  private final ConfigProperty<String> sound = new ConfigProperty<>(
      "minecraft:entity.player.levelup"
  );

  /** Zeitpunkt des letzten Sendens (Millis seit Epoch), wird mit der Config gespeichert. */
  private final ConfigProperty<Long> lastSent = new ConfigProperty<>(0L);

  public ConfigProperty<String> title() {
    return this.title;
  }

  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<String> message() {
    return this.message;
  }

  public ConfigProperty<String> sound() {
    return this.sound;
  }

  public ConfigProperty<Long> lastSent() {
    return this.lastSent;
  }
}
