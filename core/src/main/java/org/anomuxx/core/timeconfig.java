package org.anomuxx.core;

/**
 * Zugriff auf die letzte Sendezeit pro Werbung.
 * Werte liegen in {@link AdSlot#lastSent()} und werden mit der Addon-Config gespeichert.
 */
public class timeconfig {

  private final AdAddon addon;

  public timeconfig(AdAddon addon) {
    this.addon = addon;
  }

  public long getTime(int index) {
    AdSlot slot = this.slot(index);
    if (slot == null) {
      return 0L;
    }
    Long value = slot.lastSent().get();
    return value == null ? 0L : value;
  }

  public void setTime(int index, long time) {
    AdSlot slot = this.slot(index);
    if (slot == null) {
      return;
    }
    slot.lastSent().set(time);
  }

  private AdSlot slot(int index) {
    var slots = this.addon.adManager().slots();
    if (index < 0 || index >= slots.size()) {
      return null;
    }
    return slots.get(index);
  }
}
