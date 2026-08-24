ALTER TABLE encontros
    ADD COLUMN fechamento_automatico BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE encontros
SET fechamento_automatico = TRUE
WHERE justificativa = 'discipulador ou colider não registraram a frequência';
