# Revisão das decisões do projeto

Este documento consolida as decisões de produto, domínio, arquitetura e operação do SGD tomadas até 18 de julho de 2026. Ele registra o estado efetivamente adotado no código e as decisões explícitas das últimas implementações, além de separar o que continua pendente.

## Como ler este documento

- **Adotada**: decisão vigente e refletida no projeto.
- **Implementada**: decisão adotada e já entregue no código.
- **Adiada**: decisão discutida, mas conscientemente retirada do escopo atual.
- **Pendente**: direção proposta que ainda depende de implementação ou confirmação.

O levantamento considera a documentação em `docs/`, o código atual, o histórico Git recente e as decisões registradas durante a implementação de NR1, NR2, NR4 e NR5. A referência técnica consultada foi a branch `codex/password-recovery-dashboard`, no commit `dcd00c3`.

## 1. Produto e escopo

### DEC-001 — O SGD substitui o controle manual de frequência

**Status:** Adotada.

O produto existe para substituir listas em papel por uma aplicação web que centraliza encontros, frequência, visitantes, histórico, indicadores e relatórios dos discipulados de adolescentes.

As visões do sistema são orientadas aos quatro perfis de negócio: `ADMIN`, `GERENTE`, `DISCIPULADOR` e `CO_LIDER`.

### DEC-002 — A entrega será evolutiva por domínio

**Status:** Adotada.

A evolução do produto segue a ordem geral: autenticação e segurança, estrutura organizacional, adolescentes, frequência, dashboards, relatórios e melhorias. Funcionalidades futuras não devem antecipar mudanças incompatíveis nos domínios já entregues.

## 2. Domínio e autorização

### DEC-003 — Papéis podem ser acumulados

**Status:** Implementada.

Um usuário pode possuir mais de um papel. Suas permissões e visões são a união dos papéis atribuídos, salvo nas telas cujo escopo é determinado pela associação de liderança. O papel `ADMIN` possui visão global quando a regra funcional assim determina.

### DEC-004 — Liderança é única por usuário

**Status:** Implementada.

Um usuário pode atuar como discipulador ou co-líder em apenas um discipulado no total, mesmo que possua os dois papéis. Cada discipulado tem exatamente um discipulador ativo e até dois co-líderes.

O painel do líder considera exclusivamente o discipulado no qual o usuário exerce liderança. A existência de nenhuma associação retorna ausência do recurso; mais de uma associação é tratada como conflito de integridade.

### DEC-005 — O escopo organizacional é aplicado no backend

**Status:** Implementada.

O backend é a autoridade sobre o acesso aos dados:

- administradores acessam o escopo global;
- gerentes acessam as gerências e os discipulados sob sua responsabilidade;
- discipuladores e co-líderes acessam somente o grupo que lideram;
- somente administradores transferem adolescentes entre discipulados.

Ocultar uma ação no frontend é apenas uma medida de experiência do usuário e não substitui a autorização no backend.

### DEC-006 — Vínculos atuais não apagam o histórico

**Status:** Implementada.

Um adolescente pertence a um único discipulado por vez, mas transferências e inativações preservam os registros no discipulado original. Consultas históricas devem usar a interseção entre o período solicitado e a vigência dos vínculos, além de preservar frequências já registradas.

### DEC-007 — Inatividade não significa exclusão

**Status:** Implementada.

Adolescentes sem participação por três meses tornam-se inativos, sem perda de histórico. Inativação e transferência são mudanças de estado ou vínculo; não são exclusões físicas dos registros históricos.

## 3. Arquitetura e contratos

### DEC-008 — O projeto é um monorepo web

**Status:** Adotada.

O repositório reúne:

- frontend em React 18, TypeScript, Vite, Material UI e ECharts;
- backend em Java 21, Spring Boot, Spring Security, Spring Data JPA e Hibernate;
- PostgreSQL como banco de produção;
- Docker e Docker Compose para execução integrada;
- Nginx para servir o frontend em container;
- GitHub Actions para integração contínua.

O frontend e o backend permanecem projetos independentes dentro do mesmo repositório, com contratos HTTP explícitos entre eles.

### DEC-009 — O banco evolui exclusivamente por Flyway

**Status:** Implementada.

Toda alteração de esquema deve ser criada como uma nova migration em `backend/src/main/resources/db/migration/`. O Hibernate usa `ddl-auto=validate`; ele valida o esquema, mas não é responsável por criá-lo ou alterá-lo em produção.

Migrations já publicadas não devem ser reescritas para representar mudanças novas.

### DEC-010 — A API é versionada e erros não expõem detalhes internos

**Status:** Implementada.

Os endpoints de negócio usam o prefixo `/api/v1`; saúde permanece disponível em `/api/health` e os endpoints operacionais do Actuator ficam em `/actuator`.

Falhas HTTP são normalizadas como `application/problem+json`. Respostas de autenticação e autorização não expõem exceções internas e incluem um identificador de rastreio.

### DEC-011 — Consultas analíticas podem usar SQL nativo

**Status:** Adotada.

Painéis, auditoria e relatórios podem usar consultas nativas quando agregação e desempenho justificarem. Essas consultas devem ser exercitadas em PostgreSQL real, não apenas no H2.

Tipos retornados por drivers diferentes devem ser normalizados na fronteira da aplicação. Para timestamps nativos, a projeção de auditoria aceita os tipos temporais dos dois drivers e os converte para `Instant`.

## 4. Autenticação e segurança

### DEC-012 — A API usa autenticação stateless com JWT

**Status:** Implementada.

A API não mantém sessão HTTP. O access token JWT tem validade curta e o refresh token é persistido somente como hash. A renovação rotaciona o refresh token anterior; logout e redefinição de senha revogam tokens aplicáveis.

Usuários inativos não podem autenticar nem renovar sessão. Senhas locais são armazenadas com BCrypt e nunca em texto puro.

### DEC-013 — O administrador inicial é criado por configuração

**Status:** Implementada.

O primeiro administrador pode ser criado na inicialização a partir de `ADMIN_INITIAL_EMAIL` e `ADMIN_INITIAL_PASSWORD`. A criação é idempotente por e-mail e as credenciais não devem ser versionadas.

### DEC-014 — OAuth continua parte do escopo

**Status:** Adotada.

Google e Microsoft são os provedores previstos para login OAuth. Identidades externas são associadas a usuários locais, sem remover o fluxo de credenciais locais para os perfis que o utilizam.

## 5. Recuperação de senha e e-mail

### DEC-015 — Recuperação de senha é pública para todos os perfis com senha local

**Status:** Implementada.

`ADMIN`, `GERENTE`, `DISCIPULADOR` e `CO_LIDER` podem usar o mesmo fluxo público de recuperação quando possuem credencial local. A funcionalidade não amplia a permissão de um usuário para alterar a senha de terceiros e não se aplica a identidades exclusivamente SSO.

O frontend implementa as telas públicas sem adicionar um roteador: a navegação usa History/URL API e reconhece `/esqueci-senha` e `/redefinir-senha?token=...`.

### DEC-016 — O fluxo não revela a existência de contas

**Status:** Implementada.

A solicitação de recuperação retorna uma resposta neutra para e-mail inexistente, usuário inativo e falha de entrega. Tokens são aleatórios, persistidos somente como hash, têm validade configurável, são de uso único e invalidam solicitações anteriores do mesmo usuário.

Após a troca de senha, todos os refresh tokens do usuário são revogados e a operação é auditada.

### DEC-017 — Produção envia e-mail por SMTP e falha cedo sem configuração básica

**Status:** Implementada.

Fora dos perfis `local` e `test`, o link de redefinição é enviado por SMTP. `PASSWORD_RESET_FRONTEND_URL` e `MAIL_FROM` são obrigatórios para criar o notificador de produção; o ambiente via Compose também exige host e credenciais SMTP.

Segredos de e-mail pertencem ao gerenciador de segredos da plataforma e nunca ao Git, à imagem, ao frontend ou aos logs. Produção não deve usar os perfis `local` ou `test`, pois o notificador local existe apenas para desenvolvimento e testes.

## 6. Frequência, painéis e relatórios

### DEC-018 — Frequência pertence a um encontro e alterações são auditáveis

**Status:** Implementada.

Encontros podem ser realizados ou não realizados. Discipulador e co-líder registram presença no próprio grupo. A frequência pode ser alterada durante uma janela de três horas; após esse prazo, somente administradores podem alterá-la. Alterações relevantes geram auditoria.

### DEC-019 — O progresso individual usa taxa mensal de presença

**Status:** Implementada como NR4.

O painel do líder reutiliza um intervalo configurável e mostra um discípulo por vez. A métrica é:

```text
presentes / (presentes + ausentes) * 100
```

Somente encontros realizados e registros persistidos entram no cálculo. A resposta inclui totais e evolução mensal individual, contempla vinculados sem frequência e ex-integrantes com registros no período e evita consultas por discípulo.

Quando não existe denominador, o percentual individual é `null` e a interface apresenta “sem registros”, nunca `0%`. O gráfico ECharts possui tabela equivalente para navegação por teclado e leitores de tela.

### DEC-020 — O relatório diário omite o identificador visual do encontro

**Status:** Implementada como NR1.

O relatório diário apresenta a data, sem exibir `Encontro: #<id>`. O `encontroId` foi preservado no contrato e como chave interna para evitar uma quebra de API desnecessária.

O escopo respeita a união dos papéis, inclui somente encontros realizados e preserva participantes históricos. Cada encontro gera uma página independente para impressão.

### DEC-021 — Presença e ausência recebem ênfase diferente somente na impressão

**Status:** Implementada como NR2.

No relatório impresso, “Presente” usa peso `700` e “Ausente” usa peso `400`. A regra é derivada dos valores semânticos `PRESENTE` e `AUSENTE` e fica restrita a `@media print`, sem modificar a apresentação normal da tela.

### DEC-022 — A impressão atual usa o recurso nativo do navegador

**Status:** Implementada.

O relatório diário é formatado em A4 e impresso ou salvo como PDF pelo diálogo nativo do navegador. JasperReports e exportação Excel continuam no roadmap; não fazem parte desta implementação do relatório diário.

## 7. Qualidade e integração contínua

### DEC-023 — A estratégia de testes combina isolamento e banco real

**Status:** Implementada.

Testes unitários e HTTP rápidos usam o perfil `test` e H2 quando apropriado. Consultas nativas, migrations e diferenças de driver são validadas também com PostgreSQL 16 via Testcontainers e Flyway.

Os testes Testcontainers podem ser ignorados localmente quando Docker não está disponível, mas devem executar no CI. O perfil `test` deve permanecer ativo nesses testes para que integrações externas de produção, como SMTP, não sejam inicializadas.

### DEC-024 — O CI valida os dois projetos separadamente

**Status:** Implementada.

O job de backend executa `mvn clean verify` com Java 21. O job de frontend instala com lockfile congelado e executa lint e build com Node 22 e pnpm 11.7.0.

A suíte frontend existe e deve continuar sendo executada antes de entregas, embora o workflow atual ainda não tenha uma etapa explícita de `pnpm test`.

### DEC-025 — Commits devem ser semânticos e separados por responsabilidade

**Status:** Adotada.

Mudanças independentes devem formar commits coerentes de `feat`, `fix`, `test` ou `docs`, evitando misturar autenticação, painel, relatório e infraestrutura no mesmo commit.

## 8. Decisões adiadas e pendências conhecidas

### DEC-026 — Redesenho da seleção de co-líderes

**Status:** Adiada como NR3.

O NR3 foi explicitamente retirado do escopo da última implementação. A proposta discutida foi substituir a lista de checkboxes por um `Autocomplete` múltiplo assíncrono, com busca no servidor, chips e manutenção do limite de dois co-líderes. Como não foi implementada, essa proposta não deve ser tratada como contrato vigente.

### DEC-027 — Endurecimento operacional do envio de e-mail

**Status:** Pendente.

Antes de uma operação de maior escala, ainda devem ser decididos e implementados:

- timeouts explícitos de conexão, leitura e escrita SMTP;
- rate limiting neutro por e-mail e IP no endpoint de recuperação;
- retentativas por fila ou outbox;
- tratamento de bounces, reclamações e supressões;
- observabilidade e alertas de falha de entrega;
- remoção antecipada do token da barra de endereço e política `Referrer-Policy` restritiva.

### DEC-028 — Metas automáticas de cobertura

**Status:** Pendente.

Foi proposta a adoção de JaCoCo com limites progressivos de linhas e ramificações, mas o `pom.xml` atual ainda não configura o plugin nem um `jacoco:check`. A cobertura deve priorizar ramos de autenticação, frequência, escopo organizacional e consultas PostgreSQL, e não cobertura artificial de código sem regra.

### DEC-029 — Relatórios adicionais

**Status:** Pendente.

Exportações dedicadas em PDF e Excel continuam previstas. A impressão HTML atual atende ao relatório diário, mas não encerra o item mais amplo de relatórios do roadmap.

## 9. Divergências documentais identificadas

Alguns textos antigos deixaram de representar o estado do código:

- o `README.md` ainda afirma que a camada de domínio não foi implementada;
- o mesmo arquivo afirma que o envio de recuperação ainda precisa ser conectado a um provedor, embora o SMTP de produção já esteja implementado;
- `docs/07-arquitetura.md` cita JasperReports como tecnologia de relatórios, enquanto o relatório diário atual usa HTML/CSS e impressão do navegador;
- o pipeline executa testes frontend nas validações manuais, mas o workflow versionado contém somente lint e build para esse projeto.

Até que esses documentos sejam atualizados, o código, o contrato OpenAPI e as decisões deste arquivo são a referência para o comportamento já entregue.

## 10. Manutenção deste registro

Uma nova decisão relevante deve:

1. receber um identificador `DEC-xxx`;
2. declarar seu status;
3. explicar a decisão e seus limites, não apenas a alteração de código;
4. indicar quando substitui ou invalida uma decisão anterior;
5. mover propostas não confirmadas para a seção de pendências.

Mudanças de redação que apenas esclareçam uma decisão existente podem editar a entrada atual. Mudanças de direção devem criar uma nova entrada e marcar a anterior como substituída, preservando o histórico.
