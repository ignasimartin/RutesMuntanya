# Rutes Muntanya — Guia d'instal·lació pas a pas

Aquesta guia t'acompanya des de zero fins a tenir l'app instal·lada al teu mòbil Android.
No cal saber programar: només seguir els passos.

---

## 1. Què necessites

- Un ordinador (Windows, macOS o Linux).
- El teu mòbil Android (Android 8.0 o superior).
- Un cable USB per connectar el mòbil a l'ordinador (recomanat, però hi ha una alternativa sense cable al pas 6).
- Connexió a internet a l'ordinador (la primera compilació descarrega components; després no cal).

---

## 2. Instal·lar Android Studio

Android Studio és el programa oficial de Google per crear apps Android. Ja inclou tot el que fa falta (Java/JDK i el SDK d'Android).

1. Ves a **https://developer.android.com/studio**
2. Descarrega **Android Studio** i instal·la'l acceptant les opcions per defecte.
3. La primera vegada que l'obris, deixa que completi l'assistent de configuració ("Standard"): descarregarà l'Android SDK. Això pot trigar una estona segons la connexió.

> Aquesta és la part que més pesa i triga. Un cop feta, ja no cal repetir-la.

---

## 3. Obrir el projecte

1. Descomprimeix el fitxer **RutesMuntanya.zip** en una carpeta fàcil de trobar (per exemple, l'Escriptori).
2. Obre **Android Studio**.
3. Clica **Open** (o *File → Open*).
4. Selecciona la carpeta **RutesMuntanya** (la que conté el fitxer `settings.gradle`) i clica **OK**.

---

## 4. Deixar que sincronitzi (Gradle Sync)

En obrir el projecte, Android Studio farà una **sincronització de Gradle**: descarrega automàticament les llibreries que fa servir l'app (el motor de mapes osmdroid, entre altres).

- Ho veuràs a la barra inferior ("Sync", "Downloading…").
- **La primera vegada triga uns minuts.** És normal.
- Quan acabi, hauria de dir *"Sync successful"* (o similar) sense errors vermells.

> Si et demana instal·lar algun component que falta (per exemple una versió concreta de l'SDK o "build tools"), accepta i deixa que ho instal·li.

---

## 5. Preparar el mòbil (Mode desenvolupador)

Perquè l'ordinador pugui instal·lar apps al teu mòbil, cal activar dues opcions al telèfon:

1. Ves a **Configuració → Sobre el telèfon** (o *Informació del telèfon*).
2. Busca **Número de compilació** ("Build number") i **toca'l 7 vegades** seguides. Sortirà un missatge: *"Ja ets desenvolupador"*.
3. Torna enrere i entra a **Configuració → Sistema → Opcions per a desenvolupadors** (a alguns mòbils està directament a Configuració).
4. Activa **Depuració per USB** ("USB debugging").

---

## 6. Instal·lar l'app al mòbil

Tens dues opcions. La **A (cable USB)** és la més còmoda per a la primera vegada.

### Opció A — Directament per cable USB (recomanada)

1. Connecta el mòbil a l'ordinador amb el cable.
2. Al mòbil pot aparèixer un avís *"Permetre la depuració USB?"* → accepta ("Permet sempre des d'aquest ordinador").
3. A Android Studio, a la barra superior, hauria d'aparèixer el nom del teu mòbil al selector de dispositius.
4. Clica el botó **▶ Run** (el triangle verd) a dalt.
5. Espera: compilarà l'app i l'instal·larà i obrirà automàticament al mòbil.

### Opció B — Generar un fitxer APK i passar-lo al mòbil (sense cable)

1. A Android Studio, menú **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
2. Quan acabi, apareixerà un avís a baix a la dreta amb l'enllaç **"locate"**. Clica-hi per obrir la carpeta on és l'APK.
   - Normalment és a: `RutesMuntanya/app/build/outputs/apk/debug/app-debug.apk`
3. Passa el fitxer `app-debug.apk` al mòbil (per correu, Google Drive, cable, WhatsApp Web, etc.).
4. Al mòbil, obre el fitxer per instal·lar-lo. La primera vegada et demanarà permetre **"Instal·lar apps d'origen desconegut"** → accepta-ho per a aquesta app.
5. Instal·la i obre.

> Nota: és una versió de *proves* (debug), perfecta per a ús personal. Android pot mostrar un avís genèric d'app no verificada; és normal en apps que no venen de la Play Store.

---

## 7. Com funciona l'app

Quan l'obris per primera vegada et demanarà **permís d'ubicació**: accepta ("Mentre s'utilitza l'app") perquè pugui mostrar on ets.

Botons (a baix):

- **Carrega GPX** — tria un fitxer `.gpx` del teu mòbil. La ruta apareixerà dibuixada en taronja i el mapa s'ajustarà per veure-la sencera. A dalt sortirà la **distància total** i el **desnivell** (pujada ↑ / baixada ↓) i l'altitud mínima/màxima.
- **Satèl·lit / Topogràfic** — canvia entre el mapa topogràfic (corbes de nivell, senders) i la vista de satèl·lit. El botó mostra la capa a la qual canviaràs.
- **Baixa mapes** — amb una ruta carregada, descarrega les tessel·les del mapa (topo + satèl·lit) al voltant de la ruta perquè funcionin **sense cobertura**. Fes-ho **amb WiFi i abans de sortir de casa**.
- **Centra'm** — mou el mapa fins a la teva posició actual (GPS).

El teu punt de posició s'actualitza sol amb el GPS. El mapa **no gira ni et segueix automàticament**: el mous tu amb el dit, i quan vulguis tornar a la teva posició prems *Centra'm*.

---

## 8. Ús a la muntanya (offline)

Molt important per a excursions sense cobertura:

1. A casa, amb WiFi: obre l'app, **Carrega GPX** de la ruta que faràs, i prem **Baixa mapes**. Espera que acabi.
2. Comprova (activant el mode avió, per exemple) que la zona es continua veient.
3. Ja pots sortir: encara que no tinguis cobertura, veuràs el mapa, la ruta i la teva posició per GPS (el GPS funciona sense dades mòbils).

---

## 9. Si alguna cosa falla

- **La sincronització de Gradle dona error de versió de Java/JDK**: a Android Studio, *File → Settings → Build, Execution, Deployment → Build Tools → Gradle*, i a "Gradle JDK" tria la versió **17** (o la JDK inclosa "jbr-17"/"Embedded JDK").
- **No apareix el mòbil al connectar-lo**: prova un altre cable o port USB, i confirma que has acceptat l'avís de depuració USB al telèfon. A alguns mòbils cal canviar el mode de connexió USB a "Transferència de fitxers".
- **El mapa es veu en blanc la primera vegada**: necessita internet per carregar les tessel·les el primer cop. Connecta't a WiFi/dades i mou el mapa; després ja pots baixar-les per a offline.
- **Els mapes no es baixen o donen errors de tessel·les**: alguns servidors limiten la velocitat. Torna-ho a provar amb bona connexió; una zona molt gran a zoom alt pot ser molta descàrrega.

---

## 10. Notes tècniques

- Mapa topogràfic: **OpenTopoMap** (basat en OpenStreetMap). Satèl·lit: **Esri World Imagery**. Tots dos són gratuïts per a ús personal i moderat; respecta'n les condicions d'ús no fent descàrregues massives.
- No es fan servir tessel·les de Google Maps perquè les seves condicions no permeten usar-les fora del seu propi SDK. La vista de satèl·lit d'Esri és visualment equivalent i sí que permet ús offline.
- Android mínim: **8.0 (API 26)**. Provat amb Android Studio i Gradle 8.9 / plugin d'Android 8.6.

Bones rutes! 🏔️
