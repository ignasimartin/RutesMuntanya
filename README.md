# Rutes Muntanya

App Android per seguir rutes `.gpx` a la muntanya, amb mapa topogràfic i de satèl·lit,
la teva posició per GPS i suport per fer servir els mapes **sense cobertura** (offline).

## Funcions
- Carrega una ruta des d'un fitxer `.gpx` i la dibuixa sobre el mapa.
- Punt d'ubicació en temps real amb el GPS del mòbil.
- Dues capes commutables: **topogràfic** (OpenTopoMap) i **satèl·lit** (Esri World Imagery).
- Descàrrega de la zona de la ruta per fer-la servir offline.
- Distància total i desnivell acumulat (pujada/baixada) de la ruta.

## Com instal·lar-la
Segueix **GUIA_INSTALLACIO.md** — explicat pas a pas des de zero, sense necessitat de saber programar.

## Estructura
- `app/src/main/java/com/exemple/rutesmuntanya/MainActivity.kt` — pantalla principal i lògica del mapa.
- `app/src/main/java/com/exemple/rutesmuntanya/GpxParser.kt` — lectura de fitxers GPX i càlcul d'estadístiques.
- `app/src/main/res/` — interfície, textos, colors i icona.

## Requisits
- Android Studio (inclou JDK i SDK).
- Android 8.0 (API 26) o superior al mòbil.
