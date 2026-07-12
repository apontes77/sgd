CREATE TABLE adolescentes (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    data_nascimento DATE NOT NULL,
    telefone VARCHAR(40),
    instagram VARCHAR(120),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vinculos_adolescente_discipulado (
    id BIGSERIAL PRIMARY KEY,
    adolescente_id BIGINT NOT NULL REFERENCES adolescentes(id) ON DELETE RESTRICT,
    discipulado_id BIGINT NOT NULL REFERENCES discipulados(id) ON DELETE RESTRICT,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_vinculo_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio),
    CONSTRAINT ck_vinculo_ativo_fim CHECK ((ativo AND data_fim IS NULL) OR (NOT ativo AND data_fim IS NOT NULL))
);

CREATE UNIQUE INDEX uk_vinculo_adolescente_ativo
    ON vinculos_adolescente_discipulado(adolescente_id) WHERE ativo;
CREATE INDEX ix_vinculo_discipulado_ativo
    ON vinculos_adolescente_discipulado(discipulado_id, ativo);
CREATE INDEX ix_vinculo_adolescente_periodo
    ON vinculos_adolescente_discipulado(adolescente_id, data_inicio, data_fim);
