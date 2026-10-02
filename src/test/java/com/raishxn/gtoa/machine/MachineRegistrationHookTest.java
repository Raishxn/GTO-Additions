package com.raishxn.gtoa.machine;

import org.junit.jupiter.api.Test;
import org.openjdk.nashorn.api.scripting.JSObject;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.script.Invocable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class MachineRegistrationHookTest {
    private JSObject hook() throws Exception {
        var engine = new NashornScriptEngineFactory().getScriptEngine();
        engine.eval(Files.readString(Path.of("src/main/resources/coremods/gtoa_machine_registration.js")));
        var hooks = (JSObject) ((Invocable) engine).invokeFunction("initializeCoreMod");
        return (JSObject) hooks.getMember("gtoa_after_gto_machines_clinit");
    }

    @Test
    void injectsBeforeEverySuccessfulInitializationExit() throws Exception {
        var hook = hook();
        var target = (JSObject) hook.getMember("target");
        assertEquals("com.gtocore.common.data.GTOMachines", target.getMember("class"));
        assertEquals("<clinit>", target.getMember("methodName"));
        var method = new MethodNode();
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        ((JSObject) hook.getMember("transformer")).call(hook, method);
        int count = 0;
        for (var instruction : method.instructions) {
            if (instruction.getOpcode() == Opcodes.RETURN) {
                var call = assertInstanceOf(MethodInsnNode.class, instruction.getPrevious());
                assertEquals("com/raishxn/gtoa/GTOAMachines", call.owner);
                assertEquals("init", call.name);
                assertEquals("()V", call.desc);
                assertEquals(Opcodes.INVOKESTATIC, call.getOpcode());
                count++;
            }
        }
        assertEquals(2, count);
    }

    @Test
    void refusesAnIncompatibleTargetRatherThanSilentlySkippingRegistration() throws Exception {
        var hook = hook();
        var method = new MethodNode();
        method.instructions.add(new InsnNode(Opcodes.ATHROW));
        assertThrows(Exception.class, () -> ((JSObject) hook.getMember("transformer")).call(hook, method));
    }

    @Test
    void worksOnTheActualDev9GtoMachineInitializer() throws Exception {
        var clazz = new ClassNode();
        try (var zip = new ZipFile("libs/gtocore-26.9.5.jar")) {
            new ClassReader(zip.getInputStream(zip.getEntry("com/gtocore/common/data/GTOMachines.class")))
                    .accept(clazz, 0);
        }
        var method = clazz.methods.stream().filter(m -> m.name.equals("<clinit>") && m.desc.equals("()V"))
                .findFirst().orElseThrow();
        var hook = hook();
        ((JSObject) hook.getMember("transformer")).call(hook, method);
        boolean registered = false;
        for (var instruction : method.instructions) {
            if (instruction.getOpcode() == Opcodes.RETURN) {
                var call = assertInstanceOf(MethodInsnNode.class, instruction.getPrevious());
                assertEquals("com/raishxn/gtoa/GTOAMachines", call.owner);
                registered = true;
            }
        }
        assertTrue(registered);
    }
}
