package org.mtr.mapping.tool;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.SimpleResource;
import org.apache.commons.io.IOUtils;
import java.io.InputStream;

@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
public final class LegacyResources {
    private LegacyResources() {}
    public static void close(IResource resource) {
        IOUtils.closeQuietly(resource.getInputStream());
        // Native IResource has no close method, but SimpleResource also owns an optional metadata stream.
        if (resource instanceof SimpleResource) IOUtils.closeQuietly((InputStream) ReflectionHelper.getPrivateValue(
            SimpleResource.class, (SimpleResource) resource, "mcmetaInputStream", "field_110531_d"));
    }
}
