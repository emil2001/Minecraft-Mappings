package org.mtr.mapping.holder;

import org.mtr.mapping.tool.DummyClass;
import java.awt.Desktop;
import java.io.File;
import java.net.URI;

public final class Util {
    private static final OperatingSystem operatingSystem=new OperatingSystem();
    private Util() {}
    public static OperatingSystem getOperatingSystem() { return operatingSystem; }
    public static final class OperatingSystem {
        private OperatingSystem() {}
        public void open(String url) {
            try { Desktop.getDesktop().browse(URI.create(url)); }
            catch (Exception exception) { DummyClass.logException(exception); }
        }
        public void open(File file) {
            try { Desktop.getDesktop().open(file); }
            catch (Exception exception) { DummyClass.logException(exception); }
        }
    }
}
