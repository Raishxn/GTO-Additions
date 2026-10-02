package com.raishxn.gtoa.machine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import javax.tools.ToolProvider;
import static org.junit.jupiter.api.Assertions.*;

/** Executes the production scheduler bytecode with deterministic game/IO fixtures. */
class UniversalFactorySchedulerTest {
    @TempDir Path directory;
    private static final String MACHINE = "com.raishxn.gtoa.machine.";
    private static final String RECIPE = "com.gregtechceu.gtceu.api.recipe.";
    private static final String TRAIT = "com.gregtechceu.gtceu.api.machine.trait.";
    private static final String HANDLER = RECIPE + "handler.";

    @Test void differentRecipeTypesOccupyThreadsTogetherThenStopAndRestartAfterIdle() throws Exception {
        try (var loader = fixture()) {
            var factoryClass = loader.loadClass(MACHINE + "UniversalFactoryMachine");
            var factory = factoryClass.getConstructor().newInstance();
            var logicClass = loader.loadClass(MACHINE + "UniversalFactoryRecipeLogic");
            var constructor = logicClass.getDeclaredConstructor(factoryClass);
            constructor.setAccessible(true);
            var logic = constructor.newInstance(factory);
            var tick = logicClass.getDeclaredMethod("serverTick");
            tick.setAccessible(true);
            var enqueue = factoryClass.getMethod("enqueue", String.class, String.class, int.class);
            enqueue.invoke(factory, "loom", "cloth", 3);
            enqueue.invoke(factory, "laminator", "board", 3);
            tick.invoke(logic);
            assertEquals(2, factoryClass.getField("threads").getInt(factory));
            assertEquals(5L, factoryClass.getField("parallel").getLong(factory));
            var base = loader.loadClass(TRAIT + "RecipeLogic");
            assertEquals(1, base.getField("status").getInt(logic));
            for (int i = 0; i < 3; i++) tick.invoke(logic);
            assertEquals(0, factoryClass.getField("threads").getInt(factory));
            assertEquals(0, base.getField("status").getInt(logic), "Last output must immediately leave IDLE");
            assertFalse(base.getField("unsubscribed").getBoolean(logic), "Empty queue must keep searching");
            assertNull(base.getField("lastRecipe").get(logic));
            assertEquals(2, factoryClass.getField("outputs").getInt(factory));
            for (int i = 0; i < 10; i++) tick.invoke(logic);
            assertEquals(0, base.getField("status").getInt(logic));
            assertEquals(6, factoryClass.getField("energyTicks").getInt(factory), "Idle consumes no energy");
            enqueue.invoke(factory, "loom", "cloth", 1);
            enqueue.invoke(factory, "laminator", "board", 1);
            tick.invoke(logic);
            assertEquals(2, factoryClass.getField("threads").getInt(factory));
            tick.invoke(logic);
            assertEquals(4, factoryClass.getField("outputs").getInt(factory));
            assertEquals(0, base.getField("status").getInt(logic));
        }
    }

    @Test void emptyFirstStartNeverReportsWorkingAndControllerKeepsPolling() throws Exception {
        try (var loader = fixture()) {
            var factoryClass = loader.loadClass(MACHINE + "UniversalFactoryMachine");
            var factory = factoryClass.getConstructor().newInstance();
            var logicClass = loader.loadClass(MACHINE + "UniversalFactoryRecipeLogic");
            var constructor = logicClass.getDeclaredConstructor(factoryClass);
            constructor.setAccessible(true);
            var logic = constructor.newInstance(factory);
            var tick = logicClass.getDeclaredMethod("serverTick");
            tick.setAccessible(true);
            tick.invoke(logic);
            assertEquals(0, loader.loadClass(TRAIT + "RecipeLogic").getField("status").getInt(logic));
            assertEquals(0, factoryClass.getField("energyTicks").getInt(factory));
        }
        var machine = read(MACHINE + "UniversalFactoryMachine");
        var polling = machine.methods.stream().filter(m -> m.name.equals("keepSubscribing")).findFirst().orElseThrow();
        assertEquals(java.util.List.of(Opcodes.ICONST_1, Opcodes.IRETURN),
                java.util.stream.StreamSupport.stream(polling.instructions.spliterator(), false)
                        .filter(i -> i.getOpcode() >= 0).map(i -> i.getOpcode()).toList());
    }

    private ClassNode read(String name) throws Exception {
        var node = new ClassNode();
        new ClassReader(Files.readAllBytes(Path.of("build/classes/java/main", name.replace('.', '/') + ".class"))).accept(node, 0);
        return node;
    }
    private URLClassLoader fixture() throws Exception {
        Map<String, String> sources = Map.ofEntries(
            Map.entry("com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine", "public interface IRecipeLogicMachine { boolean matchRecipeOutput(com.gregtechceu.gtceu.api.recipe.GTRecipe r); }"),
            Map.entry("net.minecraft.resources.ResourceLocation", "public record ResourceLocation(String value) {}"),
            Map.entry(RECIPE + "GTRecipeType", "public class GTRecipeType { public net.minecraft.resources.ResourceLocation registryName; public GTRecipeType(String id) { registryName = new net.minecraft.resources.ResourceLocation(id); } }"),
            Map.entry(RECIPE + "GTRecipeDefinition", "public class GTRecipeDefinition { public net.minecraft.resources.ResourceLocation id; public GTRecipeType recipeType; public GTRecipe runtime; }"),
            Map.entry(RECIPE + "GTRecipe", "public class GTRecipe { public GTRecipeDefinition definition; public int duration; public long parallels; }"),
            Map.entry(HANDLER + "RecipeHandlerUnit", "public class RecipeHandlerUnit {}"),
            Map.entry("com.gregtechceu.gtceu.api.machine.TickableSubscription", "public class TickableSubscription { public int cycle; }"),
            Map.entry("com.raishxn.gtoa.factory.FactoryPatternGuard", "public class FactoryPatternGuard { public static boolean matches(com.gregtechceu.gtceu.api.recipe.handler.RecipeHandlerUnit u, com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition d) { return true; } }"),
            Map.entry("com.mojang.logging.LogUtils", "public class LogUtils { public static LogUtils getLogger() {return new LogUtils();} public void error(String s, Object o) {} }"),
            Map.entry(TRAIT + "RecipeLogic", """
                import com.gregtechceu.gtceu.api.recipe.*;
                import com.gregtechceu.gtceu.api.recipe.handler.*;
                public class RecipeLogic implements java.util.function.BiPredicate<RecipeHandlerUnit, GTRecipeDefinition> {
                    public static final int IDLE=0, WORKING=1, WAITING=2, SUSPEND=3;
                    public int status, interval, progress, duration;
                    public boolean isActive, suspendAfterFinish, unsubscribed;
                    public GTRecipe lastRecipe;
                    public GTRecipeDefinition lockedRecipe;
                    public com.gregtechceu.gtceu.api.machine.TickableSubscription subscription;
                    private final com.raishxn.gtoa.machine.UniversalFactoryMachine machine;
                    public RecipeLogic(com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine m) {machine=(com.raishxn.gtoa.machine.UniversalFactoryMachine)m;}
                    public void setStatus(int s) {status=s;}
                    public void markLastRecipeDirty() {}
                    public void unsubscribe() {unsubscribed=true;}
                    public boolean test(RecipeHandlerUnit unit, GTRecipeDefinition definition) {return setupRecipe(unit, definition.runtime);}
                    public boolean setupRecipe(RecipeHandlerUnit unit, GTRecipe recipe) {return false;}
                }
                """),
            Map.entry(MACHINE + "UniversalFactoryMachine", """
                import com.gregtechceu.gtceu.api.recipe.*;
                import com.gregtechceu.gtceu.api.recipe.handler.*;
                public class UniversalFactoryMachine implements com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine {
                    public int threads, outputs, energyTicks;
                    public long parallel;
                    private final com.raishxn.gtoa.factory.FactoryBalance balance = new com.raishxn.gtoa.factory.FactoryBalance();
                    private final GTRecipeType[] types={new GTRecipeType("loom"),new GTRecipeType("laminator")};
                    private final java.util.List<GTRecipe> pending=new java.util.ArrayList<>();
                    private final RecipeHandlerUnit unit=new RecipeHandlerUnit();
                    public void enqueue(String type, String id, int duration) {
                        GTRecipe recipe=new GTRecipe(); recipe.duration=duration; recipe.parallels=type.equals("loom")?2:3;
                        GTRecipeDefinition d=new GTRecipeDefinition(); d.id=new net.minecraft.resources.ResourceLocation(id);
                        d.recipeType=types[type.equals("loom")?0:1]; d.runtime=recipe; recipe.definition=d; pending.add(recipe);
                    }
                    public boolean isRecipeLogicAvailable() {return true;}
                    public int threadLimit() {return 128;}
                    public int parallelLimit() {return 512;}
                    public int operatingTier() {return 3;}
                    public long remainingBudget() {return Long.MAX_VALUE;}
                    public com.raishxn.gtoa.factory.FactoryBalance balance() {return balance;}
                    public GTRecipeType[] getAvailableRecipeTypes() {return types;}
                    public boolean findRecipe(GTRecipeType t, java.util.function.BiPredicate<RecipeHandlerUnit,GTRecipeDefinition> test, GTRecipeDefinition locked) {
                        for (GTRecipe r: new java.util.ArrayList<>(pending)) if(r.definition.recipeType==t && test.test(unit,r.definition)) return true;
                        return false;
                    }
                    public boolean matchTickRecipe(GTRecipe r) {return true;}
                    public boolean handleTickRecipe(GTRecipe r) {energyTicks++;return true;}
                    public boolean matchRecipeOutput(GTRecipe r) {return true;}
                    public boolean handleRecipeOutput(GTRecipe r) {outputs++;return true;}
                    public boolean handleRecipeInput(RecipeHandlerUnit u, GTRecipe r) {return pending.remove(r);}
                    public void onWorking() {}
                    public void afterWorking() {}
                    public void beforeWorking(RecipeHandlerUnit u, GTRecipe r) {}
                    public void setRecipeType(GTRecipeType t) {}
                    public void syncLanes(int t, long p) {threads=t;parallel=p;}
                    public void onChanged() {}
                    public boolean keepSubscribing() {return true;}
                }
                """)
        );
        var arguments = new java.util.ArrayList<String>();
        arguments.addAll(java.util.List.of("-classpath", System.getProperty("java.class.path"), "-d", directory.toString()));
        for (var entry : sources.entrySet()) {
            var file = directory.resolve(entry.getKey().replace('.', '/') + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, "package " + entry.getKey().substring(0, entry.getKey().lastIndexOf('.')) + ";\n" + entry.getValue());
            arguments.add(file.toString());
        }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, arguments.toArray(String[]::new)));
        var logic = read(MACHINE + "UniversalFactoryRecipeLogic");
        // Keep actual scheduling, admission, and filtering; omit world subscriptions and NBT codecs.
        logic.methods.removeIf(m -> !Set.of("<init>", "serverTick", "setupRecipe", "test", "occupied", "lambda$serverTick$0", "lambda$serverTick$1", "lambda$test$2").contains(m.name));
        var writer = new ClassWriter(0); logic.accept(writer);
        Files.write(directory.resolve((MACHINE + "UniversalFactoryRecipeLogic").replace('.', '/') + ".class"), writer.toByteArray());
        Files.copy(Path.of("build/classes/java/main", (MACHINE + "RecipeLaneTracker").replace('.', '/') + ".class"), directory.resolve((MACHINE + "RecipeLaneTracker").replace('.', '/') + ".class"));
        return new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()}, getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (sources.containsKey(name) || name.equals(MACHINE + "UniversalFactoryRecipeLogic") || name.equals(MACHINE + "RecipeLaneTracker")) {
                    synchronized (getClassLoadingLock(name)) {
                        var found = findLoadedClass(name);
                        if (found == null) found = findClass(name);
                        if (resolve) resolveClass(found);
                        return found;
                    }
                }
                return super.loadClass(name, resolve);
            }
        };
    }
}
