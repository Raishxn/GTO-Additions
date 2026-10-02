// Register during GTO's native machine window, following the GTOHJS approach.
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

function initializeCoreMod() {
    return {
        'gtoa_after_gto_machines_clinit': {
            'target': {
                'type': 'METHOD',
                'class': 'com.gtocore.common.data.GTOMachines',
                'methodName': '<clinit>',
                'methodDesc': '()V'
            },
            'transformer': function(method) {
                var nodes = method.instructions.toArray();
                var injected = 0;
                for (var i = 0; i < nodes.length; i++) {
                    if (nodes[i].getOpcode() === Opcodes.RETURN) {
                        method.instructions.insertBefore(nodes[i], new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            'com/raishxn/gtoa/GTOAMachines',
                            'init', '()V', false));
                        injected++;
                    }
                }
                if (injected === 0) {
                    throw new Error('GTO-Additions: no RETURN in GTOMachines.<clinit>; machine registration hook incompatible');
                }
                return method;
            }
        }
    };
}
