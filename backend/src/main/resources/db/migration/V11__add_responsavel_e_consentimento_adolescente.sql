ALTER TABLE adolescentes
    ADD COLUMN responsavel_nome VARCHAR(120),
    ADD COLUMN responsavel_telefone VARCHAR(40),
    ADD COLUMN consentimento_em DATE,
    ADD COLUMN anonimizado_em TIMESTAMPTZ;

COMMENT ON COLUMN adolescentes.responsavel_nome IS 'Nome do responsavel legal que forneceu o consentimento (LGPD art. 14).';
COMMENT ON COLUMN adolescentes.consentimento_em IS 'Data em que o consentimento do responsavel foi obtido.';
COMMENT ON COLUMN adolescentes.anonimizado_em IS 'Momento em que os dados pessoais foram anonimizados a pedido do titular/responsavel.';
