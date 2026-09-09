# PROJEKTPLAN — DocExtract

**Modulweite Aufwands- und Blockplanung der CAS-AISE-Projektarbeit**
Begleitdokument zu [SPEC.md](SPEC.md) · Stand: 08/2026

> **Zweck dieses Dokuments:** Die [SPEC.md](SPEC.md) beschreibt das _Was_ (Vision, Anforderungen, Guardrails) und enthält die **internen** Meilensteine M1–M3 der Konzeptionsphase. Dieser Projektplan beschreibt das _Wann_ über die **fünf Modul-Blöcke** hinweg und ordnet den Gesamtaufwand von **~185 h** (≈ 145 h Block 1–4 + ≈ 40 h Block 5) den einzelnen Etappen zu.

---

## 1. Rahmen laut Aufgabenstellung

| Merkmal           | Vorgabe                                                  |
| ----------------- | -------------------------------------------------------- |
| Form              | Einzelarbeit, keine Gruppenarbeit                        |
| Struktur          | 5 aufeinander aufbauende Blöcke (durchgängiges Artefakt) |
| Aufwand Block 1–4 | ~145 h (formative Meilensteine)                          |
| Aufwand Block 5   | ~40 h (Schlussabgabe)                                    |
| **Gesamt**        | **~185 h**                                               |
| Abgabe            | 2 Wochen nach PVA 5                                      |
| Validierung       | PVA-Präsentation (~10 Min) + Projektbericht (20–30 S.)   |
| Bewertung         | 18 Kriterien · 100 Punkte                                |

---

## 2. Aufwandsverteilung im Überblick

| Block   | Thema                  | Aufwand | Kumuliert |
| ------- | ---------------------- | ------: | --------: |
| Block 1 | Konzeption (bis PVA 2) |   ~30 h |      30 h |
| Block 2 | Frontend (bis PVA 3)   |   ~38 h |      68 h |
| Block 3 | Services (bis PVA 4)   |   ~42 h |     110 h |
| Block 4 | Persistenz (bis PVA 5) |   ~35 h |     145 h |
| Block 5 | Deployment & Abgabe    |   ~40 h | **185 h** |

> **Kritischster Block:** Block 3 (Services) — MCP-Tool, OpenAPI, Sicherheitskonzept und erste KI-Integration fallen zusammen. Hier früh anfangen.

---

## 3. Block 1 — Konzeption · ~30 h _(bis PVA 2)_

| #   | Aufgabe                                                      |   h | Status                             | Raster-Bezug   |
| --- | ------------------------------------------------------------ | --: | ---------------------------------- | -------------- |
| 1.1 | Vision, Stakeholder, 3–5 Kernfunktionen + KI-Nutzen          |   6 | ✅ SPEC fertig                     | Spez. (1)(3)   |
| 1.2 | C4 L1 + L2 (System-/Container-Sicht)                         |   8 | ✅ ARCHITECTURE § 3/5.1            | Entwurf (4)(5) |
| 1.3 | **ADR** Grundentscheid (modularer Monolith vs. Alternativen) |   4 | ✅ ARCHITECTURE § 9 (ADR-001..003) | Entwurf (4)    |
| 1.4 | Lauffähiges Skelett + „Hello World"-Endpoint (M1)            |   8 | ✅ Umsetzung abgeschlossen         | Prog. (7)(8)   |
| 1.5 | Evaluationsbasis (E-1…E-5)                                   |   2 | ✅ SPEC § 5.4                      | Valid. (11)    |
| 1.6 | Projekt-Kontext-Dokument v0.1 (arc42-orientiert)             |   2 | ✅ ARCHITECTURE.md fertig          | KI/Arch (17)   |

**Exit-Kriterium:** SPEC + ARCHITECTURE + erstes ADR + lauffähiges Skelett im Repo.

---

## 4. Block 2 — Frontend · ~38 h _(bis PVA 3)_

| #   | Aufgabe                                                         |   h | Raster-Bezug          |
| --- | --------------------------------------------------------------- | --: | --------------------- |
| 2.1 | Wireframes der 3–5 Kern-Screens (Upload, Vorschau, Validierung) |   5 | Entwurf (5)           |
| 2.2 | Frontend-Impl. (Angular, iframe) gegen statische Testdaten      |  18 | Prog. (7)(8)          |
| 2.3 | Generierte Unit-Tests + Diff-Notiz (KI korrigiert/verworfen)    |   7 | Valid. (13) · KI (15) |
| 2.4 | A11y-/Security-/Performance-Budget-Prüfung                      |   4 | Valid. (12)           |
| 2.5 | **ADR** Präsentationsschicht (CSR-Entscheid)                    |   2 | Entwurf (4)           |
| 2.6 | Projekt-Kontext-Dokument v0.2                                   |   2 | KI/Arch (17)          |

**Exit-Kriterium:** Bedienbares Frontend gegen Mock-Daten; zweites ADR; dokumentierte KI-Korrekturen (Diffs).

---

## 5. Block 3 — Services · ~42 h _(bis PVA 4)_ — **schwerster Block**

| #   | Aufgabe                                                               |   h | Raster-Bezug          |
| --- | --------------------------------------------------------------------- | --: | --------------------- |
| 3.1 | Zerlegung in ≥ 2 Module mit expliziten Verträgen                      |   6 | KI/Arch (17)          |
| 3.2 | **OpenAPI**-Specs inkl. Fehlerfälle (+ Idempotenz/PII für 1 Endpoint) |   6 | Prog. (8)             |
| 3.3 | **MCP-Tool** (Service agent-konsumierbar, → SPEC FR-6)                |   8 | KI/Arch (16)          |
| 3.4 | Sicherheitskonzept Agent/MCP (AuthZ, min. Scopes, Consent, Audit)     |   6 | Valid. (12) · KI (16) |
| 3.5 | Kommunikationsentscheid (sync/async begründen)                        |   3 | Entwurf (4)           |
| 3.6 | **Erste KI-Integration** (FR-4 Extraktion / FR-3 Retrieval)           |   9 | KI/Arch (16)          |
| 3.7 | Modulübergreifende, kontextgetriebene KI-Änderung + Review-Diff       |   4 | KI/Arch (15)(18)      |

**Exit-Kriterium:** Zwei Module mit Verträgen; MCP-Tool lauffähig; erste KI-Funktion integriert und abgesichert.

---

## 6. Block 4 — Persistenz · ~35 h _(bis PVA 5)_

| #   | Aufgabe                                                           |   h | Raster-Bezug |
| --- | ----------------------------------------------------------------- | --: | ------------ |
| 4.1 | Datenmodell (PostgreSQL) + Migrations-Skripte                     |   7 | Entwurf (6)  |
| 4.2 | Abstraktionsschicht (Hibernate/Panache o. ä.)                     |   6 | Prog. (8)    |
| 4.3 | **pgvector Retrieval-Spike** + Integrationsentscheid              |   8 | KI/Arch (16) |
| 4.4 | Retrieval-Evaluation (versionierter Korpus, ≥ 1 Kennzahl → NfA-6) |   6 | Valid. (14)  |
| 4.5 | Dynamische Abfrage / Begründung statischer Abfragen               |   3 | Prog. (8)    |
| 4.6 | Workflow-State-Persistenz (ggf. Transactional Outbox)             |   3 | Prog. (8)    |
| 4.7 | Projekt-Kontext-Dokument v0.4                                     |   2 | KI/Arch (17) |

**Exit-Kriterium:** Relationales Datenmodell + Migrations; pgvector-Retrieval mit Evaluationskennzahl; State-Persistenz begründet.

---

## 7. Block 5 — Deployment & Abgabe · ~40 h _(2 Wochen nach PVA 5)_

| #   | Aufgabe                                                                          |   h | Raster-Bezug          |
| --- | -------------------------------------------------------------------------------- | --: | --------------------- |
| 5.1 | Container (Compose), reproduzierbar, Basis-Images per Digest                     |   6 | KI/Arch (17)          |
| 5.2 | **CI/CD-Pfad** bis versioniertem Container-Image                                 |   6 | Prog. (8)             |
| 5.3 | Monitoring/Observability-Hooks (Logging, Metriken, Traces)                       |   4 | Valid. (14)           |
| 5.4 | **KI-Betriebsdaten**: Latenz, **Token/Kosten (→ NfA-7)**, Eval, Guardrail-Events |   5 | Valid. (14) · KI (16) |
| 5.5 | **DevSecOps-Gate** (SAST/Dependency/Secret/Image-Scan) + Befund                  |   4 | Valid. (12)           |
| 5.6 | Abnahmenachweis für alle Kernfunktionen                                          |   5 | Valid. (11)           |
| 5.7 | **Projektbericht PDF (20–30 S.)** + Eigenständigkeitserklärung                   |   8 | alle Gruppen          |
| 5.8 | PVA-Präsentation vorbereiten (~10 Min)                                           |   2 | Authentizität         |

**Exit-Kriterium:** Vollständige, containerisierte Lösung; CI/CD; Bericht; Präsentation.

---

## 8. Scope-Cut — bewusst _nicht_ implementiert (ohne Punktverlust)

Diese Punkte bleiben **konzeptionell in der SPEC beschrieben**, werden aber **nicht** umgesetzt. Das Raster fordert Tiefe an einer Stelle, nicht Feature-Breite.

| Element                                 | Entscheidung               | Begründung                                        |
| --------------------------------------- | -------------------------- | ------------------------------------------------- |
| Self-Learning-Loop / Vorlagen-Rückfluss | ❌ nur konzeptionell (T-2) | Human-in-the-Loop (FR-5) genügt fürs Raster       |
| Cluster-Modus / horizontale Skalierung  | ❌ ein Compose-Stack       | Raster: „als Container lauffähig", nicht skaliert |
| Eval-Set 15–20 Dok. produktiv           | ⚠️ optional (M3)           | Modul verlangt nur „3–5 repräsentative Fälle"     |
| Cross-ACL vollständig produktiv         | ⚠️ Konzept + 1 Testfall    | NfA-4 als Nachweis, kein volles ACL-Modell        |
| Cloud-Inferenzpfad / Multi-Tenancy      | ❌ Ausbaustufe             | rasterirrelevant                                  |

---

## 9. Kritische Pflichtteile — früh einplanen

Diese sind **Modul-Pflicht** und dürfen nicht am Ende untergehen:

- 🔴 **MCP-Tool** (Block 3, FR-6) — konsumierbar für Agenten.
- 🔴 **Kosten-Indikator** (NfA-7) — Token/Kosten je Dokument, ab Block 3 mitloggen.
- 🔴 **ADRs** — mind. 3 wichtigste Entscheidungen (ab Block 1 sammeln).
- 🟠 **Projekt-Kontext-Dokument** — v0.1 → v1.0 über alle Blöcke pflegen.
- 🟠 **Entwicklungstagebuch** — jede relevante KI-Nutzung (Prompt, Tool, Übernahme/Korrektur/Veto) _laufend_ festhalten. Zahlt direkt auf 19 Raster-Punkte (KI-Nutzung 12 + Reflexion 7) ein.
- 🟠 **DevSecOps-Gate** (Block 5) — mind. ein automatischer Security-Check.

---

## 10. Zeitpuffer & Empfehlung

- Die ~185 h sind **eng kalkuliert**; plane **10–15 % Puffer** (≈ 20 h) für Unvorhergesehenes, indem du den Scope-Cut (§ 8) konsequent einhältst.
- **Baue früh den dünnen vertikalen Schnitt:** Upload → docling → LLM-Extraktion → menschliche Bestätigung → Rückschreiben. Damit steht der repräsentative Pfad für die PVA-Präsentation schon nach Block 3.
- **Doku parallel zur Entwicklung**, nicht am Ende — sonst gehen die 19 „billigen" KI-/Reflexions-Punkte verloren.
