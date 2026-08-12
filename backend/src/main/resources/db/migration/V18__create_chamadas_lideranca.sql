CREATE TABLE chamadas_lideranca (
    id BIGSERIAL PRIMARY KEY,
    data DATE NOT NULL,
    observacao_geral VARCHAR(1000),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_chamada_lideranca_data UNIQUE (data)
);

CREATE TABLE chamadas_lideranca_discipulados (
    id BIGSERIAL PRIMARY KEY,
    chamada_id BIGINT NOT NULL REFERENCES chamadas_lideranca(id) ON DELETE CASCADE,
    discipulado_id BIGINT NOT NULL REFERENCES discipulados(id) ON DELETE RESTRICT,
    observacao VARCHAR(500),
    CONSTRAINT uk_chamada_lideranca_discipulado UNIQUE (chamada_id, discipulado_id)
);

CREATE TABLE presencas_lideranca (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES chamadas_lideranca_discipulados(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    papel VARCHAR(20) NOT NULL,
    situacao VARCHAR(10) NOT NULL,
    CONSTRAINT uk_presenca_lideranca_item_usuario UNIQUE (item_id, usuario_id),
    CONSTRAINT ck_presenca_lideranca_papel CHECK (papel IN ('DISCIPULADOR', 'CO_LIDER')),
    CONSTRAINT ck_presenca_lideranca_situacao CHECK (situacao IN ('PRESENTE', 'AUSENTE'))
);

CREATE INDEX ix_chamadas_lideranca_discipulados_chamada ON chamadas_lideranca_discipulados(chamada_id);
CREATE INDEX ix_presencas_lideranca_item ON presencas_lideranca(item_id);
