package com.docmind.api.sensitive.application;

import com.docmind.api.sensitive.api.SensitiveRuleInput;
import com.docmind.api.sensitive.domain.SensitiveDataType;
import com.docmind.api.sensitive.domain.SensitiveRecognizerKind;

public record ValidatedSensitiveRule(
    SensitiveRuleInput input,
    SensitiveDataType dataType,
    SensitiveRecognizerKind recognizerKind,
    int position) {}
