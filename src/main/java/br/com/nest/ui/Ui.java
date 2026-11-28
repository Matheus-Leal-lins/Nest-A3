package br.com.nest.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Painter;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

final class Ui {
    private Ui() {}

    private static boolean darkMode = false;

    // Paleta Clara
    private static final Color L_PRIMARY = new Color(0x1D5B96);
    private static final Color L_PRIMARY_DARK = new Color(0x143F6A);
    private static final Color L_PRIMARY_SOFT = new Color(0xDCEBFF);
    private static final Color L_ACCENT = new Color(0x1F7A4C);
    private static final Color L_BG = new Color(0xF4F7FA);
    private static final Color L_CARD = new Color(0xFFFFFF);
    private static final Color L_BORDER = new Color(0xCBD5E1);
    private static final Color L_TEXT = new Color(0x0F172A);
    private static final Color L_MUTED = new Color(0x475569);
    private static final Color L_DANGER = new Color(0xB91C1C);

    // Paleta Escura
    private static final Color D_PRIMARY = new Color(0x3B82F6);
    private static final Color D_PRIMARY_DARK = new Color(0x1D4ED8);
    private static final Color D_PRIMARY_SOFT = new Color(0x1E3A5F);
    private static final Color D_ACCENT = new Color(0x34D399);
    private static final Color D_BG = new Color(0x0B1120);
    private static final Color D_CARD = new Color(0x162032);
    private static final Color D_BORDER = new Color(0x2E3F5E);
    private static final Color D_TEXT = new Color(0xF8FAFC);
    private static final Color D_MUTED = new Color(0x94A3B8);
    private static final Color D_DANGER = new Color(0xF87171);

    static final Color PRIMARY = L_PRIMARY;
 @SuppressWarnings("unused") static final Color PRIMARY_DARK = L_PRIMARY_DARK; 
 @SuppressWarnings("unused")  static final Color PRIMARY_SOFT = L_PRIMARY_SOFT;
    static final Color ACCENT = L_ACCENT;
 @SuppressWarnings("unused")  static final Color BG = L_BG;
 @SuppressWarnings("unused")   static final Color CARD = L_CARD;
 @SuppressWarnings("unused")   static final Color BORDER = L_BORDER;
 @SuppressWarnings("unused")   static final Color TEXT = L_TEXT;
    static final Color MUTED = L_MUTED;
 @SuppressWarnings("unused")   static final Color DANGER = L_DANGER;

    static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter TS_IN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static final DateTimeFormatter TS_OUT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static boolean isDarkMode() {
        return darkMode;
    }

    static void setDarkMode(boolean enabled) {
        darkMode = enabled;
        syncUiManagerDefaults();
    }

    static Color primary() {
        return darkMode ? D_PRIMARY : L_PRIMARY;
    }

    static Color primaryDark() {
        return darkMode ? D_PRIMARY_DARK : L_PRIMARY_DARK;
    }

    static Color primarySoft() {
        return darkMode ? D_PRIMARY_SOFT : L_PRIMARY_SOFT;
    }

    static Color accent() {
        return darkMode ? D_ACCENT : L_ACCENT;
    }

    static Color bg() {
        return darkMode ? D_BG : L_BG;
    }

    static Color card() {
        return darkMode ? D_CARD : L_CARD;
    }

    static Color border() {
        return darkMode ? D_BORDER : L_BORDER;
    }

    static Color text() {
        return darkMode ? D_TEXT : L_TEXT;
    }

    static Color mutedColor() {
        return darkMode ? D_MUTED : L_MUTED;
    }

    static Color danger() {
        return darkMode ? D_DANGER : L_DANGER;
    }

    private static void syncUiManagerDefaults() {
        Color bg = bg();
        Color card = card();
        Color text = text();
        Color border = border();
        Color primary = primary();
        Color soft = primarySoft();

        UIManager.put("control", bg);
        UIManager.put("background", bg);
        UIManager.put("info", card);
        UIManager.put("nimbusBase", primary);
        UIManager.put("nimbusFocus", primary);
        UIManager.put("nimbusLightBackground", card);
        UIManager.put("nimbusSelectionBackground", soft);

        UIManager.put("OptionPane.background", bg);
        UIManager.put("OptionPane.messageForeground", text);
        UIManager.put("OptionPane[Enabled].backgroundPainter",
                (Painter<JComponent>) (g, c, w, h) -> {
                    g.setColor(bg());
                    g.fillRect(0, 0, w, h);
                });

        UIManager.put("Panel.background", bg);
        UIManager.put("Label.foreground", text);
        UIManager.put("TabbedPane.background", bg);
        UIManager.put("TabbedPane.foreground", text);
        UIManager.put("TabbedPane.contentAreaColor", bg);
        UIManager.put("TabbedPane.selected", card);
        UIManager.put("TabbedPane.focus", border);

        UIManager.put("Table.background", card);
        UIManager.put("Table.foreground", text);
        UIManager.put("Table.selectionBackground", soft);
        UIManager.put("Table.selectionForeground", text);
        UIManager.put("Table.gridColor", border);
        UIManager.put("TableHeader.background", bg);
        UIManager.put("TableHeader.foreground", text);

        UIManager.put("ScrollPane.background", card);
        UIManager.put("Viewport.background", card);

        UIManager.put("TextField.background", card);
        UIManager.put("TextField.foreground", text);
        UIManager.put("TextField.caretForeground", primary);
        UIManager.put("PasswordField.background", card);
        UIManager.put("PasswordField.foreground", text);
        UIManager.put("PasswordField.caretForeground", primary);
        UIManager.put("ComboBox.background", card);
        UIManager.put("ComboBox.foreground", text);
        UIManager.put("ComboBox.selectionBackground", soft);
        UIManager.put("ComboBox.selectionForeground", text);
    }

    static void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ignored) {
        }
        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        syncUiManagerDefaults();
    }

    static void applyTheme(Component root) {
        if (root == null) return;
        syncUiManagerDefaults();
        applyToComponent(root);
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyTheme(child);
            }
        }
        root.revalidate();
        root.repaint();
    }

    private static void applyToComponent(Component c) {
        Color bg = bg();
        Color card = card();
        Color text = text();
        Color border = border();

        if (c instanceof JComponent jc) {
            Border b = jc.getBorder();
            if (b instanceof TitledBorder tb) {
                tb.setTitleColor(text);
                tb.setBorder(BorderFactory.createLineBorder(border, 1));
            }
            Object cardHeadingFlag = jc.getClientProperty("nest.cardHeading");
            if (Boolean.TRUE.equals(cardHeadingFlag) && c instanceof JLabel lbl) {
                lbl.setForeground(text);
            }
            Object mutedFlag = jc.getClientProperty("nest.muted");
            if (Boolean.TRUE.equals(mutedFlag) && c instanceof JLabel lbl) {
                lbl.setForeground(mutedColor());
            }
            Object primaryBtnFlag = jc.getClientProperty("nest.primaryButton");
            if (Boolean.TRUE.equals(primaryBtnFlag) && c instanceof AbstractButton btn) {
                btn.setBackground(primary());
                btn.setForeground(Color.WHITE);
            } else if (Boolean.FALSE.equals(primaryBtnFlag) && c instanceof AbstractButton btn
                    && jc.getClientProperty("nest.profilePill") == null) {
                btn.setBackground(card);
                btn.setForeground(text);
            }
        }

        if (c instanceof JPanel panel) {
            Object surface = panel.getClientProperty("nest.surface");
            if ("card".equals(surface) || "bar".equals(surface)) {
                panel.setBackground(card);
            } else if (panel.isOpaque()) {
                panel.setBackground(bg);
            }
        } else if (c instanceof JScrollPane sp) {
            sp.setBackground(card);
            sp.setBorder(BorderFactory.createLineBorder(border, 1));
            if (sp.getViewport() != null) {
                sp.getViewport().setBackground(card);
            }
        } else if (c instanceof JTable table) {
            table.setBackground(card);
            table.setForeground(text);
            table.setGridColor(border);
            table.setSelectionBackground(primarySoft());
            table.setSelectionForeground(text);
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                               boolean hasFocus, int row, int column) {
                    Component comp = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                    if (isSelected) {
                        comp.setBackground(primarySoft());
                        comp.setForeground(text());
                    } else {
                        comp.setBackground(card());
                        comp.setForeground(text);
                    }
                    return comp;
                }
            });
            styleTableHeader(table);
        } else if (c instanceof JTabbedPane tabbedPane) {
            tabbedPane.setBackground(bg);
            tabbedPane.setForeground(text);
        } else if (c instanceof JTextField tf && !(c instanceof JPasswordField)) {
            styleInput(tf);
        } else if (c instanceof JPasswordField pf) {
            pf.setBackground(card);
            pf.setForeground(text);
            pf.setCaretColor(primary());
            pf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border, 1),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));
        } else if (c instanceof JComboBox<?> combo) {
            styleComboBox(combo);
        } else if (c instanceof JSpinner spinner) {
            spinner.setBackground(card);
            spinner.setForeground(text);
            JComponent editor = spinner.getEditor();
            if (editor != null) {
                editor.setBackground(card);
                editor.setForeground(text);
            }
        } else if (c instanceof JLabel label) {
            if (!Boolean.TRUE.equals(label.getClientProperty("nest.muted"))
                    && !Boolean.TRUE.equals(label.getClientProperty("nest.statSpecial"))) {
                label.setForeground(text);
            }
        }
    }

    static <E> void styleComboBox(JComboBox<E> combo) {
        combo.setBackground(card());
        combo.setForeground(text());
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setOpaque(true);
                lbl.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                if (isSelected) {
                    lbl.setBackground(primarySoft());
                    lbl.setForeground(text());
                } else {
                    lbl.setBackground(card());
                    lbl.setForeground(text());
                }
                return lbl;
            }
        });
    }

    private static void styleTableHeader(JTable table) {
        JTableHeader header = table.getTableHeader();
        if (header == null) return;
        header.setReorderingAllowed(false);
        header.setBackground(bg());
        header.setForeground(text());
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                lbl.setOpaque(true);
                lbl.setBackground(bg());
                lbl.setForeground(text());
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, border()),
                        BorderFactory.createEmptyBorder(5, 8, 5, 8)
                ));
                return lbl;
            }
        });
        header.repaint();
    }

    static String minutes(int m) {
        return m >= 60 ? (m / 60) + "h " + String.format("%02d", m % 60) + "min" : m + " min";
    }

    static String formatTs(String ts) {
        if (ts == null || ts.isBlank()) return "—";
        try {
            return LocalDateTime.parse(ts, TS_IN).format(TS_OUT);
        } catch (Exception e) {
            return ts;
        }
    }

    static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 17f));
        l.setForeground(text());
        return l;
    }

    static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        l.setForeground(text());
        return l;
    }

    static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(12f));
        l.setForeground(mutedColor());
        l.putClientProperty("nest.muted", Boolean.TRUE);
        return l;
    }

    static void styleInput(JTextField field) {
        field.setBackground(card());
        field.setForeground(text());
        field.setCaretColor(primary());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border(), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    static JButton button(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(card());
        b.setForeground(text());
        b.putClientProperty("nest.primaryButton", Boolean.FALSE);
        return b;
    }

    static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(primary());
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.putClientProperty("nest.primaryButton", Boolean.TRUE);
        return b;
    }

    static JPanel pad(JPanel p, int gap) {
        p.setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, gap));
        p.setBackground(bg());
        return p;
    }

    static JPanel card(String heading, JComponent body) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(card());
        p.putClientProperty("nest.surface", "card");
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border(), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        if (heading != null && !heading.isBlank()) {
            JLabel h = new JLabel(heading);
            h.setFont(h.getFont().deriveFont(Font.BOLD, 13.5f));
            h.setForeground(text());
            h.putClientProperty("nest.cardHeading", Boolean.TRUE);
            p.add(h, BorderLayout.NORTH);
        }
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
        t.setRowHeight(26);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(false);
        t.setBackground(card());
        t.setForeground(text());
        t.setGridColor(border());
        t.setSelectionBackground(primarySoft());
        t.setSelectionForeground(text());
        styleTableHeader(t);
        return t;
    }

    static JTable widths(JTable t, int... widths) {
        for (int i = 0; i < widths.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            if (widths[i] <= 150) t.getColumnModel().getColumn(i).setMaxWidth(widths[i] + 40);
        }
        return t;
    }

    private static JPanel messageBox(String message) {
        JLabel label = new JLabel("<html><body style='width: 360px;'>"
                + escapeHtml(message).replace("\n", "<br>") + "</body></html>");
        label.setForeground(text());
        JPanel box = new JPanel(new BorderLayout());
        box.setOpaque(true);
        box.setBackground(bg());
        box.add(label, BorderLayout.CENTER);
        return box;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static void info(Component parent, String title, String message) {
        syncUiManagerDefaults();
        JOptionPane.showMessageDialog(parent, messageBox(message), title, JOptionPane.INFORMATION_MESSAGE);
    }

    static void safe(Component parent, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException ex) {
            syncUiManagerDefaults();
            JOptionPane.showMessageDialog(parent, messageBox(ex.getMessage()),
                    "Atenção", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            syncUiManagerDefaults();
            String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            JOptionPane.showMessageDialog(parent, messageBox(msg),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}