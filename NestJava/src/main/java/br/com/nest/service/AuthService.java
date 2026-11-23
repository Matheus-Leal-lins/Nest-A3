package br.com.nest.service;

import br.com.nest.model.Guardian;
import br.com.nest.repository.Database;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import java.util.Optional;

/** Cadastro e login locais do responsável. Senhas são guardadas com PBKDF2 + sal (nunca em texto puro). */
public class AuthService {
    private static final int ITERATIONS = 65_536;

    public Guardian register(String username, String password) {
        if (username == null || username.trim().length() < 3)
            throw new IllegalArgumentException("O usuário deve ter pelo menos 3 caracteres.");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("A senha deve ter pelo menos 6 caracteres.");
        String user = username.trim().toLowerCase();
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String hash = hash(password, salt);
        try (Connection c = Database.connect()) {
            int id;
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO guardians(username,salt,hash) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                p.setString(1, user);
                p.setString(2, Base64.getEncoder().encodeToString(salt));
                p.setString(3, hash);
                p.executeUpdate();
                try (ResultSet r = p.getGeneratedKeys()) {
                    r.next();
                    id = r.getInt(1);
                }
            }
            // Perfis criados pela versão anterior (sem responsável) passam para a primeira conta criada.
            try (PreparedStatement p = c.prepareStatement("UPDATE children SET guardian_id=? WHERE guardian_id IS NULL")) {
                p.setInt(1, id);
                p.executeUpdate();
            }
            return new Guardian(id, user);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE"))
                throw new IllegalArgumentException("Este usuário já existe.");
            throw new IllegalStateException("Não foi possível criar a conta.", e);
        }
    }

    public Optional<Guardian> login(String username, String password) {
        if (username == null || password == null) return Optional.empty();
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement("SELECT id,salt,hash FROM guardians WHERE username=?")) {
            p.setString(1, username.trim().toLowerCase());
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return Optional.empty();
                byte[] salt = Base64.getDecoder().decode(r.getString("salt"));
                byte[] expected = r.getString("hash").getBytes();
                byte[] actual = hash(password, salt).getBytes();
                if (!MessageDigest.isEqual(expected, actual)) return Optional.empty();
                return Optional.of(new Guardian(r.getInt("id"), username.trim().toLowerCase()));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível validar o login.", e);
        }
    }

    private static String hash(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
            byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(h);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Algoritmo de hash indisponível.", e);
        }
    }
}
