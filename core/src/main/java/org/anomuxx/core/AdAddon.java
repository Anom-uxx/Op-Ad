package org.anomuxx.core;

import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;
import org.anomuxx.core.commands.OpAdCommand;
import org.anomuxx.core.hud.AdHudWidget;
import org.anomuxx.core.listener.ChatListener;

@AddonMain
public class AdAddon extends LabyAddon<configuration> {

  private static AdAddon instance;

  private timeconfig time;
  private ChatListener chatListener;

  public static AdAddon get() {
    return instance;
  }

  @Override
  protected void enable() {
    try {
      instance = this;
      this.logger().info("[OP Werbung] enable() gestartet");

      this.registerSettingCategory();
      this.time = new timeconfig(this);
      this.chatListener = new ChatListener(this);
      this.registerListener(this.chatListener);
      this.registerCommand(new OpAdCommand(this.chatListener));

      for (int i = 0; i < adManager.SLOT_COUNT; i++) {
        final int index = i;
        this.labyAPI().hudWidgetRegistry().register(
            () -> new AdHudWidget(this, this.chatListener, index)
        );
      }

      this.logger().info("[OP Werbung] Enabled the Addon");
    } catch (Throwable throwable) {
      this.logger().error("[OP Werbung] Fehler in enable()", throwable);
      throw throwable;
    }
  }

  public adManager adManager() {
    return this.configuration().adManager();
  }

  public timeconfig timeconfig() {
    return this.time;
  }

  public ChatListener chatListener() {
    return this.chatListener;
  }

  @Override
  protected Class<configuration> configurationClass() {
    return configuration.class;
  }
}
