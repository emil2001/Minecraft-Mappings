package org.mtr.mapping.core;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public final class SoundResolverTransformer implements IClassTransformer {
    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        boolean handler = "net.minecraft.client.audio.SoundHandler".equals(transformedName);
        boolean manager = "net.minecraft.client.audio.SoundManager".equals(transformedName);
        if (!handler && !manager) return bytes;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); boolean[] found={false,false,false,false,false};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM4,writer) {
            @Override public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access,method,descriptor,signature,exceptions);
                if (handler && (method.equals("stopSounds") || method.equals("func_147690_c")) && descriptor.equals("()V")) {
                    found[4]=true;
                    return new MethodVisitor(Opcodes.ASM4,visitor) {
                        @Override public void visitCode() {
                            super.visitCode();super.visitVarInsn(Opcodes.ALOAD,0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,"org/mtr/mapping/tool/LegacySounds","stopMtrSounds","(Lnet/minecraft/client/audio/SoundHandler;)V");
                        }
                    };
                }
                if (handler && method.equals("<clinit>")) return new MethodVisitor(Opcodes.ASM4,visitor) {
                    @Override public void visitTypeInsn(int opcode, String type) {
                        super.visitTypeInsn(opcode,type.equals("net/minecraft/client/audio/SoundListSerializer") ? "org/mtr/mapping/tool/LegacySoundListSerializer" : type);
                    }
                    @Override public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                        if (owner.equals("net/minecraft/client/audio/SoundListSerializer") && name.equals("<init>")) {
                            owner="org/mtr/mapping/tool/LegacySoundListSerializer"; found[1]=true;
                        }
                        super.visitMethodInsn(opcode,owner,name,desc);
                    }
                };
                if (handler && (method.equals("loadSoundResource") || method.equals("func_147693_a"))) return new MethodVisitor(Opcodes.ASM4,visitor) {
                    boolean entryCast; int entry=-1;
                    @Override public void visitTypeInsn(int opcode, String type) {
                        super.visitTypeInsn(opcode,type);
                        if (opcode==Opcodes.CHECKCAST && type.equals("net/minecraft/client/audio/SoundList$SoundEntry")) entryCast=true;
                    }
                    @Override public void visitVarInsn(int opcode, int var) {
                        super.visitVarInsn(opcode,var);
                        if (entryCast && opcode==Opcodes.ASTORE) { entry=var; entryCast=false; }
                    }
                    @Override public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                        if (owner.equals("net/minecraft/client/audio/SoundEventAccessorComposite") && (name.equals("addSoundToEventPool") || name.equals("func_148727_a"))) {
                            if (entry<0) throw new IllegalStateException("Could not locate native sound entry");
                            super.visitVarInsn(Opcodes.ALOAD,entry);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,"org/mtr/mapping/tool/LegacySounds","withDistance",
                                "(Lnet/minecraft/client/audio/ISoundEventAccessor;Lnet/minecraft/client/audio/SoundList$SoundEntry;)Lnet/minecraft/client/audio/ISoundEventAccessor;");
                            found[2]=true;
                        }
                        super.visitMethodInsn(opcode,owner,name,desc);
                    }
                };
                if (manager && (method.equals("playSound") || method.equals("func_148611_c"))) return new MethodVisitor(Opcodes.ASM4,visitor) {
                    boolean sampleReturned; int sample=-1;
                    @Override public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                        super.visitMethodInsn(opcode,owner,name,desc);
                        if (owner.equals("net/minecraft/client/audio/SoundEventAccessorComposite") && name.equals("func_148720_g")) sampleReturned=true;
                    }
                    @Override public void visitVarInsn(int opcode, int var) {
                        super.visitVarInsn(opcode,var);
                        if (sampleReturned && opcode==Opcodes.ASTORE) { sample=var; sampleReturned=false; }
                    }
                    @Override public void visitLdcInsn(Object constant) {
                        if (Float.valueOf(16).equals(constant)) {
                            if (sample<0) throw new IllegalStateException("Could not locate selected native sound sample");
                            super.visitVarInsn(Opcodes.ALOAD,sample);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,"org/mtr/mapping/tool/LegacySounds","distance","(Lnet/minecraft/client/audio/SoundPoolEntry;)F");
                            found[3]=true;
                        } else super.visitLdcInsn(constant);
                    }
                };
                if (!(method.equals("getSound") || method.equals("func_147680_a")) || !descriptor.equals("(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/audio/SoundEventAccessorComposite;")) return visitor;
                found[0]=true;
                return new MethodVisitor(Opcodes.ASM4,visitor) {
                    @Override public void visitCode() {
                        super.visitCode(); visitVarInsn(Opcodes.ALOAD,0); visitVarInsn(Opcodes.ALOAD,1);
                        visitMethodInsn(Opcodes.INVOKESTATIC,"org/mtr/mapping/tool/LegacySounds","resolve",
                            "(Lnet/minecraft/client/audio/SoundHandler;Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/audio/SoundEventAccessorComposite;");
                        visitInsn(Opcodes.DUP); Label vanilla=new Label(); visitJumpInsn(Opcodes.IFNULL,vanilla); visitInsn(Opcodes.ARETURN);
                        visitLabel(vanilla); visitFrame(Opcodes.F_SAME1,0,null,1,new Object[]{"net/minecraft/client/audio/SoundEventAccessorComposite"}); visitInsn(Opcodes.POP);
                    }
                };
            }
        },0);
        if (handler && (!found[0] || !found[1] || !found[2] || !found[4]) || manager && !found[3]) throw new IllegalStateException("Could not find native sound hooks in " + transformedName);
        return writer.toByteArray();
    }
}
