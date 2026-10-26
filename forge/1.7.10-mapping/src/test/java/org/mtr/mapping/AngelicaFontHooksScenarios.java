package org.mtr.mapping;

import com.gtnewhorizons.angelica.client.font.BatchingFontRenderer;
import com.gtnewhorizons.angelica.client.font.FontProvider;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.mtr.mapping.compat.angelica.AngelicaTextSupport;
import org.mtr.mapping.core.StyledTextTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.invoke.MethodHandles;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;

public class AngelicaFontHooksScenarios {
    private static final String STRATEGIST = "com.gtnewhorizons.angelica.client.font.FontStrategist";
    private static final String BATCHER = "com.gtnewhorizons.angelica.client.font.BatchingFontRenderer";
    private static final ResourceLocation MTR = new ResourceLocation("mtr:mtr");

    public void fontScopeRestoresAfterNestedNativeDrawAndExceptionAndSpacesUseTheSelectedFont() throws Exception {
        try (NativeClientFixture fixture = resources()) {
            assertNull(AngelicaTextSupport.provider('M'));
            assertEquals(2.5F, AngelicaTextSupport.whitespace(2.5F));
            AngelicaTextSupport.withFont(MTR, () -> {
                FontProvider selected = AngelicaTextSupport.provider('M');
                assertEquals(11.125F, selected.getXAdvance('M'), 0.001F);
                FontProvider space = AngelicaTextSupport.provider(' ');
                assertEquals(space.getXAdvance(' ') / 4, AngelicaTextSupport.whitespace(2.5F), 0.001F);
                AngelicaTextSupport.withFont(null, () -> {
                    assertNull(AngelicaTextSupport.provider('M'));
                    assertEquals(2.5F, AngelicaTextSupport.whitespace(2.5F));
                    return null;
                });
                assertSame(selected, AngelicaTextSupport.provider('M'));
                assertThrows(IllegalArgumentException.class, () -> AngelicaTextSupport.withFont(null, () -> { throw new IllegalArgumentException("fixture"); }));
                assertSame(selected, AngelicaTextSupport.provider('M'));
                return null;
            });
            assertNull(AngelicaTextSupport.provider('M'), "The next unrelated Minecraft label must retain its native provider");
            assertEquals(2.5F, AngelicaTextSupport.whitespace(2.5F));
        }
    }

    public void actualPinnedStrategistBytecodeReturnsTheSelectedProviderAndOtherwiseKeepsItsOriginalBranch() throws Exception {
        try (NativeClientFixture fixture = resources()) {
            byte[] transformed = new StyledTextTransformer().transform(STRATEGIST, STRATEGIST, pinned(STRATEGIST));
            // A normal development launch remaps SRG Minecraft names to MCP; reproduce just these two references here.
            ClassWriter writer = new ClassWriter(0);
            new ClassReader(transformed).accept(new ClassVisitor(Opcodes.ASM5, writer) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                        @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean itf) {
                            super.visitMethodInsn(opcode, owner, owner.equals("net/minecraft/client/Minecraft") && name.equals("func_71410_x") ? "getMinecraft" : name, descriptor, itf);
                        }
                        @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                            super.visitFieldInsn(opcode, owner, owner.equals("net/minecraft/client/Minecraft") && name.equals("field_71412_D") ? "mcDataDir" : name, descriptor);
                        }
                    };
                }
            }, 0);
            ClassWriter renamed = new ClassWriter(0);
            new ClassReader(writer.toByteArray()).accept(new RemappingClassAdapter(renamed, new Remapper() {
                @Override public String map(String name) { return name.equals(STRATEGIST.replace('.', '/')) ? name + "$MtrFontFixture" : name; }
            }), ClassReader.EXPAND_FRAMES);
            // Keep the actual runtime package: the native branch reads package-private BatchingFontRenderer fields.
            Object lookup = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class)
                .invoke(null, FontProvider.class, MethodHandles.lookup());
            Class<?> transformedClass = (Class<?>) MethodHandles.Lookup.class.getMethod("defineClass", byte[].class)
                .invoke(lookup, (Object) renamed.toByteArray());
            Method select = transformedClass.getDeclaredMethod("getFontProvider", BatchingFontRenderer.class, char.class, boolean.class, boolean.class);
            FontProvider chosen = AngelicaTextSupport.withFont(MTR, () -> {
                try { return (FontProvider) select.invoke(null, null, 'M', false, false); }
                catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
            });
            assertEquals(11.125F, chosen.getXAdvance('M'), 0.001F);
            InvocationTargetException nativeBranch = assertThrows(InvocationTargetException.class, () -> select.invoke(null, null, 'M', false, false));
            assertInstanceOf(NullPointerException.class, nativeBranch.getCause(), "Outside our font scope the original method must still dereference its native batcher");
        }
    }

    public void pinnedWhitespaceMethodHooksEveryReturnExactlyOnce() throws Exception {
        byte[] transformed = new StyledTextTransformer().transform(BATCHER, BATCHER, pinned(BATCHER));
        int[] hooks = {0}, returns = {0};
        new ClassReader(transformed).accept(new ClassVisitor(Opcodes.ASM5) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (!name.equals("getWhitespaceScale") || !descriptor.equals("()F")) return null;
                return new MethodVisitor(Opcodes.ASM5) {
                    boolean hook;
                    @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean itf) {
                        hook = owner.equals("org/mtr/mapping/compat/angelica/AngelicaTextSupport") && name.equals("whitespace") && descriptor.equals("(F)F");
                        if (hook) hooks[0]++;
                    }
                    @Override public void visitInsn(int opcode) {
                        if (opcode == Opcodes.FRETURN) { assertTrue(hook, "Every native float return must pass through the font-aware space width"); returns[0]++; }
                        hook = false;
                    }
                };
            }
        }, 0);
        assertEquals(1, returns[0]); assertEquals(returns[0], hooks[0]);
    }

    private static NativeClientFixture resources() throws Exception {
        NativeClientFixture fixture = new NativeClientFixture();
        SimpleReloadableResourceManager resources = new SimpleReloadableResourceManager(new IMetadataSerializer());
        Path assets = Paths.get(System.getProperty("mtr.modelResources")).getParent().getParent().getParent();
        resources.reloadResourcePack(new FolderResourcePack(assets.toFile()));
        ReflectionHelper.setPrivateValue(Minecraft.class, fixture.client, resources, "mcResourceManager", "field_110451_am");
        ReflectionHelper.setPrivateValue(Minecraft.class, fixture.client, assets.toFile(), "mcDataDir", "field_71412_D");
        return fixture;
    }
    private static byte[] pinned(String className) throws Exception {
        URL source = FontProvider.class.getProtectionDomain().getCodeSource().getLocation();
        if (source.getProtocol().equals("jar")) source = ((JarURLConnection) source.openConnection()).getJarFileURL();
        Path client = Paths.get(source.toURI());
        try (ZipFile zip = new ZipFile(client.toFile()); InputStream input = zip.getInputStream(zip.getEntry(className.replace('.', '/') + ".class")); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }
}
