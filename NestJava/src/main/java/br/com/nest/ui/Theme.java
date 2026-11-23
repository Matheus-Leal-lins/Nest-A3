package br.com.nest.ui;

/** Ponto de entrada público para aplicar o visual do Nest (as demais classes de UI são internas ao pacote). */
public final class Theme {
    private static boolean installed;

    private Theme() {}

    public static synchronized void install() {
        if (installed) return;
        Ui.installLookAndFeel();
        installed = true;
    }
}
