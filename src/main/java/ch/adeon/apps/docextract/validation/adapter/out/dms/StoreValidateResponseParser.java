package ch.adeon.apps.docextract.validation.adapter.out.dms;

import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
import ch.adeon.apps.docextract.validation.adapter.out.dms.DmsPropertyPayloadMapper.StoreValidatePayload;
import ch.adeon.apps.docextract.validation.port.PropertyValue;
import ch.adeon.apps.docextract.validation.port.StoreValidationError;
import ch.adeon.apps.docextract.validation.port.StoreValidationResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Turns the HTTP answer of {@code storevalidate} into a {@link StoreValidationResult} (ADR-016,
 * decision 3): 200 = valid, 400 = exactly one error, everything else cannot be a validation result.
 * DMS error codes are configuration-dependent, so there is deliberately no table of them: the code
 * in square brackets is only passed through. Neither the message nor any value is logged (C-4).
 */
@Component
public class StoreValidateResponseParser {

  private static final Logger log = LoggerFactory.getLogger(StoreValidateResponseParser.class);
  private static final Pattern DMS_CODE = Pattern.compile("\\[(\\d+)]");

  private final ObjectMapper objectMapper;
  private final DmsPropertyPayloadMapper mapper;

  public StoreValidateResponseParser(ObjectMapper objectMapper, DmsPropertyPayloadMapper mapper) {
    this.objectMapper = objectMapper;
    this.mapper = mapper;
  }

  /**
   * @param allowlist ids of the properties that may be reported back
   * @param submitted the values that were sent, handed through into a valid result
   * @throws AdapterUnavailableException for any status other than 200 and 400
   */
  public StoreValidationResult parse(
      int status, String body, Set<String> allowlist, List<PropertyValue> submitted) {
    return switch (status) {
      case 200 -> valid(body, allowlist, submitted);
      case 400 -> invalid(body, allowlist);
      default -> throw new AdapterUnavailableException("storevalidate answered HTTP " + status);
    };
  }

  private StoreValidationResult valid(
      String body, Set<String> allowlist, List<PropertyValue> submitted) {
    List<PropertyValue> returned = List.of();
    try {
      StoreValidatePayload payload = objectMapper.readValue(body, StoreValidatePayload.class);
      returned = mapper.fromResponse(payload, allowlist);
    } catch (RuntimeException ex) {
      // Valid without adopting anything: the sent values stay.
      log.warn("storevalidate 200 response not readable: {}", ex.getClass().getSimpleName());
    }
    log.info("storevalidate valid=true");
    return new StoreValidationResult.Valid(submitted, returned);
  }

  private StoreValidationResult invalid(String body, Set<String> allowlist) {
    StoreValidationError error;
    try {
      ErrorBody parsed = objectMapper.readValue(body, ErrorBody.class);
      String propertyId =
          parsed.propertyFieldId() != null && allowlist.contains(parsed.propertyFieldId())
              ? parsed.propertyFieldId()
              : null;
      error = new StoreValidationError(propertyId, dmsCode(parsed.message()), parsed.message());
    } catch (RuntimeException ex) {
      error = new StoreValidationError(null, null, null);
    }
    log.info(
        "storevalidate valid=false propertyId={} dmsCode={}", error.propertyId(), error.dmsCode());
    return new StoreValidationResult.Invalid(error);
  }

  private static String dmsCode(String message) {
    if (message == null) {
      return null;
    }
    Matcher matcher = DMS_CODE.matcher(message);
    return matcher.find() ? matcher.group(1) : null;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ErrorBody(String message, String propertyFieldId) {}
}
