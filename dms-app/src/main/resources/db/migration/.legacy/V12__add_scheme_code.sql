-- V12: 为 md_decoct_scheme 补充缺失字段
ALTER TABLE md_decoct_scheme ADD COLUMN code VARCHAR(50);
ALTER TABLE md_decoct_scheme ADD COLUMN alarm_high_temp DECIMAL(5,2);
ALTER TABLE md_decoct_scheme ADD COLUMN alarm_low_temp DECIMAL(5,2);
