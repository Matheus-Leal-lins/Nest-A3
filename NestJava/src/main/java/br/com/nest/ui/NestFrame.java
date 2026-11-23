package br.com.nest.ui;

import br.com.nest.model.ChildProfile;
import br.com.nest.model.Guardian;
import br.com.nest.service.DemoService;
import br.com.nest.service.NestService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Janela principal: seletor de perfil no topo e uma aba para cada tela do hub. */
public class NestFrame extends JFrame implements AppContext {
    private final NestService service;
    private final JComboBox<ChildProfile> childBox = new JComboBox<>();
    private final JTabbedPane tabs = new JTabbedPane();
    private final List<Screen> screens;
    private final int alertsTab;
    private boolean updating;

    public NestFrame(Guardian guardian, Runnable onLogout) {
        this(guardian, new NestService(guardian.id()), onLogout);
    }

    /** Permite injetar o serviço (útil em testes). */
    public NestFrame(Guardian guardian, NestService service, Runnable onLogout) {
        super("Nest — Hub de Controles Parentais");
        this.service = service;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1060, 680);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);

        Screen dashboard = new DashboardScreen(this), services = new ServicesScreen(this), details = new DetailsScreen(this),
                reports = new ReportsScreen(this), alerts = new AlertsScreen(this), settings = new SettingsScreen(this);
        screens = List.of(dashboard, services, details, reports, alerts, settings);
        tabs.addTab("Painel", dashboard);
        tabs.addTab("Serviços", services);
        tabs.addTab("Detalhes", details);
        tabs.addTab("Relatórios", reports);
        tabs.addTab("Alertas", alerts);
        tabs.addTab("Configurações", settings);
        alertsTab = 4;

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildTopBar(guardian, onLogout), BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);

        childBox.addActionListener(e -> {
            if (!updating) refreshScreens();
        });
        refreshAll();
    }

    private JComponent buildTopBar(Guardian guardian, Runnable onLogout) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Ui.PRIMARY_DARK);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel brand = new JLabel("🪺 Nest");
        brand.setForeground(Color.WHITE);
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 20f));
        left.add(brand);
        JLabel who = new JLabel("   Perfil:");
        who.setForeground(Color.WHITE);
        left.add(who);
        childBox.setPreferredSize(new Dimension(190, 28));
        left.add(childBox);
        JButton add = Ui.button("+ Perfil"), remove = Ui.button("Remover"), sync = Ui.button("Sincronizar (simulado)"),
                demo = Ui.button("Dados de demonstração");
        add.addActionListener(e -> Ui.safe(this, this::addChild));
        remove.addActionListener(e -> Ui.safe(this, this::removeChild));
        sync.addActionListener(e -> Ui.safe(this, this::syncSelected));
        demo.addActionListener(e -> Ui.safe(this, this::loadDemo));
        left.add(add);
        left.add(remove);
        left.add(sync);
        left.add(demo);
        bar.add(left, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JLabel user = new JLabel("Responsável: " + guardian.username());
        user.setForeground(Color.WHITE);
        JButton out = Ui.button("Sair");
        out.addActionListener(e -> {
            dispose();
            onLogout.run();
        });
        right.add(user);
        right.add(out);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void addChild() {
        JTextField name = new JTextField(16);
        JSpinner age = new JSpinner(new SpinnerNumberModel(10, 0, 17, 1));
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Nome do perfil (use nome fictício no protótipo):"));
        form.add(name);
        form.add(new JLabel("Idade (0–17):"));
        form.add(age);
        if (JOptionPane.showConfirmDialog(this, form, "Novo perfil", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        ChildProfile c = service.addChild(name.getText(), (Integer) age.getValue());
        refreshAll();
        childBox.setSelectedItem(c);
    }

    private void removeChild() {
        ChildProfile c = selectedChild();
        if (c == null) throw new IllegalArgumentException("Nenhum perfil selecionado.");
        if (JOptionPane.showConfirmDialog(this, "Remover o perfil " + c.name() + " e TODOS os seus dados (vínculos, uso e alertas)?",
                "Remover perfil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        service.removeChild(c.id());
        refreshAll();
    }

    private void syncSelected() {
        ChildProfile c = selectedChild();
        if (c == null) throw new IllegalArgumentException("Selecione ou cadastre um perfil primeiro.");
        int n = new DemoService(service).sync(c.id());
        if (n == 0) throw new IllegalArgumentException("Vincule ao menos um serviço (aba Serviços) antes de sincronizar.");
        refreshAll();
    }

    private void loadDemo() {
        if (JOptionPane.showConfirmDialog(this, "Criar os perfis fictícios Ana e Bia, com serviços vinculados, limites e uso simulado?",
                "Dados de demonstração", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        new DemoService(service).loadDemoFamily();
        refreshAll();
    }

    // ---- AppContext

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
            if (keep != null)
                for (int i = 0; i < childBox.getItemCount(); i++)
                    if (childBox.getItemAt(i).id() == keep.id()) childBox.setSelectedIndex(i);
        } finally {
            updating = false;
        }
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
