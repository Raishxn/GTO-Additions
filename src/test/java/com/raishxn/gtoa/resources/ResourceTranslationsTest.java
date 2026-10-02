package com.raishxn.gtoa.resources;

import org.junit.jupiter.api.Test;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.JSObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class ResourceTranslationsTest {
    @Test void allAddonUiAndTooltipKeysExistInBothLanguages() throws Exception {
        var pattern = Pattern.compile("\"(gtoa\\.[a-z0-9_.]+)\"");
        for (String language : new String[] {"en_us", "pt_br"}) {
            var engine = new NashornScriptEngineFactory().getScriptEngine();
            engine.put("json", Files.readString(Path.of("src/main/resources/assets/gtoa/lang/" + language + ".json")));
            var entries = (JSObject) engine.eval("JSON.parse(json)");
            try (var files = Files.walk(Path.of("src/main/java"))) {
                for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    var matcher = pattern.matcher(Files.readString(file));
                    while (matcher.find()) assertTrue(entries.hasMember(matcher.group(1)), language + ": " + matcher.group(1));
                }
            }
        }
    }
    @Test void allTierCoverItemsHaveModelsAndNames() throws Exception {
        var engine = new NashornScriptEngineFactory().getScriptEngine();
        for (String lang : new String[] {"en_us", "pt_br"}) {
            engine.put("json", Files.readString(Path.of("src/main/resources/assets/gtoa/lang/" + lang + ".json")));
            var entries = (JSObject) engine.eval("JSON.parse(json)");
            for (String tier : new String[] {"ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "uhv", "uev", "uiv", "uxv", "opv", "max"}) {
                var id = tier + "_production_boost_cover";
                assertTrue(entries.hasMember("item.gtoa." + id), lang + ": " + id);
                assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/gtoa/models/item/" + id + ".json")), id);
                assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/gtoa/textures/item/cover/" + id + ".png")), "cover texture: " + id);
            }
        }
    }
    @Test void allRequiredTexturesExist() {
        String[] required = {
            "src/main/resources/assets/gtoa/textures/item/primitive_furnace_kit.png",
            "src/main/resources/assets/gtoa/textures/item/entangled_vein_card.png",
            "src/main/resources/assets/gtoa/textures/item/entangled_fluid_card.png",
            "src/main/resources/assets/gtoa/textures/block/cover/overlay_production_boost.png",
            "src/main/resources/assets/gtoa/textures/block/cover/overlay_production_boost_emissive.png",
            "src/main/resources/assets/gtoa/textures/block/machines/entangled_miner/overlay_front.png",
            "src/main/resources/assets/gtoa/textures/block/machines/entangled_miner/overlay_front_active.png",
            "src/main/resources/assets/gtoa/textures/block/machines/entangled_miner/overlay_front_active_emissive.png",
            "src/main/resources/assets/gtoa/textures/block/machines/steam_entangled_miner/overlay_front.png",
            "src/main/resources/assets/gtoa/textures/block/machines/steam_entangled_miner/overlay_front_active.png",
            "src/main/resources/assets/gtoa/textures/block/machines/entangled_oil_drill/overlay_front.png",
            "src/main/resources/assets/gtoa/textures/block/machines/entangled_oil_drill/overlay_front_active.png",
            "src/main/resources/assets/gtoa/textures/block/machines/entangled_oil_drill/overlay_front_active_emissive.png",
            "src/main/resources/assets/gtoa/textures/block/machines/steam_entangled_oil_drill/overlay_front.png",
            "src/main/resources/assets/gtoa/textures/block/machines/steam_entangled_oil_drill/overlay_front_active.png",
            "src/main/resources/assets/gtoa/textures/block/overlay/machine/overlay_thermostat_hatch.png",
            "src/main/resources/assets/gtoa/textures/block/overlay/machine/overlay_thermostat_hatch_emissive.png"
        };
        for (String path : required) {
            assertTrue(Files.isRegularFile(Path.of(path)), "Missing texture: " + path);
        }
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/gtoa/models/block/machine/part/distillation_thermostat_hatch.json")),
                "Missing distillation thermostat hatch model");
    }
    @Test void allMagicGeneratorsHaveTranslationsAndRecipes() throws Exception {
        var engine = new NashornScriptEngineFactory().getScriptEngine();
        for (String lang : new String[] {"en_us", "pt_br"}) {
            engine.put("json", Files.readString(Path.of("src/main/resources/assets/gtoa/lang/" + lang + ".json")));
            var entries = (JSObject) engine.eval("JSON.parse(json)");
            for (String tier : new String[] {"ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "uhv", "uev", "uiv", "uxv", "opv", "max"}) {
                var id = tier + "_magic_generator";
                assertTrue(entries.hasMember("block.gtoa." + id), lang + ": " + id);
                assertTrue(Files.isRegularFile(Path.of("src/main/resources/data/gtoa/recipes/" + id + ".json")), "recipe: " + id);
            }
        }
    }
}
