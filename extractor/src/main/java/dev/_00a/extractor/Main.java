package dev._00a.extractor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import dev._00a.extractor.extractors.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.reflect.ReflectionFactory;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeMap;
import java.nio.charset.StandardCharsets;

public class Main implements ModInitializer {
    public static final String MOD_ID = "mc_extractor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("Starting extractors...");

            DummyWorld.INSTANCE.registryManager = server.getRegistryManager();

            var extractors = new Extractor[]{
                new Attributes(),
                new Blocks(),
                new Effects(),
                new Enchants(),
                new Entities(),
                new Misc(),
                new Items(),
                new Packets(),
                new Sounds(),
                new TranslationKeys(),
                new Tags(),
            };

            Path outputDirectory;
            try {
                outputDirectory = Files.createDirectories(Paths.get("extractor_output"));
            } catch (IOException e) {
                LOGGER.info("Failed to create output directory.", e);
                return;
            }

            var gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();

            for (var extractor : extractors) {
                var fileName = extractor.fileName();

                try {
                    var out = outputDirectory.resolve(fileName);
                    var stream = new DataOutputStream(new FileOutputStream(out.toFile()));

                    extractor.extract(server, stream, gson);

                    stream.close();
                    LOGGER.info("Wrote " + out.toAbsolutePath());
                } catch (Exception e) {
                    LOGGER.error("Extractor for \"" + fileName + "\" failed.", e);
                }
            }

            LOGGER.info("Done! Errors below this line can be ignored.");

            server.shutdown();
        });
    }

    public interface Extractor {
        /** The name of the file generated by this extractor. */
        String fileName();
        /** Extracts the data to the provided data output. */
        void extract(MinecraftServer server, DataOutput output, Gson gson) throws Exception;
    }

    public record Pair<T, U>(T left, U right) {
    }

    /**
     * Magically creates an instance of a <i>concrete</i> class without calling its constructor.
     */
    public static <T> T magicallyInstantiate(Class<T> clazz) {
        var rf = ReflectionFactory.getReflectionFactory();
        try {
            var objCon = Object.class.getDeclaredConstructor();
            var con = rf.newConstructorForSerialization(clazz, objCon);
            return clazz.cast(con.newInstance());
        } catch (Throwable e) {
            throw new IllegalArgumentException("Failed to magically instantiate " + clazz.getName(), e);
        }
    }

    public static void writeJson(DataOutput output, Gson gson, JsonElement element) throws IOException {
        output.write(gson.toJson(element).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts a string to PascalCase.
     * 
     * @param str The string to convert.
     * @return The converted string.
     */
    public static String toPascalCase(String str) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c) || c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }

    /**
     * Converts a TreeMap to a JsonArray, ignoring the keys.
     */
    public static <A, B extends JsonElement> JsonArray treeMapToJsonArray(TreeMap<A, B> map) {
        var array = new JsonArray();
        for (var value : map.values()) {
            array.add(value);
        }
        return array;
    }
}
