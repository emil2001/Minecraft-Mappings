package org.mtr.mapping.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public final class StyledTextTransformer implements IClassTransformer {
    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        boolean font = "net.minecraft.client.gui.FontRenderer".equals(transformedName);
        boolean chat = "net.minecraft.client.gui.GuiNewChat".equals(transformedName);
        boolean strategist = "com.gtnewhorizons.angelica.client.font.FontStrategist".equals(transformedName);
        boolean batcher = "com.gtnewhorizons.angelica.client.font.BatchingFontRenderer".equals(transformedName);
        if (bytes == null || !font && !chat && !strategist && !batcher) return bytes;
        int api = asmApi();
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        int[] found = {0};
        new ClassReader(bytes).accept(new ClassVisitor(api, writer) {
            @Override public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, method, descriptor, signature, exceptions);
                if (font && (method.equals("renderStringAtPos") || method.equals("func_78255_a")) && descriptor.equals("(Ljava/lang/String;Z)V")) {
                    return new MethodVisitor(api, visitor) {
                        boolean colorArray;
                        @Override public void visitFieldInsn(int opcode, String owner, String field, String desc) {
                            super.visitFieldInsn(opcode, owner, field, desc);
                            if (opcode == Opcodes.GETFIELD && owner.equals("net/minecraft/client/gui/FontRenderer")
                                && (field.equals("colorCode") || field.equals("field_78285_g")) && desc.equals("[I")) colorArray = true;
                        }
                        @Override public void visitInsn(int opcode) {
                            super.visitInsn(opcode);
                            if (colorArray && opcode == Opcodes.IALOAD) {
                                super.visitVarInsn(Opcodes.ALOAD, 1);
                                super.visitVarInsn(Opcodes.ILOAD, 3);
                                super.visitVarInsn(Opcodes.ILOAD, 2);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "org/mtr/mapping/mapper/LegacyStyledText", "color", "(ILjava/lang/String;IZ)I", false);
                                colorArray = false;
                                found[0]++;
                            }
                        }
                    };
                }
                if (chat && (method.equals("drawChat") || method.equals("func_146230_a")) && descriptor.equals("(I)V")) {
                    return new MethodVisitor(api, visitor) {
                        int lastLoaded = -1, chatLine = -1;
                        String componentMethod;
                        @Override public void visitVarInsn(int opcode, int index) {
                            super.visitVarInsn(opcode, index);
                            if (opcode == Opcodes.ALOAD) lastLoaded = index;
                        }
                        @Override public void visitMethodInsn(int opcode, String owner, String called, String desc, boolean isInterface) {
                            if (owner.equals("net/minecraft/client/gui/ChatLine") && (called.equals("getChatComponent") || called.equals("func_151461_a"))
                                && desc.equals("()Lnet/minecraft/util/IChatComponent;")) { chatLine = lastLoaded; componentMethod = called; }
                            if (owner.equals("net/minecraft/client/gui/FontRenderer") && (called.equals("drawStringWithShadow") || called.equals("func_78261_a"))
                                && desc.equals("(Ljava/lang/String;III)I")) {
                                if (chatLine < 0) throw new IllegalStateException("Cannot locate native styled chat component");
                                super.visitVarInsn(Opcodes.ALOAD, chatLine);
                                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/client/gui/ChatLine", componentMethod, "()Lnet/minecraft/util/IChatComponent;", false);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "org/mtr/mapping/mapper/LegacyStyledText", "drawChat",
                                    "(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;IIILnet/minecraft/util/IChatComponent;)I", false);
                                found[0]++;
                            } else super.visitMethodInsn(opcode, owner, called, desc, isInterface);
                        }
                    };
                }
                if (strategist && method.equals("getFontProvider") && descriptor.equals("(Lcom/gtnewhorizons/angelica/client/font/BatchingFontRenderer;CZZ)Lcom/gtnewhorizons/angelica/client/font/FontProvider;")) {
                    found[0]++;
                    return new MethodVisitor(api, visitor) {
                        @Override public void visitCode() {
                            super.visitCode();
                            super.visitVarInsn(Opcodes.ILOAD, 1);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "org/mtr/mapping/compat/angelica/AngelicaTextSupport", "provider", "(C)Lcom/gtnewhorizons/angelica/client/font/FontProvider;", false);
                            super.visitInsn(Opcodes.DUP);
                            Label nativeProvider = new Label();
                            super.visitJumpInsn(Opcodes.IFNULL, nativeProvider);
                            super.visitInsn(Opcodes.ARETURN);
                            super.visitLabel(nativeProvider);
                            super.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"com/gtnewhorizons/angelica/client/font/FontProvider"});
                            super.visitInsn(Opcodes.POP);
                        }
                    };
                }
                if (batcher && method.equals("getWhitespaceScale") && descriptor.equals("()F")) {
                    return new MethodVisitor(api, visitor) {
                        @Override public void visitInsn(int opcode) {
                            if (opcode == Opcodes.FRETURN) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, "org/mtr/mapping/compat/angelica/AngelicaTextSupport", "whitespace", "(F)F", false);
                                found[0]++;
                            }
                            super.visitInsn(opcode);
                        }
                    };
                }
                return visitor;
            }
        }, 0);
        if (found[0] != 1) throw new IllegalStateException("Could not locate native styled text hook in " + transformedName + ": " + found[0]);
        return writer.toByteArray();
    }
    private static int asmApi() {
        try { return Opcodes.class.getField("ASM9").getInt(null); }
        catch (ReflectiveOperationException legacyAsm) { return Opcodes.ASM5; }
    }
}

