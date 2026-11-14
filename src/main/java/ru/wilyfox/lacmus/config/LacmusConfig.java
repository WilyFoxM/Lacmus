package ru.wilyfox.lacmus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import ru.wilyfox.lacmus.Lacmus;
import ru.wilyfox.lacmus.core.HudSettings;

/**
 * The settings file {@code lacmus.json} in the loader's config directory, written atomically. A
 * field missing from a saved file gets its current default; saved values are never overwritten by a
 * newer default. The loader adapter hands over the directory.
 */
public final class LacmusConfig {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final String FILE = Lacmus.MOD_ID + ".json";

  private static HudSettings instance = new HudSettings();
  private static Path directory;

  private LacmusConfig() {}

  public static HudSettings get() {
    return instance;
  }

  public static Path path() {
    return directory.resolve(FILE);
  }

  static HudSettings decode(String text) {
    var json = JsonParser.parseString(text);
    if (!json.isJsonObject()) {
      throw new IllegalArgumentException("Config must be an object");
    }
    HudSettings settings = GSON.fromJson(json, HudSettings.class);
    if (settings.schemaVersion != HudSettings.SCHEMA_VERSION) {
      throw new IllegalArgumentException("Unsupported schema version " + settings.schemaVersion);
    }
    settings.sanitize();
    return settings;
  }

  /** Reads the settings from {@code configDirectory}, or writes the defaults there. */
  public static void load(Path configDirectory) {
    directory = configDirectory;
    try {
      if (Files.isRegularFile(path())) {
        instance = decode(Files.readString(path()));
      } else {
        save();
      }
    } catch (Exception failure) {
      // Keep the unreadable original; a later explicit settings change may replace it.
      Lacmus.LOGGER.warn("Could not read {}; using defaults", FILE, failure);
    }
  }

  public static void save() {
    instance.sanitize();
    try {
      AtomicFileWriter.write(path(), writer -> GSON.toJson(instance, writer));
    } catch (Exception failure) {
      throw new IllegalStateException("Could not save " + FILE, failure);
    }
  }

  public static void defaults() {
    instance = new HudSettings();
  }
}
