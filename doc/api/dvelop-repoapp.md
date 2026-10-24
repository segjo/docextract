Abfragen der Liste der Repositoryverbindungen (RepoApp)
Freigegeben: JSON-Repräsentation

Sie benötigen für die Implementierung eigener Funktionen Informationen zu den konfigurierten Repositoryverbindungen. Mit der RepoApp können Sie die Liste der Repositoryverbindungen mithilfe einer HTTP GET-Anforderung für die REST-Ressource /repo/repositories abrufen.

Request

GET /repo/repositories
Accept: application/hal+json
Code kopieren
Sie erhalten im Erfolgsfall den Statuscode 200 und folgendes JSON-Objekt:

Response

{
"repositories": [
{
"_links": {
"self": {
"href": "/repo/repositories/ccdb395f-8edb-5dd1-a6e4-27365f6049e8/"
}
},
"id": "ccdb395f-8edb-5dd1-a6e4-27365f6049e8",
"name": "Contoso",
"serverId": "P",
"host": "d3.contoso.com",
"port": "3400",
"groupId": "72110c2d-e5e0-4e8b-9454-8d351ff86be8",
"isDefault": false,
"isAvailable": true,
"isReachable": true
},
...
]
}
Code kopieren
Eigenschaft

Eigenschaft eines enthaltenen Objekts

Beschreibung

repositories

id

Gibt die ID der Verbindung an. Die ID entspricht der vom d.3-Server automatisch generierten ID zur Identifizierung eines Repositorys.

name

Gibt den Anzeigenamen des Repositorys an. Dies kann ein beliebiger Name sein. Der Name dient den Anwendern als Orientierung.

serverId

Gibt die ID des d.3-Repositorys an, mit dem eine Verbindung hergestellt wird.

host

Gibt den Namen des Computers an, auf dem sich das d.3-Repository befindet.

port

Gibt die Portnummer an, über die auf ein d.3-Repository zugegriffen wird.

publicKey

Gibt den Schlüssel für eine verschlüsselte Kommunikation mit dem d.3-Repository (D3FC-Verschlüsselung) an. Haben Sie oder hat der Administrator beim Herstellen einer Verbindung keinen Schlüssel für die Verbindung angegeben, ist die Eigenschaft nicht vorhanden.

groupId

Gibt die ID einer Benutzergruppe in d.ecs identity provider an, für die dieses d.3-Repository verfügbar sein soll. Der Wert ist eine GUID oder leer. Wenn der Wert leer ist, dann ist dieses d.3-Repository für alle Benutzer sichtbar.

Sie können jederzeit die Benutzergruppe ändern oder entfernen.

isDefault

Gibt an, ob das Repository als Standardrepository definiert wurde.

isAvailable

Gibt an, ob der aktuell angemeldete Benutzer im d.3-Repository existiert.

isReachable

Gibt an, ob das d.3-Repository erreichbar ist.

Sie erhalten auf die Anforderung folgende mögliche Statuscodes:

Statuscode

Beschreibung

200

Das Liste der Repositoryverbindungen wurde erfolgreich ermittelt.

500

Es trat ein interner Fehler bei der Verarbeitung auf.
