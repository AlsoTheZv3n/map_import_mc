# Map Import

Fabric-Mod für Minecraft 26.3: Ein Bild vom eigenen PC wird zu Karten (`filled_map`), die man in Itemrahmen hängen kann. Der Client wandelt das Bild um, der Server legt die Karten an. Der Mod muss auf **Client und Server** installiert sein.

## Benutzung

```
/mapimport <spalten> <zeilen> [optionen…]
```

Öffnet den Dateidialog. Das Bild wird auf `spalten × zeilen` Karten verteilt (je 128 × 128 Pixel, höchstens 8 × 8) und landet im Inventar.

| Option | Wirkung |
| --- | --- |
| `fit` (Standard) / `fill` / `stretch` | Seitenverhältnis halten mit durchsichtigem Rand / zuschneiden / verzerren |
| `smooth` (Standard) / `pixel` | weich skalieren für Fotos / Nearest Neighbour für Pixel-Art |
| `dither` (Standard) / `nodither` | Floyd-Steinberg an / aus (aus ist besser für Logos und Flächen) |
| `compuphase` (Standard) / `deltae2000` | Farbabstand: schnell und gut / langsamer, genauer |
| `preview` | nichts hochladen, nur `.minecraft/mapimport/<name>-preview.png` speichern |

Beispiel: `/mapimport 4 3 fill deltae2000`

Auf dem Server dürfen nur Spieler im Kreativmodus oder mit Operator-Rechten (Stufe 2) importieren, höchstens 64 Karten pro Minute.

## Bauen

Braucht JDK 25. Gradle sucht es über `gradle/gradle-daemon-jvm.properties` selbst, auch wenn `JAVA_HOME` auf ein älteres JDK zeigt.

```
gradlew build        # Jar in build/libs, inkl. Unit-Tests
gradlew runClient    # Dev-Client zum Ausprobieren
```

## Aufbau

- `convert/`: Palette aus `MapColor`, Skalieren, Farbabgleich (CompuPhase, CIEDE2000), Dithering, Kacheln. Läuft auf dem Client in einem eigenen Thread.
- `network/MapTilePayload`: eine Kachel pro Paket (16.384 Bytes; Pakete zum Server dürfen höchstens 32 KiB groß sein).
- `server/MapCreator`: prüft Rechte, Format und Farb-IDs und erzeugt gesperrte Karten in einer Pseudo-Dimension, damit die Welt sie nie übermalt.
- `client/`: Befehl `/mapimport`, Dateidialog über SDL3 (wird mit Minecraft 26.x mitgeliefert).
