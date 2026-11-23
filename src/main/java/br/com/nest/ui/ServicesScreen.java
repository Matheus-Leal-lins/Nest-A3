package br.com.nest.ui;

import br.com.nest.model.ChildProfile;
import br.com.nest.model.PlatformLink;
import br.com.nest.model.Platforms;
import br.com.nest.service.DemoService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Serviços vinculados: vincular, desvincular, sincronizar (simulado) e abrir o serviço original. */
class ServicesScreen extends Screen {
    private final DefaultTableModel model = Ui.model("Serviço", "Conta (rótulo)", "Status", "Última sincronização");
    private final JTable table = Ui.table(model);
    private List<PlatformLink> links = new ArrayList<>();

    ServicesScreen(AppContext ctx) {
        super(ctx);
        add(Ui.title("Serviços vinculados"), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton link = Ui.primaryButton("Vincular serviço");
        JButton unlink = Ui.button("Desvincular");
        JButton sync = Ui.button("Sincronizar (simulado)");
        JButton open = Ui.button("Abrir serviço original");
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
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setOpaque(false);
        south.add(bar, BorderLayout.NORTH);
        south.add(Ui.muted("<html>Vínculos são registros locais: o Nest <b>não</b> pede senhas e ainda não se conecta "
                + "às APIs de Google, Apple ou Nintendo. A sincronização gera dados de demonstração.</html>"), BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
    }

    private void addLink() {
        ChildProfile c = requireChild();
        JComboBox<String> platform = new JComboBox<>(Platforms.names());
        JTextField label = new JTextField(20);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Serviço:"));
        form.add(platform);
        form.add(new JLabel("Rótulo da conta (ex.: e-mail ou apelido; NÃO informe senha):"));
        form.add(label);
        if (JOptionPane.showConfirmDialog(this, form, "Vincular serviço a " + c.name(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        ctx.service().linkPlatform(c.id(), (String) platform.getSelectedItem(), label.getText());
        ctx.refreshAll();
    }

    private PlatformLink selectedLink() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= links.size()) throw new IllegalArgumentException("Selecione um serviço na tabela.");
        return links.get(row);
    }

    private void removeLink() {
        PlatformLink l = selectedLink();
        if (JOptionPane.showConfirmDialog(this, "Remover o vínculo com " + l.platform() + "?\nO histórico de uso é mantido.",
                "Desvincular", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        ctx.service().unlink(l.id());
        ctx.refreshAll();
    }

    private void sync() {
        ChildProfile c = requireChild();
        int n = new DemoService(ctx.service()).sync(c.id());
        if (n == 0) throw new IllegalArgumentException("Vincule ao menos um serviço antes de sincronizar.");
        ctx.refreshAll();
        JOptionPane.showMessageDialog(this, n + " serviço(s) atualizados com dados SIMULADOS dos últimos 7 dias.",
                "Sincronização simulada", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openOriginal() {
        OriginalLinks.open(this, selectedLink().platform());
    }

    @Override
    void refresh() {
        model.setRowCount(0);
        ChildProfile c = ctx.selectedChild();
        links = c == null ? new ArrayList<>() : ctx.service().listLinks(c.id());
        for (PlatformLink l : links)
            model.addRow(new Object[]{l.platform(), l.accountLabel(), l.status(), Ui.formatTs(l.lastSync())});
    }
}
