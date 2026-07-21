DO $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM encontros
         GROUP BY discipulado_id, data
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Existem encontros duplicados para o mesmo discipulado e data. Corrija-os antes de aplicar a migration V9.';
    END IF;
END
$$;

ALTER TABLE encontros
    ADD CONSTRAINT uk_encontro_discipulado_data
    UNIQUE (discipulado_id, data);
