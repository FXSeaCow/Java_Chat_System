BEGIN;

CREATE TABLE IF NOT EXISTS public.app_launches (
    launch_id UUID PRIMARY KEY,
    opened_at TIMESTAMPTZ(3) NOT NULL
);
ALTER TABLE public.app_launches ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS reset_password_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reset_password_expires_at TIMESTAMPTZ;

COMMIT;
