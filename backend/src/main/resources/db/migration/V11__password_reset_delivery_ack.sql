ALTER TABLE tokens_redefinicao_senha
    ADD COLUMN enviado_em TIMESTAMPTZ;

CREATE INDEX ix_tokens_redefinicao_senha_enviado
    ON tokens_redefinicao_senha(usuario_id, tipo, enviado_em DESC);
