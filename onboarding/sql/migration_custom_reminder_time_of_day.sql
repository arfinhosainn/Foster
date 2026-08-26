-- Migration: custom reminder time-of-day (notification plan §3.2 / D1).
--
-- Adds the optional local clock time (24h "HH:mm") for custom reminders so a
-- time-specific reminder ("take medication at 20:00") can fire standalone at
-- its own clock time instead of the default custom-reminder hour.
--
-- Nullable with no default: existing rows are date-only reminders and keep
-- firing at the app-level fallback hour (CUSTOM_REMINDER_HOUR). Idempotent.

ALTER TABLE public.custom_reminders
    ADD COLUMN IF NOT EXISTS time_of_day text;
