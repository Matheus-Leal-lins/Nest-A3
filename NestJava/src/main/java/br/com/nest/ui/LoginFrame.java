package br.com.nest.ui;

import br.com.nest.model.Guardian;
import br.com.nest.service.AuthService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.function.Consumer;

/** Tela de login e cadastro do responsável (idade mínima de 18 anos). */
public class LoginFrame extends JFrame {
    private static final int MIN_GUARDIAN_AGE = 18;

    private final AuthService auth = new AuthService();

    // Campos de login
    private final JTextField loginUser = new JTextField(20);
    private final JPasswordField loginPass = new JPasswordField(20);

    // Campos de cadastro
    private final JTextField regName = new JTextField(20);
    private final JTextField regUser = new JTextField(20);
    private final JTextField regBirthDate = new JTextField(20);
    private final JPasswordField regPass = new JPasswordField(20);
    private final JPasswordField regPassConfirm = new JPasswordField(20);

    public LoginFrame(Consumer<Guardian> onSuccess) {
        super("Nest — Acesso do Responsável");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Ui.BG);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        root.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane modeTabs = new JTabbedPane();
        modeTabs.addTab("Entrar", buildLoginPanel(onSuccess));
        modeTabs.addTab("Criar conta (+18)", buildRegisterPanel(onSuccess));
        root.add(modeTabs, BorderLayout.CENTER);

        JLabel footerNote = Ui.muted(
                "<html><center>Conta local neste computador • Responsável deve ter ao menos 18 anos.<br>"
                        + "Use no mínimo 3 caracteres no usuário e 6 na senha. Não reutilize senhas de terceiros.</center></html>"
        );
        footerNote.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(footerNote, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(480, Math.max(getHeight(), 470)));
        setLocationRelativeTo(null);
    }

    private JComponent buildHeader() {
        JLabel brand = new JLabel("🪺 Nest", SwingConstants.CENTER);
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 30f));
        brand.setForeground(Ui.PRIMARY_DARK);

        JLabel sub = Ui.muted("Hub de controles parentais — gestão centralizada de tempo, serviços e alertas");
        sub.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel head = new JPanel(new GridLayout(0, 1, 0, 4));
        head.setOpaque(false);
        head.add(brand);
        head.add(sub);
        return head;
    }

    private JComponent buildLoginPanel(Consumer<Guardian> onSuccess) {
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(16, 12, 12, 12));

        form.add(labelBold("Usuário do responsável"));
        form.add(loginUser);
        form.add(labelBold("Senha"));
        form.add(loginPass);

        JButton loginBtn = Ui.primaryButton("Entrar e ver perfis");
        Runnable doLogin = () -> Ui.safe(this, () -> {
            String username = loginUser.getText().trim();
            String password = new String(loginPass.getPassword());
            if (username.isBlank() || password.isBlank()) {
                throw new IllegalArgumentException("Informe usuário e senha para entrar.");
            }
            Optional<Guardian> g = auth.login(username, password);
            if (g.isEmpty()) {
                throw new IllegalArgumentException("Usuário ou senha incorretos.");
            }
            validateGuardianAgeIfAvailable(g.get());
            dispose();
            onSuccess.accept(g.get());
        });

        loginBtn.addActionListener(e -> doLogin.run());
        loginPass.addActionListener(e -> doLogin.run());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        actions.setOpaque(false);
        actions.add(loginBtn);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(form, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildRegisterPanel(Consumer<Guardian> onSuccess) {
        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        regBirthDate.setToolTipText("Formato AAAA-MM-DD, ex: 1990-05-14");

        form.add(labelBold("Nome completo ou de exibição"));
        form.add(regName);
        form.add(labelBold("Usuário de acesso (mín. 3 caracteres)"));
        form.add(regUser);
        form.add(labelBold("Data de nascimento (AAAA-MM-DD — mín. 18 anos)"));
        form.add(regBirthDate);
        form.add(labelBold("Senha (mín. 6 caracteres)"));
        form.add(regPass);
        form.add(labelBold("Confirmar senha"));
        form.add(regPassConfirm);

        JButton registerBtn = Ui.primaryButton("Cadastrar responsável");
        registerBtn.addActionListener(e -> Ui.safe(this, () -> {
            String name = regName.getText().trim();
            String username = regUser.getText().trim();
            LocalDate birthDate = parseAndValidateAdult(regBirthDate.getText().trim());
            String password = new String(regPass.getPassword());
            String confirm = new String(regPassConfirm.getPassword());

            if (name.isBlank()) {
                throw new IllegalArgumentException("Informe o nome do responsável.");
            }
            if (!password.equals(confirm)) {
                throw new IllegalArgumentException("A confirmação de senha não confere.");
            }

            // Se o seu AuthService.register aceitar (username, password, name, birthDate),
            // basta passar os parâmetros adicionais aqui.
            Guardian g = auth.register(username, password);
            dispose();
            onSuccess.accept(g);
        }));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        actions.setOpaque(false);
        actions.add(registerBtn);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(form, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private static LocalDate parseAndValidateAdult(String rawDate) {
        if (rawDate.isBlank()) {
            throw new IllegalArgumentException("Informe a data de nascimento no formato AAAA-MM-DD.");
        }
        LocalDate birth;
        try {
            birth = LocalDate.parse(rawDate);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data de nascimento inválida. Use o formato AAAA-MM-DD (ex.: 1995-08-20).");
        }
        LocalDate today = LocalDate.now();
        if (birth.isAfter(today)) {
            throw new IllegalArgumentException("A data de nascimento não pode estar no futuro.");
        }
        int age = Period.between(birth, today).getYears();
        if (age < MIN_GUARDIAN_AGE) {
            throw new IllegalArgumentException(
                    "Cadastro bloqueado: é necessário ter pelo menos " + MIN_GUARDIAN_AGE + " anos para criar conta de responsável."
            );
        }
        return birth;
    }

    private static void validateGuardianAgeIfAvailable(Guardian guardian) {
        // Caso Guardian tenha getBirthDate() / birthDate(), valida também no login.
        // Quando você colar Guardian.java e AuthService.java, deixo essa verificação tipada diretamente.
    }

    private static JLabel labelBold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12.5f));
        return l;
    }
}