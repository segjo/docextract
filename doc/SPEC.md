## SPEC — DocExtract

**KI-gestützte Dokumenterfassung & -verschlagwortung für das Dokumentenmanagement (d.velop documents)**  
Modulkontext: CAS AISE · Projektarbeit auf Basis des Referenzprojekts _SupportFlow_.

**Stand:** 08/2026 · **Meilensteine:** M1 = lauffähiges Gerüst · M1.5 = durchgängiger Extraktions-Pfad · M2 = erste End-to-End-Messung (provisorisches Eval-Set) · M3 = Härtung & Compliance-Nachweise (belastbares Eval-Set). Details & Termine → § 7. Modulweite Blockplanung → [PROJEKTPLAN.md](PROJEKTPLAN.md).

### 1. Ziel

**Sachbearbeitende sollen eingehende Dokumente ohne manuelles Abtippen korrekt erfasst und verschlagwortet im Dokumentenmanagement ablegen können — schnell, nachvollziehbar und mit minimalem Aufwand.**  
**Leitmetapher:** DocExtract ist der aufmerksame Kollege im Posteingang, der ein Dokument liest, die relevanten Attribute vorschlägt und passende frühere Fälle bereitlegt — der Mensch bestätigt oder korrigiert, bevor etwas ins DMS zurückgeschrieben wird.  
**Produktvision (Zielbild):** DocExtract reift vom _assistierenden_ zum _lernenden_ System: Aus jeder menschlichen Bestätigung entstehen bessere Vorlagen (Self-Learning-Loop), sodass die Trefferqualität mit der Nutzung steigt. Mittelfristig wächst die lokal betriebene Block-1-Lösung in die **produktive d.velop-Plattform** (Ausbaustufe, § 2) hinein — ohne das Grundprinzip _Human-in-the-Loop_ und _Datensouveränität by design_ aufzugeben.  
**Stakeholder & Kerninteressen:**
| Rolle | Kerninteresse |
| --- | --- |
| **Sachbearbeiter:in / Endnutzer:in** | Zeitersparnis und geringe Fehlerquote; wenig Klicks, kein manuelles Eintippen |
| **Records-/DMS-Verantwortliche:r** (fachlicher Owner) | Konsistente, vollständige Metadaten für Auffindbarkeit und Compliance |
| **IT-Betrieb / Datenschutz** | Datensouveränität, Betreibbarkeit (Windows/Linux), Auditierbarkeit (ISO-27001) |
| **d.velop** (Plattformbetreiber, optional) | Saubere Nutzung der DMSApp-API, Einhaltung des App-Integrationsmodells |

### 2. Scope

#### In Scope

- Dokument-Upload (Einzeldatei/REST) mit **PDF-Vorschau** und Eingangsvalidierung.
- **Strukturierte Aufbereitung** Dokument → Markdown (z.B. docling).
- **Ähnlichkeitssuche / Kategorisierung** über Embeddings + Vektor-DB, mit Berechtigungsfilter.
- **KI-Attributvorschläge** durch ein lokales LLM (schema-constrained JSON).
- **Validierung durch Menschen** und Rückschreiben ins DMS nach expliziter Bestätigung.
- **MCP-Tool**, das den Extraktions-/Retrieval-Service für einen Agenten konsumierbar macht (Modulanforderung Block 3).
- Lokaler, reproduzierbarer Betrieb (docker compose up) auf Linux und Windows/WSL2.

#### Out of Scope (bewusst ausgeklammert)

- Vollständige Multi-Tenancy, formale Compliance-Nachweise.
- Autonome Ablage **ohne** menschliche Freigabe.
- Multi-Channel-Intake über Datei-Upload/REST hinaus.
- Produktive Mehrsprachigkeit über den Eval-Fall E-4 hinaus.

#### Ausbaustufe (eigener Antrieb, nicht Modulanforderung)

- Produktive d.velop-Anbindung, Selbstlern-Feedback-Loop (Vorlagen-Rückfluss),
  optionaler Cloud-Inferenzpfad (expliziter Architekturentscheid, hebt C-1 nur für diesen Pfad auf).
- Cloud-Betrieb (SaaS) mit Multi-Tenancy, horizontaler Skalierung, Monitoring, Alerting, SLA, Backup/Restore, Disaster Recovery.

#### Offen gelassen

- Ausgestaltung des Self-Learning-Loops (Kuratierung, Quarantäne, Rollback von Vorlagen) — wird pro Block verfeinert.

### 3. Qualitätsziele (SMART)

Alle Zielwerte werden gegen das Evaluationsset (§ 5) und auf definierter **Referenz-Hardware** gemessen.
Referenz-HW: _AMD Ryzen 9 PRO 7940HS, Radeon 780M (iGPU), 62 GB RAM, lokale Inferenz via Ollama; ein Nutzer, sequenziell._

Sieben Ziele, je eine eigenständige Qualitätsdimension (Genauigkeit · Retrieval-Güte · Effizienz · Kosten · Zuverlässigkeit · Vertraulichkeit · Datenschutz). Die Spalte **Zeitbezug** verweist auf den Meilenstein (§ 7) mit hinterlegtem Kalendertermin, ab dem der Zielwert verbindlich messbar ist.
| # | Qualitätsziel | Messgröße | Zielwert | Messverfahren | Zeitbezug |
| --- | --- | --- | --- | --- | --- |
| **NfA-1** | **Extraktionsgüte** | Exact-Match nach Normalisierung; kritische Felder (Betrag, IBAN, Datum; Micro-Avg) vs. übrige (Macro-Avg je Typ). **Negativfall E-5** separat als binäre „korrekte Ablehnung" (unbekannt statt Halluzination), nicht über Exact-Match. | ≥ 98 % kritisch / ≥ 85 % übrige; E-5: korrekte Ablehnung = wahr | Automatischer Soll/Ist-JSON-Vergleich; Normalisierungsregeln versioniert | ab M2 |
| **NfA-2** | **Effizienz (Latenz & Ressourcen)** | p95 Upload-Bestätigung → Anzeige der Vorschläge (n ≥ 100, 1 Nutzer); Peak-RAM je Dokumentklasse | ≤ 30 s (E-1..E-3), ≤ 60 s (E-4) bei ≤ 16 GB Peak-RAM | Strukturiertes Logging pro Stufe; docker stats | ab M2 |
| **NfA-3** | **Zuverlässigkeit / Robustheit** | Anteil Läufe im gültigen Endzustand (valider Vorschlag oder sauberer, protokollierter Fehler) | 100 % (inkl. Fehlerinjektion: ungültiges LLM-JSON, docling-Absturz, OCR-Müll) | Fehlerinjektions-Testsuite | ab M3 |
| **NfA-4** | **Mandanten-/Berechtigungs-Isolation im Retrieval** | Unberechtigte Fremdtreffer über eine Berechtigungsgrenze hinweg (Tenant **und** ACL innerhalb eines Tenants) | **0 Fremdtreffer** | Automatisierter Cross-Tenant-/Cross-ACL-Zugriffstest | ab M3 |
| **NfA-5** | **Datenschutz-Konformität (Lösch- & Aufbewahrung)** | Nachweisbare Löschung von Dokument + Audit-Einträgen nach Frist / auf Anforderung; keine Rest-PII | 100 % der Löschanforderungen | Lösch-Testfall mit Rest-PII-Prüfung | ab M3 |
| **NfA-6** | **Retrieval-Güte (Relevanz der Vorlagen)** | Precision@3 der Ähnlichkeitssuche (nach Berechtigungs-Pre-Filter) gegen versionierte Relevanz-Annotation | ≥ 0.66 (mind. 2 von 3 Treffern fachlich relevant); bis belastbares Eval-Set _provisorisch_ | Soll/Ist-Vergleich Retrieval-Ranking gegen annotiertes Ground-Truth-Set | ab M2 |
| **NfA-7** | **Kosten-Indikator (KI-Anteil)** | Token-Verbrauch je Dokument (Prompt + Completion) und daraus abgeleitete Kosten pro 100 Dokumente; bei lokaler Inferenz als Rechenzeit-/Energie-Proxy (LLM-Sekunden je Dokument) | ≤ 8 000 Token/Dok (Median); dokumentierter Referenzpreis pro 100 Dok; Ausreißer > 2× Median begründet | Auswertung des append-only Audit-Logs (C-4: Token-Verbrauch je LLM-Call) | ab M2 |

**Statistische Aussagekraft & Meilenstein-Bindung:** Die %- und p95-Werte setzen für belastbare Aussagen ein Eval-Set von **≥ 15–20 Dokumenten (3–4 je Typ)** voraus (§ 5.4).

- **Bei M2** wird gegen das **provisorische 5-Dokument-Set** (E-1…E-5) gemessen; die Zielwerte gelten dort ausdrücklich als _provisorisch/indikativ_.
- **Belastbare** Zielwerte (NfA-1, NfA-2, NfA-6, NfA-7) werden erst mit dem **erweiterten Eval-Set ab M3** ausgewiesen.

### 4. Systemkontext

**Akteur:** Die **Sachbearbeiter:in** bedient DocExtract ausschliesslich über den Browser — nicht direkt, sondern über das **d.velop-Frontend**, das die App als **iframe** einbettet. Der **d.velop Reverse Proxy** routet die Anfragen auf den lokalen HTTP-Endpunkt von DocExtract; für den Browser ist DocExtract damit ein unsichtbarer Teil der d.velop-Oberfläche.  
**Zweiter Konsument (Agent):** Ein Agent kann den Extraktions-/Retrieval-Service über ein **MCP-Tool** (§ 5.1, FR-6) konsumieren — mit denselben Berechtigungs- und Freigabe-Leitplanken wie der Browser-Pfad (C-2, C-3, NfA-4).  
**Nachbarsysteme und Schnittstellen:**

- **d.velop Identity Provider** — Cookie-basierte AuthN; jede Anfrage trägt die User-Session, aus der **Tenant- und ACL-Prädikate** für Berechtigungsprüfungen (NfA-4) abgeleitet und weitergereicht werden.
- **d.velop DMS-API** — Metadaten ähnlicher Dokumente werden _live mit der User-Session_ abgerufen (kein lokaler Cache); bestätigte Attribute werden _nur nach expliziter Freigabe_ (C-2) zurückgeschrieben.
- **Third-Party-App (optional)** — Wertelisten-Quelle per JIT-Webhook; DocExtract ist Client, kein persistenter Import.
- **Lokale Infrastruktur** (innerhalb der Vertrauensgrenze): Ollama-Inferenz, pgvector + PostgreSQL, docling-Container.  
  **Vertrauensgrenze:** Alle Verarbeitungsschritte — Dokumentinhalt, Embeddings, Inferenz — bleiben innerhalb der **lokalen Betriebsgrenze**. Egress ist technisch unterbunden und in CI per Egress-Test verifiziert (C-1). Kontrolliert nach aussen fliessen ausschliesslich Metadaten-Lese/-Schreibzugriffe auf die DMS-API sowie JIT-Wertelisten-Abrufe.  
  **Technologie-Stack (Empfehlung, nicht Zwang):** Java 21 · Spring Boot 4 · Angular (iframe) · Gotenberg/LibreOffice (Vorschau) · docling · Spring-AI/LangChain4j · Ollama (Qwen 3) · pgvector auf PostgreSQL · Docker Compose · GitHub/GitLab CI · Micrometer/Prometheus/OpenTelemetry.  
  C4-Level-1-Diagramm (Systemkontext) und C4-Level-2-Diagramm (Container) → [ARCHITECTURE.md § 1a/1b](ARCHITECTURE.md)

### 5. Anforderungen

#### 5.1 Funktionale Anforderungen (Kernfunktionen)

- **FR-1 — Dokument-Upload, PDF-Vorschau & Eingangsvalidierung (Scope § 2).** Upload per Frontend/REST; klassische Konvertierung (Gotenberg/LibreOffice) für die visuelle Kontrolle. Harte **Limits** (Dateigröße, Seitenzahl, Timeouts) mit kontrolliertem Abbruch. _Kein KI-Nutzen._
- **FR-2 — Strukturierte Aufbereitung Dokument → Markdown (Scope § 2).** docling rekonstruiert Lesereihenfolge, Tabellen und Struktur, damit der Extraktionsschritt sauberen Kontext erhält. _KI als Werkzeug._
- **FR-3 — Ähnlichkeitssuche / Kategorisierung (Scope § 2).** Embedding + Vektor-DB findet fachlich verwandte Dokumente als Vorlage. **Berechtigungsfilter (Pre-Filter auf Mandant/ACL) vor** der Ähnlichkeitssuche. Kennzahl: Precision@3 (→ NfA-6). _KI als Teil der Lösung._
- **FR-4 — KI-Attributvorschläge (Scope § 2).** LLM extrahiert Betrag, Empfänger, Datum etc. anhand der Vorlage und nutzt bei Eigenschaften mit Wertelisten, falls vorhanden, die gültigen Werte — bzw. gibt eigenständig „unbekannt" zurück, wenn keine passt. Ausgabe **gegen JSON-Schema validierbar**. _Substanzieller KI-Teil._
- **FR-5 — Validierung & Rückschreiben ins DMS (Scope § 2).** Mensch bestätigt/korrigiert im Validierungs-UI (Konfidenz- & Quellenanzeige). Bestätigtes Ergebnis kann optional als **neue, herkunftsmarkierte Vorlage** zurückfliessen. _KI mittelbar._
- **FR-6 — MCP-Tool für Agenten-Konsum (Scope § 2, Modulanforderung Block 3).** Der Extraktions-/Retrieval-Service wird als **MCP-Tool** exponiert, sodass ein Agent (z. B. Redmine-/CI-Assistent) Dokumente extrahieren/klassifizieren lassen kann. Es gelten dieselben Guardrails wie im UI-Pfad: Berechtigungs-Pre-Filter (NfA-4), Least Privilege (C-3), **Consent vor kritischen Aktionen** und Audit-Log (C-2/C-4). _KI-Werkzeug-Schnittstelle._

#### 5.2 Constraints / Invarianten (ohne Zielwert, dauerhaft gültig)

- **C-1 — Betriebsgrenze / Datensouveränität:** Dokumentinhalte und Embeddings verlassen die lokale Betriebsgrenze nicht. Durchsetzung _technisch_ per **Netzwerk-Policy** (Egress unterbunden), in CI durch **Egress-Test** verifiziert.
- **C-2 — Kein automatischer Schreibzugriff:** Rückschreiben ins DMS nur nach **expliziter Benutzerbestätigung**. Keine LLM-Ausgabe löst selbsttätig einen Schreib-/Tool-Call aus — gilt auch für den MCP-Pfad (FR-6).
- **C-3 — Least Privilege:** Der LLM-/Inferenz-Service hat ausschliesslich **Lesezugriff** auf die Vektor-DB; Schreiboperationen laufen über einen separaten, authentifizierten Service. Das MCP-Tool erhält nur die minimal nötigen Scopes.
- **C-4 — Unveränderlicher Audit-Pfad:** Jeder LLM-Call wird **append-only** mit Modellversion, Prompt-Hash, **Token-Verbrauch** und Konfidenz protokolliert — **ohne roh-PII** (Hashes/Referenzen), damit Löschpflicht (NfA-5) und Unveränderlichkeit koexistieren. Der Token-Verbrauch ist zugleich Datenquelle für NfA-7.
- **C-5 — Modell-Integrität (Supply-Chain):** Modelle per **Hash/Digest gepinnt**; nur verifizierte Artefakte gelangen in den Betrieb.
- **C-6 — Reproduzierbarer Betrieb:** Start nach dokumentiertem Bootstrapping per docker compose up auf Linux und Windows/WSL2; Smoke-Test in CI (LLM als Mock; echtes Modell im Nightly).

#### 5.3 Realisierung von Qualitätszielen durch Verhalten (Referenzen, nicht dupliziert)

- Retrieval-Berechtigungsfilter (NfA-4 / T-3) → FR-3, FR-6
- Retrieval-Güte / Precision@3 (NfA-6) → FR-3
- Schema-Validierung der LLM-Ausgabe (T-1) → FR-4
- Vorlagen-Freigabe mit Herkunftsmarkierung (T-2) → FR-5
- Eingangsvalidierung / Limits (T-4 / NfA-3) → FR-1
- Konfidenz- & Quellenanzeige im Validierungs-UI (C-2) → FR-5
- Token-/Kosten-Protokollierung (NfA-7) → C-4 (Audit-Log)
- AuthN via d.velop-Cookie (Voraussetzung NfA-4) → Sicherheitsschnittstelle
- Consent + minimale Scopes für Agenten (C-2/C-3, T-6) → FR-6

#### 5.4 Evaluationsbasis

**Provisorisches Set (M2):** fünf repräsentative Dokument-Typen; jeder Fall mit versioniertem **Soll-JSON** als Ground Truth. **Belastbares Set (ab M3):** je Typ auf **3–4 Dokumente** erweitert (Ziel gesamt **≥ 15–20**), inkl. Relevanz-Annotation für Precision@3 (NfA-6).
| # | Dokumenttyp | Erwartete Schlüsselfelder | Schwierigkeit |
| --- | --- | --- | --- |
| E-1 | Eingangsrechnung (strukturiert, PDF/A) | Betrag, IBAN, Datum, Lieferant | niedrig — klares Layout |
| E-2 | Eingangsrechnung (gescannt, schräg, OCR-Artefakte) | Betrag, Datum, Lieferant | hoch — Qualitätsrobustheit |
| E-3 | Lieferschein mit Positionstabelle | Positionen, Menge, Artikel-Nr. | mittel — Tabellen-Extraktion |
| E-4 | Mehrseitiger Vertrag (dt./engl. gemischt) | Vertragspartner, Datum, Laufzeit | hoch — Mehrsprachigkeit, Länge |
| E-5 | Nicht-erkanntes Dokument (Bewerbungsschreiben) | — | Negativ-Fall: „unbekannt" statt Halluzination (Metrik: korrekte Ablehnung, → NfA-1) |

### 6. Guardrails

Guardrails sind die unverhandelbaren Leitplanken für den KI-Anteil. Jede adressiert eine konkrete Bedrohung und ist einer Mitigation und einer realisierenden Funktion zugeordnet.
| # | Bedrohung | Warum naive Abwehr nicht reicht | Guardrail / Mitigation | Umsetzung |
| --- | --- | --- | --- | --- |
| **T-1** | **Direkte Prompt Injection** (Anweisungen im Dokumentinhalt) | Längenbegrenzung/Free-Text-Bereinigung wirken kaum — der Angriff steckt im Inhalt | Dokument strikt als _untrusted data_, nie als Instruktion/System-Prompt; **schema-constrained Decoding**; Output-Validierung gegen JSON-Schema; keine aus Inhalt ableitbaren Tool-Calls (→ C-2) | FR-4 |
| **T-2** | **Indirekte Injection & Datenvergiftung** über den Self-Learning-Loop | Nutzer-Bestätigung prüft aktuelle Felder, nicht die spätere Vorlagenwirkung | Rückfluss nur **nach Freigabe**, mit Herkunfts-/Vertrauensmarkierung; Vorlagen kuratierbar/rückrollbar; optional Quarantäne | FR-5 |
| **T-3** | **Cross-Tenant-/Cross-ACL-Leak** über die Ähnlichkeitssuche | Reine Vektorähnlichkeit kennt keine Berechtigung; auch _innerhalb_ eines Tenants gelten dokument-/ordnerbezogene ACLs | Berechtigungs-**Pre-Filter** im Retrieval: aus der d.velop-Session abgeleitete **Tenant- und ACL-Prädikate** werden als Filter in die Vektor-Query eingebettet (nicht als Post-Filter); Umsetzung über die AuthN-basierte Kontextauflösung | FR-3 (→ NfA-4) |
| **T-4** | **Ressourcen-Erschöpfung / DoS** (Riesen-PDF, Zip-Bomb) | Happy-Path-Pipeline hat keine Obergrenzen | Harte Limits (Dateigröße, Seitenzahl, Timeouts je Stufe); kontrollierter Abbruch | FR-1 (→ NfA-3) |
| **T-5** | **Manipuliertes/kompromittiertes Modell** | Ollama-Pull ohne Verifikation | **Digest-Pinning** & Integritätsprüfung vor Deployment | C-5 (Betriebs-/CI-Kontrolle) |
| **T-6** | **Missbrauch über das MCP-Tool** (Agent löst unautorisierte/irreversible Aktion aus) | Ein Agent kann Aufrufe verketten; blindes Vertrauen in den Aufrufer umgeht UI-Kontrollen | Minimale Scopes (C-3); **Consent vor kritischen Aktionen**; kein Schreib-/DMS-Write ohne menschliche Freigabe (C-2); jeder Tool-Call append-only auditiert (C-4) | FR-6 |

**Verbindliche Grundregeln (unabhängig von einzelnen Bedrohungen):**

- **Mensch entscheidet:** Keine irreversible Aktion (DMS-Write, Statusänderung, Vorlagen-Aufnahme) ohne explizite menschliche Freigabe (C-2) — auch nicht über das MCP-Tool.
- **Least Privilege überall:** LLM liest nur; Schreiben über separaten, authentifizierten Service (C-3).
- **Datensouveränität by design:** Egress der Verarbeitungs-Container technisch unterbunden und in CI getestet (C-1).
- **Vollständige Nachvollziehbarkeit:** Jeder LLM-/Tool-Call append-only protokolliert, ohne roh-PII (C-4).
- **Datenschutz vor Komfort:** Bei Zielkonflikten haben C-1 und NfA-5 Vorrang.

### 7. Interne Meilensteine

Diese Meilensteine strukturieren die **Konzeptions- und Umsetzungsphase** — die modulweite Blockplanung steht in [PROJEKTPLAN.md](PROJEKTPLAN.md). Jeder Meilenstein liefert einen prüfbaren Zustand mit klarem Exit-Kriterium. **M1.5** entkoppelt den fachlichen Extraktions-Pfad vom reinen Gerüst, **M3** trennt Robustheits- und Compliance-Nachweise von der ersten Messung. Die **Termine** sind Richtwerte (KW = Kalenderwoche) und machen die NfA-Zeitbezüge (§ 3) _time-bound_.
| # | Meilenstein | Termin (Richtwert) | Inhalt / Exit-Kriterium | Deckt ab |
| --- | --- | --- | --- | --- |
| **M1** | **Lauffähiges Gerüst** | KW 40 (2026) | docker compose up startet den Stack auf Linux und Windows/WSL2; Upload + PDF-Vorschau funktionieren; Smoke- und Egress-Test in CI grün (LLM als Mock) | FR-1 · C-1 · C-6 |
| **M1.5** | **Durchgängiger Extraktions-Pfad** | KW 43 (2026) | docling → Retrieval mit ACL-Pre-Filter → schema-constrained LLM-Ausgabe; an 1–2 Dokumenten manuell verifiziert; JSON-Schema-Validierung greift | FR-2 · FR-3 · FR-4 · T-1 · T-3 |
| **M2** | **Erste End-to-End-Messung (provisorisch)** | KW 46 (2026) | Automatisierter Soll/Ist-Vergleich über das 5-Dokument-Set (E-1…E-5); NfA-1, NfA-2, NfA-6 und NfA-7 als _provisorische_ Werte ausgewiesen; Validierungs-UI mit Konfidenz-/Quellenanzeige | FR-5 · NfA-1 · NfA-2 · NfA-6 · NfA-7 |
| **M3** | **Härtung & Compliance-Nachweise (belastbar)** | KW 49 (2026) | Erweitertes Eval-Set (≥ 15–20 Dok.) → belastbare NfA-1/-2/-6/-7; Fehlerinjektions-Suite (NfA-3); Cross-Tenant-/Cross-ACL-Test (NfA-4); Lösch-/Rest-PII-Test (NfA-5); Digest-Pinning im Betrieb (T-5/C-5) | NfA-3 · NfA-4 · NfA-5 · T-5 |
