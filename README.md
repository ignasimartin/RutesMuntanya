# Rutes Muntanya 🏔️

![Build APK](https://github.com/ignasimartin/RutesMuntanya/actions/workflows/build.yml/badge.svg)
![Llicència: MIT](https://img.shields.io/badge/Llic%C3%A8ncia-MIT-green.svg)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84.svg?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF.svg?logo=kotlin&logoColor=white)

App Android per **seguir rutes `.gpx` a la muntanya**: carrega la ruta, veu la teva
posició en temps real amb el GPS i fes servir els mapes **sense cobertura** (offline).
Feta amb mapes lliures (OpenStreetMap / OpenTopoMap i Esri World Imagery), sense
dependre dels serveis de Google.

## ✨ Funcions

- **Carrega rutes `.gpx`** i les dibuixa sobre el mapa, amb el nom de la ruta a dalt.
- **Dues capes de mapa** commutables: **topogràfic** (OpenTopoMap) i **satèl·lit** (Esri World Imagery).
- **La teva posició en temps real**: punt blau amb una **fletxa d'orientació** (brúixola).
- **Mapes offline**: descarrega la zona de la ruta i fes-la servir sense cobertura.
- **Track amb gradient de desnivell**: verd a les baixades, vermell a les pujades, amb **fletxes de sentit** de la marxa.
- **Estadístiques**: distància total i desnivell acumulat (pujada ↑ / baixada ↓).
- **Perfil d'altitud** de la ruta, que es pot mostrar/amagar, i **selecció de punts**: toca el perfil i es marca el lloc corresponent al mapa.

## 📲 Com aconseguir l'app

No cal compilar res: cada canvi es compila automàticament amb GitHub Actions.

1. Ves a la pestanya **[Actions](../../actions)** del repositori.
2. Obre l'execució més recent de **Build APK** i, a **Artifacts**, descarrega `RutesMuntanya-debug-apk`.
3. Descomprimeix-lo, passa `app-debug.apk` al mòbil i instal·la'l (cal permetre "instal·lar apps d'origen desconegut").

Si vols compilar-la tu mateix amb Android Studio, tens la guia pas a pas a
**[GUIA_INSTALLACIO.md](GUIA_INSTALLACIO.md)** (des de zero, sense saber programar).

## 🥾 Ús a la muntanya (offline)

A casa, amb WiFi: obre l'app, **carrega el `.gpx`** de la ruta i prem **Baixa mapes**.
Un cop descarregada la zona, ja pots sortir: encara que no tinguis cobertura, veuràs
el mapa, la ruta i la teva posició per GPS (el GPS funciona sense dades mòbils).

## 🛠️ Tecnologia

- **Kotlin** + **Android SDK** (mínim Android 8.0 / API 26).
- **[osmdroid](https://github.com/osmdroid/osmdroid)** per als mapes raster i el suport offline.
- Mapes: **OpenTopoMap** (topogràfic) i **Esri World Imagery** (satèl·lit).

## 📁 Estructura del codi

| Fitxer | Descripció |
|--------|------------|
| `MainActivity.kt` | Pantalla principal, mapa, GPS, descàrrega offline i estadístiques. |
| `GpxParser.kt` | Lectura de fitxers GPX i càlcul de distància/desnivell. |
| `ElevationProfileView.kt` | Vista del perfil d'altitud i selecció de punts. |
| `Graphics.kt` | Icones (punt blau, fletxa) i colors segons el pendent. |
| `HeadingArrowOverlay.kt` | Fletxa d'orientació sobre la posició actual. |
| `RouteArrowsOverlay.kt` | Fletxes de sentit al llarg de la ruta. |
| `app/src/main/res/` | Interfície, textos, colors i icona. |

## 🗺️ Crèdits dels mapes

- Mapa topogràfic: **© OpenTopoMap** (CC-BY-SA), dades **© OpenStreetMap** contributors.
- Imatge de satèl·lit: **© Esri** World Imagery.

## 📄 Llicència

Aquest projecte es distribueix sota la llicència **MIT**. Consulta el fitxer
[LICENSE](LICENSE) per als detalls.
