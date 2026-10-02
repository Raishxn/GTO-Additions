package com.raishxn.gtoa.machine;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

/** Checks the critical finish protocol against the installed dev9 implementation. */
class EntangledMinerContractTest {
    private ClassNode local(String owner) throws Exception {
        var node = new ClassNode();
        new ClassReader(Files.readAllBytes(Path.of("build/classes/java/main/" + owner + ".class"))).accept(node, 0);
        return node;
    }
    @Test void nativeFinishTruePreservesProgressAndAvoidsAnotherEnergyTick() throws Exception {
        var owner = "com/gregtechceu/gtceu/api/machine/trait/RecipeLogic";
        var node = new ClassNode();
        try (var zip = new ZipFile("libs/gtceu-26.9.70.jar")) {
            new ClassReader(zip.getInputStream(zip.getEntry(owner + ".class"))).accept(node, 0);
        }
        var method = node.methods.stream().filter(m -> m.name.equals("serverTick")).findFirst().orElseThrow();
        var finish = java.util.stream.StreamSupport.stream(method.instructions.spliterator(), false)
                .filter(n -> n instanceof MethodInsnNode m && m.name.equals("onRecipeFinish")).findFirst().orElseThrow();
        var branch = assertInstanceOf(JumpInsnNode.class, finish.getNext());
        assertEquals(Opcodes.IFEQ, branch.getOpcode());
        assertEquals(Opcodes.RETURN, branch.getNext().getOpcode());
        new Analyzer<>(new BasicInterpreter()).analyze(owner, method);
    }
    @Test void blockedOutputReturnsTrueBeforeNativeFinishCanCommit() throws Exception {
        var node = local("com/raishxn/gtoa/machine/EntangledMinerRecipeLogic");
        var method = node.methods.stream().filter(m -> m.name.equals("onRecipeFinish")).findFirst().orElseThrow();
        boolean checked = false, held = false, delegated = false;
        for (var insn : method.instructions) {
            if (insn instanceof MethodInsnNode call && call.name.equals("matchRecipeOutput")) checked = true;
            if (insn.getOpcode() == Opcodes.IRETURN && insn.getPrevious().getOpcode() == Opcodes.ICONST_1) {
                assertTrue(checked);
                assertFalse(delegated);
                held = true;
            }
            if (insn instanceof MethodInsnNode call && call.name.equals("onRecipeFinish")) {
                assertTrue(held);
                assertEquals(Opcodes.INVOKESPECIAL, call.getOpcode());
                delegated = true;
            }
        }
        assertTrue(held && delegated);
        new Analyzer<>(new BasicInterpreter()).analyze(node.name, method);
    }
    @Test void steamSimulationDoesNotConsumeFuelAndFailedNativeTicksReturnBeforeBurning() throws Exception {
        var clazz = local("com/raishxn/gtoa/machine/EntangledMachine");
        var probe = clazz.methods.stream().filter(m -> m.name.equals("matchTickRecipe")).findFirst().orElseThrow();
        for (var instruction : probe.instructions) {
            if (instruction instanceof MethodInsnNode call) assertNotEquals("extractItemInternal", call.name);
            if (instruction instanceof FieldInsnNode field) assertNotEquals(Opcodes.PUTFIELD, field.getOpcode());
        }
        var tick = clazz.methods.stream().filter(m -> m.name.equals("handleTickRecipe")).findFirst().orElseThrow();
        boolean checked = false, consumed = false, decremented = false;
        for (var instruction : tick.instructions) {
            if (instruction instanceof MethodInsnNode call && call.name.equals("handleTickRecipe")) {
                assertEquals(Opcodes.INVOKESPECIAL, call.getOpcode());
                var branch = assertInstanceOf(JumpInsnNode.class, call.getNext());
                assertEquals(Opcodes.IFNE, branch.getOpcode());
                assertEquals(Opcodes.ICONST_0, branch.getNext().getOpcode());
                assertEquals(Opcodes.IRETURN, branch.getNext().getNext().getOpcode());
                checked = true;
            }
            if (instruction instanceof MethodInsnNode call && call.name.equals("extractItemInternal")) {
                assertTrue(checked);
                consumed = true;
            }
            if (instruction instanceof FieldInsnNode field && field.name.equals("fuelTicks")
                    && field.getOpcode() == Opcodes.PUTFIELD && field.getPrevious().getOpcode() == Opcodes.ISUB) {
                assertTrue(checked && consumed);
                decremented = true;
            }
        }
        assertTrue(decremented);
        new Analyzer<>(new BasicInterpreter()).analyze(clazz.name, tick);
    }
    @Test void bothMachinesUseTheSingleblockInventoryAndPersistFuelAndCycleState() throws Exception {
        var base = local("com/raishxn/gtoa/machine/EntangledMachine");
        assertEquals("com/gregtechceu/gtceu/api/machine/SimpleTieredMachine", base.superName);
        for (String owner : new String[] {"EntangledMinerMachine", "EntangledOilDrillMachine"})
            assertEquals(base.name, local("com/raishxn/gtoa/machine/" + owner).superName);
        for (String field : new String[] {"fuelTicks", "completedCycles"}) {
            var node = base.fields.stream().filter(f -> f.name.equals(field)).findFirst().orElseThrow();
            assertTrue(node.visibleAnnotations.stream().anyMatch(a -> a.desc.equals("Lcom/gto/datasynclib/annotations/SaveToDisk;")));
        }
    }
    @Test void registeredTypesUseGtoSubclassAndCycleBuildersRemainUnregistered() throws Exception {
        var types = local("com/raishxn/gtoa/GTOARecipeTypes");
        var register = types.methods.stream().filter(m -> m.name.equals("register")).findFirst().orElseThrow();
        boolean createsGtoType = false;
        for (var instruction : register.instructions) if (instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW) {
            assertNotEquals("com/gregtechceu/gtceu/api/recipe/GTRecipeType", type.desc);
            createsGtoType |= type.desc.equals("com/gtolib/api/recipe/RecipeType");
        }
        assertTrue(createsGtoType);
        var base = local("com/raishxn/gtoa/machine/EntangledMachine");
        var cycle = base.methods.stream().filter(m -> m.name.equals("cycleBuilder")).findFirst().orElseThrow();
        assertTrue(java.util.stream.StreamSupport.stream(cycle.instructions.spliterator(), false)
                .anyMatch(n -> n instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW
                        && type.desc.equals("com/gregtechceu/gtceu/api/recipe/GTRecipeBuilder")));
    }
    @Test void minerAndCardDoNotInvokeWorldExtractionOrChunkLoading() throws Exception {
        for (String owner : new String[] {"machine/EntangledMinerMachine", "machine/EntangledOilDrillMachine", "machine/EntangledMachine", "item/EntangledVeinCardItem", "item/EntangledFluidCardItem", "mining/RawOrePool"}) {
            var clazz = local("com/raishxn/gtoa/" + owner);
            for (var method : clazz.methods) for (var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call) {
                    assertFalse(call.owner.contains("OreGenerator") || call.owner.contains("BedrockOreVeinSavedData"), call.toString());
                    assertFalse(java.util.Set.of("setBlock", "setBlockAndUpdate", "destroyBlock", "getChunk", "depleteVein", "decreaseOperations", "setOperationsRemaining", "consumeChunkVeins").contains(call.name), call.name);
                }
            }
        }
    }
}
