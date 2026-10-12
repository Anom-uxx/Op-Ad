package org.anomuxx.core;

import java.util.List;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.annotation.SpriteTexture;

@ConfigName("admanager")
@SpriteTexture("icon.png")
public class adManager extends Config {

  public static final int SLOT_COUNT = 20;

  private final AdSlot ad0 = new AdSlot();
  private final AdSlot ad1 = new AdSlot();
  private final AdSlot ad2 = new AdSlot();
  private final AdSlot ad3 = new AdSlot();
  private final AdSlot ad4 = new AdSlot();
  private final AdSlot ad5 = new AdSlot();
  private final AdSlot ad6 = new AdSlot();
  private final AdSlot ad7 = new AdSlot();
  private final AdSlot ad8 = new AdSlot();
  private final AdSlot ad9 = new AdSlot();
  private final AdSlot ad10 = new AdSlot();
  private final AdSlot ad11 = new AdSlot();
  private final AdSlot ad12 = new AdSlot();
  private final AdSlot ad13 = new AdSlot();
  private final AdSlot ad14 = new AdSlot();
  private final AdSlot ad15 = new AdSlot();
  private final AdSlot ad16 = new AdSlot();
  private final AdSlot ad17 = new AdSlot();
  private final AdSlot ad18 = new AdSlot();
  private final AdSlot ad19 = new AdSlot();

  /** Alle Werbungen in fester Reihenfolge (Index 0 bis 19). */
  public List<AdSlot> slots() {
    return List.of(
        ad0, ad1, ad2, ad3, ad4,
        ad5, ad6, ad7, ad8, ad9,
        ad10, ad11, ad12, ad13, ad14,
        ad15, ad16, ad17, ad18, ad19);
  }
}
