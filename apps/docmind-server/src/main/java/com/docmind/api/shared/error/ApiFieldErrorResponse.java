package com.docmind.api.shared.error;

public record ApiFieldErrorResponse(String path, String code, String message) {}
