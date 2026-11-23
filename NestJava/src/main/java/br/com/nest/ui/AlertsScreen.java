package br.com.nest.ui;

import br.com.nest.model.Alert;
import br.com.nest.model.ChildProfile;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Central de alertas gerados pelo Nest (limites diários excedidos). */
class AlertsScreen extends Screen {
    private final DefaultTableModel model = Ui.model("Quando", "Serviço", "Mensagem", "Situação");

    AlertsScreen(AppContext ctx) {
        super(ctx);
        add(Ui.title("Alertas e notificações"), BorderLayout.NORTH);
        add(new JScrollPane(Ui.widths(Ui.table(model), 120, 140, 600, 70)), BorderLayout.CENTER);
        JButton read = Ui.button("Marcar todos como lidos");
        read.addActionListener(e -> Ui.safe(this, () -> {
            ctx.service().markAllRead(requireChild().id());
            ctx.refreshAll();
        }));
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(Ui.muted("Alertas surgem quando o uso passa do limite diário definido em \"Configurações\"."), BorderLayout.CENTER);
        south.add(read, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);
    }

    @Override
    void refresh() {
        model.setRowCount(0);
        ChildProfile c = ctx.selectedChild();
        if (c == null) return;
        for (Alert a : ctx.service().listAlerts(c.id()))
            model.addRow(new Object[]{Ui.formatTs(a.createdAt()), a.platform(), a.message(), a.read() ? "Lido" : "Novo"});
    }
}
