package br.com.nest.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

import br.com.nest.model.Alert;
import br.com.nest.model.ChildProfile;
import br.com.nest.model.PlatformLink;

class DashboardScreen extends Screen {
    private final JLabel profileTitle = new JLabel("Nenhum perfil selecionado");
    private final JLabel profileSub = Ui.muted("Selecione ou crie um perfil para acompanhar o uso.");

    private final JLabel today = statValue(), week = statValue(), avg = statValue(), alerts = statValue();
    private final DefaultTableModel services = Ui.model("Serviço", "Conta", "Status", "Sincronização");
    private final DefaultTableModel recent = Ui.model("Quando", "Alerta");
    private final BarChart chart = new BarChart(Ui.primary());

    DashboardScreen(AppContext ctx) {
        super(ctx);
        alerts.putClientProperty("nest.statSpecial", Boolean.TRUE);

        profileTitle.setFont(profileTitle.getFont().deriveFont(Font.BOLD, 16f));
        profileTitle.setForeground(Ui.text());

        JPanel headerInfo = new JPanel(new GridLayout(0, 1, 0, 2));
        headerInfo.setOpaque(false);
        headerInfo.add(profileTitle);
        headerInfo.add(profileSub);

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        cards.add(statCard("Hoje", today));
        cards.add(statCard("7 dias", week));
        cards.add(statCard("Média diária", avg));
        cards.add(statCard("Alertas", alerts));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(Ui.card(null, headerInfo), BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));
        center.setOpaque(false);
        center.add(Ui.card("Uso diário (min)", chart));

        JPanel right = new JPanel(new GridLayout(2, 1, 0, 12));
        right.setOpaque(false);
        right.add(Ui.card("Serviços", new JScrollPane(Ui.table(services))));
        right.add(Ui.card("Alertas recentes", new JScrollPane(Ui.widths(Ui.table(recent), 125, 480))));
        center.add(right);

        add(center, BorderLayout.CENTER);
    }

    private static JLabel statValue() {
        JLabel l = new JLabel("—");
        l.setFont(l.getFont().deriveFont(Font.BOLD, 24f));
        l.setForeground(Ui.text());
        return l;
    }

    private static JPanel statCard(String label, JLabel value) {
        JPanel body = new JPanel(new BorderLayout(0, 4));
        body.setOpaque(false);
        body.add(Ui.muted(label), BorderLayout.NORTH);
        body.add(value, BorderLayout.CENTER);
        return Ui.card(null, body);
    }

    @Override
    void refresh() {
        services.setRowCount(0);
        recent.setRowCount(0);
        ChildProfile c = ctx.selectedChild();
        if (c == null) {
            profileTitle.setText("Nenhum perfil selecionado");
            profileSub.setText("Use \"+ Perfil\" ou \"Demo\" na barra superior para começar.");
            today.setText("—");
            week.setText("—");
            avg.setText("—");
            alerts.setText("—");
            alerts.setForeground(Ui.text());
            chart.setData(null);
            return;
        }

        List<PlatformLink> links = ctx.service().listLinks(c.id());
        int unread = ctx.service().unreadAlerts(c.id());

        profileTitle.setText(c.name() + " · " + c.age() + " anos");
        profileSub.setText(links.size() + " serviço(s) vinculado(s) · " + unread + " alerta(s) não lido(s)");

        LocalDate d = LocalDate.now();
        Map<LocalDate, Integer> days = ctx.service().minutesByDay(c.id(), d.minusDays(6), d);
        int total = days.values().stream().mapToInt(Integer::intValue).sum();
        today.setText(Ui.minutes(ctx.service().totalMinutes(c.id(), d)));
        week.setText(Ui.minutes(total));
        avg.setText(Ui.minutes(total / 7));
        alerts.setText(String.valueOf(unread));
        alerts.setForeground(unread > 0 ? Ui.danger() : Ui.accent());

        Map<String, Integer> labeled = new LinkedHashMap<>();
        days.forEach((k, v) -> labeled.put(k.format(DateTimeFormatter.ofPattern("dd/MM")), v));
        chart.setData(labeled);

        for (PlatformLink l : links) {
            services.addRow(new Object[]{l.platform(), l.accountLabel(), l.status(), Ui.formatTs(l.lastSync())});
        }
        int shown = 0;
        for (Alert a : ctx.service().listAlerts(c.id())) {
            if (shown++ == 6) break;
            recent.addRow(new Object[]{Ui.formatTs(a.createdAt()), a.message()});
        }
    }
}