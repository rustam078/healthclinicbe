package com.clinic.dto;

import com.clinic.enums.Access;
import com.clinic.enums.TemplateType;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Template layout. All dimensions are CSS pixels. */
@Getter
@Setter
public class TemplateDto extends AuditedDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private TemplateType templateType;

    @NotBlank(message = "Document title is required")
    @Size(max = 120, message = "Title must be at most 120 characters")
    private String title;

    @NotNull(message = "Page width is required")
    @Min(value = 200, message = "Page width must be at least 200 px")
    @Max(value = 2000, message = "Page width must be at most 2000 px")
    private Integer pageWidth;

    @NotNull(message = "Page height is required")
    @Min(value = 200, message = "Page height must be at least 200 px")
    @Max(value = 3000, message = "Page height must be at most 3000 px")
    private Integer pageHeight;

    @NotNull(message = "Header height is required")
    @Min(value = 40, message = "Header height must be at least 40 px")
    @Max(value = 400, message = "Header height must be at most 400 px")
    private Integer headerHeight;

    @NotNull(message = "Logo area width is required")
    @Min(value = 20, message = "Logo area width must be at least 20 px")
    @Max(value = 600, message = "Logo area width must be at most 600 px")
    private Integer logoAreaWidth;

    @NotNull(message = "Logo area height is required")
    @Min(value = 20, message = "Logo area height must be at least 20 px")
    @Max(value = 300, message = "Logo area height must be at most 300 px")
    private Integer logoAreaHeight;

    @NotNull(message = "Padding is required")
    @Min(value = 0, message = "Padding cannot be negative")
    @Max(value = 100, message = "Padding must be at most 100 px")
    private Integer padding;

    @NotNull(message = "Margin is required")
    @Min(value = 0, message = "Margin cannot be negative")
    @Max(value = 100, message = "Margin must be at most 100 px")
    private Integer margin;

    @NotNull(message = "Choose whether to show the logo")
    private Boolean showLogo;

    @NotNull(message = "Choose whether to show the footer")
    private Boolean showFooter;

    @Size(max = 255, message = "Footer text must be at most 255 characters")
    private String footerText;

    @NotBlank(message = "Accent colour is required")
    @Pattern(regexp = Patterns.HEX_COLOR, message = "Accent colour must look like #0f766e")
    private String accentColor;

    /** Builds default values; dims = width, height, header, logo width, logo height, padding, margin. */
    public static TemplateDto of(String title, int[] dims, String footer) {
        TemplateDto dto = new TemplateDto();
        dto.setTitle(title);
        dto.setPageWidth(dims[0]);
        dto.setPageHeight(dims[1]);
        dto.setHeaderHeight(dims[2]);
        dto.setLogoAreaWidth(dims[3]);
        dto.setLogoAreaHeight(dims[4]);
        dto.setPadding(dims[5]);
        dto.setMargin(dims[6]);
        return withFlags(dto, footer);
    }

    private static TemplateDto withFlags(TemplateDto dto, String footer) {
        dto.setShowLogo(true);
        dto.setShowFooter(true);
        dto.setFooterText(footer);
        dto.setAccentColor("#0f766e");
        return dto;
    }
}
