# Observabilidade

O backend não usa OpenTelemetry nesta versão. A correlação de erros nas respostas `application/problem+json` usa um `traceId` gerado como UUID por requisição/erro.

## Saúde operacional

- `GET /actuator/health/readiness` — readiness da API e do banco
- Métricas de memória e restarts no painel do Render
- Logs do serviço no Render

## Proteção de dados

Não registre senhas, JWTs, refresh tokens, tokens de redefinição, cabeçalho `Authorization`, corpos completos de requisição/resposta, parâmetros SQL ou dados pessoais.
