package org.mtr.mapping.gltest;

import org.objectweb.asm.ClassReader;
import java.io.IOException;
import java.util.Arrays;

/** Checks the interface-lambda regression before any native GL setup. */
public final class LegacyGlRedirectScenarios {
    public static void main(String[] args) throws IOException { run(); }

    public static void run() throws IOException {
        String name = "org.mtr.mapping.mapper.DirectionHelper";
        byte[] original = new ClassReader(name).b;
        byte[] transformed = new LegacyGlRedirect().transform(name, name, original);
        if (!Arrays.equals(original, transformed))
            throw new AssertionError("Classes without LWJGL references must preserve interface-lambda bytecode");
        System.out.println("Legacy GL redirect preserves interface-lambda bytecode");
    }
}
