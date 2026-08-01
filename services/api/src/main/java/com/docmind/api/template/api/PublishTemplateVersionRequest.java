package com.docmind.api.template.api;

import jakarta.validation.constraints.Size;

public record PublishTemplateVersionRequest(@Size(max = 2000) String note) {}
