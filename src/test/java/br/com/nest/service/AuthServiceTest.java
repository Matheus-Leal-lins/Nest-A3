package br.com.nest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private AuthService auth;

    @BeforeEach
    void setUp(@TempDir Path dir) {
        System.setProperty("nest.db", dir.resolve("auth-test.db").toString());
        auth = new AuthService();
    }

    @Test
    void cadastraELogaComSenhaCorreta() {
        auth.register("Maria", "segredo1");
        assertTrue(auth.login("maria", "segredo1").isPresent());
    }

    @Test
    void rejeitaSenhaErradaEUsuarioInexistente() {
        auth.register("maria", "segredo1");
        assertTrue(auth.login("maria", "errada!").isEmpty());
        assertTrue(auth.login("ninguem", "segredo1").isEmpty());
    }

    @Test
    void rejeitaUsuarioDuplicadoEDadosInvalidos() {
        auth.register("maria", "segredo1");
        assertThrows(IllegalArgumentException.class, () -> auth.register("MARIA", "outrasenha"));
        assertThrows(IllegalArgumentException.class, () -> auth.register("ab", "segredo1"));
        assertThrows(IllegalArgumentException.class, () -> auth.register("joao", "123"));
    }
}
