-- Add habit_group column to habits table for manual group assignment
alter table habits add column habit_group integer default 3;

-- Update all existing habits to PERSONAL_IMPROVEMENT group (ordinal 3)
-- RELIGIOUS = 0, CAREER_WORK = 1, SOCIAL_FAMILY = 2, PERSONAL_IMPROVEMENT = 3
update habits set habit_group = 3 where habit_group is null;
