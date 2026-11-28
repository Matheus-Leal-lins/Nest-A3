package br.com.nest.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import br.com.nest.model.ChildProfile;
import br.com.nest.model.PlatformLink;
import br.com.nest.model.Platforms;
import br.com.nest.model.SettingRequest;
import br.com.nest.service.NestService;


class SettingsScreen extends Screen {
    private final JComboBox<String> platformBox = new JComboBox<>();
    private final JComboBox<String> settingBox =
            new JComboBox<>(new String[]{NestService.SETTING_DAILY_LIMIT, NestService.SETTING_QUIET_HOURS});
    private final JTextField value = new JTextField(14);
    private final JLabel hint = Ui.muted(" ");
    private final DefaultTableModel model = Ui.model("Quando", "Serviço", "Configuração", "Valor", "Status");

    SettingsScreen(AppContext ctx) {
        super(ctx);
        Ui.styleInput(value);
        add(Ui.title("Configurações"), BorderLayout.NORTH);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setOpaque(false);
        form.add(Ui.fieldLabel("Serviço"));
        form.add(platformBox);
        form.add(Ui.fieldLabel("Regra"));
        form.add(settingBox);
        form.add(Ui.fieldLabel("Valor"));
        form.add(value);

        JButton send = Ui.primaryButton("Salvar solicitação");
        send.addActionListener(e -> Ui.safe(this, this::request));
        form.add(send);
        settingBox.addActionListener(e -> updateHint());

        JPanel requestBox = new JPanel(new BorderLayout(0, 8));
        requestBox.setOpaque(false);
        JPanel head = new JPanel(new BorderLayout(0, 4));
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

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(Ui.card("Limites e horários", requestBox), BorderLayout.CENTER);
        body.add(Ui.card("Serviços originais", originals), BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
        updateHint();
    }

    private void updateHint() {
        boolean limit = NestService.SETTING_DAILY_LIMIT.equals(settingBox.getSelectedItem());
        hint.setText(limit
                ? "Ex.: 90 (minutos por dia). Gera alerta ao ultrapassar."
                : "Ex.: 21:00-07:00 (horário de descanso).");
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
        for (SettingRequest r : ctx.service().listRequests(c.id())) {
            model.addRow(new Object[]{Ui.formatTs(r.createdAt()), r.platform(), r.setting(), r.value(), r.status()});
        }
    }
}