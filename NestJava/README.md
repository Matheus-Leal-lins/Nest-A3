# Nest — Hub de Controles Parentais (MVP)

Aplicação acadêmica **Java 17 + Swing + SQLite + Maven**. Reúne, em um só painel, os dados de controles parentais
(Google Family Link, Apple Screen Time, Nintendo Switch e outros) de cada criança/adolescente.

> **Importante:** as integrações com Google, Apple e Nintendo **ainda são simuladas**. O Nest não se conecta a APIs
> de fabricantes, não pede senhas externas e não altera configurações reais. Use apenas dados fictícios.

## Como executar
```bash
mvn test            # testes (usa bancos temporários)
mvn exec:java       # abre o aplicativo
```
Também funciona abrindo a pasta como projeto Maven no NetBeans/IntelliJ (classe principal `br.com.nest.Main`).
Os dados ficam em `nest.db` (para outro caminho: `-Dnest.db=/caminho/arquivo.db`).

## Primeiro uso (roteiro de demonstração)
1. **Criar conta** na tela de login (usuário ≥ 3 caracteres, senha ≥ 6; senha guardada com PBKDF2 + sal).
2. Clique em **Dados de demonstração** (cria Ana e Bia, vínculos, limites e uso simulado) — ou crie perfis com **+ Perfil**.
3. **Painel:** uso de hoje, 7 dias, média, alertas e serviços do perfil selecionado.
4. **Serviços:** vincular/desvincular, **Sincronizar (simulado)** e **Abrir serviço original** (site oficial).
5. **Detalhes:** filtros por serviço/período e **Registrar uso manual**.
6. **Relatórios:** gráficos por dia e por serviço, comparação com o período anterior e **Exportar CSV**.
7. **Alertas:** gerados quando o uso passa do limite diário (1 alerta por perfil/serviço/dia).
8. **Configurações:** solicitar limite diário/horário de descanso (registrado como **SIMULADO**; o limite gera alertas no Nest) e atalhos para os serviços originais.

## Telas do pré-projeto × MVP
| Prevista | No MVP |
|---|---|
| Login e cadastro | `LoginFrame` (conta local) |
| Dashboard | `DashboardScreen` |
| Serviços vinculados | `ServicesScreen` |
| Detalhes por serviço/dispositivo | `DetailsScreen` (por serviço e período) |
| Relatórios e gráficos | `ReportsScreen` + `BarChart` (Java2D) |
| Alertas e notificações | `AlertsScreen` |
| Configurações | `SettingsScreen` |

## Estrutura
```
br.com.nest
├── Main
├── model       ChildProfile, UsageSession, PlatformLink, Alert, SettingRequest, Guardian, Platforms
├── repository  Database (esquema + migração automática do nest.db antigo)
├── service     AuthService, NestService (regras e SQL), DemoService (sincronização simulada), UsageReportService
└── ui          LoginFrame, NestFrame, *Screen, BarChart
```
Bancos criados pela versão anterior são migrados automaticamente; os perfis antigos passam para a primeira conta criada.

## Limitações e próximos passos
- Integrações reais dependem de APIs oficiais/permissões de cada fabricante (OAuth, escopos, termos de uso).
- Segurança de produção: criptografia do banco, recuperação de senha, trilha de auditoria e conformidade com LGPD/ECA Digital (dados de menores).
- Ideias: notificações push, múltiplos dispositivos por perfil, gráficos mais ricos, empacotar JAR executável.
