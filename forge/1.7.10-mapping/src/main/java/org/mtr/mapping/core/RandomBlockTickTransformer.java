package org.mtr.mapping.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public final class RandomBlockTickTransformer implements IClassTransformer {
    @Override public byte[] transform(String name,String transformedName,byte[] bytes) {
        if (!"net.minecraft.world.WorldServer".equals(transformedName) || bytes==null) return bytes;
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS); int[] found={0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM4,writer) {
            @Override public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions) {
                MethodVisitor visitor=super.visitMethod(access,method,descriptor,signature,exceptions);
                if (!(method.equals("func_147456_g") || method.equals("tickBlocksAndAmbiance")) || !descriptor.equals("()V")) return visitor;
                return new MethodVisitor(Opcodes.ASM4,visitor) {
                    @Override public void visitMethodInsn(int opcode,String owner,String called,String desc) {
                        if (opcode==Opcodes.INVOKEVIRTUAL && owner.equals("net/minecraft/block/Block")
                                && (called.equals("updateTick") || called.equals("func_149674_a"))
                                && desc.equals("(Lnet/minecraft/world/World;IIILjava/util/Random;)V")) {
                            // Scheduled calls share updateTick but must remain on their original native path.
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,"org/mtr/mapping/tool/RandomBlockTicks","tick",
                                "(Lnet/minecraft/block/Block;Lnet/minecraft/world/World;IIILjava/util/Random;)V");
                            found[0]++;
                        } else super.visitMethodInsn(opcode,owner,called,desc);
                    }
                };
            }
        },0);
        if (found[0]!=1) throw new IllegalStateException("Could not locate native random block tick: "+found[0]);
        return writer.toByteArray();
    }
}
