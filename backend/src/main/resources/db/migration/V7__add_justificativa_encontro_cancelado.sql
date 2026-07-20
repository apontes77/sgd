ALTER TABLE encontros ADD COLUMN justificativa VARCHAR(500);

UPDATE encontros
   SET justificativa = 'Não informada — registro anterior à funcionalidade'
 WHERE situacao = 'CANCELADO';

ALTER TABLE encontros
    ADD CONSTRAINT ck_encontro_justificativa
    CHECK (
        (situacao = 'REALIZADO' AND justificativa IS NULL)
        OR
        (situacao = 'CANCELADO' AND justificativa IS NOT NULL AND LENGTH(TRIM(justificativa)) BETWEEN 1 AND 500)
    );
