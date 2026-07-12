CREATE TABLE identidades_oauth (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    provedor VARCHAR(20) NOT NULL,
    subject_externo VARCHAR(255) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_identidades_oauth_provedor CHECK (provedor IN ('GOOGLE', 'MICROSOFT')),
    CONSTRAINT uk_identidade_oauth_provedor_subject UNIQUE (provedor, subject_externo),
    CONSTRAINT uk_identidade_oauth_provedor_usuario UNIQUE (provedor, usuario_id)
);

CREATE INDEX ix_identidades_oauth_usuario_id ON identidades_oauth(usuario_id);
