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
| Adolescentes — leitura | Total | Da gerência | Próprio | Próprio |
| Adolescentes — cadastro/edição/inativação | Total | Não, salvo se acumular papel de líder | Próprio | Próprio |
| Adolescentes — transferência | Total | Não | Não | Não |
| Encontros e chamada | Total | Da gerência, leitura | Próprio | Próprio |
| Relatório diário de frequência | Todos | Gerências ativas próprias | Grupos liderados | Grupos liderados |
| Painel do discipulado | Se também for líder | Se também for líder | Próprio | Próprio |
| Auditoria | Total | Não | Não | Não |

Além do perfil, a API valida o vínculo do usuário com o discipulado solicitado.

Os acessos são cumulativos: `GERENTE + DISCIPULADOR` recebe “Minha gerência” e “Meu discipulado”; `ADMIN + DISCIPULADOR` recebe o painel administrativo e “Meu discipulado”. Administradores não exercem o papel de gerente no cenário atual, sem impedir futuras combinações de perfis.

## Regras importantes

- Um adolescente possui somente um vínculo ativo por vez; transferências usam `POST /adolescentes/{adolescenteId}/vinculos` e preservam o histórico.
- Um discipulado tem exatamente um discipulador e até dois co-líderes.
- Um encontro é `REALIZADO` ou `CANCELADO`; `CANCELADO` é apresentado como “Não realizado”, exige justificativa e somente pode ser definido ou desfeito por `ADMIN`.
- Há uma única frequência por adolescente e encontro.
- A chamada usa os vínculos ativos atuais sem comparar a data de início do vínculo com a data do encontro; participantes anteriormente registrados permanecem disponíveis para preservar o histórico.
- Discipulador e co-líder alteram frequência por até três horas; após isso, somente `ADMIN` pode fazê-lo. Todas as alterações são auditadas.
- Discipulador e co-líder registram encontros realizados, chamadas e visitantes somente nos discipulados em que exercem liderança; `ADMIN` pode registrar em qualquer discipulado ativo e marcar encontros como não realizados.
- Gerentes visualizam no painel as não realizações e justificativas dos discipulados da própria gerência, sem permissão de alteração.
- Discipulador e co-líder cadastram, atualizam e inativam adolescentes somente no próprio discipulado; somente `ADMIN` transfere adolescentes entre grupos.
- Um usuário exerce liderança em apenas um discipulado no total, seja como discipulador ou co-líder.
- Permissões e painéis são cumulativos. O painel “Meu discipulado” sempre usa a associação de liderança, mesmo quando o usuário também é `ADMIN` ou `GERENTE`.
- O relatório diário reúne a união dos escopos dos perfis do usuário, considera somente encontros realizados e exibe apenas frequências persistidas; a impressão A4 e o salvamento em PDF usam o diálogo nativo do navegador.

## Domínios disponíveis

| Domínio | Rotas principais |
| --- | --- |
| Usuários | `/usuarios` |
| Estrutura | `/gerencias`, `/discipulados`, `/discipulados/liderados`, `/discipulados/{id}/co-lideres` |
| Cadastro | `/adolescentes`, `/adolescentes/{id}/vinculos` |
| Frequência | `/encontros`, `/encontros/{id}/frequencias`, `/encontros/{id}/visitantes` |
| Indicadores | `/painel/lider`, `/painel/gerencia` |
| Relatórios | `/relatorios/frequencia-diaria`, `/relatorios/mensal` |
| Auditoria | `/auditoria` |

Consulte o arquivo OpenAPI para payloads, enums, respostas e códigos de status completos.
