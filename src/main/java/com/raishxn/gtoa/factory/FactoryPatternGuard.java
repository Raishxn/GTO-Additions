package com.raishxn.gtoa.factory;

import com.gtocore.common.machine.trait.InternalSlotRecipeHandler;
import com.gtocore.common.machine.multiblock.part.ae.MEPatternBufferPartMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.api.recipe.handler.RecipeHandlerUnit;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEFluidKey;
import java.lang.reflect.Field;
import java.util.HashMap;

/** Validate the unscaled recipe against the encoded outputs before any inputs are consumed. */
public final class FactoryPatternGuard {
    private FactoryPatternGuard() {}
    private static final Field SLOT = slotField();
    private static Field slotField() {
        try {
            var field = InternalSlotRecipeHandler.AbstractRHL.class.getDeclaredField("slot");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Incompatible GTO Pattern Buffer slot API", error);
        }
    }
    public static boolean matches(RecipeHandlerUnit unit, GTRecipeDefinition definition) {
        if (!(unit instanceof InternalSlotRecipeHandler.AbstractRHL<?>)) return true;
        try {
            if (!(SLOT.get(unit) instanceof MEPatternBufferPartMachine.InternalSlot slot)) return true;
            var pattern = slot.machine.getDetailsSlotMap().inverse().get(slot);
            // Manual inventories without a processing pattern use the native cached recipe.
            if (pattern == null) return true;
            var expected = new HashMap<AEKey, Long>();
            for (var output : pattern.getOutputs()) {
                if (output != null) expected.merge(output.what(), output.amount(), Math::addExact);
            }
            var actual = new HashMap<AEKey, Long>();
            for (var content : definition.itemOutputs) {
                var key = AEItemKey.of(content.inner.getInnerItemStack());
                if (key != null) actual.merge(key, content.amount, Math::addExact);
            }
            for (var content : definition.fluidOutputs) {
                var key = AEFluidKey.of(content.inner.getFluidStack());
                if (key != null) actual.merge(key, content.amount, Math::addExact);
            }
            return PatternOutputs.matches(expected, actual);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Cannot read GTO Pattern Buffer slot", error);
        }
    }
}
