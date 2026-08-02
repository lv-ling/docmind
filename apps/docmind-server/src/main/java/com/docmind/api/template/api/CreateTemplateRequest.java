package com.docmind.api.template.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTemplateRequest(@NotBlank @Size(max = 200) String name) {}
