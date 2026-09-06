-- =============================================================================
-- Move the Green Flower badge out of the achievement catalog.
--
-- The Green Flower (threshold 90) was the 7th badge in the unlock sequence
-- (Soil 1 / Lotus 15 / Sunflower 30 / Brown 45 / Blue 60 / Pink 75 / Green 90 /
-- Mushrooms 115). It has been retired from the app, so the catalog becomes a
-- 7-badge progression ending at Mushrooms (115).
--
--   * `public.user_badges.badge_id` has an ON DELETE CASCADE FK, so any user
--     who already unlocked the Green Flower loses that unlock row automatically
--     (their other unlocks are untouched).
--   * The `unlock_badges_on_check_in` trigger iterates by threshold ascending,
--     so the 75 -> 115 gap is not a problem.
--
-- Run this in the Supabase SQL editor once. If you are setting up a FRESH
-- database, `badges.sql` now handles this itself and this file is only needed
-- for databases that were seeded before the retirement.
-- =============================================================================

delete from public.badges
where name = 'Green Flower';