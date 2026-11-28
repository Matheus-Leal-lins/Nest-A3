package br.com.nest.ui;


public final class Theme {
    private static boolean installed;

    private Theme() {}

    public static synchronized void install() {
        if (installed) return;
        Ui.installLookAndFeel();
        installed = true;
    }
}
