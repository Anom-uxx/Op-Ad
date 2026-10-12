package org.anomuxx.core;

import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.annotation.SpriteSlot;
import net.labymod.api.configuration.loader.annotation.SpriteTexture;
import net.labymod.api.configuration.loader.property.ConfigProperty;

@ConfigName("settings")
@SpriteTexture("icon.png")
public class configuration extends AddonConfig {

  @SpriteSlot(x = 0, y = 0, size = 128)
  private final adManager adManager = new adManager();

  @SpriteSlot(x = 0, y = 0, size = 128)
  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  @SpriteSlot(x = 0, y = 0, size = 128)
  @SliderSetting(min = 3, max = 20, steps = 1)
  private final ConfigProperty<Integer> adDuration = new ConfigProperty<>(15);

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<Integer> getAdDuration() {
    return this.adDuration;
  }

  public adManager adManager() {
    return this.adManager;
  }
}
