package org.mtr.mapping.holder;
import org.mtr.mapping.tool.HolderBase;

public final class Matrix4f extends HolderBase<org.lwjgl.util.vector.Matrix4f> {
    public Matrix4f() { super(new org.lwjgl.util.vector.Matrix4f()); }
    public Matrix4f(org.lwjgl.util.vector.Matrix4f data) { super(data); }
    public Matrix4f(Matrix4f other) { super(new org.lwjgl.util.vector.Matrix4f(other.data)); }
}
