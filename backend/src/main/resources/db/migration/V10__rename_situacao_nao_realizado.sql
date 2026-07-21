ALTER TABLE encontros DROP CONSTRAINT ck_encontro_situacao;
ALTER TABLE encontros DROP CONSTRAINT ck_encontro_justificativa;

ALTER TABLE encontros ALTER COLUMN situacao TYPE VARCHAR(20);

UPDATE encontros SET situacao = 'NAO_REALIZADO' WHERE situacao = 'CANCELADO';

ALTER TABLE encontros
    ADD CONSTRAINT ck_encontro_situacao
    CHECK (situacao IN ('REALIZADO', 'NAO_REALIZADO'));

ALTER TABLE encontros
    ADD CONSTRAINT ck_encontro_justificativa
    CHECK (
        (situacao = 'REALIZADO' AND justificativa IS NULL)
        OR
        (situacao = 'NAO_REALIZADO' AND justificativa IS NOT NULL AND LENGTH(TRIM(justificativa)) BETWEEN 1 AND 500)
    );
