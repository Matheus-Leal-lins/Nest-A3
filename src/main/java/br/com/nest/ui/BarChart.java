package br.com.nest.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JPanel;

class BarChart extends JPanel {
    private Map<String, Integer> data = new LinkedHashMap<>();
    private final Color barColorOverride;

    BarChart(Color barColor) {
        this.barColorOverride = barColor;
        setBackground(Ui.card());
        setPreferredSize(new Dimension(380, 220));
    }

    void setData(Map<String, Integer> data) {
        this.data = data == null ? new LinkedHashMap<>() : data;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        g.setColor(Ui.card());
        g.fillRect(0, 0, w, h);

        int max = data.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (data.isEmpty() || max == 0) {
            g.setColor(Ui.mutedColor());
            String msg = "Sem dados no período";
            g.drawString(msg, (w - g.getFontMetrics().stringWidth(msg)) / 2, h / 2);
            g.dispose();
            return;
        }

        int left = 36;
        int right = 8;
        int top = 22;
        int bottom = 34;
        int chartW = w - left - right;
        int chartH = h - top - bottom;

        g.setColor(Ui.border());
        g.drawLine(left, top + chartH, left + chartW, top + chartH);
        g.drawLine(left, top, left, top + chartH);

        g.setColor(Ui.mutedColor());
        g.drawString(String.valueOf(max), 4, top + 10);
        g.drawString("0", 22, top + chartH);

        int n = data.size();
        int slot = chartW / n;
        int barW = Math.max(8, Math.min(48, slot - 12));
        int i = 0;
        FontMetrics fm = g.getFontMetrics();
        Color activeBarColor = Ui.isDarkMode() ? Ui.primary() : barColorOverride;

        for (Map.Entry<String, Integer> e : data.entrySet()) {
            int bh = (int) Math.round(chartH * (e.getValue() / (double) max));
            int x = left + i * slot + (slot - barW) / 2;

            g.setColor(activeBarColor);
            g.fillRoundRect(x, top + chartH - bh, barW, bh, 6, 6);

            g.setColor(Ui.text());
            String v = String.valueOf(e.getValue());
            g.drawString(v, x + (barW - fm.stringWidth(v)) / 2, top + chartH - bh - 4);

            String label = shorten(e.getKey(), fm, slot - 2);
            g.setColor(Ui.mutedColor());
            g.drawString(label, left + i * slot + (slot - fm.stringWidth(label)) / 2, top + chartH + 16);
            i++;
        }
        g.dispose();
    }

    private static String shorten(String s, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(s) <= maxWidth) return s;
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "…") > maxWidth) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }
}