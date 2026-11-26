package br.com.nest.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import br.com.nest.model.Guardian;
import br.com.nest.service.AuthService;

public class LoginFrame extends JFrame {
    private static final int MIN_GUARDIAN_AGE = 18;
    private static final DateTimeFormatter BR_DATE_STRICT = DateTimeFormatter
            .ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private static final String CARD_LOGIN = "LOGIN";
    private static final String CARD_REGISTER = "REGISTER";

    private final AuthService auth = new AuthService();

    private final JTextField loginUser = new JTextField(22);
    private final JPasswordField loginPass = new JPasswordField(22);

    private final JTextField regName = new JTextField(22);
    private final JTextField regUser = new JTextField(22);
    private final JTextField regBirthDate = createAutoSlashDateField();
    private final JPasswordField regPass = new JPasswordField(22);
    private final JPasswordField regPassConfirm = new JPasswordField(22);

    private final CardLayout cardLayout = new CardLayout() {
        @Override
        public Dimension preferredLayoutSize(Container parent) {
            for (Component comp : parent.getComponents()) {
                if (comp.isVisible()) {
                    return comp.getPreferredSize();
                }
            }
            return super.preferredLayoutSize(parent);
        }
    };

    private final JPanel cardsPanel = new JPanel(cardLayout);
    private final JButton tabLoginBtn = Ui.primaryButton("Entrar");
    private final JButton tabRegisterBtn = Ui.button("Criar conta");

    public LoginFrame(Consumer<Guardian> onSuccess) {
        super("NEST");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        Ui.styleInput(loginUser);
        Ui.styleInput(loginPass);
        Ui.styleInput(regName);
        Ui.styleInput(regUser);
        Ui.styleInput(regBirthDate);
        Ui.styleInput(regPass);
        Ui.styleInput(regPassConfirm);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Ui.bg());
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(buildHeader(), BorderLayout.NORTH);
        top.add(buildModeSwitcher(), BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        JComponent loginCard = buildLoginPanel(onSuccess);
        loginCard.setName(CARD_LOGIN);

        JComponent registerCard = buildRegisterPanel(onSuccess);
        registerCard.setName(CARD_REGISTER);

        cardsPanel.setOpaque(false);
        cardsPanel.add(loginCard, CARD_LOGIN);
        cardsPanel.add(registerCard, CARD_REGISTER);
        root.add(cardsPanel, BorderLayout.CENTER);

        JLabel footer = Ui.muted("Conta local • Responsável deve ter 18 anos ou mais");
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        switchMode(CARD_LOGIN);
    }

    private JComponent buildHeader() {
        JLabel brand = new JLabel("NEST", SwingConstants.CENTER);
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 28f));
        brand.setForeground(Ui.text());

        JLabel sub = Ui.muted("Controle parental");
        sub.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel head = new JPanel(new GridLayout(0, 1, 0, 2));
        head.setOpaque(false);
        head.add(brand);
        head.add(sub);
        return head;
    }

    private JComponent buildModeSwitcher() {
        JPanel switcher = new JPanel(new GridLayout(1, 2, 8, 0));
        switcher.setOpaque(false);

        tabLoginBtn.addActionListener(e -> switchMode(CARD_LOGIN));
        tabRegisterBtn.addActionListener(e -> switchMode(CARD_REGISTER));

        switcher.add(tabLoginBtn);
        switcher.add(tabRegisterBtn);
        return switcher;
    }

    private void switchMode(String mode) {
        boolean isLogin = CARD_LOGIN.equals(mode);

        tabLoginBtn.putClientProperty("nest.primaryButton", isLogin);
        tabRegisterBtn.putClientProperty("nest.primaryButton", !isLogin);

        for (Component comp : cardsPanel.getComponents()) {
            comp.setVisible(false);
        }
        cardLayout.show(cardsPanel, mode);
        for (Component comp : cardsPanel.getComponents()) {
            if (mode.equals(comp.getName())) {
                comp.setVisible(true);
            }
        }

        Ui.applyTheme(this);
        cardsPanel.invalidate();
        getContentPane().invalidate();
        setMinimumSize(null);
        pack();
        int width = Math.max(420, getWidth());
        setSize(width, getHeight());
        setMinimumSize(new Dimension(420, getHeight()));
        setLocationRelativeTo(null);
    }

    private JComponent buildLoginPanel(Consumer<Guardian> onSuccess) {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(8, 6, 4, 6));

        JLabel userLabel = Ui.fieldLabel("Usuário");
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginUser.setPreferredSize(new Dimension(340, 34));
        loginUser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel passLabel = Ui.fieldLabel("Senha");
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginPass.setPreferredSize(new Dimension(340, 34));
        loginPass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        form.add(userLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(loginUser);
        form.add(Box.createVerticalStrut(12));
        form.add(passLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(loginPass);

        JButton loginBtn = Ui.primaryButton("Entrar");
        Runnable doLogin = () -> Ui.safe(this, () -> {
            String u = loginUser.getText().trim();
            String p = new String(loginPass.getPassword());
            if (u.isBlank() || p.isBlank()) {
                throw new IllegalArgumentException("Preencha usuário e senha.");
            }
            Optional<Guardian> g = auth.login(u, p);
            if (g.isEmpty()) {
                throw new IllegalArgumentException("Usuário ou senha incorretos.");
            }
            dispose();
            onSuccess.accept(g.get());
        });

        loginBtn.addActionListener(e -> doLogin.run());
        loginPass.addActionListener(e -> doLogin.run());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        actions.setOpaque(false);
        actions.add(loginBtn);

        JPanel panel = Ui.card(null, form);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildRegisterPanel(Consumer<Guardian> onSuccess) {
        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));

        form.add(Ui.fieldLabel("Nome"));
        form.add(regName);
        form.add(Ui.fieldLabel("Usuário (mín. 3 caracteres)"));
        form.add(regUser);
        form.add(Ui.fieldLabel("Data de nascimento (dd/mm/aaaa)"));
        form.add(regBirthDate);
        form.add(Ui.fieldLabel("Senha (mín. 6 caracteres)"));
        form.add(regPass);
        form.add(Ui.fieldLabel("Confirmar senha"));
        form.add(regPassConfirm);

        JButton registerBtn = Ui.primaryButton("Criar conta");
        registerBtn.addActionListener(e -> Ui.safe(this, () -> {
            String name = regName.getText().trim();
            String user = regUser.getText().trim();
            LocalDate birth = parseAdultBirthDate(regBirthDate.getText().trim());
            String pass = new String(regPass.getPassword());
            String confirm = new String(regPassConfirm.getPassword());

            if (name.isBlank()) {
                throw new IllegalArgumentException("Informe o nome.");
            }
            if (!pass.equals(confirm)) {
                throw new IllegalArgumentException("As senhas não coincidem.");
            }
            if (birth == null) {
                throw new IllegalArgumentException("Data de nascimento inválida.");
            }

            Guardian g = auth.register(user, pass);
            dispose();
            onSuccess.accept(g);
        }));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        actions.setOpaque(false);
        actions.add(registerBtn);

        JPanel panel = Ui.card(null, form);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private static JTextField createAutoSlashDateField() {
        JTextField field = new JTextField(22);
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE
                        || code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT) {
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    String rawDigits = field.getText().replaceAll("\\D", "");
                    String limitedDigits = rawDigits.length() > 8 ? rawDigits.substring(0, 8) : rawDigits;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < limitedDigits.length(); i++) {
                        if (i == 2 || i == 4) {
                            sb.append('/');
                        }
                        sb.append(limitedDigits.charAt(i));
                    }
                    String formatted = sb.toString();
                    if (!formatted.equals(field.getText())) {
                        field.setText(formatted);
                        field.setCaretPosition(formatted.length());
                    }
                });
            }
        });
        return field;
    }

    private static LocalDate parseAdultBirthDate(String raw) {
        if (raw.isBlank()) {
            throw new IllegalArgumentException("Informe a data de nascimento (dd/mm/aaaa).");
        }
        LocalDate birth;
        try {
            birth = LocalDate.parse(raw, BR_DATE_STRICT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data inválida. Use dd/mm/aaaa.");
        }
        LocalDate today = LocalDate.now();
        if (birth.isAfter(today)) {
            throw new IllegalArgumentException("A data de nascimento não pode estar no futuro.");
        }
        if (Period.between(birth, today).getYears() < MIN_GUARDIAN_AGE) {
            throw new IllegalArgumentException("É necessário ter pelo menos 18 anos.");
        }
        return birth;
    }
}