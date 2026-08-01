package com.docmind.api.sensitive.domain;

public enum SensitiveDataType {
  CHINA_NATIONAL_ID,
  IDENTITY_DOCUMENT,
  PASSPORT,
  PHONE_NUMBER,
  EMAIL_ADDRESS,
  CREDIT_CARD,
  BANK_ACCOUNT,
  IP_ADDRESS,
  PERSON_NAME,
  LOCATION,
  CUSTOM;

  public String wireValue() {
    return name().toLowerCase();
  }
}
