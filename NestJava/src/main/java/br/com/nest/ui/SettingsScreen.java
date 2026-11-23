package br.com.nest.ui;

import br.com.nest.model.ChildProfile;
import br.com.nest.model.PlatformLink;
import br.com.nest.model.Platforms;
import br.com.nest.model.SettingRequest;
import br.com.nest.service.NestService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Configurações parentais (simuladas), atalhos para os serviços originais e nota de privacidade. */
class SettingsScreen extends Screen {
    private final JComboBox<String> platformBox = new JComboBox<>();
    private final JComboBox<String> settingBox =
            new JComboBox<>(new String[]{NestService.SETTING_DAILY_LIMIT, NestService.SETTING_QUIET_HOURS});
    private final JTextField value = new JTextField(14);
    private final JLabel hint = Ui.muted(" ");
    private final DefaultTableModel model = Ui.model("Quando", "Serviço", "Configuração", "Valor", "Status");

    SettingsScreen(AppContext ctx) {
        super(ctx);
        add(Ui.title("Configurações"), BorderLayout.NORTH);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setOpaque(false);
        form.add(new JLabel("Serviço:"));
        form.add(platformBox);
        form.add(new JLabel("Configuração:"));
        form.add(settingBox);
        form.add(new JLabel("Valor:"));
        form.add(value);
        JButton send = Ui.primaryButton("Solicitar alteração (simulada)");
        send.addActionListener(e -> Ui.safe(this, this::request));
        form.add(send);
        settingBox.addActionListener(e -> updateHint());

        JPanel requestBox = new JPanel(new BorderLayout(0, 6));
        requestBox.setOpaque(false);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(form, BorderLayout.NORTH);
        head.add(hint, BorderLayout.SOUTH);
        requestBox.add(head, BorderLayout.NORTH);
        requestBox.add(new JScrollPane(Ui.table(model)), BorderLayout.CENTER);

        JPanel originals = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        originals.setOpaque(false);
        for (String p : Platforms.names()) {
            if (Platforms.OFFICIAL_URLS.get(p).isBlank()) continue;
            JButton b = Ui.button("Abrir " + p);
            b.addActionListener(e -> OriginalLinks.open(this, p));
            originals.add(b);
        }
        JPanel privacy = new JPanel(new BorderLayout(0, 6));
        privacy.setOpaque(false);
        privacy.add(originals, BorderLayout.NORTH);
        JLabel note = Ui.muted("<html><b>Privacidade:</b> os dados ficam apenas neste computador (arquivo nest.db). "
                + "O Nest não guarda senhas de serviços externos. Use somente dados fictícios neste protótipo — "
                + "dados reais de menores exigem consentimento, criptografia e adequação à LGPD/ECA Digital.</html>");
        privacy.add(note, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(Ui.card("Solicitar alteração de configuração", requestBox), BorderLayout.CENTER);
        body.add(Ui.card("Gerenciar no serviço original", privacy), BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
        updateHint();
    }

    private void updateHint() {
        boolean limit = NestService.SETTING_DAILY_LIMIT.equals(settingBox.getSelectedItem());
        hint.setText(limit
                ? "Ex.: 90. O limite fica salvo no Nest e gera alertas quando excedido; NÃO é enviado ao fabricante. "
                + "Para aplicar no serviço, use o botão de abrir o serviço original."
                : "Ex.: 21:00-07:00. A solicitação é apenas registrada (SIMULADA); nada é enviado ao fabricante.");
    }

    private void request() {
        ChildProfile c = requireChild();
        String platform = (String) platformBox.getSelectedItem();
        ctx.service().requestSetting(c.id(), platform, (String) settingBox.getSelectedItem(), value.getText());
        value.setText("");
        ctx.refreshAll();
    }

    @Override
    void refresh() {
        model.setRowCount(0);
        platformBox.removeAllItems();
        ChildProfile c = ctx.selectedChild();
        if (c == null) return;
        platformBox.addItem(NestService.ALL);
        List<PlatformLink> links = ctx.service().listLinks(c.id());
        links.forEach(l -> platformBox.addItem(l.platform()));
        for (SettingRequest r : ctx.service().listRequests(c.id()))
            model.addRow(new Object[]{Ui.formatTs(r.createdAt()), r.platform(), r.setting(), r.value(), r.status()});
    }
}
