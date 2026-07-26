ALTER TABLE encontros
    ADD COLUMN chamada_salva_em TIMESTAMPTZ NULL;

UPDATE encontros e
SET chamada_salva_em = (
    SELECT MIN(f.registrada_em)
    FROM frequencias f
    WHERE f.encontro_id = e.id
)
WHERE EXISTS (SELECT 1 FROM frequencias f WHERE f.encontro_id = e.id);
