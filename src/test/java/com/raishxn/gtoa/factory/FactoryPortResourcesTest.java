package com.raishxn.gtoa.factory;

import org.junit.jupiter.api.Test;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.JSObject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

/** Regression evidence against an original GTNA source snapshot, with only namespace adapted. */
class FactoryPortResourcesTest {
    private String reference() throws Exception { return Files.readString(Path.of("src/test/resources/factory/gtna-reference.json")); }
    @Test void bothRecipesAreOneToOneWithGtna() throws Exception {
        var engine = new NashornScriptEngineFactory().getScriptEngine();
        engine.eval("function canonical(x) { if (x === null || typeof x !== 'object') return JSON.stringify(x); if (Array.isArray(x)) return '[' + x.map(canonical).join(',') + ']'; return '{' + Object.keys(x).sort().map(function(k) { return JSON.stringify(k) + ':' + canonical(x[k]); }).join(',') + '}'; }");
        engine.put("reference", reference().replace("gtna:", "gtoa:"));
        for (String name : new String[] {"universal_factory", "universal_factory_casing"}) {
            engine.put("name", name);
            engine.put("actual", Files.readString(Path.of("src/main/resources/data/gtoa/recipes/" + name + ".json")));
            assertEquals(true, engine.eval("canonical(JSON.parse(actual)) === canonical(JSON.parse(reference).recipes[name])"), name);
        }
    }
    @Test void textureIsOriginalGtnaBytes() throws Exception {
        var engine = new NashornScriptEngineFactory().getScriptEngine();
        engine.put("reference", reference());
        var expected = (String) engine.eval("JSON.parse(reference).textureSha256");
        var bytes = Files.readAllBytes(Path.of("src/main/resources/assets/gtoa/textures/block/casings/universal_factory_casing.png"));
        assertEquals(expected, java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    }
    @Test void exactlyOriginalTypesPlusNineApprovedTypesExistInDev9() throws Exception {
        var engine = new NashornScriptEngineFactory().getScriptEngine();
        engine.put("reference", reference());
        var original = (JSObject) engine.eval("JSON.parse(reference).originalTypes");
        var expected = new HashSet<String>();
        original.values().forEach(name -> expected.add((String) name));
        expected.addAll(Set.of("LAMINATOR_RECIPES", "LOOM_RECIPES", "LASER_WELDER_RECIPES", "CLUSTER_RECIPES", "ROLLING_RECIPES", "DEHYDRATOR_RECIPES", "UNPACKER_RECIPES", "ELECTROMAGNETIC_SEPARATOR_RECIPES", "ALLOY_SMELTER_RECIPES"));
        var source = Files.readString(Path.of("src/main/java/com/raishxn/gtoa/GTOAMachines.java"));
        var actual = new HashSet<String>();
        var matcher = Pattern.compile("builder\\.recipeType\\(com\\.gtocore\\.common\\.data\\.GTORecipeTypes\\.(\\w+)\\)").matcher(source);
        int calls = 0;
        while (matcher.find()) { actual.add(matcher.group(1)); calls++; }
        assertEquals(41, calls);
        assertEquals(expected, actual);
        var api = new ClassNode();
        try (var zip = new ZipFile("libs/gtocore-26.9.5.jar")) {
            new ClassReader(zip.getInputStream(zip.getEntry("com/gtocore/common/data/GTORecipeTypes.class"))).accept(api, 0);
        }
        var fields = new HashSet<String>();
        api.fields.forEach(field -> fields.add(field.name));
        assertTrue(fields.containsAll(expected));
    }
}
