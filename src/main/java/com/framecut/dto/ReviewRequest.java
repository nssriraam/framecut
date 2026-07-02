package com.framecut.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull
    @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 2000)
    private String content;

    @Min(0) @Max(10)
    private Double score;

    @Size(max = 50)
    private String summary;

    private Boolean hasSpoilers;
}
