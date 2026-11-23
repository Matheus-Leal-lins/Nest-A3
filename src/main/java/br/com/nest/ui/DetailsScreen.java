package br.com.nest.ui;

import br.com.nest.model.ChildProfile;
import br.com.nest.model.Platforms;
import br.com.nest.model.UsageSession;
import br.com.nest.service.NestService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Detalhes por serviço/período e lançamento manual de uso. */
class DetailsScreen extends Screen {
    private static final String[] PERIODS = {"Hoje", "Últimos 7 dias", "Últimos 30 dias"};
    private static final int[] DAYS = {0, 6, 29};

    private final JComboBox<String> platformBox = new JComboBox<>();
    private final JComboBox<String> periodBox = new JComboBox<>(PERIODS);
    private final DefaultTableModel model = Ui.model("Data", "Serviço", "Tempo", "Minutos", "Origem");
    private final JLabel total = Ui.muted(" ");
    private boolean loading;

    DetailsScreen(AppContext ctx) {
        super(ctx);
        periodBox.setSelectedIndex(1);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(Ui.title("Detalhes de uso"));
        filters.add(new JLabel("   Serviço:"));
        filters.add(platformBox);
        filters.add(new JLabel("Período:"));
        filters.add(periodBox);
        platformBox.addActionListener(e -> reload());
        periodBox.addActionListener(e -> reload());
        add(filters, BorderLayout.NORTH);
        add(new JScrollPane(Ui.table(model)), BorderLayout.CENTER);

        JButton add = Ui.primaryButton("Registrar uso manual");
        add.addActionListener(e -> Ui.safe(this, this::addUsage));
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(total, BorderLayout.CENTER);
        south.add(add, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);
    }

    private void addUsage() {
        ChildProfile c = requireChild();
        Set<String> options = new LinkedHashSet<>();
        ctx.service().listLinks(c.id()).forEach(l -> options.add(l.platform()));
        for (String p : Platforms.names()) options.add(p);
        JComboBox<String> platform = new JComboBox<>(options.toArray(new String[0]));
        JTextField date = new JTextField(LocalDate.now().toString());
        JSpinner minutes = new JSpinner(new SpinnerNumberModel(30, 0, 1440, 5));
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Serviço:"));
        form.add(platform);
        form.add(new JLabel("Data (aaaa-mm-dd):"));
        form.add(date);
        form.add(new JLabel("Minutos de uso:"));
        form.add(minutes);
        if (JOptionPane.showConfirmDialog(this, form, "Registrar uso de " + c.name(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        LocalDate d;
        try {
            d = LocalDate.parse(date.getText().trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida. Use o formato aaaa-mm-dd, por exemplo " + LocalDate.now() + ".");
        }
        if (d.isAfter(LocalDate.now())) throw new IllegalArgumentException("A data não pode estar no futuro.");
        ctx.service().addUsage(c.id(), (String) platform.getSelectedItem(), d, (Integer) minutes.getValue());
        ctx.refreshAll();
    }

    private void reload() {
        if (loading) return;
        model.setRowCount(0);
        ChildProfile c = ctx.selectedChild();
        if (c == null) {
            total.setText(" ");
            return;
        }
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(DAYS[Math.max(0, periodBox.getSelectedIndex())]);
        String platform = (String) platformBox.getSelectedItem();
        List<UsageSession> sessions = ctx.service().listSessions(c.id(), platform, from, to);
        int sum = 0;
        for (UsageSession s : sessions) {
            sum += s.minutes();
            model.addRow(new Object[]{s.date().format(Ui.BR_DATE), s.platform(), Ui.minutes(s.minutes()), s.minutes(), s.source()});
        }
        total.setText("Total no período: " + Ui.minutes(sum) + " em " + sessions.size() + " registro(s).");
    }

    @Override
    void refresh() {
        loading = true;
        Object keep = platformBox.getSelectedItem();
        platformBox.removeAllItems();
        platformBox.addItem(NestService.ALL);
        ChildProfile c = ctx.selectedChild();
        if (c != null) {
            Set<String> names = new LinkedHashSet<>();
            ctx.service().listLinks(c.id()).forEach(l -> names.add(l.platform()));
            ctx.service().minutesByPlatform(c.id(), LocalDate.now().minusDays(365), LocalDate.now()).keySet().forEach(names::add);
            names.forEach(platformBox::addItem);
        }
        platformBox.setSelectedItem(keep != null ? keep : NestService.ALL);
        if (platformBox.getSelectedItem() == null) platformBox.setSelectedIndex(0);
        loading = false;
        reload();
    }
}
