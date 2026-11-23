package br.com.nest.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/** Utilitários e identidade visual compartilhados pelas telas. */
final class Ui {
    private Ui() {}

    static final Color PRIMARY = new Color(0x2F6F5E);
    static final Color PRIMARY_DARK = new Color(0x1F4D41);
    static final Color ACCENT = new Color(0xE9A23B);
    static final Color BG = new Color(0xF4F6F5);
    static final Color CARD = Color.WHITE;
    static final Color BORDER = new Color(0xD9E0DD);
    static final Color MUTED = new Color(0x5F6B66);
    static final Color DANGER = new Color(0xC0453A);

    static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter TS_IN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static final DateTimeFormatter TS_OUT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // mantém o visual padrão
        }
        UIManager.put("control", BG);
        UIManager.put("nimbusBase", PRIMARY);
        UIManager.put("nimbusFocus", ACCENT);
        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 13));
    }

    static String minutes(int m) {
        return m >= 60 ? (m / 60) + "h " + String.format("%02d", m % 60) + "min" : m + " min";
    }

    static String formatTs(String ts) {
        if (ts == null || ts.isBlank()) return "—";
        try {
            return java.time.LocalDateTime.parse(ts, TS_IN).format(TS_OUT);
        } catch (Exception e) {
            return ts;
        }
    }

    static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 16f));
        l.setForeground(PRIMARY_DARK);
        return l;
    }

    static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        return l;
    }

    static JButton button(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        return b;
    }

    static JButton primaryButton(String text) {
        JButton b = button(text);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        return b;
    }

    static JPanel pad(JPanel p, int gap) {
        p.setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, gap));
        p.setBackground(BG);
        return p;
    }

    static JPanel card(String heading, JComponent body) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        if (heading != null) p.add(title(heading), BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(24);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(false);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    static JTable widths(JTable t, int... widths) {
        for (int i = 0; i < widths.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            if (widths[i] <= 150) t.getColumnModel().getColumn(i).setMaxWidth(widths[i] + 40);
        }
        return t;
    }

    /** Executa a ação e mostra mensagens de validação/erro em vez de derrubar a interface. */
    static void safe(Component parent, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "Atenção", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent, ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
