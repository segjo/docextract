package ch.adeon.apps.docextract.security.application;

/**
 * Outbound port resolving the d.velop system base URI (jstore, with properties
 * fallback).
 */
public interface AppConfigPort {

    String systemBaseUri();
}
