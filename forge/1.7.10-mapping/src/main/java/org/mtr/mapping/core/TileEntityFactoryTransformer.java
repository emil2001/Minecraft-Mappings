package org.mtr.mapping.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

/** Routes MTR factory data before vanilla attempts reflective no-argument construction. */
public final class TileEntityFactoryTransformer implements IClassTransformer {
    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (!"net.minecraft.tileentity.TileEntity".equals(transformedName)) return bytes;
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        boolean[] found = {false};
        reader.accept(new ClassVisitor(Opcodes.ASM4, writer) {
            @Override public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, method, descriptor, signature, exceptions);
                if ((access & Opcodes.ACC_STATIC) == 0 || !(method.equals("createAndLoadEntity") || method.equals("func_145827_c"))
                        || !descriptor.equals("(Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/tileentity/TileEntity;")) return visitor;
                found[0] = true;
                return new MethodVisitor(Opcodes.ASM4, visitor) {
                    @Override public void visitCode() {
                        super.visitCode();
                        visitVarInsn(Opcodes.ALOAD, 0);
                        visitMethodInsn(Opcodes.INVOKESTATIC, "org/mtr/mapping/registry/BlockEntityFactoryRegistry", "load", descriptor);
                        visitInsn(Opcodes.DUP);
                        Label vanilla = new Label();
                        visitJumpInsn(Opcodes.IFNULL, vanilla);
                        visitInsn(Opcodes.ARETURN);
                        visitLabel(vanilla);
                        visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"net/minecraft/tileentity/TileEntity"});
                        visitInsn(Opcodes.POP);
                    }
                };
            }
        }, 0);
        if (!found[0]) throw new IllegalStateException("Could not find the Forge 1.7.10 TileEntity loader");
        return writer.toByteArray();
    }
}
