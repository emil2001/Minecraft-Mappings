package org.mtr.mapping.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public final class EntityFactoryTransformer implements IClassTransformer {
    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (!"net.minecraft.entity.EntityList".equals(transformedName)) return bytes;
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        int[] found = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM4,writer) {
            @Override public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access,method,descriptor,signature,exceptions);
                String factory;
                if ((method.equals("createEntityByName") || method.equals("func_75620_a")) && descriptor.equals("(Ljava/lang/String;Lnet/minecraft/world/World;)Lnet/minecraft/entity/Entity;")) factory="byName";
                else if ((method.equals("createEntityFromNBT") || method.equals("func_75615_a")) && descriptor.equals("(Lnet/minecraft/nbt/NBTTagCompound;Lnet/minecraft/world/World;)Lnet/minecraft/entity/Entity;")) factory="fromNbt";
                else return visitor;
                found[0]++;
                return new MethodVisitor(Opcodes.ASM4,visitor) {
                    @Override public void visitCode() {
                        super.visitCode();
                        visitVarInsn(Opcodes.ALOAD,0); visitVarInsn(Opcodes.ALOAD,1);
                        visitMethodInsn(Opcodes.INVOKESTATIC,"org/mtr/mapping/registry/EntityFactoryRegistry",factory,descriptor);
                        visitInsn(Opcodes.DUP); Label vanilla = new Label(); visitJumpInsn(Opcodes.IFNULL,vanilla); visitInsn(Opcodes.ARETURN);
                        visitLabel(vanilla); visitFrame(Opcodes.F_SAME1,0,null,1,new Object[]{"net/minecraft/entity/Entity"}); visitInsn(Opcodes.POP);
                    }
                };
            }
        },0);
        if (found[0] != 2) throw new IllegalStateException("Could not find native entity factory loaders");
        return writer.toByteArray();
    }
}
