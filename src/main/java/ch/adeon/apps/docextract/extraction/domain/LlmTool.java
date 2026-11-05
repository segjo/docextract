package ch.adeon.apps.docextract.extraction.domain;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * A read-only tool the LLM may call during one {@code LlmPort} call (ADR-014). DocExtract executes
 * {@code handler} itself; the LLM never gets credentials or direct access to the DMS (C-3). The
 * handler receives the model's JSON arguments and returns a JSON result.
 */
public record LlmTool(
    String name,
    String description,
    Map<String, Object> inputSchema,
    UnaryOperator<String> handler) {}
