package br.com.nest.ui;

import br.com.nest.model.ChildProfile;
import br.com.nest.model.UsageSession;
import br.com.nest.service.NestService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Relatórios e gráficos: uso por dia, distribuição por serviço, comparação com o período anterior e exportação CSV. */
class ReportsScreen extends Screen {
    private static final int[] RANGE = {7, 14, 30};

    private final JComboBox<String> periodBox = new JComboBox<>(new String[]{"7 dias", "14 dias", "30 dias"});
    private final BarChart byDay = new BarChart(Ui.PRIMARY);
    private final BarChart byPlatform = new BarChart(Ui.ACCENT);
    private final JLabel summary = Ui.muted(" ");
    private boolean loading;

    ReportsScreen(AppContext ctx) {
        super(ctx);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(Ui.title("Relatórios"));
        top.add(new JLabel("   Período:"));
        top.add(periodBox);
        JButton export = Ui.button("Exportar CSV");
        export.addActionListener(e -> Ui.safe(this, this::exportCsv));
        top.add(export);
        periodBox.addActionListener(e -> {
            if (!loading) refresh();
        });
        add(top, BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(1, 2, 10, 0));
        charts.setOpaque(false);
        charts.add(Ui.card("Tempo de uso por dia (min)", byDay));
        charts.add(Ui.card("Distribuição por serviço (min)", byPlatform));
        add(charts, BorderLayout.CENTER);
        add(summary, BorderLayout.SOUTH);
    }

    private int days() {
        return RANGE[Math.max(0, periodBox.getSelectedIndex())];
    }

    private void exportCsv() {
        ChildProfile c = requireChild();
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("nest-" + c.name().toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + LocalDate.now() + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        LocalDate to = LocalDate.now();
        List<UsageSession> rows = ctx.service().listSessions(c.id(), NestService.ALL, to.minusDays(days() - 1), to);
        StringBuilder sb = new StringBuilder("perfil;data;servico;minutos;origem\n");
        for (UsageSession s : rows)
            sb.append(c.name()).append(';').append(s.date()).append(';').append(s.platform()).append(';')
                    .append(s.minutes()).append(';').append(s.source()).append('\n');
        try {
            Files.writeString(fc.getSelectedFile().toPath(), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível salvar o arquivo.", e);
        }
        JOptionPane.showMessageDialog(this, "Relatório salvo em:\n" + fc.getSelectedFile().getAbsolutePath());
    }

    @Override
    void refresh() {
        ChildProfile c = ctx.selectedChild();
        if (c == null) {
            byDay.setData(null);
            byPlatform.setData(null);
            summary.setText(" ");
            return;
        }
        int n = days();
        LocalDate to = LocalDate.now(), from = to.minusDays(n - 1);
        Map<LocalDate, Integer> days = ctx.service().minutesByDay(c.id(), from, to);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM");
        Map<String, Integer> labeled = new LinkedHashMap<>();
        days.forEach((k, v) -> labeled.put(k.format(f), v));
        byDay.setData(labeled);
        byPlatform.setData(ctx.service().minutesByPlatform(c.id(), from, to));

        int total = days.values().stream().mapToInt(Integer::intValue).sum();
        int previous = ctx.service().minutesByDay(c.id(), from.minusDays(n), from.minusDays(1))
                .values().stream().mapToInt(Integer::intValue).sum();
        String trend;
        if (previous == 0) trend = "sem dados no período anterior para comparar";
        else {
            long pct = Math.round((total - previous) * 100.0 / previous);
            trend = (pct >= 0 ? "+" : "") + pct + "% em relação aos " + n + " dias anteriores (" + Ui.minutes(previous) + ")";
        }
        summary.setText(c.name() + " — total: " + Ui.minutes(total) + " | média por dia: " + Ui.minutes(total / n) + " | " + trend);
    }
}
