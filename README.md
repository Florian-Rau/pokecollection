# Pokémon-Sammlung

Getestet auf Windows 11 mit
Azul OpenJDK 21.0.12.1
Maven 3.9.11
NPM 11.19.0

Dieses Repository enthält die erste Version der Pokémon-Sammlungs-App:

* `backend/` — Spring Boot API und statische Dateien
* `frontend/` — Eigenständige Angular-SPA (Standalone-SPA)

## Starten der Anwendung

Aus dem Verzeichnis `backend/`:

```
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

Sobald die Anwendung startet, öffne `http://localhost:8080`. Ein nicht authentifizierter Aufruf leitet zur Registrierung/Anmeldung weiter, während ein angemeldeter Trainer standardmäßig auf der Seite „Meine Sammlung“ landet. Das Backend speichert die Daten in einer H2-Datei unter `backend/data/`.

Aktuell nicht implementiert:
- L10N
- Auditing
- Administration von Trainern
- Caching PokeApi
- Pokémon Details
- JWT Refresh

Vereinfachung:
- Hardcoded JWT Secret Key, wenn Environment variable nicht gesetzt ist.
