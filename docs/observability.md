# Observabilidade

O backend não usa OpenTelemetry nesta versão. A correlação de erros nas respostas `application/problem+json` usa um `traceId` gerado como UUID por requisição/erro.

## Saúde operacional

| Endpoint | Autenticação | O que cobre |
| --- | --- | --- |
| `GET /api/health` | Público | Liveness simples (`status` + `timestamp`); **não** consulta o banco |
| `GET /actuator/health/readiness` | Público | Readiness da aplicação e do indicador `db` |
| `GET /actuator/health/**` | Público (demais probes Spring) | Uso operacional; não faz parte do contrato de negócio (`docs/08-api.yaml`) |

Em produção (Render): métricas de memória/restarts e logs do serviço no painel. A API roda com **uma** instância — jobs `@Scheduled` executam em cada réplica.

## Jobs agendados (`America/Sao_Paulo`)

| Job | Cron | Efeito |
| --- | --- | --- |
| `InativacaoAdolescenteJob` | `0 15 3 * * *` (03:15) | RN019 — `ativo=false` se cadastro > 3 meses sem `PRESENTE` no período |
| `FechamentoFrequenciaJob` | `0 20 3 * * *` (03:20) | RN047 — sextas de discipulado **padrão** sem chamada viram `NAO_REALIZADO` com `fechamentoAutomatico` |
| `AfastamentoGoeAutomatico` | na subida (`ApplicationRunner`) | RN043 — aplica afastamento GOE a quem já cumpre o critério nos grupos regulares |

Formação (`emFormacao`) fica fora do fechamento automático e do afastamento GOE na chamada.

## Proteção de dados

Não registre senhas, JWTs, refresh tokens, tokens de redefinição, cabeçalho `Authorization`, corpos completos de requisição/resposta, parâmetros SQL ou dados pessoais. Tokens de sessão no frontend ficam em `localStorage` (`sgd.access-token` / `sgd.refresh-token`) — trate dumps de browser/DevTools como material sensível.
