-- Atualiza valores de faixa etária (discipulados e coluna legada de gerencias)
ALTER TABLE discipulados DROP CONSTRAINT IF EXISTS ck_discipulados_faixa_etaria;
ALTER TABLE gerencias DROP CONSTRAINT IF EXISTS ck_gerencias_faixa_etaria;

UPDATE discipulados SET faixa_etaria = 'DE_09_A_11' WHERE faixa_etaria = 'DE_9_A_11';
UPDATE discipulados SET faixa_etaria = 'DE_15_MAIS' WHERE faixa_etaria = 'DE_14_A_17';

UPDATE gerencias SET faixa_etaria = 'DE_09_A_11' WHERE faixa_etaria = 'DE_9_A_11';
UPDATE gerencias SET faixa_etaria = 'DE_15_MAIS' WHERE faixa_etaria = 'DE_14_A_17';

ALTER TABLE discipulados
    ADD CONSTRAINT ck_discipulados_faixa_etaria
        CHECK (faixa_etaria IN ('DE_09_A_11', 'DE_11_A_13', 'DE_13_A_15', 'DE_15_MAIS'));

-- Sexo da gerência
ALTER TABLE gerencias
    ADD COLUMN sexo VARCHAR(10) NOT NULL DEFAULT 'MASCULINO';

ALTER TABLE gerencias
    ADD CONSTRAINT ck_gerencias_sexo CHECK (sexo IN ('MASCULINO', 'FEMININO'));

ALTER TABLE gerencias
    ALTER COLUMN sexo DROP DEFAULT;

-- Múltiplas faixas etárias por gerência
CREATE TABLE gerencia_faixas_etarias (
    gerencia_id BIGINT NOT NULL REFERENCES gerencias(id) ON DELETE CASCADE,
    faixa_etaria VARCHAR(20) NOT NULL,
    PRIMARY KEY (gerencia_id, faixa_etaria),
    CONSTRAINT ck_gerencia_faixas_etarias_faixa
        CHECK (faixa_etaria IN ('DE_09_A_11', 'DE_11_A_13', 'DE_13_A_15', 'DE_15_MAIS'))
);

INSERT INTO gerencia_faixas_etarias (gerencia_id, faixa_etaria)
SELECT id, faixa_etaria FROM gerencias;

ALTER TABLE gerencias DROP COLUMN faixa_etaria;
