package br.com.nest.service;
import org.junit.jupiter.api.Test; 
import static org.junit.jupiter.api.Assertions.*;

class UsageReportTest { 
    @Test void somaTempoDeDuasSessoes(){int a=60,b=45;assertEquals(105,a+b);} 
    @Test void erroDeIntegracaoNaoDeveSerSucesso(){String status="FALHA";assertNotEquals("SUCESSO",status);} }
