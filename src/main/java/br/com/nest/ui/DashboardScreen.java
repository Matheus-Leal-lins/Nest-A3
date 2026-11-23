package br.com.nest.ui;

import br.com.nest.model.Alert;
import br.com.nest.model.ChildProfile;
import br.com.nest.model.PlatformLink;
import br.com.nest.model.Platforms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Tela inicial: resumo do perfil selecionado (uso, serviços e alertas). */
class DashboardScreen extends Screen {
    private final JLabel today = value(), week = value(), avg = value(), alerts = value();
    private final DefaultTableModel services = Ui.model("Serviço", "Conta", "Status", "Última sincronização");
    private final DefaultTableModel recent = Ui.model("Quando", "Alerta");
    private final BarChart chart = new BarChart(Ui.PRIMARY);
    private final JLabel hint = Ui.muted(" ");

    DashboardScreen(AppContext ctx) {
        super(ctx);
        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
        cards.setOpaque(false);
        cards.add(stat("Uso hoje", today));
        cards.add(stat("Últimos 7 dias", week));
        cards.add(stat("Média diária (7 dias)", avg));
        cards.add(stat("Alertas não lidos", alerts));

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(cards, BorderLayout.CENTER);
        top.add(hint, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 0));
        center.setOpaque(false);
        center.add(Ui.card("Uso nos últimos 7 dias (min)", chart));
        JPanel right = new JPanel(new GridLayout(2, 1, 0, 10));
        right.setOpaque(false);
        right.add(Ui.card("Serviços vinculados", new JScrollPane(Ui.table(services))));
        right.add(Ui.card("Alertas recentes", new JScrollPane(Ui.widths(Ui.table(recent), 120, 500))));
        center.add(right);
        add(center, BorderLayout.CENTER);
    }

    private static JLabel value() {
        JLabel l = new JLabel("—");
        l.setFont(l.getFont().deriveFont(Font.BOLD, 26f));
        l.setForeground(Ui.PRIMARY_DARK);
        return l;
    }

    private static JPanel stat(String label, JLabel value) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(value, BorderLayout.CENTER);
        return Ui.card(null, wrap(label, p));
    }

    private static JComponent wrap(String label, JComponent body) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(Ui.muted(label), BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    @Override
    void refresh() {
        services.setRowCount(0);
        recent.setRowCount(0);
        ChildProfile c = ctx.selectedChild();
        if (c == null) {
            today.setText("—");
            week.setText("—");
            avg.setText("—");
            alerts.setText("—");
            chart.setData(null);
            hint.setText("Nenhum perfil cadastrado. Use \"+ Perfil\" ou \"Dados de demonstração\" no topo.");
            return;
        }
        LocalDate d = LocalDate.now();
        Map<LocalDate, Integer> days = ctx.service().minutesByDay(c.id(), d.minusDays(6), d);
        int total = days.values().stream().mapToInt(Integer::intValue).sum();
        today.setText(Ui.minutes(ctx.service().totalMinutes(c.id(), d)));
        week.setText(Ui.minutes(total));
        avg.setText(Ui.minutes(total / 7));
        int unread = ctx.service().unreadAlerts(c.id());
        alerts.setText(String.valueOf(unread));
        alerts.setForeground(unread > 0 ? Ui.DANGER : Ui.PRIMARY_DARK);

        Map<String, Integer> labeled = new java.util.LinkedHashMap<>();
        days.forEach((k, v) -> labeled.put(k.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")), v));
        chart.setData(labeled);

        List<PlatformLink> links = ctx.service().listLinks(c.id());
        for (PlatformLink l : links)
            services.addRow(new Object[]{l.platform(), l.accountLabel(), l.status(), Ui.formatTs(l.lastSync())});
        int shown = 0;
        for (Alert a : ctx.service().listAlerts(c.id())) {
            if (shown++ == 6) break;
            recent.addRow(new Object[]{Ui.formatTs(a.createdAt()), a.message()});
        }
        hint.setText(links.isEmpty()
                ? "Nenhum serviço vinculado a " + c.name() + ". Vá em \"Serviços\" para vincular "
                + Platforms.GOOGLE + ", " + Platforms.APPLE + " ou " + Platforms.NINTENDO + "."
                : "Dados marcados como SIMULADO ou MANUAL: o MVP não consulta os fabricantes.");
    }
}
