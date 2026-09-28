-- More layout control per template: page padding top / bottom (the existing "padding" is now the left & right
-- padding), space above the header content and the alignment of the clinic details in the header.
ALTER TABLE document_templates
    ADD COLUMN padding_top        INT,
    ADD COLUMN padding_bottom     INT,
    ADD COLUMN header_padding_top INT         NOT NULL DEFAULT 0 CHECK (header_padding_top BETWEEN 0 AND 200),
    ADD COLUMN header_text_align  VARCHAR(10) NOT NULL DEFAULT 'AUTO' CHECK (header_text_align IN ('AUTO', 'LEFT', 'CENTER', 'RIGHT'));

-- same look as before: top and bottom start with the old all-round padding
UPDATE document_templates SET padding_top = padding, padding_bottom = padding;

ALTER TABLE document_templates
    ALTER COLUMN padding_top SET NOT NULL,
    ALTER COLUMN padding_bottom SET NOT NULL,
    ADD CONSTRAINT chk_template_padding_top CHECK (padding_top BETWEEN 0 AND 200),
    ADD CONSTRAINT chk_template_padding_bottom CHECK (padding_bottom BETWEEN 0 AND 200);
