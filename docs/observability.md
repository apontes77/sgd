# Observabilidade com OpenTelemetry

O backend usa o OpenTelemetry Java Agent sem Collector. Em produção, ele envia traces, métricas e logs diretamente ao endpoint OTLP configurado no Render. Sem `OTEL_EXPORTER_OTLP_ENDPOINT`, os três exporters ficam desabilitados, preservando a execução local sem erros de conexão.

## Identidade e correlação

Toda telemetria deve conter:

- `service.name=sgd-api`;
- `deployment.environment.name=production`;
- `service.version=<commit do Render>`;
- `trace_id` e `span_id` nos logs gerados dentro de uma requisição.

As respostas `application/problem+json` usam o trace ID corrente do OpenTelemetry. Quando não existe um span válido, como em testes unitários isolados, a aplicação usa um UUID como fallback.

## Dashboards mínimos

### Serviço

- requisições por segundo agrupadas por `http.route`;
- taxa de respostas 5xx e 4xx;
- duração p50, p95 e p99 de `http.server.request.duration`;
- disponibilidade dos testes sintéticos;
- versões da aplicação ativas no intervalo.

### JVM e banco

- heap usado e limite;
- pausas e frequência de GC;
- threads e CPU do processo;
- conexões Hikari ativas, ociosas e pendentes;
- duração e erros dos spans JDBC, sem valores de parâmetros SQL.

### Logs e erros

- volume por severidade;
- principais exceções e rotas afetadas;
- erros por `service.version`;
- tabela de logs recentes com link para o trace correspondente.

Os nomes exatos das consultas variam entre vendors. OpenTelemetry torna os sinais e atributos portáveis; o formato de dashboard, alertas e métricas derivadas de spans continua específico do destino.

## Verificações sintéticas e alertas

Configure no vendor:

- `GET` da URL raiz do frontend a cada minuto;
- `GET /actuator/health/readiness` da API a cada minuto;
- alerta após três falhas consecutivas;
- alerta de 5xx acima de 2% por cinco minutos;
- alerta de p95 acima de 1 segundo por dez minutos;
- alerta de heap sustentado acima de 85%.

O site estático não possui runtime OTEL nem RUM nesta versão. Erros executados no navegador exigirão uma etapa posterior com endpoint público de ingestão, controle de abuso e política de privacidade.

## Proteção de dados

Não capture ou registre:

- senhas, JWTs e refresh tokens;
- tokens de redefinição de senha;
- cabeçalho `Authorization`;
- corpos completos de requisição/resposta;
- parâmetros SQL ou dados pessoais de usuários e adolescentes;
- credenciais do banco, SMTP ou OTLP.

Ao habilitar novos atributos do agente ou MDC, use uma lista explícita. Nunca habilite captura irrestrita de headers, parâmetros ou MDC em produção.

## Quando introduzir um Collector

Adicione um OpenTelemetry Collector como serviço separado somente quando houver pelo menos uma destas necessidades:

- mais de um serviço enviando telemetria;
- exportação simultânea para mais de um destino;
- filtragem ou mascaramento central de dados;
- tail sampling;
- transformação de atributos ou roteamento por ambiente;
- requisitos de resiliência que justifiquem operar outra peça de infraestrutura.
