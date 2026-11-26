package br.com.nest.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import br.com.nest.model.ChildProfile;
import br.com.nest.model.PlatformLink;
import br.com.nest.model.Platforms;
import br.com.nest.service.DemoService;

class ServicesScreen extends Screen {
    private static final Map<String, String> SPECS = Map.of(
            Platforms.GOOGLE, "Tempo de tela, apps e compras (Android / Google Play)",
            Platforms.APPLE, "Tempo de Uso, restrições e compras (iOS / App Store)",
            Platforms.NINTENDO, "Tempo de jogo diário e faixa etária (Nintendo Switch)"
    );

    private final DefaultTableModel model = Ui.model("Serviço", "Escopo", "Conta", "Status", "Sincronização");
    private final JTable table = Ui.table(model);
    private final JLabel detail = Ui.muted("Selecione um serviço para ver os detalhes.");
    private List<PlatformLink> links = new ArrayList<>();

    ServicesScreen(AppContext ctx) {
        super(ctx);
        add(Ui.title("Serviços"), BorderLayout.NORTH);

        Ui.widths(table, 120, 320, 170, 95, 135);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateDetail();
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton link = Ui.primaryButton("Vincular");
        JButton unlink = Ui.button("Desvincular");
        JButton sync = Ui.button("Sincronizar");
        JButton open = Ui.button("Abrir original");

        link.addActionListener(e -> Ui.safe(this, this::addLink));
        unlink.addActionListener(e -> Ui.safe(this, this::removeLink));
        sync.addActionListener(e -> Ui.safe(this, this::sync));
        open.addActionListener(e -> Ui.safe(this, this::openOriginal));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(link);
        bar.add(unlink);
        bar.add(sync);
        bar.add(open);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(Ui.card(null, detail), BorderLayout.NORTH);
        south.add(bar, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
    }

    private static String specOf(String platform) {
        return SPECS.getOrDefault(platform, "Controle de tempo e uso da plataforma.");
    }

    private void updateDetail() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= links.size()) {
            detail.setText("Selecione um serviço para ver o que ele cobre.");
            return;
        }
        PlatformLink l = links.get(row);
        detail.setText(l.platform() + " (" + l.accountLabel() + "): " + specOf(l.platform()));
    }

    private void addLink() {
        ChildProfile c = requireChild();
        JComboBox<String> platform = new JComboBox<>(Platforms.names());
        Ui.styleComboBox(platform);
        JLabel specPreview = Ui.muted(specOf((String) platform.getSelectedItem()));
        platform.addActionListener(e -> specPreview.setText(specOf((String) platform.getSelectedItem())));

        JTextField label = new JTextField(20);
        Ui.styleInput(label);

        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.add(Ui.fieldLabel("Serviço"));
        form.add(platform);
        form.add(specPreview);
        form.add(Ui.fieldLabel("Rótulo da conta (e-mail ou apelido)"));
        form.add(label);
        Ui.applyTheme(form);

        if (JOptionPane.showConfirmDialog(this, form, "Vincular serviço",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        ctx.service().linkPlatform(c.id(), (String) platform.getSelectedItem(), label.getText());
        ctx.refreshAll();
        Ui.applyTheme(ctx.frame());
    }

    private PlatformLink selectedLink() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= links.size()) throw new IllegalArgumentException("Selecione um serviço na tabela.");
        return links.get(row);
    }

    private void removeLink() {
        PlatformLink l = selectedLink();
        if (JOptionPane.showConfirmDialog(this, "Remover vínculo com " + l.platform() + "?",
                "Desvincular", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        ctx.service().unlink(l.id());
        ctx.refreshAll();
        Ui.applyTheme(ctx.frame());
    }

    private void sync() {
        ChildProfile c = requireChild();
        int n = new DemoService(ctx.service()).sync(c.id());
        if (n == 0) throw new IllegalArgumentException("Vincule ao menos um serviço antes de sincronizar.");
        ctx.refreshAll();
        Ui.applyTheme(ctx.frame());
    }

    private void openOriginal() {
        OriginalLinks.open(this, selectedLink().platform());
    }

    @Override
    void refresh() {
        model.setRowCount(0);
        ChildProfile c = ctx.selectedChild();
        links = c == null ? new ArrayList<>() : ctx.service().listLinks(c.id());
        for (PlatformLink l : links) {
            model.addRow(new Object[]{l.platform(), specOf(l.platform()), l.accountLabel(), l.status(), Ui.formatTs(l.lastSync())});
        }
        updateDetail();
    }
}