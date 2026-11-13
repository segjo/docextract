package ch.adeon.apps.docextract.validation.adapter.in.rest;

import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.BAD_REQUEST_REF;
import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.PROBLEM_JSON;
import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.PROBLEM_SCHEMA_REF;
import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.UNAUTHORIZED_REF;

import ch.adeon.apps.docextract.audit.application.AuditChannel;
import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
import ch.adeon.apps.docextract.shared.error.InvalidRequestException;
import ch.adeon.apps.docextract.shared.hash.Sha256;
import ch.adeon.apps.docextract.validation.application.CandidateAttribute;
import ch.adeon.apps.docextract.validation.application.ConfirmAndWriteBack;
import ch.adeon.apps.docextract.validation.application.ConfirmationCommand;
import ch.adeon.apps.docextract.validation.application.ConfirmationResult;
import ch.adeon.apps.docextract.validation.application.RejectSuggestion;
import ch.adeon.apps.docextract.validation.application.ValidateAttributes;
import ch.adeon.apps.docextract.validation.application.ValidationReport;
import ch.adeon.apps.docextract.validation.domain.RejectionCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/validation")
@Tag(
    name = "validation",
    description = "Menschliche Freigabe/Ablehnung (FR-5, C-2, ADR-015), DMS-Prüfung (ADR-016)")
public class ValidationController {

  private final ConfirmAndWriteBack confirmAndWriteBack;
  private final RejectSuggestion rejectSuggestion;
  private final ValidateAttributes validateAttributes;
  private final AuthContextPort authContextPort;
  private final OutboundCredentialPort outboundCredentialPort;
  private final AuditPort auditPort;
  private final ObjectMapper objectMapper;

  public ValidationController(
      ConfirmAndWriteBack confirmAndWriteBack,
      RejectSuggestion rejectSuggestion,
      ValidateAttributes validateAttributes,
      AuthContextPort authContextPort,
      OutboundCredentialPort outboundCredentialPort,
      AuditPort auditPort,
      ObjectMapper objectMapper) {
    this.confirmAndWriteBack = confirmAndWriteBack;
    this.rejectSuggestion = rejectSuggestion;
    this.validateAttributes = validateAttributes;
    this.authContextPort = authContextPort;
    this.outboundCredentialPort = outboundCredentialPort;
    this.auditPort = auditPort;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/check")
  @Operation(
      operationId = "checkAttributes",
      summary = "Attribute im DMS prüfen (lesend, ohne Schreiben)",
      description =
          """
          Prüft die aktuellen Werte über storevalidate gegen die Objektdefinition des DMS (ADR-016).
          Es entsteht kein Dokument; die Freigabe (confirm) bleibt unabhängig davon möglich (C-2).
          Keine lokale Pflichtfeldprüfung: Hooks im DMS können Pflichtfelder setzen.

          200 immer mit ValidationReport. valid = true: attributes enthält die Eigenschaften, wie
          das DMS sie speichern würde (origin, changedByDms). valid = false: error enthält den
          einen Fehler des DMS (propertyId nur für beschreibbare Eigenschaften des Dokumenttyps).
          Werte und DMS-Meldung gehen nur an die Aufrufer:in und werden nie geloggt oder
          auditiert. Audit validation.check.valid|invalid|unavailable (nur Hash der Argumente).
          Die Agent-Gruppe erhält 403.\
          """)
  @ApiResponse(responseCode = "200", description = "Prüfergebnis (gültig oder ungültig).")
  @ApiResponse(responseCode = "400", ref = BAD_REQUEST_REF)
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_REF)
  @ApiResponse(
      responseCode = "404",
      description = "Prozess unbekannt oder gehört einem anderen Tenant (NfA-4).",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  @ApiResponse(
      responseCode = "502",
      description = "DMS nicht erreichbar oder antwortet ohne Prüfergebnis.",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  public ValidationReport check(@RequestBody CheckRequest request) {
    if (request.processId() == null
        || request.processId().isBlank()
        || request.documentTypeId() == null
        || request.documentTypeId().isBlank()) {
      throw new InvalidRequestException("processId and documentTypeId are required");
    }
    AuthContext caller = authContextPort.current();
    // Only a hash goes into the audit, never values (C-4).
    String argsHash =
        Sha256.hex(objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));
    ValidationReport report;
    try {
      report =
          validateAttributes.validate(
              caller.tenantId(),
              request.processId(),
              request.documentTypeId(),
              request.attributes() == null ? List.of() : request.attributes(),
              outboundCredentialPort.current());
    } catch (AdapterUnavailableException ex) {
      // Fail closed: without the audit entry the caller does not even get the 502.
      auditPort.append(checkEvent(caller, "unavailable", argsHash));
      throw ex;
    }
    // Fail closed: without the audit entry the caller does not get the result.
    auditPort.append(checkEvent(caller, report.valid() ? "valid" : "invalid", argsHash));
    return report;
  }

  private static AuditEvent checkEvent(AuthContext caller, String outcome, String argsHash) {
    return AuditEvent.byUser(
        caller.tenantId(),
        AuditChannel.UI,
        caller.agent(),
        caller.actorHash(),
        "validation.check." + outcome,
        argsHash);
  }

  @PostMapping("/confirm")
  @Operation(
      operationId = "confirm",
      summary = "Freigabe: Attribute ins DMS schreiben und Embedding promoten",
      description =
          """
          Consent-Gate (C-2): einziger Pfad, der ins DMS schreibt. Saga gemäss ADR-015:
          Vorprüfung (Prozess und PENDING-Embedding gehören dem Tenant der Session, Quarantäne nicht
          abgelaufen) → Marker DMS_WRITE_STARTED → DMS-Write → FINALIZED_INDEX_PENDING →
          Promotion PENDING→APPROVED → COMPLETED → Blobs löschen.

          Idempotenz (Schlüssel: processId, siehe x-idempotency): Liegt für den Prozess bereits
          FINALIZED_INDEX_PENDING, COMPLETED oder INDEX_FAILED vor, erfolgt kein zweiter DMS-Write;
          die Antwort ist 200 mit den gespeicherten DMS-IDs (writtenBack = true) und dem aktuellen
          indexStatus. Bei FINALIZED_INDEX_PENDING wird nur die idempotente Promotion wiederholt.
          Attribute einer Wiederholung werden nicht ausgewertet.

          200 auch bei fehlgeschlagener Promotion: das DMS ist final, indexStatus =
          FINALIZED_INDEX_PENDING (Recovery-Job promotet nach).\
          """,
      extensions = {
        @Extension(
            name = "x-idempotency",
            properties = {
              @ExtensionProperty(name = "key", value = "processId"),
              @ExtensionProperty(
                  name = "repeat-after-dms-write",
                  value =
                      "FINALIZED_INDEX_PENDING | COMPLETED | INDEX_FAILED: kein zweiter"
                          + " DMS-Write; 200 mit gespeichertem repositoryId/documentId,"
                          + " writtenBack = true, aktuellem indexStatus; bei"
                          + " FINALIZED_INDEX_PENDING nur Promotion-Retry."),
              @ExtensionProperty(
                  name = "request-body-on-repeat",
                  value =
                      "Bei FINALIZED_INDEX_PENDING/COMPLETED/INDEX_FAILED wird der"
                          + " Request-Body nicht ausgewertet; Antwort 200 mit dem gespeicherten"
                          + " Ergebnis."),
              @ExtensionProperty(
                  name = "repeat-with-unknown-outcome",
                  value =
                      "DMS_WRITE_STARTED (Ausgang des früheren Writes unbekannt): DMS-Write"
                          + " wird erneut versucht; lehnt das DMS ab, 409 (Dokument möglicherweise"
                          + " bereits gespeichert), kein Duplikat."),
              @ExtensionProperty(
                  name = "other-tenant",
                  value = "404, ohne DMS-Write und ohne Existenzhinweis (NfA-4)."),
              @ExtensionProperty(
                  name = "evidence",
                  value = "ValidationControllerIdempotencyTest, ConfirmAndWriteBackServiceTest")
            }),
        @Extension(
            name = "x-data-classification",
            properties = {
              @ExtensionProperty(
                  name = "request",
                  value =
                      "personal-data: attributes enthält vom Menschen bestätigte"
                          + " Attributwerte, potenziell Personendaten."),
              @ExtensionProperty(
                  name = "response",
                  value = "internal: nur IDs und Status, keine Attributwerte."),
              @ExtensionProperty(
                  name = "logging-audit",
                  value =
                      "Attributwerte nie loggen oder auditieren; Audit nur Ereignis, Hashes"
                          + " und Referenzen, ohne Roh-PII (C-4).")
            }),
        @Extension(
            name = "x-slo",
            properties = {
              @ExtensionProperty(
                  name = "latency",
                  parseValue = true,
                  value =
                      "{\"percentile\": \"p95\", \"target\": \"2s\", \"window\": \"30d\","
                          + " \"measuredAt\": \"Backend, inkl. DMS-Write + Promotion\"}"),
              @ExtensionProperty(
                  name = "availability",
                  parseValue = true,
                  value =
                      "{\"target\": \"99.5%\", \"window\": \"30d\", \"definition\":"
                          + " \"Anteil Antworten ohne 5xx; 4xx zählen nicht\"}"),
              @ExtensionProperty(
                  name = "measurement",
                  value = "strukturiertes Logging (§ 8.3); Metriken ab Block 5.3")
            })
      })
  @ApiResponse(responseCode = "200", description = "DMS-Write erfolgt (auch bei Wiederholung).")
  @ApiResponse(responseCode = "400", ref = BAD_REQUEST_REF)
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_REF)
  @ApiResponse(
      responseCode = "404",
      description = "Prozess unbekannt oder gehört einem anderen Tenant (NfA-4).",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  @ApiResponse(
      responseCode = "409",
      description =
          "Früherer DMS-Write mit unbekanntem Ausgang, DMS weist erneuten Write ab: Dokument"
              + " möglicherweise bereits im DMS gespeichert (ADR-015).",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  @ApiResponse(
      responseCode = "410",
      description = "Quarantäne-Embedding abgelaufen; Dokument neu hochladen.",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  @ApiResponse(
      responseCode = "422",
      description = "DMS lehnt die Attribute ab; Validierung mit korrigierten Werten wiederholen.",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  @ApiResponse(
      responseCode = "502",
      description =
          "DMS nicht erreichbar oder Serverfehler; Ausgang unbekannt, Marker bleibt (ADR-015).",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  public ResponseEntity<ConfirmationResult> confirm(@RequestBody ConfirmRequest request) {
    ConfirmationResult result =
        confirmAndWriteBack.confirm(
            new ConfirmationCommand(
                request.processId(), request.dmsSourceCategory(), request.attributes()));
    return ResponseEntity.ok(result);
  }

  @PostMapping("/reject")
  @Operation(
      operationId = "reject",
      summary = "Ablehnung: PENDING-Embedding und Blobs löschen",
      description = "Nur vor dem DMS-Write möglich (ADR-015).")
  @ApiResponse(responseCode = "204", description = "Abgelehnt, Quarantänedaten gelöscht.")
  @ApiResponse(responseCode = "400", ref = BAD_REQUEST_REF)
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_REF)
  @ApiResponse(
      responseCode = "404",
      description = "Prozess unbekannt oder gehört einem anderen Tenant (NfA-4).",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  @ApiResponse(
      responseCode = "409",
      description = "Prozess bereits im DMS finalisiert.",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  public ResponseEntity<Void> reject(@RequestBody RejectRequest request) {
    rejectSuggestion.reject(new RejectionCommand(request.processId()));
    return ResponseEntity.noContent().build();
  }

  public record ConfirmRequest(
      @Schema(description = "Prozess aus dem Upload (Idempotenzschlüssel).") String processId,
      @Schema(description = "Vom Menschen gewählte DMS-Kategorie (Dokumentart).")
          String dmsSourceCategory,
      @Schema(
              description =
                  "Bestätigte Attributwerte je DMS-Property-ID. Potenziell Personendaten (C-4).")
          Map<String, Object> attributes) {}

  public record CheckRequest(
      @Schema(description = "Prozess aus dem Upload.") String processId,
      @Schema(description = "Dokumenttyp (DMS-Kategorie), muss ein Kandidat des Prozesses sein.")
          String documentTypeId,
      @Schema(
              description =
                  "Aktuelle Werte je Eigenschaft; nicht beschreibbare Eigenschaften werden"
                      + " ignoriert. Potenziell Personendaten (C-4).")
          List<CandidateAttribute> attributes) {}

  public record RejectRequest(String processId) {}
}
