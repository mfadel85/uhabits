-- Add priority column to habits table for weighted scoring
alter table habits add column priority integer default 1;

-- Update all existing habits to NORMAL priority (ordinal 1)
-- HIGH = 0, NORMAL = 1, LOW = 2
update habits set priority = 1 where priority is null;
