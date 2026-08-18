CREATE TABLE fichas_familia (
    id BIGSERIAL PRIMARY KEY,
    adolescente_id BIGINT NOT NULL REFERENCES adolescentes(id) ON DELETE CASCADE,
    cep VARCHAR(20) NOT NULL,
    rua VARCHAR(200) NOT NULL,
    numero VARCHAR(30) NOT NULL,
    complemento VARCHAR(120) NOT NULL,
    bairro VARCHAR(120) NOT NULL,
    cidade VARCHAR(120) NOT NULL,
    situacao_igreja VARCHAR(40) NOT NULL,
    atua_onde VARCHAR(200) NOT NULL,
    situacao_pais VARCHAR(40) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    desafio_financeiro BOOLEAN NOT NULL DEFAULT FALSE,
    desafio_emocional BOOLEAN NOT NULL DEFAULT FALSE,
    desafio_espiritual BOOLEAN NOT NULL DEFAULT FALSE,
    desafios_descricao VARCHAR(1000) NOT NULL,
    atividades_juntas VARCHAR(1000) NOT NULL,
    rotina_semana VARCHAR(1000) NOT NULL,
    irmao_dokmos VARCHAR(200) NOT NULL,
    pedido_oracao VARCHAR(1000) NOT NULL,
    intervencao VARCHAR(2000) NOT NULL,
    observacao_discipulador VARCHAR(1000) NOT NULL,
    observacao_gerente VARCHAR(1000) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ficha_familia_adolescente UNIQUE (adolescente_id),
    CONSTRAINT ck_ficha_familia_situacao_igreja CHECK (
        situacao_igreja IN (
            'FDV_ATUANTES',
            'FDV_NAO_ATUANTES',
            'OUTRA_IGREJA',
            'NAO_CRISTA',
            'AFASTADA',
            'NAO_CONSTA'
        )
    ),
    CONSTRAINT ck_ficha_familia_situacao_pais CHECK (
        situacao_pais IN (
            'CASADOS',
            'SEPARADOS',
            'FALECIDOS',
            'ADOTIVOS',
            'HOMOAFETIVOS',
            'NULOS',
            'NAO_CONSTA'
        )
    )
);

CREATE TABLE ficha_familia_responsaveis (
    id BIGSERIAL PRIMARY KEY,
    ficha_id BIGINT NOT NULL REFERENCES fichas_familia(id) ON DELETE CASCADE,
    ordem SMALLINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    parentesco VARCHAR(80) NOT NULL,
    data_nascimento DATE,
    estado_civil VARCHAR(80) NOT NULL,
    profissao VARCHAR(120) NOT NULL,
    telefone VARCHAR(40) NOT NULL,
    email VARCHAR(120) NOT NULL,
    interesse_pessoal VARCHAR(200) NOT NULL,
    CONSTRAINT uk_ficha_familia_responsavel_ordem UNIQUE (ficha_id, ordem),
    CONSTRAINT ck_ficha_familia_responsavel_ordem CHECK (ordem IN (1, 2))
);

CREATE INDEX ix_fichas_familia_adolescente ON fichas_familia(adolescente_id);
CREATE INDEX ix_ficha_familia_responsaveis_ficha ON ficha_familia_responsaveis(ficha_id);
