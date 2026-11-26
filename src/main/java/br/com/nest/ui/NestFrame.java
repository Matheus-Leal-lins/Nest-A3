package br.com.nest.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import br.com.nest.model.ChildProfile;
import br.com.nest.model.Guardian;
import br.com.nest.service.DemoService;
import br.com.nest.service.NestService;

public class NestFrame extends JFrame implements AppContext {
    private final NestService service;
    private final JComboBox<ChildProfile> childBox = new JComboBox<>();
    private final JPanel profilesStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
    private final JTabbedPane tabs = new JTabbedPane();
    private final List<Screen> screens;
    private final int alertsTab;
    private final JButton themeBtn = Ui.button(Ui.isDarkMode() ? "Modo Claro" : "Modo Escuro");
    private boolean updating;

    public NestFrame(Guardian guardian, Runnable onLogout) {
        this(guardian, new NestService(guardian.id()), onLogout);
    }

    public NestFrame(Guardian guardian, NestService service, Runnable onLogout) {
        super("NEST");
        this.service = service;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1080, 700);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);

        Screen dashboard = new DashboardScreen(this);
        Screen services = new ServicesScreen(this);
        Screen details = new DetailsScreen(this);
        Screen reports = new ReportsScreen(this);
        Screen alerts = new AlertsScreen(this);
        Screen settings = new SettingsScreen(this);
        screens = List.of(dashboard, services, details, reports, alerts, settings);

        tabs.addTab("Painel", dashboard);
        tabs.addTab("Serviços", services);
        tabs.addTab("Detalhes", details);
        tabs.addTab("Relatórios", reports);
        tabs.addTab("Alertas", alerts);
        tabs.addTab("Configurações", settings);
        alertsTab = 4;

        JPanel north = new JPanel(new BorderLayout());
        north.add(buildTopBar(guardian, onLogout), BorderLayout.NORTH);
        north.add(buildProfilesBar(), BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBackground(Ui.bg());
        root.add(north, BorderLayout.NORTH);
        JPanel tabHost = Ui.pad(new JPanel(new BorderLayout()), 6);
        tabHost.add(tabs, BorderLayout.CENTER);
        root.add(tabHost, BorderLayout.CENTER);
        setContentPane(root);

        childBox.addActionListener(e -> {
            if (!updating) {
                rebuildProfilesStrip();
                refreshScreens();
            }
        });
        refreshAll();
        Ui.applyTheme(this);
    }

    private JComponent buildTopBar(Guardian guardian, Runnable onLogout) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.putClientProperty("nest.surface", "bar");
        bar.setBackground(Ui.card());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.border()),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel brand = new JLabel("NEST");
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 19f));
        brand.setForeground(Ui.text());
        left.add(brand);

        JButton add = Ui.primaryButton("+ Perfil");
        JButton remove = Ui.button("Remover");
        JButton sync = Ui.button("Sincronizar");
        JButton demo = Ui.button("Demo");

        add.addActionListener(e -> Ui.safe(this, this::addChild));
        remove.addActionListener(e -> Ui.safe(this, this::removeChild));
        sync.addActionListener(e -> Ui.safe(this, this::syncSelected));
        demo.addActionListener(e -> Ui.safe(this, this::loadDemo));

        left.add(add);
        left.add(remove);
        left.add(sync);
        left.add(demo);
        bar.add(left, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(Ui.muted(guardian.username()));

        themeBtn.addActionListener(e -> {
            Ui.setDarkMode(!Ui.isDarkMode());
            themeBtn.setText(Ui.isDarkMode() ? "Modo Claro" : "Modo Escuro");
            refreshAll();
            Ui.applyTheme(this);
        });
        right.add(themeBtn);

        JButton out = Ui.button("Sair");
        out.addActionListener(e -> {
            dispose();
            onLogout.run();
        });
        right.add(out);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JComponent buildProfilesBar() {
        JPanel wrapper = new JPanel(new BorderLayout(12, 0));
        wrapper.setBackground(Ui.bg());
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.border()),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));

        wrapper.add(Ui.fieldLabel("Perfis"), BorderLayout.WEST);
        profilesStrip.setOpaque(false);
        wrapper.add(profilesStrip, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(Ui.muted("Selecionado:"));
        childBox.setPreferredSize(new Dimension(165, 28));
        right.add(childBox);
        wrapper.add(right, BorderLayout.EAST);
        return wrapper;
    }

    private void rebuildProfilesStrip() {
        profilesStrip.removeAll();
        List<ChildProfile> children = service.listChildren();
        ChildProfile active = selectedChild();

        if (children.isEmpty()) {
            profilesStrip.add(Ui.muted("Nenhuma criança cadastrada."));
            JButton quickAdd = Ui.primaryButton("+ Adicionar perfil");
            quickAdd.addActionListener(e -> Ui.safe(this, this::addChild));
            profilesStrip.add(quickAdd);
        } else {
            for (ChildProfile c : children) {
                boolean selected = active != null && active.id() == c.id();
                JButton pill = new JButton(c.name() + " · " + c.age() + " anos");
                pill.putClientProperty("nest.profilePill", Boolean.TRUE);
                pill.setFocusPainted(false);
                pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                pill.setFont(pill.getFont().deriveFont(selected ? Font.BOLD : Font.PLAIN, 12.5f));
                pill.setBackground(selected ? Ui.primary() : Ui.card());
                pill.setForeground(selected ? Color.WHITE : Ui.text());
                pill.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(selected ? Ui.primaryDark() : Ui.border(), 1, true),
                        BorderFactory.createEmptyBorder(5, 12, 5, 12)
                ));
                pill.addActionListener(e -> {
                    updating = true;
                    try {
                        childBox.setSelectedItem(c);
                    } finally {
                        updating = false;
                    }
                    rebuildProfilesStrip();
                    refreshScreens();
                    Ui.applyTheme(this);
                });
                profilesStrip.add(pill);
            }
        }
        profilesStrip.revalidate();
        profilesStrip.repaint();
    }

    private void addChild() {
        JTextField name = new JTextField(18);
        Ui.styleInput(name);
        JSpinner age = new JSpinner(new SpinnerNumberModel(10, 0, 17, 1));
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.add(Ui.fieldLabel("Nome"));
        form.add(name);
        form.add(Ui.fieldLabel("Idade (0–17)"));
        form.add(age);
        Ui.applyTheme(form);
        if (JOptionPane.showConfirmDialog(this, form, "Novo perfil", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        ChildProfile c = service.addChild(name.getText(), (Integer) age.getValue());
        refreshAll();
        childBox.setSelectedItem(c);
        Ui.applyTheme(this);
    }

    private void removeChild() {
        ChildProfile c = selectedChild();
        if (c == null) throw new IllegalArgumentException("Nenhum perfil selecionado.");
        if (JOptionPane.showConfirmDialog(this, "Remover o perfil " + c.name() + " e seus dados?",
                "Remover perfil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        service.removeChild(c.id());
        refreshAll();
        Ui.applyTheme(this);
    }

    private void syncSelected() {
        ChildProfile c = selectedChild();
        if (c == null) throw new IllegalArgumentException("Selecione ou cadastre um perfil primeiro.");
        int n = new DemoService(service).sync(c.id());
        if (n == 0) throw new IllegalArgumentException("Vincule ao menos um serviço antes de sincronizar.");
        refreshAll();
        Ui.applyTheme(this);
    }

    private void loadDemo() {
        if (JOptionPane.showConfirmDialog(this, "Carregar perfis de demonstração (Ana e Bia)?",
                "Demonstração", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        new DemoService(service).loadDemoFamily();
        refreshAll();
        Ui.applyTheme(this);
    }

    @Override
    public NestService service() {
        return service;
    }

    @Override
    public ChildProfile selectedChild() {
        return (ChildProfile) childBox.getSelectedItem();
    }

    @Override
    public void refreshAll() {
        updating = true;
        try {
            ChildProfile keep = selectedChild();
            childBox.removeAllItems();
            for (ChildProfile c : service.listChildren()) childBox.addItem(c);
            if (keep != null) {
                for (int i = 0; i < childBox.getItemCount(); i++) {
                    if (childBox.getItemAt(i).id() == keep.id()) {
                        childBox.setSelectedIndex(i);
                        break;
                    }
                }
            }
            if (childBox.getSelectedItem() == null && childBox.getItemCount() > 0) {
                childBox.setSelectedIndex(0);
            }
        } finally {
            updating = false;
        }
        rebuildProfilesStrip();
        refreshScreens();
    }

    private void refreshScreens() {
        for (Screen s : screens) s.refresh();
        ChildProfile c = selectedChild();
        int unread = c == null ? 0 : service.unreadAlerts(c.id());
        tabs.setTitleAt(alertsTab, unread > 0 ? "Alertas (" + unread + ")" : "Alertas");
    }

    @Override
    public Component frame() {
        return this;
    }
}