##  Lernziel

- Ich kann mein Projekt begründet in mindestens zwei Module oder Services mit expliziten Verträgen zerlegen und für eine Naht einen Architekturentscheid mit Preis, Störfall und Prüfung festhalten.
- Ich kann einen maschinenlesbaren API-Vertrag (OpenAPI) mit Fehlerfällen spezifizieren und für einen zentralen Endpunkt Idempotenz, Datenklasse und ein messbares Service-Ziel festlegen.
- Ich kann einen Dienst über ein MCP-Werkzeug für einen Agenten nutzbar machen und die Grenze dort setzen, wo sie wirkt: im Dienst selbst.
- Ich kann eine erste KI-Integration in meine Services einbauen und KI-generierten Code, auch eine repo-weite Änderung, im Diff prüfen und begründet übernehmen, korrigieren oder verwerfen.

##  Auftrag

Was Sie am Präsenztag an SupportFlow geübt haben, liefern Sie bis zur nächsten PVA an Ihrem eigenen Projekt. Beginnen Sie mit dem Blatt TRANSFER (in den Übungsblättern Block 3, «Materialien &amp; Ergebnisse»): ein Entscheid, wie zwei Ihrer Module miteinander reden, mit Preis, Störfall und Prüfung. Er geht unverändert in Artefakt 5. Danach acht Artefakte:

1. Fachliche Zerlegung: mindestens zwei klar abgegrenzte Module oder Services mit expliziten Verträgen, als Skizze mit zwei bis drei Sätzen Begründung (Änderungsgrund, Kopplung, Resilienz). Verteilt wird nur, wenn eine Qualitätsanforderung es verlangt.
2. OpenAPI mit Fehlerfällen für die öffentlich sichtbaren HTTP-Endpunkte. Ein zentraler Endpunkt vollständig: Idempotenzregel, Datenklasse oder Grenze für Personendaten und ein messbares Service-Ziel.
3. MCP-Werkzeug: mindestens eines, das einen Ihrer Services für einen Agenten nutzbar macht, wie Ihr Raumbuchungsdienst in A3 und der Ticket-Service in A5.
4. Sicherheitskonzept für Agent und Werkzeug: Autorisierung, minimale Rechte, Prüfung der Eingaben, Freigabe durch einen Menschen vor einer kritischen Aktion und ein Audit-Protokoll. Die Prüfung sitzt im Dienst, nicht in der Beschreibung des Werkzeugs.
5. Kommunikationsentscheid: eine geeignete Stelle asynchron umsetzen oder begründen, warum synchron genügt. In beiden Fällen Choreografie und Orchestrierung als Alternativen beurteilen. Grundlage ist Ihr Entscheid aus dem TRANSFER.
6. Erste KI-Integration in die Services, zum Beispiel Klassifikation, Suche in eigenen Daten oder eine Aktion durch einen Agenten. Das Modell steht hinter einem Port, wie in A5.
7. Repo-weite Änderung: Lassen Sie mit dem Projekt-Kontext-Dokument als Rahmen eine Änderung über mehrere Module vorschlagen (zum Beispiel eine Fehlerform für alle Dienste, wie in A2) und lesen Sie den Diff je Datei: übernommen, korrigiert oder verworfen, mit Grund.

8. Projekt-Kontext-Dokument v0.3: Service-Verträge und Leitplanken für Agenten (Berechtigungen, Freigaben) nachgeführt.

Dokumentieren Sie beide Seiten: wo die KI generiert hat und wo Sie korrigiert oder verworfen haben, und warum. Je Stelle ein Diff oder ein Test als Beleg.

Abgabe: Ein PDF (Zerlegung, TRANSFER-Entscheid, OpenAPI-Auszug oder Verweis auf die Datei, Sicherheitskonzept, Beschreibung von MCP-Werkzeug und KI-Integration, Notiz zur repo-weiten Änderung) plus URL auf das Git-Repository. Die Teilabgabe ist formativ, ihre Artefakte müssen spätestens in der Schlussabgabe nachgewiesen sein. Einzelarbeit.

##  Termin

Bis zur nächsten Präsenzveranstaltung (PVA 4).
