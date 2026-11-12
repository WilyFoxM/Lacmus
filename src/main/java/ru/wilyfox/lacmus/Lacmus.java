package ru.wilyfox.lacmus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loader-neutral entry point; loader adapters call {@link #initClient()}. */
public final class Lacmus {
  public static final String MOD_ID = "lacmus";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  private Lacmus() {}

  public static void initClient() {
    LOGGER.info("Lacmus client initialized");
  }
}
