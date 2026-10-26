package org.mtr.mapping.tool;

import org.lwjgl.util.vector.*;
import java.util.ArrayDeque;
import java.util.Deque;

public final class LegacyMatrices {
    private final Deque<Entry> stack = new ArrayDeque<>();
    public LegacyMatrices() { stack.push(new Entry(new Matrix4f(), null)); }
    public void push() { Entry entry = stack.peek(); stack.push(new Entry(new Matrix4f(entry.position), entry.normal)); }
    public void pop() {
        if (stack.size() == 1) throw new IllegalStateException("Cannot pop the root transform");
        stack.pop();
    }
    public Matrix4f position() { return stack.peek().position; }
    public void set(Matrix4f matrix) { position().load(matrix); stack.peek().normal=null; }
    public void translate(double x, double y, double z) { Matrix4f.translate(new Vector3f((float)x, (float)y, (float)z), position(), position()); }
    public void scale(float x, float y, float z) { Matrix4f.scale(new Vector3f(x, y, z), position(), position()); stack.peek().normal = null; }
    public void rotate(float radians, float x, float y, float z) {
        Matrix4f.rotate(radians, new Vector3f(x, y, z), position(), position()); stack.peek().normal = null;
    }
    public Vector3f normal(float x, float y, float z) {
        Entry entry = stack.peek();
        if (entry.normal == null) {
            entry.normal = Matrix4f.invert(entry.position, null);
            if (entry.normal == null) return new Vector3f();
            entry.normal.transpose();
        }
        Vector4f normal = Matrix4f.transform(entry.normal, new Vector4f(x, y, z, 0), null);
        Vector3f result = new Vector3f(normal.x, normal.y, normal.z);
        return result.lengthSquared() == 0 ? result : result.normalise(result);
    }
    private static final class Entry {
        final Matrix4f position;
        Matrix4f normal;
        Entry(Matrix4f position, Matrix4f normal) { this.position = position; this.normal = normal; }
    }
}
