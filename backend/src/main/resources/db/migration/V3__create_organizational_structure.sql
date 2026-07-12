CREATE TABLE gerencias (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    gerente_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE discipulados (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    sexo VARCHAR(10) NOT NULL,
    gerencia_id BIGINT NOT NULL REFERENCES gerencias(id) ON DELETE RESTRICT,
    discipulador_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_discipulados_sexo CHECK (sexo IN ('MASCULINO', 'FEMININO'))
);

CREATE TABLE discipulado_co_lideres (
    discipulado_id BIGINT NOT NULL REFERENCES discipulados(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    PRIMARY KEY (discipulado_id, usuario_id)
);

CREATE INDEX ix_gerencias_gerente_id ON gerencias(gerente_id);
CREATE INDEX ix_discipulados_gerencia_id ON discipulados(gerencia_id);
CREATE INDEX ix_discipulados_discipulador_id ON discipulados(discipulador_id);
CREATE INDEX ix_discipulado_co_lideres_usuario_id ON discipulado_co_lideres(usuario_id);
