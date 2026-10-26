package org.mtr.mapping.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public final class PlayerRenderOffsetTransformer implements IClassTransformer {
    @Override public byte[] transform(String name,String transformedName,byte[] bytes) {
        if (!"net.minecraft.client.renderer.entity.RenderPlayer".equals(transformedName) || bytes==null) return bytes;
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS); int[] found={0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM4,writer) {
            @Override public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions) {
                MethodVisitor visitor=super.visitMethod(access,method,descriptor,signature,exceptions);
                if (!(method.equals("doRender") || method.equals("func_76986_a")) || !descriptor.equals("(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V")) return visitor;
                found[0]++;
                return new MethodVisitor(Opcodes.ASM4,visitor) {
                    @Override public void visitCode() {
                        super.visitCode(); super.visitVarInsn(Opcodes.ALOAD,1); super.visitVarInsn(Opcodes.DLOAD,4);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC,"org/mtr/mapping/mapper/EntityHelper","renderY","(Lnet/minecraft/entity/player/EntityPlayer;D)D");
                        super.visitVarInsn(Opcodes.DSTORE,4);
                    }
                };
            }
        },0);
        if (found[0]!=1) throw new IllegalStateException("Could not locate native player render offset: "+found[0]);
        return writer.toByteArray();
    }
}
