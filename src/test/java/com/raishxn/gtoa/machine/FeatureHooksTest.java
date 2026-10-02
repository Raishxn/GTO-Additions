package com.raishxn.gtoa.machine;

import org.junit.jupiter.api.Test;
import org.openjdk.nashorn.api.scripting.JSObject;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import javax.script.Invocable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

class FeatureHooksTest {
    private JSObject hooks() throws Exception {
        var engine = new NashornScriptEngineFactory().getScriptEngine();
        engine.eval(Files.readString(Path.of("src/main/resources/coremods/gtoa_features.js")));
        return (JSObject) ((Invocable) engine).invokeFunction("initializeCoreMod");
    }
    private ClassNode readClass(String jar, String owner) throws Exception {
        var clazz = new ClassNode();
        try (var zip = new ZipFile("libs/" + jar)) {
            new ClassReader(zip.getInputStream(zip.getEntry(owner + ".class"))).accept(clazz, 0);
        }
        return clazz;
    }
    private MethodNode transform(String name) throws Exception {
        var hook = (JSObject) hooks().getMember(name);
        var target = (JSObject) hook.getMember("target");
        String owner = ((String) target.getMember("class")).replace('.', '/');
        var clazz = readClass("gtceu-26.9.70.jar", owner);
        var method = clazz.methods.stream().filter(m -> m.name.equals(target.getMember("methodName"))
                && m.desc.equals(target.getMember("methodDesc"))).findFirst().orElseThrow();
        ((JSObject) hook.getMember("transformer")).call(hook, method);
        // Check transformed stack types and branch frames against the actual dev9 bytecode.
        new Analyzer<>(new BasicInterpreter()).analyze(owner, method);
        return method;
    }
    @Test void registersRecipeTypesBeforeTheRegistryFreeze() throws Exception {
        var method = transform("gtoa_recipe_types_registration");
        var call = assertInstanceOf(MethodInsnNode.class, method.instructions.getFirst());
        assertEquals("com/raishxn/gtoa/GTOARecipeTypes", call.owner);
        assertTrue(java.util.stream.StreamSupport.stream(method.instructions.spliterator(), false)
                .anyMatch(node -> node instanceof MethodInsnNode nativeCall && nativeCall.name.equals("freeze")));
    }
    @Test void registersCoversBeforeTheRegistryFreeze() throws Exception {
        var method = transform("gtoa_covers_registration");
        var call = assertInstanceOf(MethodInsnNode.class, method.instructions.getFirst());
        assertEquals("com/raishxn/gtoa/GTOACovers", call.owner);
    }
    @Test void modifiesRuntimeRecipesImmediatelyAfterFullModification() throws Exception {
        var method = transform("gtoa_singleblock_recipe_boost");
        int calls = 0;
        for (var node : method.instructions) {
            if (node instanceof MethodInsnNode call && call.owner.equals("com/raishxn/gtoa/recipe/CoverRecipeBoost")) {
                assertEquals("modify", call.name);
                assertEquals("machine", assertInstanceOf(FieldInsnNode.class, call.getPrevious()).name);
                var fullModify = assertInstanceOf(MethodInsnNode.class, call.getPrevious().getPrevious().getPrevious());
                assertEquals("fullModifyRecipe", fullModify.name);
                calls++;
            }
        }
        assertEquals(1, calls);
    }
    @Test void coversBothDistillationFillPathsWithoutChangingOrdinaryCapabilities() throws Exception {
        var method = transform("gtoa_distillation_output_boost");
        int calls = 0;
        for (var node : method.instructions) {
            if (node instanceof MethodInsnNode call && call.owner.equals("com/raishxn/gtoa/recipe/DistillationOutputBoost")) {
                assertEquals(Opcodes.INVOKESTATIC, call.getOpcode());
                assertTrue(call.name.equals("fillInternal") || call.name.equals("fill"));
                calls++;
            }
        }
        assertEquals(2, calls);
    }
    @Test void changesOnlyThePrimitiveTowersStructureAndBothHeatPositions() throws Exception {
        var hook = (JSObject) hooks().getMember("gtoa_primitive_distillation_structure");
        var clazz = readClass("gtocore-26.9.5.jar", "com/gtocore/common/data/machines/MultiBlockC");
        ((JSObject) hook.getMember("transformer")).call(hook, clazz);
        int changed = 0;
        for (var method : clazz.methods) {
            int calls = 0, heatPositions = 0;
            for (var node : method.instructions) {
                if (node instanceof MethodInsnNode call && call.owner.equals("com/raishxn/gtoa/recipe/DistillationStructure")) calls++;
                if (node instanceof FieldInsnNode field && field.name.equals("HEAT_HATCH")) heatPositions++;
            }
            if (calls > 0) {
                assertEquals(2, heatPositions);
                new Analyzer<>(new BasicInterpreter()).analyze(clazz.name, method);
                changed++;
            }
        }
        assertEquals(1, changed);
    }
    @Test void failsClearlyWhenRequiredNativeTargetsChange() throws Exception {
        var hooks = hooks();
        for (String name : new String[] {"gtoa_singleblock_recipe_boost", "gtoa_distillation_output_boost"}) {
            var hook = (JSObject) hooks.getMember(name);
            assertThrows(Exception.class, () -> ((JSObject) hook.getMember("transformer")).call(hook, new MethodNode()));
        }
        var structure = (JSObject) hooks.getMember("gtoa_primitive_distillation_structure");
        assertThrows(Exception.class, () -> ((JSObject) structure.getMember("transformer")).call(structure, new ClassNode()));
    }
}
