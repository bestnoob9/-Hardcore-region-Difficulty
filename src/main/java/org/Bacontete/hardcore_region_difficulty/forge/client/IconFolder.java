package org.Bacontete.hardcore_region_difficulty.forge.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.Bacontete.hardcore_region_difficulty.common.HardcoreRegionDifficulty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Own icons: every .png in config/hardcore_region_config/icon/iconlevel/ can be picked as an icon.
 * It is stored in the config as "iconlevel:file_name" (file name without .png, lowercase a-z 0-9 _ - .).
 * Client side only. This class does not import Cloth Config.
 */
public final class IconFolder {

    public static final String NAMESPACE = "iconlevel";

    private static final Map<String, Long> LOADED = new HashMap<>();
    private static final Map<String, Boolean> OK = new HashMap<>();

    private IconFolder() {}

    /** config/hardcore_region_config/icon/iconlevel (created when missing). */
    public static Path dir() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("hardcore_region_config").resolve("icon").resolve("iconlevel");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            HardcoreRegionDifficulty.LOGGER.warn("Could not create icon folder {}", dir, e);
        }
        return dir;
    }

    /** Creates the folder at game start so players can find it. */
    public static void init() {
        dir();
    }

    private static boolean validName(String name) {
        return !name.isEmpty() && name.matches("[a-z0-9._-]+");
    }

    /** Icon values ("iconlevel:name") of all usable .png files in the folder, sorted. */
    public static List<String> list() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir())) {
            files.filter(Files::isRegularFile).forEach(p -> {
                String file = p.getFileName().toString();
                if (!file.toLowerCase(Locale.ROOT).endsWith(".png")) {
                    return;
                }
                String name = file.substring(0, file.length() - 4);
                if (validName(name)) {
                    out.add(NAMESPACE + ":" + name);
                } else {
                    HardcoreRegionDifficulty.LOGGER.warn(
                            "Icon file '{}' ignored: use only lowercase letters, digits, '_', '-' and '.'", file);
                }
            });
        } catch (IOException e) {
            HardcoreRegionDifficulty.LOGGER.warn("Could not list icon folder", e);
        }
        out.sort(String::compareTo);
        return out;
    }

    /**
     * Makes sure the texture can be drawn: loads (or reloads, when the file changed) a file icon, or checks
     * that a resource-pack texture exists. Call on the render thread right before drawing.
     */
    public static boolean ensure(ResourceLocation tex) {
        if (tex == null) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!NAMESPACE.equals(tex.getNamespace())) {
            return mc.getResourceManager().getResource(tex).isPresent();
        }
        String name = tex.getPath();
        Path file = dir().resolve(name + ".png");
        long modified;
        try {
            if (!validName(name) || !Files.isRegularFile(file)) {
                OK.remove(name);
                LOADED.remove(name);
                return false;
            }
            modified = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return false;
        }
        Long done = LOADED.get(name);
        if (done != null && done == modified) {
            return OK.getOrDefault(name, false);
        }
        LOADED.put(name, modified);
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            mc.getTextureManager().register(tex, new DynamicTexture(image));
            OK.put(name, true);
        } catch (Exception e) {
            HardcoreRegionDifficulty.LOGGER.warn("Could not load icon {}", file, e);
            OK.put(name, false);
        }
        return OK.getOrDefault(name, false);
    }
}
