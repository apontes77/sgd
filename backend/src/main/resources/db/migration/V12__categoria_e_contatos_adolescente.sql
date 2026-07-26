ALTER TABLE adolescentes
    ADD COLUMN categoria VARCHAR(20) NOT NULL DEFAULT 'DISCIPULO',
    ADD COLUMN telefone_mae VARCHAR(40),
    ADD COLUMN telefone_pai VARCHAR(40),
    ADD COLUMN estrutura VARCHAR(120),
    ADD COLUMN motivo_afastamento VARCHAR(500);

ALTER TABLE adolescentes
    ADD CONSTRAINT ck_adolescente_categoria
        CHECK (categoria IN ('DISCIPULO', 'VISITANTE', 'DISCIPULO_GOE'));

ALTER TABLE adolescentes
    ADD CONSTRAINT ck_adolescente_motivo_goe
        CHECK (
            (categoria = 'DISCIPULO_GOE' AND motivo_afastamento IS NOT NULL AND btrim(motivo_afastamento) <> '')
            OR (categoria <> 'DISCIPULO_GOE' AND motivo_afastamento IS NULL)
        );
