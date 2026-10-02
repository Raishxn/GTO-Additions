var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
function initializeCoreMod() {
    return {
        'gtoa_recipe_types_registration': {
            target: {type: 'METHOD', class: 'com.gregtechceu.gtceu.common.data.GTRecipeTypes', methodName: 'init', methodDesc: '()V'},
            transformer: function(method) {
                method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, 'com/raishxn/gtoa/GTOARecipeTypes', 'init', '()V', false));
                return method;
            }
        },
        'gtoa_covers_registration': {
            target: {type: 'METHOD', class: 'com.gregtechceu.gtceu.common.data.GTCovers', methodName: 'init', methodDesc: '()V'},
            transformer: function(method) {
                method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, 'com/raishxn/gtoa/GTOACovers', 'init', '()V', false));
                return method;
            }
        },
        'gtoa_singleblock_recipe_boost': {
            target: {type: 'METHOD', class: 'com.gregtechceu.gtceu.api.machine.trait.RecipeLogic', methodName: 'checkMatchedRecipeAvailable',
                     methodDesc: '(Lcom/gregtechceu/gtceu/api/recipe/handler/RecipeHandlerUnit;Lcom/gregtechceu/gtceu/api/recipe/GTRecipeDefinition;)Z'},
            transformer: function(method) {
                var nodes = method.instructions.toArray();
                var count = 0;
                for (var i = 0; i < nodes.length; i++) {
                    var node = nodes[i];
                    if (node instanceof MethodInsnNode && node.owner === 'com/gregtechceu/gtceu/api/machine/feature/IRecipeLogicMachine' && node.name === 'fullModifyRecipe') {
                        var hook = new InsnList();
                        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        hook.add(new FieldInsnNode(Opcodes.GETFIELD, 'com/gregtechceu/gtceu/api/machine/trait/RecipeLogic', 'machine', 'Lcom/gregtechceu/gtceu/api/machine/feature/IRecipeLogicMachine;'));
                        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 'com/raishxn/gtoa/recipe/CoverRecipeBoost', 'modify', '(Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;Lcom/gregtechceu/gtceu/api/machine/feature/IRecipeLogicMachine;)Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;', false));
                        method.instructions.insert(node, hook);
                        count++;
                    }
                }
                if (count !== 1) throw new Error('GTO-Additions: incompatible recipe boost hook: ' + count);
                return method;
            }
        },
        'gtoa_distillation_output_boost': {
            target: {type: 'METHOD', class: 'com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistillationTower', methodName: 'applyFluidOutputs',
                     methodDesc: '(Ljava/util/List;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Z'},
            transformer: function(method) {
                var nodes = method.instructions.toArray();
                var count = 0;
                for (var i = 0; i < nodes.length; i++) {
                    var node = nodes[i];
                    if (node instanceof MethodInsnNode && (node.name === 'fillInternal' || node.name === 'fill') && node.getOpcode() === Opcodes.INVOKEINTERFACE) {
                        method.instructions.set(node, new MethodInsnNode(Opcodes.INVOKESTATIC, 'com/raishxn/gtoa/recipe/DistillationOutputBoost', node.name,
                            '(L' + node.owner + ';Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)I', false));
                        count++;
                    }
                }
                if (count !== 2) throw new Error('GTO-Additions: incompatible distillation output hook: ' + count);
                return method;
            }
        },
        'gtoa_primitive_distillation_structure': {
            target: {type: 'CLASS', name: 'com.gtocore.common.data.machines.MultiBlockC'},
            transformer: function(clazz) {
                var matches = 0;
                for (var m = 0; m < clazz.methods.size(); m++) {
                    var method = clazz.methods.get(m);
                    var nodes = method.instructions.toArray();
                    var heatReferences = 0;
                    for (var i = 0; i < nodes.length; i++) {
                        if (nodes[i] instanceof FieldInsnNode && nodes[i].owner === 'com/gtocore/common/data/GTOMachines' && nodes[i].name === 'HEAT_HATCH') heatReferences++;
                    }
                    // Only the primitive tower's structure contains its two explicit heat hatch predicates.
                    if (heatReferences !== 2) continue;
                    matches++;
                    var heatHooks = 0;
                    for (var i = 0; i < nodes.length; i++) {
                        var node = nodes[i];
                        // Preserve the native MetaMachineBlock[] overload; a Block[] return is invalid here.
                        if (node instanceof MethodInsnNode && node.owner === 'com/gregtechceu/gtceu/api/pattern/Predicates' && node.name === 'blocks'
                                && node.desc === '([Lcom/gregtechceu/gtceu/api/block/MetaMachineBlock;)Lcom/gregtechceu/gtceu/api/pattern/TraceabilityPredicate;') {
                            method.instructions.insertBefore(node, new MethodInsnNode(Opcodes.INVOKESTATIC, 'com/raishxn/gtoa/recipe/DistillationStructure', 'withThermostat',
                                '([Lcom/gregtechceu/gtceu/api/block/MetaMachineBlock;)[Lcom/gregtechceu/gtceu/api/block/MetaMachineBlock;', false));
                            heatHooks++;
                        }
                    }
                    if (heatHooks !== 2) throw new Error('GTO-Additions: incompatible heat hatch predicate overloads: ' + heatHooks);
                }
                if (matches !== 1) throw new Error('GTO-Additions: incompatible primitive distillation structure hook: ' + matches);
                return clazz;
            }
        }
    };
}
