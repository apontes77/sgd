-- Backfill de fichas a partir dos contatos familiares do adolescente (RN048).
INSERT INTO fichas_familia (
    adolescente_id,
    cep, rua, numero, complemento, bairro, cidade,
    situacao_igreja, atua_onde, situacao_pais, descricao,
    desafio_financeiro, desafio_emocional, desafio_espiritual, desafios_descricao,
    atividades_juntas, rotina_semana, irmao_dokmos, pedido_oracao, intervencao,
    observacao_discipulador, observacao_gerente
)
SELECT
    a.id,
    'Não consta', 'Não consta', 'Não consta', 'Não consta', 'Não consta', 'Não consta',
    'NAO_CONSTA', 'Não consta', 'NAO_CONSTA', 'Não consta',
    FALSE, FALSE, FALSE, 'Não consta',
    'Não consta', 'Não consta', 'Não consta', 'Não consta', 'Não consta',
    'Não consta', 'Não consta'
FROM adolescentes a
WHERE NOT EXISTS (
    SELECT 1 FROM fichas_familia f WHERE f.adolescente_id = a.id
);

-- Responsável 1: prioriza mãe; senão responsável genérico; senão "Não consta".
INSERT INTO ficha_familia_responsaveis (
    ficha_id, ordem, nome, parentesco, data_nascimento, estado_civil, profissao, telefone, email, interesse_pessoal
)
SELECT
    f.id,
    1,
    CASE
        WHEN a.nome_mae IS NOT NULL AND btrim(a.nome_mae) <> '' THEN a.nome_mae
        WHEN a.responsavel_nome IS NOT NULL AND btrim(a.responsavel_nome) <> '' THEN a.responsavel_nome
        ELSE 'Não consta'
    END,
    CASE
        WHEN a.nome_mae IS NOT NULL AND btrim(a.nome_mae) <> '' THEN 'Mãe'
        WHEN a.responsavel_nome IS NOT NULL AND btrim(a.responsavel_nome) <> '' THEN 'Responsável'
        ELSE 'Não consta'
    END,
    NULL,
    'Não consta',
    'Não consta',
    CASE
        WHEN a.nome_mae IS NOT NULL AND btrim(a.nome_mae) <> '' THEN COALESCE(NULLIF(btrim(a.telefone_mae), ''), 'Não consta')
        WHEN a.responsavel_nome IS NOT NULL AND btrim(a.responsavel_nome) <> '' THEN COALESCE(NULLIF(btrim(a.responsavel_telefone), ''), 'Não consta')
        ELSE 'Não consta'
    END,
    'Não consta',
    'Não consta'
FROM fichas_familia f
JOIN adolescentes a ON a.id = f.adolescente_id
WHERE NOT EXISTS (
    SELECT 1 FROM ficha_familia_responsaveis r WHERE r.ficha_id = f.id AND r.ordem = 1
);

-- Responsável 2: prioriza pai; senão "Não consta".
INSERT INTO ficha_familia_responsaveis (
    ficha_id, ordem, nome, parentesco, data_nascimento, estado_civil, profissao, telefone, email, interesse_pessoal
)
SELECT
    f.id,
    2,
    CASE
        WHEN a.nome_pai IS NOT NULL AND btrim(a.nome_pai) <> '' THEN a.nome_pai
        ELSE 'Não consta'
    END,
    CASE
        WHEN a.nome_pai IS NOT NULL AND btrim(a.nome_pai) <> '' THEN 'Pai'
        ELSE 'Não consta'
    END,
    NULL,
    'Não consta',
    'Não consta',
    CASE
        WHEN a.nome_pai IS NOT NULL AND btrim(a.nome_pai) <> '' THEN COALESCE(NULLIF(btrim(a.telefone_pai), ''), 'Não consta')
        ELSE 'Não consta'
    END,
    'Não consta',
    'Não consta'
FROM fichas_familia f
JOIN adolescentes a ON a.id = f.adolescente_id
WHERE NOT EXISTS (
    SELECT 1 FROM ficha_familia_responsaveis r WHERE r.ficha_id = f.id AND r.ordem = 2
);

ALTER TABLE adolescentes
    DROP COLUMN IF EXISTS nome_mae,
    DROP COLUMN IF EXISTS telefone_mae,
    DROP COLUMN IF EXISTS nome_pai,
    DROP COLUMN IF EXISTS telefone_pai,
    DROP COLUMN IF EXISTS responsavel_nome,
    DROP COLUMN IF EXISTS responsavel_telefone;
