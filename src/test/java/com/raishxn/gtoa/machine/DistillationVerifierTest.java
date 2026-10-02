package com.raishxn.gtoa.machine;

import org.junit.jupiter.api.Test;
import org.openjdk.nashorn.api.scripting.JSObject;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import javax.script.Invocable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

/** BasicInterpreter erases reference types. Use the actual JVM to check the array overload contract. */
class DistillationVerifierTest implements Opcodes {
    private static final String BLOCK = "net/minecraft/world/level/block/Block";
    private static final String META = "com/gregtechceu/gtceu/api/block/MetaMachineBlock";
    private static final String FIXTURE = "gtoa/test/TypedPredicateFixture";
    private static final String META_ARRAY = "[L" + META + ";";
    private static final String BLOCK_ARRAY = "[L" + BLOCK + ";";

    private ClassNode transformed() throws Exception {
        var engine = new NashornScriptEngineFactory().getScriptEngine();
        engine.eval(Files.readString(Path.of("src/main/resources/coremods/gtoa_features.js")));
        var hooks = (JSObject) ((Invocable) engine).invokeFunction("initializeCoreMod");
        var hook = (JSObject) hooks.getMember("gtoa_primitive_distillation_structure");
        var node = new ClassNode();
        try (var zip = new ZipFile("libs/gtocore-26.9.5.jar")) {
            new ClassReader(zip.getInputStream(zip.getEntry("com/gtocore/common/data/machines/MultiBlockC.class"))).accept(node, 0);
        }
        return (ClassNode) ((JSObject) hook.getMember("transformer")).call(hook, node);
    }
    @Test void bothNativeHeatPredicatesKeepTheirSpecificArrayTypeAndVerifyInTheJvm() throws Exception {
        int hooks = 0;
        var helper = new ClassNode();
        new ClassReader(Files.readAllBytes(Path.of("build/classes/java/main/com/raishxn/gtoa/recipe/DistillationStructure.class"))).accept(helper, 0);
        for (var method : transformed().methods) for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("com/raishxn/gtoa/recipe/DistillationStructure")) {
                assertEquals("(" + META_ARRAY + ")" + META_ARRAY, call.desc);
                assertTrue(helper.methods.stream().anyMatch(m -> m.name.equals(call.name) && m.desc.equals(call.desc)));
                var predicate = assertInstanceOf(MethodInsnNode.class, call.getNext());
                assertEquals("blocks", predicate.name);
                assertTrue(predicate.desc.startsWith("(" + META_ARRAY + ")"));
                verify(call.desc);
                hooks++;
            }
        }
        assertEquals(2, hooks);
    }
    @Test void reproducesTheReportedCrashForTheOldWidenedArrayReturn() {
        assertThrows(VerifyError.class, () -> verify("(" + META_ARRAY + ")" + BLOCK_ARRAY));
    }
    private static void verify(String descriptor) throws Exception {
        var loader = new ClassLoader(DistillationVerifierTest.class.getClassLoader()) {
            Class<?> define(String name, byte[] bytes) { return defineClass(name.replace('/', '.'), bytes, 0, bytes.length); }
        };
        loader.define(BLOCK, shell(BLOCK, "java/lang/Object"));
        loader.define(META, shell(META, BLOCK));
        var writer = new ClassWriter(0);
        writer.visit(V17, ACC_PUBLIC, FIXTURE, null, "java/lang/Object", null);
        var helper = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "withThermostat", descriptor, null, null);
        helper.visitCode(); helper.visitVarInsn(ALOAD, 0); helper.visitInsn(ARETURN); helper.visitMaxs(1, 1); helper.visitEnd();
        var predicate = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "blocks", "(" + META_ARRAY + ")V", null, null);
        predicate.visitCode(); predicate.visitInsn(RETURN); predicate.visitMaxs(0, 1); predicate.visitEnd();
        var method = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "run", "()V", null, null);
        method.visitCode(); method.visitInsn(ICONST_1); method.visitTypeInsn(ANEWARRAY, META);
        method.visitMethodInsn(INVOKESTATIC, FIXTURE, "withThermostat", descriptor, false);
        method.visitMethodInsn(INVOKESTATIC, FIXTURE, "blocks", "(" + META_ARRAY + ")V", false);
        method.visitInsn(RETURN); method.visitMaxs(1, 0); method.visitEnd(); writer.visitEnd();
        loader.define(FIXTURE, writer.toByteArray()).getMethod("run").invoke(null);
    }
    private static byte[] shell(String name, String parent) {
        var writer = new ClassWriter(0);
        writer.visit(V17, ACC_PUBLIC, name, null, parent, null);
        writer.visitEnd();
        return writer.toByteArray();
    }
}
