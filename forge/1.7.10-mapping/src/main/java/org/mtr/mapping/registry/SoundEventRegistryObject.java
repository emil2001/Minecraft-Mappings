package org.mtr.mapping.registry;
import org.mtr.mapping.holder.SoundEvent;
import org.mtr.mapping.tool.RegistryObject;
import java.util.function.Consumer;

public final class SoundEventRegistryObject extends RegistryObject<SoundEvent> {
    SoundEvent value;
    public SoundEvent get() {
        if (value == null) throw new IllegalStateException("Sound registration has not run");
        return value;
    }
    public boolean isPresent() { return value != null; }
    public void ifPresent(Consumer<SoundEvent> consumer) { if (isPresent()) consumer.accept(value); }
}
