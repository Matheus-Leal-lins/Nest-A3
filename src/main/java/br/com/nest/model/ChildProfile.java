package br.com.nest.model;

/** Perfil de uma criança ou adolescente acompanhado pelo responsável. */
public record ChildProfile(int id, String name, int age) {

    public ChildProfile {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nome é obrigatório.");
        if (age < 0 || age > 17) throw new IllegalArgumentException("A idade deve estar entre 0 e 17 anos.");
        name = name.trim();
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return name + " (" + age + " anos)";
    }
}
