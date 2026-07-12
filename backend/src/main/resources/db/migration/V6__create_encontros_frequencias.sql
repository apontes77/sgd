CREATE TABLE encontros (
    id BIGSERIAL PRIMARY KEY,
    discipulado_id BIGINT NOT NULL REFERENCES discipulados(id) ON DELETE RESTRICT,
    data DATE NOT NULL,
    situacao VARCHAR(12) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_encontro_situacao CHECK (situacao IN ('REALIZADO', 'CANCELADO'))
);

CREATE TABLE frequencias (
    id BIGSERIAL PRIMARY KEY,
    encontro_id BIGINT NOT NULL REFERENCES encontros(id) ON DELETE RESTRICT,
    adolescente_id BIGINT NOT NULL REFERENCES adolescentes(id) ON DELETE RESTRICT,
    situacao VARCHAR(10) NOT NULL,
    registrada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_frequencia_encontro_adolescente UNIQUE (encontro_id, adolescente_id),
    CONSTRAINT ck_frequencia_situacao CHECK (situacao IN ('PRESENTE', 'AUSENTE'))
);

CREATE TABLE visitantes (
    id BIGSERIAL PRIMARY KEY,
    encontro_id BIGINT NOT NULL UNIQUE REFERENCES encontros(id) ON DELETE RESTRICT,
    quantidade INTEGER NOT NULL DEFAULT 0,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_visitantes_quantidade CHECK (quantidade >= 0)
);

CREATE INDEX ix_encontros_discipulado_data ON encontros(discipulado_id, data);
CREATE INDEX ix_frequencias_adolescente ON frequencias(adolescente_id);
CREATE INDEX ix_auditoria_entidade_usuario ON auditoria(entidade, usuario_id);
