package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;
import org.Bacontete.hardcore_region_difficulty.common.quality.QualityRegistry;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Share a whole setup (the config file and the quality file) as one file, HrdSetting.json in the config folder,
 * for servers and modpacks. The mod writes it when it is missing; /hrd import file reads it.
 */
public final class ConfigShare {

    public static final String FILE_NAME = "HrdSetting.json";
    private static final String META = "_hrd_share";
    private static final String QUALITIES = "qualities_file";
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();
    private static final String[] SECTIONS = {"difficulty_regions", "difficulty_levels", "scaling_settings", "smart_auto_settings"};

    private ConfigShare() {}

    /** The setup as a JSON object, or null if the config file cannot be read. */
    public static JsonObject build(DifficultyConfig config, boolean includeQualities) {
        JsonObject root = readObject(config.getConfigFile());
        if (root == null) return null;
        JsonObject meta = new JsonObject();
        meta.addProperty("format", 1);
        root.add(META, meta);
        if (includeQualities) {
            JsonObject q = readObject(QualityRegistry.getFile());
            if (q != null) root.add(QUALITIES, q);
        }
        return root;
    }

    /** Writes HrdSetting.json from the current setup if it does not exist yet (an existing file is never touched). */
    public static void ensureFile(DifficultyConfig config) {
        File file = new File(config.getConfigDirectory(), FILE_NAME);
        if (file.exists()) return;
        JsonObject pack = build(config, true);
        if (pack != null) writeFile(pack, file);
    }

    public static boolean writeFile(JsonObject pack, File file) {
        try (Writer w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            PRETTY.toJson(pack, w);
            return true;
        } catch (IOException e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not write {}", file, e);
            return false;
        }
    }

    /** Reads a shared file; returns null if it is missing or is not a JSON object. */
    public static JsonObject readFile(File file) {
        return readObject(file);
    }

    /**
     * Replaces the config (and the quality file, if the pack has one) by the pack. The old files are kept as
     * *.bak-DATE next to them. Returns null on success, else the reason.
     */
    public static synchronized String apply(DifficultyConfig config, JsonObject pack) {
        boolean known = false;
        for (String s : SECTIONS) {
            if (pack.has(s)) known = true;
        }
        if (!known) return "this is not a Hardcore Region Difficulty setup";
        JsonObject qualities = pack.has(QUALITIES) && pack.get(QUALITIES).isJsonObject() ? pack.getAsJsonObject(QUALITIES) : null;
        if (qualities != null && !qualities.has("categories")) return "the quality part is not valid";

        JsonObject cfg = pack.deepCopy();
        cfg.remove(META);
        cfg.remove(QUALITIES);
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        try {
            backup(config.getConfigFile(), stamp);
            if (qualities != null) backup(QualityRegistry.getFile(), stamp);
            try (Writer w = Files.newBufferedWriter(config.getConfigFile().toPath(), StandardCharsets.UTF_8)) {
                PRETTY.toJson(cfg, w);
            }
            if (qualities != null) {
                try (Writer w = Files.newBufferedWriter(QualityRegistry.getFile().toPath(), StandardCharsets.UTF_8)) {
                    PRETTY.toJson(qualities, w);
                }
            }
        } catch (IOException e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not apply the shared setup", e);
            return "could not write the files: " + e.getMessage();
        }
        return null;
    }

    private static void backup(File file, String stamp) throws IOException {
        if (file != null && file.exists()) {
            Files.copy(file.toPath(), new File(file.getPath() + ".bak-" + stamp).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static JsonObject readObject(File file) {
        if (file == null || !file.exists()) return null;
        try (Reader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonElement e = COMPACT.fromJson(r, JsonElement.class);
            return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
