package com.clinic.entity;

import com.clinic.enums.TemplateType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_templates")
public class DocumentTemplate extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private TemplateType templateType;

    private String title;
    private int pageWidth;
    private int pageHeight;
    private int headerHeight;
    private int logoAreaWidth;
    private int logoAreaHeight;
    /** Left and right page padding. */
    private int padding;
    private int paddingTop;
    private int paddingBottom;
    private int margin;
    /** Space above the header content. */
    private int headerPaddingTop;
    /** Alignment of the clinic details in the header: AUTO (follows the logo position), LEFT, CENTER, RIGHT. */
    private String headerTextAlign = "AUTO";
    private boolean showLogo;
    private boolean showFooter;
    private String footerText;
    private String accentColor;
}
