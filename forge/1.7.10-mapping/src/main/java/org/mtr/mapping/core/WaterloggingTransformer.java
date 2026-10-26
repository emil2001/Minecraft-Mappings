package org.mtr.mapping.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import java.util.*;

/** Narrow adapters at vanilla fluid/physics access sites; ordinary world reads remain untouched. */
public final class WaterloggingTransformer implements IClassTransformer {
    private static final String HOOK="org/mtr/mapping/tool/Waterlogging";
    private static final String WORLD="Lnet/minecraft/world/World;",ACCESS="Lnet/minecraft/world/IBlockAccess;";
    private static final String BLOCK="Lnet/minecraft/block/Block;",LIQUID="Lnet/minecraft/block/BlockLiquid;",MATERIAL="Lnet/minecraft/block/material/Material;";
    @Override public byte[] transform(String name,String transformedName,byte[] bytes) {
        boolean liquid=transformedName!=null && Arrays.asList("net.minecraft.block.BlockLiquid","net.minecraft.block.BlockDynamicLiquid","net.minecraft.block.BlockStaticLiquid").contains(transformedName);
        boolean world="net.minecraft.world.World".equals(transformedName),entity="net.minecraft.entity.Entity".equals(transformedName),item="net.minecraft.item.Item".equals(transformedName);
        boolean bucket="net.minecraft.item.ItemBucket".equals(transformedName);
        if (bytes==null || !liquid && !world && !entity && !item && !bucket) return bytes;
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS); int[] reads={0},writes={0};
        String className=new ClassReader(bytes).getClassName();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM4,writer) {
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions) {
                boolean scoped="net.minecraft.block.BlockDynamicLiquid".equals(transformedName)
                    && (matches(name,"updateTick","func_149674_a") || name.equals("func_149812_c") || name.equals("func_149808_o"))
                    || "net.minecraft.block.BlockLiquid".equals(transformedName) && name.equals("func_149805_n");
                String implementation=scoped?"$mtr$waterlogging$"+name:name;
                if (scoped) scopeWrapper(super.visitMethod(access,name,desc,signature,exceptions),className,implementation,desc);
                MethodVisitor visitor=super.visitMethod(access,implementation,desc,signature,exceptions);
                if ((access&Opcodes.ACC_STATIC)!=0) return visitor;
                boolean replace=world && matches(name,"setBlock","func_147465_d") && desc.equals("(III"+BLOCK+"II)Z");
                boolean anyLiquid=world && matches(name,"isAnyLiquid","func_72953_d");
                boolean materialQuery=world && (matches(name,"handleMaterialAcceleration","func_72918_a") || matches(name,"isMaterialInBB","func_72875_a") || matches(name,"isAABBInMaterial","func_72830_b"));
                boolean eye=entity && matches(name,"isInsideOfMaterial","func_70055_a");
                boolean ray=item && matches(name,"getMovingObjectPositionFromPlayer","func_77621_a");
                boolean bucketDispatch=bucket && matches(name,"onItemRightClick","func_77659_a");
                if (!liquid && !replace && !anyLiquid && !materialQuery && !eye && !ray && !bucketDispatch) return visitor;
                return new LocalVariablesSorter(Opcodes.ASM4,access,desc,visitor) {
                    private final List<Integer> captures=new ArrayList<>();
                    @Override protected void updateNewLocals(Object[] locals) {
                        // Captures live only inside a straight-line adapter, never across an original branch label.
                        for(int slot:captures) locals[slot]=Opcodes.TOP;
                    }
                    @Override public void visitCode() {
                        super.visitCode();
                        if (replace) {
                            loadPosition(); super.visitVarInsn(Opcodes.ALOAD,4); super.visitVarInsn(Opcodes.ILOAD,5);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"replacementMetadata","("+WORLD+"III"+BLOCK+"I)I"); super.visitVarInsn(Opcodes.ISTORE,5);
                            loadPosition(); super.visitVarInsn(Opcodes.ALOAD,4);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"replacement","("+WORLD+"III"+BLOCK+")"+BLOCK); super.visitVarInsn(Opcodes.ASTORE,4); writes[0]++;
                        }
                    }
                    private void loadPosition() { super.visitVarInsn(Opcodes.ALOAD,0); for (int local=1;local<=3;local++) super.visitVarInsn(Opcodes.ILOAD,local); }
                    @Override public void visitInsn(int opcode) {
                        if (replace && opcode==Opcodes.IRETURN) {
                            loadPosition(); super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"changedResult","(Z"+WORLD+"III)Z");
                        }
                        super.visitInsn(opcode);
                    }
                    @Override public void visitMethodInsn(int opcode,String owner,String method,String descriptor) {
                        if (bucketDispatch && owner.equals("cpw/mods/fml/common/eventhandler/EventBus") && method.equals("post")
                                && descriptor.equals("(Lcpw/mods/fml/common/eventhandler/Event;)Z")) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"postBucketEvent","(Lcpw/mods/fml/common/eventhandler/EventBus;Lcpw/mods/fml/common/eventhandler/Event;)Z");
                            reads[0]++; return;
                        }
                        boolean worldAccess=owner.equals("net/minecraft/world/World") || owner.equals("net/minecraft/world/IBlockAccess");
                        if ((opcode==Opcodes.INVOKEVIRTUAL || opcode==Opcodes.INVOKEINTERFACE) && worldAccess) {
                            if (ray && method.equals("func_147447_a") && descriptor.equals("(Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;ZZZ)Lnet/minecraft/util/MovingObjectPosition;")) {
                                super.visitVarInsn(Opcodes.ALOAD,0);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"bucketRay","("+WORLD+"Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;ZZZLnet/minecraft/item/Item;)Lnet/minecraft/util/MovingObjectPosition;");
                                reads[0]++; return;
                            }
                            String read=matches(method,"getBlock","func_147439_a") && descriptor.equals("(III)"+BLOCK)?"Block"
                                :matches(method,"getBlockMetadata","func_72805_g") && descriptor.equals("(III)I")?"Metadata":null;
                            if (read!=null && (liquid || anyLiquid || materialQuery || eye)) {
                                String result=read.equals("Block")?BLOCK:"I";
                                if (liquid) {
                                    super.visitVarInsn(Opcodes.ALOAD,0);
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,"flow"+read,"("+ACCESS+"III"+LIQUID+")"+result);
                                } else {
                                    // Keep the original callsite for HydroEnergy and other native-call redirects.
                                    int receiver=newLocal(Type.getObjectType(owner));
                                    int x=newLocal(Type.INT_TYPE),y=newLocal(Type.INT_TYPE),z=newLocal(Type.INT_TYPE);
                                    captures.add(receiver); captures.add(x); captures.add(y); captures.add(z);
                                    mv.visitVarInsn(Opcodes.ISTORE,z); mv.visitVarInsn(Opcodes.ISTORE,y); mv.visitVarInsn(Opcodes.ISTORE,x); mv.visitVarInsn(Opcodes.ASTORE,receiver);
                                    mv.visitVarInsn(Opcodes.ALOAD,receiver); mv.visitVarInsn(Opcodes.ILOAD,x); mv.visitVarInsn(Opcodes.ILOAD,y); mv.visitVarInsn(Opcodes.ILOAD,z);
                                    mv.visitMethodInsn(opcode,owner,method,descriptor);
                                    mv.visitVarInsn(Opcodes.ALOAD,receiver); mv.visitVarInsn(Opcodes.ILOAD,x); mv.visitVarInsn(Opcodes.ILOAD,y); mv.visitVarInsn(Opcodes.ILOAD,z);
                                    if(!anyLiquid) super.visitVarInsn(Opcodes.ALOAD,eye?1:2);
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,(anyLiquid?"physicsAny":"physics")+read,"("+result+ACCESS+"III"+(anyLiquid?"":MATERIAL)+")"+result);
                                }
                                reads[0]++; return;
                            }
                            String hook=null;
                            if (liquid) {
                                if (matches(method,"setBlock","func_147465_d") && descriptor.equals("(III"+BLOCK+"II)Z")) hook="flowSet";
                                else if (matches(method,"setBlock","func_147449_b") && descriptor.equals("(III"+BLOCK+")Z")) hook="flowSet";
                                else if (matches(method,"setBlockMetadataWithNotify","func_72921_c") && descriptor.equals("(IIIII)Z")) hook="flowMetadataSet";
                                else if (matches(method,"setBlockToAir","func_147468_f") && descriptor.equals("(III)Z")) hook="flowAir";
                                else if (matches(method,"scheduleBlockUpdate","func_147464_a") && descriptor.equals("(III"+BLOCK+"I)V")) hook="flowSchedule";
                            }
                            if (hook!=null) {
                                super.visitVarInsn(Opcodes.ALOAD,0);
                                int end=descriptor.indexOf(')');
                                super.visitMethodInsn(Opcodes.INVOKESTATIC,HOOK,hook,"("+WORLD+descriptor.substring(1,end)+LIQUID+descriptor.substring(end));
                                writes[0]++; return;
                            }
                        }
                        super.visitMethodInsn(opcode,owner,method,descriptor);
                    }
                };
            }
        },ClassReader.EXPAND_FRAMES);
        if (reads[0]==0 || world && writes[0]!=1 || liquid && writes[0]==0)
            throw new IllegalStateException("Missing 1.7.10 waterlogging hooks for "+transformedName+": reads="+reads[0]+",writes="+writes[0]);
        return writer.toByteArray();
    }
    /** Keep a path-search origin without leaking it on an exception or into ordinary world reads. */
    private static void scopeWrapper(MethodVisitor out,String owner,String implementation,String descriptor) {
        Type[] args=Type.getArgumentTypes(descriptor); Type result=Type.getReturnType(descriptor);
        int next=1; java.util.List<Object> frame=new ArrayList<>(); frame.add(owner);
        for (Type arg:args) { next+=arg.getSize(); frame.add(arg.getSort()==Type.OBJECT?arg.getInternalName():arg.getSort()==Type.ARRAY?arg.getDescriptor():Opcodes.INTEGER); }
        int resultSlot=next,exceptionSlot=next+(result.getSort()==Type.VOID?0:result.getSize());
        Label start=new Label(),end=new Label(),handler=new Label();
        out.visitCode();
        out.visitVarInsn(Opcodes.ALOAD,1); for(int local=2;local<=4;local++) out.visitVarInsn(Opcodes.ILOAD,local);
        out.visitMethodInsn(Opcodes.INVOKESTATIC,"org/mtr/mapping/tool/WaterloggingFlow","enter","("+WORLD+"III)V");
        out.visitTryCatchBlock(start,end,handler,null); out.visitLabel(start);
        out.visitVarInsn(Opcodes.ALOAD,0); int local=1;
        for(Type arg:args) { out.visitVarInsn(arg.getOpcode(Opcodes.ILOAD),local); local+=arg.getSize(); }
        out.visitMethodInsn(Opcodes.INVOKESPECIAL,owner,implementation,descriptor);
        if(result.getSort()!=Type.VOID) out.visitVarInsn(result.getOpcode(Opcodes.ISTORE),resultSlot);
        out.visitLabel(end); out.visitMethodInsn(Opcodes.INVOKESTATIC,"org/mtr/mapping/tool/WaterloggingFlow","leave","()V");
        if(result.getSort()!=Type.VOID) out.visitVarInsn(result.getOpcode(Opcodes.ILOAD),resultSlot);
        out.visitInsn(result.getOpcode(Opcodes.IRETURN));
        out.visitLabel(handler); out.visitFrame(Opcodes.F_FULL,frame.size(),frame.toArray(),1,new Object[]{"java/lang/Throwable"});
        out.visitVarInsn(Opcodes.ASTORE,exceptionSlot); out.visitMethodInsn(Opcodes.INVOKESTATIC,"org/mtr/mapping/tool/WaterloggingFlow","leave","()V");
        out.visitVarInsn(Opcodes.ALOAD,exceptionSlot); out.visitInsn(Opcodes.ATHROW); out.visitMaxs(0,0); out.visitEnd();
    }
    private static boolean matches(String method,String mcp,String srg) { return method.equals(mcp) || method.equals(srg); }
}
