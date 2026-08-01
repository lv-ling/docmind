declare const isoDateTimeBrand: unique symbol;

/** RFC 3339 date-time string normalized to UTC by service boundaries. */
export type IsoDateTime = string & { readonly [isoDateTimeBrand]: true };
