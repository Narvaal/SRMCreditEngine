-- Garante gen_random_uuid() de forma portátil, independente da versão exata do Postgres.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
