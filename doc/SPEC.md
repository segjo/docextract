# SPEC: DocExtract

**KI-gestützte Dokumenterfassung & -verschlagwortung für das Dokumentenmanagement (d.velop documents)**
Modulkontext: CAS AISE · Projektarbeit auf Basis des Referenzprojekts _SupportFlow_.
**Stand:** 09/2026 · **Review:** 09/2026 · **Meilensteine:** M1 = lauffähiges Gerüst · M1.5 = durchgängiger Extraktions-Pfad · M2 = erste End-to-End-Messung (provisorisches Eval-Set) · M3 = Härtung & Compliance-Nachweise (belastbares Eval-Set). Details & Termine → § 7. Modulweite Blockplanung → [PROJEKTPLAN.md](PROJEKTPLAN.md).

## 1. Ziel

**Sachbearbeitende sollen eingehende Dokumente ohne manuelles Abtippen korrekt erfasst und verschlagwortet im Dokumentenmanagement ablegen können: schnell, nachvollziehbar und mit minimalem Aufwand.**

**Leitmetapher:** DocExtract ist der aufmerksame Kollege im Posteingang, der ein Dokument liest, die relevanten Attribute vorschlägt und passende frühere Fälle bereitlegt: der Mensch bestätigt oder korrigiert, bevor etwas ins DMS zurückgeschrieben wird.

**Produktvision (Zielbild):** DocExtract reift vom _assistierenden_ zum _lernenden_ System: Aus jeder menschlichen Bestätigung entstehen bessere Vorlagen (Self-Learning-Loop). Die Inferenz läuft **lokal oder bei einem vom Kunden konfigurierten externen LLM**; alle externen Bausteine sind über Schnittstellen austauschbar. Mittelfristig wächst die Lösung in die **produktive d.velop-Plattform** hinein: ohne die Grundprinzipien _Human-in-the-Loop_ und _Produktneutralität_ aufzugeben.

**Stakeholder & Kerninteressen:**

| Rolle                                                 | Kerninteresse                                                                                                                     |
| ----------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| **Sachbearbeiter:in / Endnutzer:in**                  | Zeitersparnis, geringe Fehlerquote; wenig Klicks, kein Eintippen                                                                  |
| **Records-/DMS-Verantwortliche:r** (fachlicher Owner) | Konsistente, vollständige Metadaten für Auffindbarkeit und Compliance                                                             |
| **IT-Betrieb / Datenschutz**                          | Kontrollierter Egress, Betreibbarkeit (Windows/Linux), Auditierbarkeit (ISO 27001), austauschbare Komponenten ohne Vendor-Lock-in |
| **d.velop** (Plattformbetreiber, optional)            | Saubere Nutzung der DMSApp-API, Einhaltung des App-Integrationsmodells                                                            |

## 2. Scope

### In Scope

- Dokument-Upload (Einzeldatei/REST) mit **PDF-Vorschau** und Eingangsvalidierung.
- **Textextraktion** als Eingabe für die Ähnlichkeitssuche (erste _N_ Wörter, kein Chunking).
- **Ähnlichkeitssuche / Kategorisierung** über Embeddings + Vektor-Store, mit Mandantenfilter (`tenant_id`).
- **KI-Attributvorschläge** über einen austauschbaren LLM-Adapter: **lokal oder extern** (schema-validiertes JSON).
- **Validierung durch Menschen** und Rückschreiben ins DMS nach expliziter Bestätigung.
- **MCP-Tool**, das den Extraktions-/Retrieval-Service für einen Agenten konsumierbar macht (Modulanforderung Block 3).
- **Austauschbare Adapter** für Vorschau, Textextraktion, LLM, Embedding, Vektor-Store (§ 4).
- Lokaler, reproduzierbarer Betrieb (`docker compose up`) auf Linux und Windows/WSL2.

### Out of Scope

- Produktive Multi-Tenancy und formale Compliance-Nachweise; Mandanten-Isolation im Retrieval (NfA-4) bleibt im Scope.
- Autonome Ablage **ohne** menschliche Freigabe.
- Wahl und rechtliche Prüfung eines externen LLM: liegt beim Kunden, der das LLM selbst konfiguriert.
- Multi-Channel-Intake über Datei-Upload/REST hinaus.
- Produktive Mehrsprachigkeit über den Eval-Fall E-4 hinaus.
- OCR Integration für gescannte Dokumente / Dokumente ohne eingebetteten Text.

### Ausbaustufe (eigener Antrieb, nicht Modulanforderung)

- Produktive d.velop-Anbindung, Selbstlern-Feedback-Loop (Vorlagen-Rückfluss).
- Cloud-Betrieb (SaaS) mit Multi-Tenancy, horizontaler Skalierung, Monitoring, Alerting, SLA, Backup/Restore, Disaster Recovery.

### Offen gelassen

- Ausgestaltung des Self-Learning-Loops (Kuratierung, Quarantäne, Rollback von Vorlagen): wird pro Block verfeinert.
- In welcher Form der LLM-Adapter den Dokumentinhalt für FR-4 erhält (zum Beispiel Volltext, strukturierte Segmente oder Seitenbilder). Diese Repräsentation ist vom Textextraktionspfad für FR-3 getrennt und muss pro Adapter dokumentiert werden.
- Welche externen Anbieter über den Nachweis-Adapter (§ 7, M2) hinaus unterstützt werden.

## 3. Qualitätsziele (SMART)

Alle Zielwerte werden gegen das Evaluationsset (§ 5.4) gemessen. **Referenz-Setup:** _AMD Ryzen 9 PRO 7940HS, Radeon 780M (iGPU), 62 GB RAM; LLM und Embedding über lokale Adapter; ein Nutzer, sequenziell._
**Anbieterbezug:** NfA-1, -3, -4, -5, -6, -8 gelten für **jeden** konfigurierten Adapter. NfA-2 und NfA-7 sind für das lokale Referenz-Setup verbindlich; externe Anbieter werden mit identischem Verfahren gemessen und **je Anbieter ausgewiesen**.
Acht Ziele, je eine Qualitätsdimension (Genauigkeit · Retrieval-Güte · Effizienz · Kosten · Zuverlässigkeit · Vertraulichkeit · Datenschutz · Austauschbarkeit). **Zeitbezug** → Meilenstein (§ 7).

| #         | Qualitätsziel                                       | Messgröße                                                                                                                                                         | Zielwert                                                                                                                                                            | Messverfahren                                                                           | Zeitbezug |
| --------- | --------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- | --------- |
| **NfA-1** | **Extraktionsgüte**                                 | Exact-Match nach Normalisierung; kritische Felder (Betrag, IBAN, Datum; Micro-Avg) vs. übrige (Macro-Avg je Typ). **E-5** separat als binäre „korrekte Ablehnung" | ≥ 98 % kritisch / ≥ 85 % übrige; E-5: korrekte Ablehnung = wahr                                                                                                     | Automatischer Soll/Ist-JSON-Vergleich je LLM-Adapter; Normalisierungsregeln versioniert | ab M2     |
| **NfA-2** | **Effizienz (Latenz & Ressourcen)**                 | p95 Upload-Bestätigung → Anzeige der Vorschläge (n ≥ 100, 1 Nutzer); Peak-RAM je Dokumentklasse                                                                   | ≤ 30 s (E-1..E-3), ≤ 60 s (E-4) bei ≤ 16 GB Peak-RAM                                                                                                                | Strukturiertes Logging pro Stufe; Container-Metriken                                    | ab M2     |
| **NfA-3** | **Zuverlässigkeit / Robustheit**                    | Anteil Läufe im gültigen Endzustand (valider Vorschlag oder sauberer, protokollierter Fehler)                                                                     | 100 % (inkl. Fehlerinjektion: ungültiges LLM-JSON, Absturz Textextraktions-Adapter, leerer Text (Scan ohne Textlayer), OCR-Müll, Timeout/Ausfall externer Anbieter) | Fehlerinjektions-Testsuite                                                              | ab M3     |
| **NfA-4** | **Mandanten-Isolation im Retrieval**                | Treffer aus fremden Mandanten (`tenant_id`)                                                                                                                       | **0 Fremdtreffer**                                                                                                                                                  | Automatisierter Cross-Tenant-Test                                                       | ab M3     |
| **NfA-5** | **Datenschutz-Konformität (Lösch- & Aufbewahrung)** | Nachweisbare Löschung von Dokument und PII-haltigen Daten nach Frist / auf Anforderung; Audit-Referenzen bleiben ohne Roh-PII erhalten                            | 100 % der Löschanforderungen; keine Rest-PII                                                                                                                        | Lösch-Testfall mit Rest-PII-Prüfung                                                     | ab M3     |
| **NfA-6** | **Retrieval-Güte**                                  | Precision@3 der Ähnlichkeitssuche (nach Mandanten-Pre-Filter) gegen versionierte Relevanz-Annotation                                                              | ≥ 0.66; bis belastbares Eval-Set _provisorisch_                                                                                                                     | Soll/Ist-Vergleich Ranking vs. Ground Truth je Embedding-Modell                         | ab M2     |
| **NfA-7** | **Kosten-Indikator (KI-Anteil)**                    | Token je Dokument (Prompt + Completion) und Kosten pro 100 Dok. gemäss Preisliste des Anbieters; lokal als Rechenzeit-Proxy (LLM-Sekunden/Dok.)                   | ≤ 8 000 Token/Dok. (Median); Referenzpreis pro 100 Dok. je Anbieter dokumentiert; Ausreisser > 2× Median begründet                                                  | Auswertung Audit-Log (C-4)                                                              | ab M2     |
| **NfA-8** | **Austauschbarkeit externer Services**              | Adapterwechsel je Port (§ 4) ohne Code-Änderung im Kern, nur per Konfiguration                                                                                    | 100 % der Ports; LLM-Port mit ≥ 2 Adaptern (1 lokal, 1 extern) grün                                                                                                 | Contract-Test-Suite je Port in CI; Architekturtest „keine Produktabhängigkeit im Kern"  | ab M3     |

**Statistische Aussagekraft:** Für NfA-1 und NfA-6 umfasst das belastbare Eval-Set mindestens 15–20 Dokumente (3–4 je Typ). Für die p95-Latenz von NfA-2 sind mindestens 100 gemessene End-to-End-Läufe erforderlich; diese dürfen als kontrollierte Wiederholungen über das Eval-Set ausgeführt werden. NfA-7 wird über dieselben Läufe ausgewertet.

- **M2:** Messung gegen das provisorische 5-Dokument-Set (E-1…E-5); alle Werte sind _provisorisch/indikativ_.
- **Ab M3:** belastbare NfA-1- und NfA-6-Werte mit erweitertem Eval-Set sowie NfA-2- und NfA-7-Werte aus mindestens 100 dokumentierten Läufen.

## 4. Systemkontext

**Akteur:** Die **Sachbearbeiter:in** bedient DocExtract über das **d.velop-Frontend**, das die App als **iframe** einbettet. Der **d.velop Reverse Proxy** routet auf den lokalen HTTP-Endpunkt von DocExtract.

**Zweiter Konsument (Agent):** Ein Agent konsumiert den Extraktions-/Retrieval-Service über ein **MCP-Tool** (FR-6): mit denselben Leitplanken wie der Browser-Pfad (C-2, C-3, NfA-4).

**Nachbarsysteme und Schnittstellen:**

- **d.velop Identity Provider**: Cookie-basierte AuthN; aus der User-Session wird die **`tenant_id`** (NfA-4) abgeleitet.
- **d.velop DMS-API**: Kategorien und Eigenschaften (inkl. Datentyp) aus `/r/{repositoryId}/objdef`; Metadaten ähnlicher Dokumente _live mit der User-Session_ aus `/dms/r/{repositoryId}/o2/{document_id}/` (kein lokaler Cache); Rückschreiben _nur nach Freigabe_ (C-2). Die `repository_id` ist ab dem ersten Aufruf bekannt.
- **Third-Party-App (optional)**: Wertelisten per JIT-Webhook; DocExtract ist Client, kein persistenter Import.
- **Lokale Adapter** (innerhalb der Vertrauensgrenze): Vorschau, Textextraktion, LLM, Embedding, Vektor-Store.
- **Externes LLM / Embedding (optional)**: vom Kunden per Konfiguration anstelle des lokalen Adapters gewählt.

**Vertrauensgrenze:** Mit lokalen Adaptern bleiben Dokumentinhalt, Embeddings und Inferenz **innerhalb der lokalen Betriebsgrenze**. Ist ein externer Adapter konfiguriert, gehen die Daten an dessen Endpunkt (TLS). Weitere kontrollierte Flüsse: Session-Validierung, JIT-Wertelisten, Metadaten und Original-/Preview-Bytes zur autorisierten DMS-API.

**Adapter (Austauschbarkeit, C-8):** Der Kern kennt nur Schnittstellen und ein produktneutrales internes Datenformat. Produkte sind **Referenzadapter, kein Zwang**.

| Port                                           | Vertrag (Mindestanforderung an jeden Adapter)                                                                            | Referenzadapter                                            |
| ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------- |
| **Vorschau**                                   | Dokument → PDF-Vorschau; Limits & Timeout einhaltbar                                                                     | Gotenberg/LibreOffice                                      |
| **Textextraktion** (nur für Ähnlichkeitssuche) | Dokument → Klartext der ersten _N_ Wörter (konfigurierbar); leerer/zu kurzer Text wird gemeldet, nicht still verarbeitet | Apache PDFBox              |
| **LLM**                                        | Prompt + JSON-Schema → Antwort; liefert Modell-ID, Token-Verbrauch; Klassifizierung lokal/extern                         | lokal: Ollama (Qwen 3); extern: mind. ein Anbieter-Adapter |
| **Embedding**                                  | Text → Vektor + Embedding-Modell-ID; Klassifizierung lokal/extern                                                        | lokal via Ollama                                           |
| **Vektor-Store**                               | Ähnlichkeitssuche mit Metadaten-Pre-Filter (`tenant_id`, Status, Modell-ID)                                              | pgvector/PostgreSQL                                        |

**Technologie-Stack (Empfehlung, nicht Zwang):** Java 21 · Spring Boot 4 · Angular (iframe) · Spring AI/LangChain4j (Abstraktion LLM-/Embedding-Port) · Docker Compose · GitHub/GitLab CI · Micrometer/Prometheus/OpenTelemetry.
C4-Level-1/-2-Diagramme → [ARCHITECTURE.md § 1a/1b](ARCHITECTURE.md)

## 5. Anforderungen

### 5.1 Funktionale Anforderungen

- **FR-1: Upload, PDF-Vorschau & Eingangsvalidierung.** Upload per Frontend/REST; native PDFs direkt als Vorschau, Nicht-PDFs über den **Vorschau-Port** konvertiert. Harte **Limits** (Dateigrösse, Seitenzahl, Timeouts) mit kontrolliertem Abbruch. _Kein KI-Nutzen._
- **FR-2: Textextraktion für die Ähnlichkeitssuche.** Der **Textextraktions-Port** liefert ausschliesslich die Eingabe für FR-3: den Klartext der **ersten _N_ Wörter** (Default 5 000, konfigurierbar; nie mehr als das Kontextfenster des Embedding-Modells). Kein Chunking, keine Struktur-/Tabellenrekonstruktion, kein persistierter Zwischenzustand. _Kein KI-Nutzen._
- **FR-3: Ähnlichkeitssuche / Kategorisierung.** Aus dem Text von FR-2 wird **ein Embedding je Dokument** (Embedding-Port) einmalig erzeugt und mit `status = PENDING`, `process_id`, `expires_at`, `extraction_source` und `embedding_model` im Vektor-Store gespeichert. Jede Suche erzwingt als Pre-Filter `tenant_id`, `status = APPROVED` und dieselbe `extraction_source` und `embedding_model`-ID. Nach Freigabe werden dieselben Vektoren atomar zu `APPROVED` hochgestuft und mit `document_id` + `repository_id` verknüpft (Metadaten später live aus dem DMS). Ein Wechsel des Embedding-Modells erfordert Re-Indexierung. Kennzahl: Precision@3 (NfA-6). _KI als Teil der Lösung._
- **FR-4: KI-Attributvorschläge.** Der **LLM-Port** schlägt Eigenschaftswerte vor. Eingaben sind: (a) eine explizit konfigurierte Repräsentation des aktuellen Dokumentinhalts, zum Beispiel Volltext oder Seitenbilder; (b) Kategorien und Eigenschaften inklusive Datentyp aus `objdef`; (c) die **fünf ähnlichsten Dokumente** aus FR-3, deren Kategorie und Werte live aus dem DMS geladen werden; sowie (d) **Wertelisten** per JIT-Webhook. Die Dokumentrepräsentation für FR-4 ist vom auf die ersten _N_ Wörter begrenzten Textextraktionspfad in FR-2 getrennt. Falls kein zulässiger Wert bestimmt werden kann, lautet die strukturierte Antwort `unbekannt`. Schema-constrained Decoding wird verwendet, sofern der Adapter dies unterstützt; die **serverseitige JSON-Schema-Validierung ist immer verpflichtend**. Ungültige Antworten werden höchstens gemäss konfigurierter Retry-Regel erneut angefordert und enden danach in einem kontrollierten Fehlerzustand. _Substanzieller KI-Teil._
- **FR-5: Validierung und Rückschreiben ins DMS.** Der Mensch bestätigt oder korrigiert die Vorschläge im Validierungs-UI. Angezeigt werden Konfidenz, Quellen und **Anbieterinformationen** mit Modell-ID sowie der Klassifizierung lokal/extern. Das Rückschreiben ins DMS erfolgt erst nach expliziter Freigabe. Die Aufnahme als **herkunftsmarkierte Vorlage** erfordert eine separate, explizite Zustimmung und darf nicht implizit aus der DMS-Freigabe abgeleitet werden. _KI mittelbar._
- **FR-6: MCP-Tool für Agenten.** Extraktions-/Retrieval-Service als **MCP-Tool**; es gelten dieselben Guardrails wie im UI-Pfad: Pre-Filter (NfA-4), Least Privilege (C-3), **Consent vor kritischen Aktionen**, Audit (C-2/C-4). _KI-Werkzeug-Schnittstelle._

### 5.2 Constraints / Invarianten

- **C-1: Kontrollierter Egress:** Dokumentdaten gehen nur an die konfigurierten Adapter-Endpunkte, nie Session-Daten, Credentials oder Daten anderer Mandanten. Übriger Egress der Verarbeitungs-Container ist per **Netzwerk-Policy** unterbunden; CI-**Egress-Test** (rein lokale Konfiguration: 0 externe Verbindungen).
- **C-2: Kein automatischer Schreibzugriff:** Rückschreiben ins DMS nur nach **expliziter Benutzerbestätigung**. Keine LLM-Ausgabe löst selbsttätig einen Schreib-/Tool-Call aus: auch nicht im MCP-Pfad.
- **C-3: Least Privilege:** LLM- und Embedding-Adapter haben keinen Zugriff auf Datenbank, Vektor-Store oder DMS-API. Der Retrieval-Adapter speichert Embeddings nur als `PENDING` mit Ablaufzeit; nur der Consent-geschützte Validierungspfad setzt `APPROVED`. Das MCP-Tool erhält nur minimale Scopes.
- **C-4: Unveränderlicher Audit-Pfad:** Jeder LLM- und Tool-Call wird **append-only** ohne Roh-PII protokolliert: Anbieter, lokal/extern, Modell-ID, ein mandantenspezifischer HMAC des kanonisierten Prompts, **Token-Verbrauch**, Konfidenz und Ergebnisstatus. Schlüsselrotation und Aufbewahrungsfristen sind dokumentiert. Der Audit-Pfad ist Datenquelle für NfA-7; Löschpflicht (NfA-5) und Unveränderlichkeit koexistieren, weil keine Rohinhalte oder direkt identifizierenden Werte im Audit-Log gespeichert werden.
- **C-5: Modell-Integrität:** Lokale Modelle per **Hash/Digest gepinnt**; externe Modelle über **explizit versionierte Modell-ID** (keine „latest"-Aliase). Modellwechsel nur per auditierter Konfigurationsänderung mit Regressionslauf gegen das Eval-Set.
- **C-6: Reproduzierbarer Betrieb:** `docker compose up` auf Linux und Windows/WSL2 nach dokumentiertem Bootstrapping; Smoke-Test in CI (LLM gemockt, externe Anbieter per Contract-Test-Stub; echtes lokales Modell im Nightly).
- **C-7: Quarantäne temporärer Embeddings:** Einträge vor Freigabe tragen `status = PENDING`, `tenant_id`, `process_id`, `expires_at`, `embedding_model`, `extraction_source`. Retrieval nur auf `APPROVED`. Freigabe aktiviert vorhandene Vektoren ohne Neuberechnung; Ablehnung, endgültiger Fehler oder TTL-Ablauf löschen sie. Ein Cleanup-Job entfernt überfällige Einträge; `FINALIZED_INDEX_PENDING` ist bis zum Abschluss der Promotion geschützt.
- **C-8: Produktneutralität (Ports & Adapter):** Der Kern enthält keine produktspezifischen Typen oder APIs. Adapter werden per Konfiguration gewählt, sind als lokal/extern klassifiziert und müssen die Contract-Tests ihres Ports bestehen. Neue externe Adapter unterliegen automatisch C-1.

### 5.3 Realisierung von Qualitätszielen durch Verhalten (Referenzen)

- Mandantenfilter (NfA-4 / T-3) → FR-3, FR-6
- Retrieval-Güte / Precision@3 (NfA-6) → FR-3
- Schema-Validierung der LLM-Ausgabe (T-1) → FR-4
- Embedding-Promotion `PENDING → APPROVED`, Herkunftsmarkierung (T-2/C-7) → FR-5
- Eingangsvalidierung / Limits (T-4 / NfA-3) → FR-1
- Konfidenz-, Quellen- & Anbieteranzeige (C-2) → FR-5
- Token-/Kosten-Protokollierung je Anbieter (NfA-7) → C-4
- Austauschbarkeit (NfA-8) → C-8
- AuthN via d.velop-Cookie (Voraussetzung NfA-4) → Sicherheitsschnittstelle
- Consent + minimale Scopes für Agenten (C-2/C-3, T-6) → FR-6

### 5.4 Evaluationsbasis

**Provisorisches Set (M2):** fünf repräsentative Dokument-Typen, je mit versioniertem **Soll-JSON**. **Belastbares Set (ab M3):** 3–4 Dokumente je Typ (gesamt **≥ 15–20**), inkl. Relevanz-Annotation für Precision@3. Eval-Läufe erfolgen **je konfiguriertem LLM-/Embedding-Adapter**; Ergebnisse je Anbieter versioniert. An externe Anbieter gehen nur **synthetische oder freigegebene** Testdokumente.

| #   | Dokumenttyp                                        | Erwartete Schlüsselfelder        | Schwierigkeit                                                           |
| --- | -------------------------------------------------- | -------------------------------- | ----------------------------------------------------------------------- |
| E-1 | Eingangsrechnung (strukturiert, PDF/A)             | Betrag, IBAN, Datum, Lieferant   | niedrig: klares Layout                                                  |
| E-2 | Eingangsrechnung (gescannt, schräg, OCR-Artefakte) | Betrag, Datum, Lieferant         | hoch: Qualitätsrobustheit                                               |
| E-3 | Lieferschein mit Positionstabelle                  | Positionen, Menge, Artikel-Nr.   | mittel: Tabellen-Extraktion über den FR-4-Dokumentpfad, nicht über FR-2 |
| E-4 | Mehrseitiger Vertrag (dt./engl. gemischt)          | Vertragspartner, Datum, Laufzeit | hoch: Mehrsprachigkeit, Länge                                           |
| E-5 | Nicht-erkanntes Dokument (Bewerbungsschreiben)     | :                                | Negativ-Fall: „unbekannt" statt Halluzination (NfA-1)                   |

## 6. Guardrails

Unverhandelbare Leitplanken für den KI-Anteil: je Bedrohung mit Mitigation und realisierender Funktion.

| #       | Bedrohung                                                             | Warum naive Abwehr nicht reicht                                        | Guardrail / Mitigation                                                                                                                                                            | Umsetzung    |
| ------- | --------------------------------------------------------------------- | ---------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------ |
| **T-1** | **Direkte Prompt Injection** (Anweisungen im Dokument)                | Längenbegrenzung/Bereinigung wirken kaum: der Angriff steckt im Inhalt | Dokument strikt als _untrusted data_; schema-constrained Decoding, wo verfügbar; serverseitige Schema-Validierung; keine aus Inhalt ableitbaren Tool-Calls (C-2)                  | FR-4         |
| **T-2** | **Indirekte Injection & Datenvergiftung** über den Self-Learning-Loop | Bestätigung prüft aktuelle Felder, nicht die spätere Vorlagenwirkung   | Embeddings vor Freigabe nur als nicht retrievalfähige `PENDING`-Einträge mit TTL; Aktivierung erst nach Freigabe; Löschung bei Ablehnung/Ablauf; Herkunftsmarkierung und Rollback | FR-5         |
| **T-3** | **Cross-Tenant-Leak** über die Ähnlichkeitssuche                      | Vektorähnlichkeit kennt keine Mandantengrenzen                         | `tenant_id` aus der d.velop-Session als **Pre-Filter** in der Vektor-Query (kein Post-Filter)                                                                                     | FR-3 (NfA-4) |
| **T-4** | **Ressourcen-Erschöpfung / DoS** (Riesen-PDF, Zip-Bomb)               | Happy-Path-Pipeline hat keine Obergrenzen                              | Harte Limits (Grösse, Seiten, Timeouts je Stufe und je Adapter); kontrollierter Abbruch                                                                                           | FR-1 (NfA-3) |
| **T-5** | **Manipuliertes Modell / stiller Modellwechsel**                      | Modell-Pull ohne Verifikation; Anbieter-Alias ändert sich unbemerkt    | Digest-Pinning lokal; versionierte Modell-ID extern; Regressionslauf bei Wechsel                                                                                                  | C-5          |
| **T-6** | **Missbrauch über das MCP-Tool**                                      | Agent kann Aufrufe verketten und UI-Kontrollen umgehen                 | Minimale Scopes (C-3); Consent vor kritischen Aktionen; kein DMS-Write ohne menschliche Freigabe (C-2); Audit (C-4)                                                               | FR-6         |

**Verbindliche Grundregeln:**

- **Mensch entscheidet:** Keine irreversible Aktion (DMS-Write, Statusänderung, Vorlagen-Aufnahme) ohne explizite Freigabe (C-2): auch nicht über MCP.
- **Least Privilege überall:** LLM liest nur; Schreiben über separaten, authentifizierten Service (C-3).
- **Produktneutralität:** Externe Services über Ports angebunden und austauschbar (C-8).
- **Vollständige Nachvollziehbarkeit:** Jeder LLM-/Tool-Call append-only protokolliert, ohne Roh-PII (C-4).
- **Datenschutz vor Komfort:** Bei Zielkonflikten haben C-1 und NfA-5 Vorrang.

## 7. Interne Meilensteine

Die Meilensteine strukturieren Konzeption und Umsetzung (modulweite Planung → [PROJEKTPLAN.md](PROJEKTPLAN.md)). Jeder liefert einen prüfbaren Zustand mit Exit-Kriterium; Termine sind Richtwerte (KW) und machen die NfA-Zeitbezüge _time-bound_.

| #        | Meilenstein                                    | Termin (Richtwert) | Inhalt / Exit-Kriterium                                                                                                                                                                                                                                                                                               | Deckt ab                             |
| -------- | ---------------------------------------------- | ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------ |
| **M1**   | **Lauffähiges Gerüst**                         | KW 40 (2026)       | `docker compose up` auf Linux und Windows/WSL2; Upload + PDF-Vorschau über Vorschau-Port; Port-Schnittstellen definiert; Smoke- und Egress-Test (Default: kein Egress) in CI grün (LLM gemockt)                                                                                                                       | FR-1 · C-1 · C-6 · C-8               |
| **M1.5** | **Durchgängiger Extraktions-Pfad**             | KW 43 (2026)       | Textextraktion → Embedding → Retrieval mit Mandanten-Pre-Filter → LLM-Port (lokaler Adapter) mit Schema-Validierung; an 1–2 Dokumenten manuell verifiziert                                                                                                                                                            | FR-2 · FR-3 · FR-4 · T-1 · T-3       |
| **M2**   | **Erste End-to-End-Messung (provisorisch)**    | KW 46 (2026)       | Soll/Ist-Vergleich über E-1…E-5; NfA-1, -2, -6, -7 provisorisch ausgewiesen; Validierungs-UI mit Konfidenz-/Quellen-/Anbieteranzeige; **ein externer LLM-Adapter** per Konfiguration lauffähig                                                                                                                        | FR-5 · NfA-1 · NfA-2 · NfA-6 · NfA-7 |
| **M3**   | **Härtung & Compliance-Nachweise (belastbar)** | KW 49 (2026)       | Erweitertes Eval-Set (≥ 15–20 Dok.) sowie ≥ 100 End-to-End-Läufe für p95 und Kostenindikatoren → belastbare NfA-1/-2/-6/-7 je Adapter; Fehlerinjektion inkl. Anbieter-Ausfall (NfA-3); Cross-Tenant-Test (NfA-4); Lösch-/Rest-PII-Test (NfA-5); Contract-Tests aller Ports (NfA-8); Modell-Pinning lokal/extern (C-5) | NfA-3 · NfA-4 · NfA-5 · NfA-8 · T-5  |
