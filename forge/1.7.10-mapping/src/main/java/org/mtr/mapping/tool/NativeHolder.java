package org.mtr.mapping.tool;

import java.util.Objects;

/** Native object identity must survive new wrappers created for each Forge callback. */
public abstract class NativeHolder<T> extends HolderBase<T> {
    protected NativeHolder(T data) { super(data); }
    @Override public final boolean equals(Object other) {
        return other instanceof NativeHolder && Objects.equals(data, ((NativeHolder<?>) other).data);
    }
    @Override public final int hashCode() { return Objects.hashCode(data); }
}
