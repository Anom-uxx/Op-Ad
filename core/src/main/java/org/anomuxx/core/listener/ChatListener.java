package org.anomuxx.core.listener;

import java.util.List;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.chat.ChatMessageSendEvent;
import net.labymod.api.event.client.lifecycle.GameTickEvent;
import org.anomuxx.core.AdSlot;
import org.anomuxx.core.AdAddon;
import org.anomuxx.core.adManager;
import org.anomuxx.core.timeconfig;

public class ChatListener {

  private static final long MINUTE_MILLIS = 60 * 1000L;
  private static final long CONFIRM_TIMEOUT_MILLIS = 30 * 1000L;
  private static final long ALLOW_WINDOW_MILLIS = 3 * 1000L;

  private final AdAddon addon;
  private final timeconfig time;

  private volatile int pendingIndex = -1;
  private volatile long pendingSince = 0L;

  private volatile String blockedText = null;
  private volatile int blockedIndex = -1;

  private volatile String allowText = null;
  private volatile long allowSince = 0L;

  private final boolean[] readyNotified = new boolean[adManager.SLOT_COUNT];

  /** Beim Welteintritt keine Ready-Sounds/Nachrichten für bereits abgelaufene Cooldowns. */
  private boolean wasIngame;

  public ChatListener(AdAddon addon) {
    this.addon = addon;
    this.time = addon.timeconfig();
    this.addon.logger().info("[OP Werbung] ChatListener erstellt!");
  }

  private long getCooldownMillis() {
    int minutes = this.addon.configuration().getAdDuration().get();
    if (minutes < 3) {
      minutes = 3;
    } else if (minutes > 20) {
      minutes = 20;
    }
    return minutes * MINUTE_MILLIS;
  }

  @Subscribe
  public void onChatMessageSend(ChatMessageSendEvent event) {
    String message = event.getMessage();

    String allowed = this.allowText;
    if (allowed != null) {
      boolean inWindow = System.currentTimeMillis() - this.allowSince <= ALLOW_WINDOW_MILLIS;
      if (inWindow && allowed.equalsIgnoreCase(message)) {
        this.addon.logger().info("[OP Werbung] Bestätigte Werbung durchgelassen: " + message);
        return;
      }
      if (!inWindow) {
        this.allowText = null;
      }
    }

    List<AdSlot> slots = this.addon.adManager().slots();
    for (int index = 0; index < slots.size(); index++) {
      AdSlot slot = slots.get(index);
      String adMessage = slot.message().get();

      if (adMessage == null || adMessage.isBlank() || !adMessage.equalsIgnoreCase(message)) {
        continue;
      }

      if (!Boolean.TRUE.equals(slot.enabled().get())) {
        continue;
      }

      this.handleAd(event, index);
      return;
    }
  }

  private void handleAd(ChatMessageSendEvent event, int index) {
    long now = System.currentTimeMillis();
    long cooldown = this.getCooldownMillis();
    long elapsed = now - this.time.getTime(index);

    if (elapsed >= cooldown) {
      this.time.setTime(index, now);
      this.readyNotified[index] = false;
      this.addon.logger().info("[OP Werbung] Werbung " + (index + 1) + " gesendet.");
      return;
    }

    event.setCancelled(true);
    this.blockedText = event.getMessage();
    this.blockedIndex = index;

    long remainingMillis = cooldown - elapsed;
    long remainingMinutes = (remainingMillis + MINUTE_MILLIS - 1) / MINUTE_MILLIS;

    Component info = Component.translatable(
        "opwerbung.messages.cooldown",
        Component.text(String.valueOf(remainingMinutes))
    );

    Component button = Component.translatable(
            "opwerbung.messages.sendAnyway", NamedTextColor.GREEN)
        .clickEvent(ClickEvent.runCommand("/opwerbung confirm " + index));

    this.show(info);
    this.show(button);
  }

  @Subscribe
  public void onGameTick(GameTickEvent event) {
    if (event.phase() != Phase.PRE) {
      return;
    }

    if (!Boolean.TRUE.equals(this.addon.configuration().enabled().get())) {
      return;
    }

    boolean ingame = Laby.labyAPI().minecraft().isIngame();
    if (!ingame) {
      this.wasIngame = false;
      return;
    }

    long now = System.currentTimeMillis();
    long cooldown = this.getCooldownMillis();
    List<AdSlot> slots = this.addon.adManager().slots();

    // Erster Tick in der Welt: bereits bereite Werbungen nur markieren, kein Sound/Chat
    boolean justJoined = !this.wasIngame;
    this.wasIngame = true;

    for (int index = 0; index < slots.size(); index++) {
      AdSlot slot = slots.get(index);

      if (!Boolean.TRUE.equals(slot.enabled().get())) {
        this.readyNotified[index] = false;
        continue;
      }

      String message = slot.message().get();
      if (message == null || message.isBlank()) {
        this.readyNotified[index] = false;
        continue;
      }

      long elapsed = now - this.time.getTime(index);
      if (elapsed >= cooldown) {
        if (!this.readyNotified[index]) {
          this.readyNotified[index] = true;
          if (!justJoined) {
            this.notifyReady(index, slot);
            this.playReadySound(slot);
          }
        }
      } else {
        this.readyNotified[index] = false;
      }
    }
  }

  private void notifyReady(int index, AdSlot slot) {
    Component titleComponent;
    String title = slot.title().get();
    if (title == null || title.isBlank()) {
      titleComponent = Component.translatable(
          "opwerbung.hud.defaultTitle",
          Component.text(String.valueOf(index + 1))
      );
    } else {
      titleComponent = Component.text(title);
    }

    Component info = Component.translatable(
        "opwerbung.messages.ready",
        titleComponent
    );

    Component button = Component.translatable(
            "opwerbung.messages.send", NamedTextColor.GREEN)
        .clickEvent(ClickEvent.runCommand("/opwerbung sendready " + index));

    this.show(info);
    this.show(button);
  }

  private void playReadySound(AdSlot slot) {
    String soundId = slot.sound().get();
    if (soundId == null || soundId.isBlank()) {
      return;
    }
    try {
      ResourceLocation location = ResourceLocation.parse(soundId);
      Laby.labyAPI().minecraft().sounds().playSound(location, 1.0F, 1.0F);
    } catch (Exception exception) {
      this.addon.logger().warn(
          "[OP Werbung] Sound konnte nicht abgespielt werden: " + soundId,
          exception
      );
    }
  }

  public boolean requestConfirmation(int index) {
    if (index < 0 || index >= this.addon.adManager().slots().size()) {
      return false;
    }

    this.pendingIndex = index;
    this.pendingSince = System.currentTimeMillis();

    Component question = Component.translatable(
        "opwerbung.messages.confirmQuestion", NamedTextColor.YELLOW);

    Component confirmButton = Component.translatable(
            "opwerbung.messages.confirmButton", NamedTextColor.RED)
        .clickEvent(ClickEvent.runCommand("/opwerbung send " + index));

    this.show(question);
    this.show(confirmButton);
    return true;
  }

  public boolean sendAnyway(int index) {
    boolean confirmed = this.pendingIndex == index
        && System.currentTimeMillis() - this.pendingSince <= CONFIRM_TIMEOUT_MILLIS;

    this.pendingIndex = -1;

    List<AdSlot> slots = this.addon.adManager().slots();
    if (!confirmed || index < 0 || index >= slots.size()) {
      this.addon.logger().info("[OP Werbung] sendAnyway abgelehnt (confirmed=" + confirmed
          + ", index=" + index + ")");
      return false;
    }

    String text = (this.blockedIndex == index && this.blockedText != null)
        ? this.blockedText
        : slots.get(index).message().get();
    this.blockedText = null;
    this.blockedIndex = -1;
    if (text == null || text.isBlank()) {
      return false;
    }

    this.time.setTime(index, System.currentTimeMillis());
    this.readyNotified[index] = false;

    this.allowText = text;
    this.allowSince = System.currentTimeMillis();

    this.addon.logger().info("[OP Werbung] Sende bestätigte Werbung " + index + ": " + text);
    Laby.labyAPI().minecraft().chatExecutor().chat(text);
    return true;
  }

  public boolean sendReady(int index) {
    List<AdSlot> slots = this.addon.adManager().slots();
    if (index < 0 || index >= slots.size()) {
      return false;
    }

    AdSlot slot = slots.get(index);
    if (!Boolean.TRUE.equals(slot.enabled().get())) {
      return false;
    }

    String text = slot.message().get();
    if (text == null || text.isBlank()) {
      return false;
    }

    long now = System.currentTimeMillis();
    long elapsed = now - this.time.getTime(index);
    if (elapsed < this.getCooldownMillis()) {
      this.addon.logger().info("[OP Werbung] sendReady abgelehnt – noch Cooldown für Index " + index);
      return false;
    }

    this.time.setTime(index, now);
    this.readyNotified[index] = false;

    this.allowText = text;
    this.allowSince = now;

    this.addon.logger().info("[OP Werbung] Sende bereite Werbung " + index + ": " + text);
    Laby.labyAPI().minecraft().chatExecutor().chat(text);
    return true;
  }


  private void show(Component component) {
    Laby.labyAPI().minecraft().chatExecutor().displayClientMessage(component);
  }
}
