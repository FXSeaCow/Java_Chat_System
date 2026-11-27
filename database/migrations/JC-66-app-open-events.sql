CREATE TABLE IF NOT EXISTS public.app_open_events (
    user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    launch_id UUID NOT NULL,
    opened_at TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, launch_id)
);

ALTER TABLE public.app_open_events ENABLE ROW LEVEL SECURITY;
