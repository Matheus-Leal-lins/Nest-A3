package br.com.nest.ui;

import java.awt.Component;
import java.awt.Desktop;
import java.net.URI;

import javax.swing.JOptionPane;

import br.com.nest.model.Platforms;

/** Abre o site oficial do controle parental original (usado quando o Nest não consegue alterar a configuração). */
final class OriginalLinks {
    private OriginalLinks() {}

    static void open(Component parent, String platform) {
        String url = Platforms.OFFICIAL_URLS.getOrDefault(platform, "");
        if (url.isBlank()) {
            JOptionPane.showMessageDialog(parent, "Não há página oficial cadastrada para \"" + platform + "\".\n"
                    + "Abra o aplicativo do serviço diretamente.", "Serviço original", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception ignored) {
            // cai na mensagem abaixo
        }
        JOptionPane.showMessageDialog(parent, "Abra no navegador:\n" + url, "Serviço original", JOptionPane.INFORMATION_MESSAGE);
    }
}
