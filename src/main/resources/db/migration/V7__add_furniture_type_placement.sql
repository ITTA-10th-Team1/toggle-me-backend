ALTER TABLE furniture_type
    ADD COLUMN position_x DECIMAL(10, 4) NULL AFTER display_order,
    ADD COLUMN position_y DECIMAL(10, 4) NULL AFTER position_x,
    ADD COLUMN rotation DECIMAL(10, 4) NULL AFTER position_y,
    ADD COLUMN render_order INT NULL AFTER rotation;
