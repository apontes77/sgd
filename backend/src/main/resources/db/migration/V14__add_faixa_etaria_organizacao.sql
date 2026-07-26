ALTER TABLE gerencias
    ADD COLUMN faixa_etaria VARCHAR(20) NOT NULL DEFAULT 'DE_14_A_17';

ALTER TABLE discipulados
    ADD COLUMN faixa_etaria VARCHAR(20) NOT NULL DEFAULT 'DE_14_A_17';

ALTER TABLE gerencias
    ADD CONSTRAINT ck_gerencias_faixa_etaria
        CHECK (faixa_etaria IN ('DE_9_A_11', 'DE_11_A_13', 'DE_14_A_17'));

ALTER TABLE discipulados
    ADD CONSTRAINT ck_discipulados_faixa_etaria
        CHECK (faixa_etaria IN ('DE_9_A_11', 'DE_11_A_13', 'DE_14_A_17'));

ALTER TABLE gerencias
    ALTER COLUMN faixa_etaria DROP DEFAULT;

ALTER TABLE discipulados
    ALTER COLUMN faixa_etaria DROP DEFAULT;
