package org.mtr.mapping.mapper;

import org.lwjgl.util.vector.Matrix4f;
import org.mtr.mapping.tool.LegacyShaderHooks;
import java.util.*;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class OptimizedRenderer {
    private final List<Job> jobs=new ArrayList<>();
    // CPU model compilation does not touch GL state during resource reload.
    public void beginReload() {}
    public void finishReload() {}
    public static boolean hasOptimizedRendering() { return true; }
    public static boolean renderingShadows() { return LegacyShaderHooks.isRenderingShadowPass(); }
    public void queue(OptimizedModel model,GraphicsHolder graphics,int color,int light) {
        jobs.add(new Job(Objects.requireNonNull(model),graphics,graphics.copyPositionMatrix().data,color,light));
    }
    public void render(boolean renderTranslucent) {
        if (jobs.isEmpty()) return;
        try {
            jobs.forEach(job -> job.graphics.flush());
            Map<OptimizedModel.Material,List<Draw>> batches=new LinkedHashMap<>();
            for (Job job : jobs) for (OptimizedModel.Mesh mesh : job.model.meshes)
                batches.computeIfAbsent(mesh.material,key -> new ArrayList<>()).add(new Draw(mesh,job));
            GraphicsHolder.createInstanceSafe(graphics -> {
                for (int pass=0;pass<(renderTranslucent?3:2);pass++) for (Map.Entry<OptimizedModel.Material,List<Draw>> batch : batches.entrySet()) {
                    OptimizedModel.Material material=batch.getKey();
                    if ((material.shader.translucent?2:material.shader.glowing?1:0)!=pass) continue;
                    graphics.createVertexConsumer(material.layer());
                    for (Draw draw : batch.getValue()) {
                        graphics.matrices.set(draw.job.matrix);
                        int color=multiply(material.color,draw.job.color), light=material.shader.bright?GraphicsHolder.getDefaultLight():draw.job.light;
                        for (float[] face : draw.mesh.faces) graphics.drawMeshFace(face,color,light);
                    }
                }
            });
        } finally { jobs.clear(); }
    }
    private static int multiply(int a,int b) {
        int color=0;
        for (int shift=0;shift<32;shift+=8) color|=((a>>>shift&255)*(b>>>shift&255)/255)<<shift;
        return color;
    }
    private static final class Job {
        final OptimizedModel model; final GraphicsHolder graphics; final Matrix4f matrix; final int color,light;
        Job(OptimizedModel model,GraphicsHolder graphics,Matrix4f matrix,int color,int light) { this.model=model; this.graphics=graphics; this.matrix=matrix; this.color=color; this.light=light; }
    }
    private static final class Draw {
        final OptimizedModel.Mesh mesh; final Job job;
        Draw(OptimizedModel.Mesh mesh,Job job) { this.mesh=mesh; this.job=job; }
    }
}
