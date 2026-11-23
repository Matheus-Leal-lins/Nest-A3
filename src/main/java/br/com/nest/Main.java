package br.com.nest;

import br.com.nest.model.Guardian;
import br.com.nest.ui.LoginFrame;
import br.com.nest.ui.NestFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::showLogin);
    }

    private static void showLogin() {
        br.com.nest.ui.Theme.install();
        new LoginFrame(Main::showApp).setVisible(true);
    }

    private static void showApp(Guardian guardian) {
        new NestFrame(guardian, Main::showLogin).setVisible(true);
    }
}
