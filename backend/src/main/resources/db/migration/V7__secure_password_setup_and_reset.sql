ALTER TABLE usuarios ALTER COLUMN senha_hash DROP NOT NULL;

ALTER TABLE tokens_redefinicao_senha
    ADD COLUMN criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN tipo VARCHAR(30) NOT NULL DEFAULT 'REDEFINICAO';

ALTER TABLE tokens_redefinicao_senha
    ADD CONSTRAINT ck_tokens_redefinicao_senha_tipo
    CHECK (tipo IN ('REDEFINICAO', 'DEFINICAO_INICIAL'));

ALTER TABLE tokens_redefinicao_senha ALTER COLUMN tipo DROP DEFAULT;

CREATE INDEX ix_tokens_redefinicao_senha_cooldown
    ON tokens_redefinicao_senha(usuario_id, tipo, criado_em DESC);
