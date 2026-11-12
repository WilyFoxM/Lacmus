package ru.wilyfox.lacmus.fabric;

import net.fabricmc.api.ClientModInitializer;
import ru.wilyfox.lacmus.Lacmus;

/** Fabric adapter: wires loader events to the loader-neutral code. */
public final class LacmusFabric implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    Lacmus.initClient();
  }
}
