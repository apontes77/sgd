# Plano de qualidade — estrutura organizacional

Este documento fixa os casos de aceite da fase de estrutura organizacional
descrita em `08-api.yaml`. Ele serve de base para testes de serviço, integração
HTTP e validação de migrações.

## Escopo

- `POST`, `GET` e `PATCH /api/v1/gerencias`;
- `POST`, `GET` e `PATCH /api/v1/discipulados`;
- `PUT /api/v1/discipulados/{discipuladoId}/co-lideres`;
- regras RN003, RN004 e RN005.

Os testes devem usar usuários ativos, com o perfil exigido, salvo quando o
caso declarar o contrário. `409 Conflict` representa violação de regra de
negócio; `404 Not Found`, referência inexistente; e `403 Forbidden`, ausência
de autorização.

## Casos de aceite

| ID | Regra | Cenário | Resultado esperado |
| --- | --- | --- | --- |
| EO-01 | RN005 | Administrador cria gerência com gerente ativo de perfil `GERENTE`. | `201`; resposta contém o gerente informado. |
| EO-02 | RN005 | Criar ou alterar gerência com `gerenteId` inexistente. | `404`. |
| EO-03 | RN005 | Criar ou alterar gerência com usuário inativo ou sem perfil `GERENTE`. | `409`. |
| EO-04 | RN005 | Atualizar uma gerência troca seu gerente. | `200`; exatamente um gerente permanece associado à gerência. |
| EO-05 | RN003 | Administrador cria discipulado com discipulador ativo de perfil `DISCIPULADOR`. | `201`; resposta contém exatamente aquele discipulador. |
| EO-06 | RN003 | Criar ou alterar discipulado sem `discipuladorId`, com id inexistente, usuário inativo ou sem perfil adequado. | `400` para campo ausente; `404` para id inexistente; `409` para usuário inválido. |
| EO-07 | RN003 | Atualizar o discipulador de um discipulado. | `200`; o novo discipulador substitui o anterior, sem estado intermediário sem discipulador. |
| EO-08 | RN004 | Definir zero, um ou dois co-líderes ativos com perfil `CO_LIDER`. | `200`; resposta preserva a lista enviada. |
| EO-09 | RN004 | Definir três co-líderes. | `400` na validação do contrato (`maxItems: 2`); a lista anterior não é alterada. |
| EO-10 | RN004 | Enviar o mesmo `usuarioId` mais de uma vez. | `400` ou `409`; a resposta não pode conter duplicatas. |
| EO-11 | RN004 | Definir co-líder inexistente, inativo ou sem perfil `CO_LIDER`. | `404` para id inexistente; `409` para usuário inválido. |
| EO-12 | Autorização | Usuário não autenticado tenta mutação organizacional. | `401`. |
| EO-13 | Autorização | Usuário autenticado sem `ADMIN` tenta criar, alterar ou trocar co-líderes. | `403`. |
| EO-14 | Inativação | `PATCH /discipulados/{id}` com `ativo=false`. | `200`; o recurso continua consultável quando solicitado com `ativo=false`, sem exclusão física. |
| EO-15 | RN028 | Associar como discipulador um usuário que já lidera ou co-lidera outro discipulado. | `409`; nenhuma associação é modificada. |
| EO-16 | RN028 | Associar como co-líder um usuário que já lidera ou co-lidera outro discipulado. | `409`; a lista anterior permanece inalterada. |
| EO-17 | RN015 | Usuário acumula `GERENTE + DISCIPULADOR` ou `ADMIN + DISCIPULADOR`. | Mantém os painéis do papel administrativo/gerencial e recebe também “Meu discipulado”. |

## Testes automatizados previstos

### Serviço

1. Criar discipulado rejeita disciplinador que não seja um usuário ativo com
   perfil `DISCIPULADOR`.
2. Atualizar discipulado mantém sempre um único disciplinador.
3. Substituir co-líderes aceita no máximo dois, elimina a associação anterior
   e rejeita ids repetidos e perfis inválidos.
4. Criar ou trocar gerente exige um usuário ativo com perfil `GERENTE`.
5. Criar ou trocar discipulador e co-líder rejeita qualquer usuário já associado a outro discipulado em uma dessas funções.

### Integração HTTP

1. Validar `201`, `200`, `400`, `401`, `403`, `404` e `409` listados acima.
2. Garantir que o payload de três co-líderes não persista modificação parcial.
3. Confirmar que os campos obrigatórios de `GerenciaRequest` e
   `DiscipuladoRequest` retornam erro de validação no formato Problem Details.
4. Confirmar `409` sem persistência parcial quando duas requisições concorrentes tentarem associar o mesmo usuário a discipulados diferentes.

### Banco de dados

1. `discipulados.discipulador_id` deve ser `NOT NULL` e referenciar
   `usuarios`.
2. `gerencias.gerente_id` deve ser `NOT NULL` e referenciar `usuarios`.
3. A tabela de co-líderes deve ter chave única em
   `(discipulado_id, usuario_id)`; o teto de dois é uma regra transacional da
   aplicação, protegida por teste de concorrência quando houver suporte.
4. A exclusividade do usuário entre `discipulados.discipulador_id` e
   `discipulado_co_lideres.usuario_id` deve ser protegida transacionalmente,
   considerando as duas relações como uma única ocupação de liderança.

## Pontos a confirmar antes da automação

- O OpenAPI exige os ids, mas não declara que gerente, discipulador e
  co-líder devem ter respectivamente os perfis `GERENTE`, `DISCIPULADOR` e
  `CO_LIDER`, nem que devem estar ativos. Este plano adota essa interpretação
  por coerência com os papéis do domínio; ela deve ser incorporada ao contrato
  ou formalizada como decisão de arquitetura.
- O contrato aceita uma lista de co-líderes, mas não declara unicidade dos
  itens. A implementação deve rejeitar duplicatas para não contrariar a
  relação N:N representada no DER.
- Os endpoints `PATCH` reutilizam `GerenciaRequest` e `DiscipuladoRequest`,
  cujos campos estruturais são obrigatórios, embora o resumo e os serviços
  suportem atualização parcial. Criar schemas próprios de atualização evita
  que clientes sejam obrigados a reenviar campos que não desejam alterar.
- A regra RN005 diz que uma gerência possui um gerente. Ela não limita um
  gerente a uma única gerência; portanto os testes não devem assumir essa
  exclusividade.
