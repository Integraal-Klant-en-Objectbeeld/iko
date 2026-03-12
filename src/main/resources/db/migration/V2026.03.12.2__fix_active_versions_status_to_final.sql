-- Fix active versions that were not migrated to FINAL status
UPDATE connector SET status = 'FINAL' WHERE is_active = TRUE AND status = 'DRAFT';
UPDATE aggregated_data_profile SET status = 'FINAL' WHERE is_active = TRUE AND status = 'DRAFT';
