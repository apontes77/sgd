CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuario_perfis (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    perfil VARCHAR(30) NOT NULL,
    PRIMARY KEY (usuario_id, perfil),
    CONSTRAINT ck_usuario_perfis_perfil CHECK (perfil IN ('ADMIN', 'GERENTE', 'DISCIPULADOR', 'CO_LIDER'))
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expira_em TIMESTAMPTZ NOT NULL,
    revogado_em TIMESTAMPTZ
);

CREATE TABLE tokens_redefinicao_senha (
    id UUID PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expira_em TIMESTAMPTZ NOT NULL,
    usado_em TIMESTAMPTZ
);

CREATE TABLE auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    data_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entidade VARCHAR(100) NOT NULL,
    acao VARCHAR(100) NOT NULL,
    detalhes TEXT
);

CREATE INDEX ix_refresh_tokens_usuario_id ON refresh_tokens(usuario_id);
CREATE INDEX ix_tokens_redefinicao_senha_usuario_id ON tokens_redefinicao_senha(usuario_id);
