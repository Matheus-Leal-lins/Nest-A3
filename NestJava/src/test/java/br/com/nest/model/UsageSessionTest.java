package br.com.nest.model;
import org.junit.jupiter.api.Test; 
import java.time.LocalDate; 
import static org.junit.jupiter.api.Assertions.*;

class UsageSessionTest { 
    @Test void aceitaSessaoValida(){assertDoesNotThrow(()->new UsageSession(1,1,"Nintendo",LocalDate.now(),30));} 
    @Test void rejeitaMinutosNegativos(){assertThrows(IllegalArgumentException.class,()->new UsageSession(1,1,"Nintendo",LocalDate.now(),-1));} }
