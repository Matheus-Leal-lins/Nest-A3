package br.com.nest.ui;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Base das telas: mostra um aviso quando não há perfil selecionado. */
abstract class Screen extends JPanel {
    protected final AppContext ctx;

    Screen(AppContext ctx) {
        super(new BorderLayout(10, 10));
        this.ctx = ctx;
        Ui.pad(this, 14);
    }

    abstract void refresh();

    protected br.com.nest.model.ChildProfile requireChild() {
        br.com.nest.model.ChildProfile c = ctx.selectedChild();
        if (c == null) throw new IllegalArgumentException("Selecione ou cadastre um perfil primeiro.");
        return c;
    }

    static JLabel emptyState() {
        JLabel l = new JLabel("Cadastre ou selecione um perfil no topo para começar.", SwingConstants.CENTER);
        l.setForeground(Ui.MUTED);
        return l;
    }
}
