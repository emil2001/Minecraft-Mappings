package org.mtr.mapping.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

/** Lets FML receive a factory-backed entity before its concrete legacy class has been constructed locally. */
public final class EntitySpawnLookupTransformer implements IClassTransformer {
    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (!"cpw.mods.fml.common.registry.EntityRegistry".equals(transformedName)) return bytes;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        boolean[] found = {false};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM4,writer) {
            @Override public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access,method,descriptor,signature,exceptions);
                if (!method.equals("lookupModSpawn") || !descriptor.equals("(Lcpw/mods/fml/common/ModContainer;I)Lcpw/mods/fml/common/registry/EntityRegistry$EntityRegistration;")) return visitor;
                found[0]=true;
                return new MethodVisitor(Opcodes.ASM4,visitor) {
                    @Override public void visitCode() {
                        super.visitCode(); visitVarInsn(Opcodes.ALOAD,1); visitVarInsn(Opcodes.ILOAD,2);
                        visitMethodInsn(Opcodes.INVOKESTATIC,"org/mtr/mapping/registry/EntityFactoryRegistry","forSpawn",descriptor);
                        visitInsn(Opcodes.DUP); Label vanilla = new Label(); visitJumpInsn(Opcodes.IFNULL,vanilla); visitInsn(Opcodes.ARETURN);
                        visitLabel(vanilla); visitFrame(Opcodes.F_SAME1,0,null,1,new Object[]{"cpw/mods/fml/common/registry/EntityRegistry$EntityRegistration"}); visitInsn(Opcodes.POP);
                    }
                };
            }
        },0);
        if (!found[0]) throw new IllegalStateException("Could not find the native FML entity spawn lookup");
        return writer.toByteArray();
    }
}
