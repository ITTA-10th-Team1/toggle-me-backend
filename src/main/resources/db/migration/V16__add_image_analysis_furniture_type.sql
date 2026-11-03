-- 가구별 태그 지침을 쓰려고 분석을 요청한 가구를 남긴다. 가구 없이 요청한 분석(기존 행 포함)은 NULL이다
ALTER TABLE image_analysis
    ADD COLUMN furniture_type_id BIGINT NULL AFTER image_key,
    ADD CONSTRAINT fk_image_analysis_furniture_type FOREIGN KEY (furniture_type_id) REFERENCES furniture_type (id);
