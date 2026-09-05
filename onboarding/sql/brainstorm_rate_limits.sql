-- =============================================================================
-- Foster — brainstorm rate limiting
-- -----------------------------------------------------------------------------
-- Tables backing the `brainstorm` Edge Function's cost-abuse defenses (the
-- Gemini call is the only real-money risk: public key + OAuth signups).
--
--   brainstorm_ip_limits — per-IP daily counter, written by the function with
--     the service role. RLS enabled with NO policies = deny-all to
--     anon/authenticated; only the service role touches it.
--
--   brainstorm_alerts — one row per (day, threshold) so the 50%/80% capacity
--     alerts are edge-triggered (fire exactly once per threshold per day).
--
-- The GLOBAL_DAILY_CEILING is a smoke-detector backstop, never the binding
-- limit: per-user (3/day) and per-IP limits do the actual work.
-- Run in the Supabase SQL editor. Idempotent.
-- =============================================================================

-- 1. Per-IP daily limiter -----------------------------------------------------

create table if not exists public.brainstorm_ip_limits (
  ip          text primary key,
  window_date date not null default current_date,
  count       integer not null default 0,
  updated_at  timestamptz not null default now()
);

-- Deny-all: RLS on, no policies. Only the service role (the Edge Function,
-- which carries SUPABASE_SERVICE_ROLE_KEY server-side) may read/write.
alter table public.brainstorm_ip_limits enable row level security;

create index if not exists idx_brainstorm_ip_limits_window
  on public.brainstorm_ip_limits (window_date);

-- 2. Alert bookkeeping (edge-triggered capacity warnings) ----------------------

create table if not exists public.brainstorm_alerts (
  window_date date not null,
  threshold   integer not null,
  fired_at    timestamptz not null default now(),
  primary key (window_date, threshold)
);

alter table public.brainstorm_alerts enable row level security;
-- Deny-all here too: nobody but the service role reads this.

-- 3. Nightly purge of stale IP rows (TTL backstop) ------------------------------
-- Enable the extension once, then schedule. The INSERT path in the function
-- also resets a row when its window_date rolls over, so this cron is purely
-- housekeeping for IPs that never came back.

create extension if not exists pg_cron;

do $$
begin
  if not exists (select 1 from cron.job where jobname = 'purge-brainstorm-ip-limits') then
    perform cron.schedule(
      'purge-brainstorm-ip-limits',
      '0 3 * * *',
      $$delete from public.brainstorm_ip_limits where window_date < current_date - interval '2 days'$$
    );
  end if;
end $$;