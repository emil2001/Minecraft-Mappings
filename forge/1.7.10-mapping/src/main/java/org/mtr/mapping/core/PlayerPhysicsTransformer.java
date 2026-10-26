package org.mtr.mapping.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public final class PlayerPhysicsTransformer implements IClassTransformer {
    private static final String HOOK="org/mtr/mapping/tool/PlayerPhysics";
    private static final String HANDLER="Lnet/minecraft/network/NetHandlerPlayServer;";
    @Override public byte[] transform(String name,String transformedName,byte[] bytes) {
        boolean living="net.minecraft.entity.EntityLivingBase".equals(transformedName);
        boolean network="net.minecraft.network.NetHandlerPlayServer".equals(transformedName);
        if (bytes==null || !living && !network) return bytes;
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS); int[] gravity={0},speed={0},flight={0},movement={0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM4,writer) {
            @Override public MethodVisitor visitMethod(int access,String method,String desc,String signature,String[] exceptions) {
                MethodVisitor visitor=super.visitMethod(access,method,desc,signature,exceptions);
                boolean physics=living && (method.equals("moveEntityWithHeading") || method.equals("func_70612_e")) && desc.equals("(FF)V");
                boolean move=network && (method.equals("processPlayer") || method.equals("func_147347_a"))
                    && desc.equals("(Lnet/minecraft/network/play/client/C03PacketPlayer;)V");
                if (!physics && !move) return visitor;
                return new MethodVisitor(Opcodes.ASM4,visitor) {
                    @Override public void visitCode() {
                        super.visitCode();
                        if (move) {
                            movement[0]++;
                            super.visitVarInsn(Opcodes.ALOAD,0); super.visitVarInsn(Opcodes.ALOAD,1);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"validate","("+HANDLER+"Lnet/minecraft/network/play/client/C03PacketPlayer;)Z");
                            Label valid=new Label(); super.visitJumpInsn(Opcodes.IFNE,valid); super.visitInsn(Opcodes.RETURN);
                            super.visitLabel(valid); super.visitFrame(Opcodes.F_SAME,0,null,0,null);
                        }
                    }
                    @Override public void visitLdcInsn(Object value) {
                        if (physics && value instanceof Double && ((Double)value==.02 || (Double)value==.08)) {
                            super.visitLdcInsn(value); super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"gravity","(DLnet/minecraft/entity/Entity;)D"); gravity[0]++;
                        } else if (move && Double.valueOf(100).equals(value)) {
                            super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"movementLimit","("+HANDLER+")D"); speed[0]++;
                        } else super.visitLdcInsn(value);
                    }
                    @Override public void visitMethodInsn(int opcode,String owner,String called,String descriptor) {
                        super.visitMethodInsn(opcode,owner,called,descriptor);
                        if (move && owner.equals("net/minecraft/server/MinecraftServer") && descriptor.equals("()Z")
                                && (called.equals("isFlightAllowed") || called.equals("func_71231_X"))) {
                            super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"allowFlight","(Z"+HANDLER+")Z"); flight[0]++;
                        }
                    }
                };
            }
        },0);
        if (living && gravity[0]!=3 || network && (movement[0]!=1 || speed[0]!=1 || flight[0]!=1))
            throw new IllegalStateException("Missing native rider physics hooks: "+transformedName+" gravity="+gravity[0]+",movement="+movement[0]+",speed="+speed[0]+",flight="+flight[0]);
        return writer.toByteArray();
    }
}
