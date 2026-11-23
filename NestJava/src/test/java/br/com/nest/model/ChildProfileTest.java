package br.com.nest.model;
import org.junit.jupiter.api.Test; 
import static org.junit.jupiter.api.Assertions.*;

class ChildProfileTest { 
    @Test void aceitaPerfilValido(){assertDoesNotThrow(()->new ChildProfile(1,"Ana",12));} 
    @Test void rejeitaNomeVazio(){assertThrows(IllegalArgumentException.class,()->new ChildProfile(1," ",12));} 
    @Test void rejeitaIdadeInvalida(){assertThrows(IllegalArgumentException.class,()->new ChildProfile(1,"Ana",18));} }