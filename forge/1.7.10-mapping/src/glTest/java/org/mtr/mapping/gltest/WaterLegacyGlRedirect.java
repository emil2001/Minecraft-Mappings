package org.mtr.mapping.gltest;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

/** Uses the installed LWJGL3ify compatibility classes, as its production RFB redirect does. */
public final class WaterLegacyGlRedirect implements IClassTransformer {
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || name.startsWith("org.lwjgl") || name.startsWith("org.mtr.mapping.gltest.")) return bytes;
        // ASM5's legacy remapper rewrites interface lambda handles incorrectly even when no type changes.
        // Unrelated classes require no LWJGL remapping and must remain byte-for-byte unchanged.
        if (!new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1).contains("org/lwjgl/")) return bytes;
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytes).accept(new RemappingClassAdapter(writer, new Remapper() {
            @Override public String map(String internalName) {
                if (!internalName.startsWith("org/lwjgl/")) return internalName;
                String shim = "org/lwjglx/" + internalName.substring("org/lwjgl/".length());
                return getClass().getClassLoader().getResource(shim + ".class") == null ? internalName : shim;
            }
        }), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }
}
