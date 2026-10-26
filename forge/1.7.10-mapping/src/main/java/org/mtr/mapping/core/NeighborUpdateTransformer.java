package org.mtr.mapping.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public final class NeighborUpdateTransformer implements IClassTransformer {
    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (!"net.minecraft.world.World".equals(transformedName)) return bytes;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        int[] found = {0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM4, writer) {
            @Override public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, method, descriptor, signature, exceptions);
                if (!(method.equals("notifyBlocksOfNeighborChange") || method.equals("func_147459_d") || method.equals("func_147441_b"))) return visitor;
                return new MethodVisitor(Opcodes.ASM4, visitor) {
                    @Override public void visitMethodInsn(int opcode, String owner, String called, String desc) {
                        if (opcode == Opcodes.INVOKEVIRTUAL && owner.equals("net/minecraft/world/World")
                                && (called.equals("notifyBlockOfNeighborChange") || called.equals("func_147460_e"))
                                && desc.equals("(IIILnet/minecraft/block/Block;)V")) {
                            // The legacy callback omits source coordinates needed by MTR's directional state updates.
                            super.visitVarInsn(Opcodes.ILOAD, 1);
                            super.visitVarInsn(Opcodes.ILOAD, 2);
                            super.visitVarInsn(Opcodes.ILOAD, 3);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "org/mtr/mapping/tool/NeighborUpdates", "notify",
                                "(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;III)V");
                            found[0]++;
                        } else super.visitMethodInsn(opcode, owner, called, desc);
                    }
                };
            }
        }, 0);
        if (found[0] != 12) throw new IllegalStateException("Could not locate all Forge 1.7.10 neighbor notifications: " + found[0]);
        return writer.toByteArray();
    }
}
