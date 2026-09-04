ALTER TABLE discipulados
    ADD COLUMN em_formacao BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE discipulados
    ALTER COLUMN gerencia_id DROP NOT NULL;

ALTER TABLE discipulados
    ADD CONSTRAINT ck_discipulado_formacao_gerencia CHECK (
        (em_formacao = FALSE AND gerencia_id IS NOT NULL)
        OR (em_formacao = TRUE AND gerencia_id IS NULL)
    );
