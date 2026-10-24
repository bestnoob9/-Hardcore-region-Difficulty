package org.Bacontete.hardcore_region_difficulty.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** The presets created by the player: config/hardcore_region_difficulty-presets.json. */
public final class PresetStore {
    public static final String FILE_NAME = "hardcore_region_difficulty-presets.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private PresetStore() {}

    public static List<DifficultyPreset> load(File configDir) {
        List<DifficultyPreset> out = new ArrayList<>();
        File file = new File(configDir, FILE_NAME);
        if (!file.exists()) return out;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonElement root = GSON.fromJson(reader, JsonElement.class);
            if (root != null && root.isJsonObject() && root.getAsJsonObject().has("presets")
                    && root.getAsJsonObject().get("presets").isJsonArray()) {
                for (JsonElement e : root.getAsJsonObject().getAsJsonArray("presets")) {
                    if (!e.isJsonObject()) continue;
                    DifficultyPreset p = DifficultyPreset.fromJson(e.getAsJsonObject());
                    if (p != null) out.add(p);
                }
            }
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not read {}", file, e);
        }
        return out;
    }

    public static boolean save(File configDir, List<DifficultyPreset> presets) {
        JsonArray array = new JsonArray();
        for (DifficultyPreset p : presets) {
            if (!p.builtIn) array.add(p.toJson());
        }
        JsonObject root = new JsonObject();
        root.add("presets", array);
        try (Writer writer = Files.newBufferedWriter(new File(configDir, FILE_NAME).toPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
            return true;
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.error("Could not write {}", FILE_NAME, e);
            return false;
        }
    }
}
