# API do Sistema de Gerenciamento de Discipulados

O contrato executável está em [08-api.yaml](08-api.yaml), no padrão OpenAPI 3.1. Ele é a fonte de verdade para implementação, testes de contrato e Swagger UI.

## Visão geral

- URL-base local: `http://localhost:8080/api/v1`
- Formato: JSON em UTF-8; datas seguem ISO 8601 e regras de negócio usam `America/Sao_Paulo`.
- Autenticação: `Authorization: Bearer <accessToken>`.
- Recursos usam português, no plural: `/usuarios`, `/gerencias`, `/discipulados`, `/adolescentes` e `/encontros`.

## Convenções

Listagens paginadas aceitam `page` (inicia em `0`) e `size` (máximo `100`). Filtros específicos são definidos por rota, como `ativo`, `gerenciaId`, `discipuladoId`, `dataInicio` e `dataFim`.

Erros usam `application/problem+json` com `type`, `title`, `status`, `detail` e `traceId`. Os códigos usuais são `400`, `401`, `403`, `404` e `409`.

Usuários, adolescentes e discipulados não são excluídos fisicamente. Use `PATCH` com `ativo: false`, preservando histórico e auditoria.

## Autenticação

| Operação | Rota | Acesso |
| --- | --- | --- |
| Login | `POST /autenticacao/login` | Público |
| Renovar sessão | `POST /autenticacao/atualizar-token` | Público, com refresh token |
| Solicitar redefinição | `POST /autenticacao/esqueci-a-senha` | Público |
| Redefinir senha | `POST /autenticacao/redefinir-senha` | Público, com token único |
| Consultar sessão | `GET /autenticacao/eu` | Autenticado |

O refresh token é rotacionado a cada renovação. Tokens de redefinição não são expostos pela API e devem ser entregues por provedor transacional de e-mail.

## Autorização

| Recurso | ADMIN | GERENTE | DISCIPULADOR | CO_LIDER |
| --- | --- | --- | --- | --- |
| Usuários e perfis | Total | Não | Não | Não |
| Gerências | Total | Própria, leitura | Não | Não |
| Discipulados | Total | Da gerência | Próprio | Próprio, leitura limitada |
| Adolescentes | Total | Da gerência | Próprio | Próprio |
| Encontros e chamada | Total | Da gerência, leitura | Próprio | Próprio |
| Auditoria | Total | Não | Não | Não |

Além do perfil, a API valida o vínculo do usuário com o discipulado solicitado.

## Regras importantes

- Um adolescente possui somente um vínculo ativo por vez; transferências usam `POST /adolescentes/{adolescenteId}/vinculos` e preservam o histórico.
- Um discipulado tem exatamente um discipulador e até dois co-líderes.
- Um encontro é `REALIZADO` ou `CANCELADO`.
- Há uma única frequência por adolescente e encontro.
- Discipulador e co-líder alteram frequência por até três horas; após isso, somente `ADMIN` pode fazê-lo. Todas as alterações são auditadas.

## Domínios disponíveis

| Domínio | Rotas principais |
| --- | --- |
| Usuários | `/usuarios` |
| Estrutura | `/gerencias`, `/discipulados`, `/discipulados/{id}/co-lideres` |
| Cadastro | `/adolescentes`, `/adolescentes/{id}/vinculos` |
| Frequência | `/encontros`, `/encontros/{id}/frequencias`, `/encontros/{id}/visitantes` |
| Indicadores | `/painel/lider`, `/painel/gerencia` |
| Exportação | `/relatorios/mensal` |
| Auditoria | `/auditoria` |

Consulte o arquivo OpenAPI para payloads, enums, respostas e códigos de status completos.
