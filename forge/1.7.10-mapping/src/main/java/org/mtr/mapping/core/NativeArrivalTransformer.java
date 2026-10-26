package org.mtr.mapping.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public final class NativeArrivalTransformer implements IClassTransformer {
    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.network.NetworkManager".equals(transformedName)) return bytes;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        int[] hooks = {0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM4, writer) {
            @Override public MethodVisitor visitMethod(int access, String method, String desc, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, method, desc, signature, exceptions);
                if (!method.equals("channelRead0") || !desc.equals("(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V")) return visitor;
                return new MethodVisitor(Opcodes.ASM4, visitor) {
                    @Override public void visitCode() {
                        super.visitCode(); hooks[0]++;
                        super.visitVarInsn(Opcodes.ALOAD, 0); super.visitVarInsn(Opcodes.ALOAD, 2);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "org/mtr/mapping/registry/LegacyNetwork", "captureClientArrival", "(Lnet/minecraft/network/NetworkManager;Lnet/minecraft/network/Packet;)V");
                    }
                };
            }
        }, 0);
        if (hooks[0] != 1) throw new IllegalStateException("Missing native MTR packet arrival hook: " + hooks[0]);
        return writer.toByteArray();
    }
}
