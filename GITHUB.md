# Treballar amb GitHub (i compilar l'APK automàticament)

Posar el projecte a GitHub et dóna tres coses:

1. **Historial de versions**: cada canvi queda desat i pots tornar enrere.
2. **Còpia de seguretat al núvol** del codi.
3. **Compilació automàtica de l'APK**: cada cop que puges canvis, GitHub compila
   l'app i et deixa l'APK a punt per descarregar. Així **no cal Android Studio**
   per obtenir l'APK (encara el pots fer servir per editar el codi si vols).

Ja he deixat el repositori **inicialitzat amb un primer commit** i el workflow de
compilació configurat (`.github/workflows/build.yml`). Només et falta connectar-lo
al teu compte de GitHub i fer el primer *push*.

---

## Pas 1 — Crear el repositori a GitHub

1. Ves a **https://github.com/new**
2. Nom: `rutes-muntanya` (o el que vulguis).
3. Deixa'l **buit** (sense README, sense .gitignore, sense llicència): el codi ja el tens.
4. Tria **Private** si el vols privat.
5. Clica **Create repository**.

GitHub et mostrarà una adreça del tipus:
`https://github.com/EL_TEU_USUARI/rutes-muntanya.git`

---

## Pas 2 — Pujar el codi (des de la carpeta del projecte)

Obre un terminal a la carpeta `RutesMuntanya` i executa (canvia l'adreça per la teva):

```bash
git remote add origin https://github.com/EL_TEU_USUARI/rutes-muntanya.git
git branch -M main
git push -u origin main
```

> La primera vegada et demanarà iniciar sessió a GitHub. El més senzill és instal·lar
> **GitHub Desktop** (https://desktop.github.com) i fer *Add existing repository* +
> *Publish*, que gestiona el login per tu sense tocar el terminal.

---

## Pas 3 — Descarregar l'APK que ha compilat GitHub

1. Al teu repositori, obre la pestanya **Actions**.
2. Entra a l'execució més recent (**Build APK**) i espera que acabi (uns minuts; punt verd ✓).
3. A baix, a **Artifacts**, descarrega **RutesMuntanya-debug-apk**.
4. Descomprimeix-lo: a dins hi ha `app-debug.apk`. Passa'l al mòbil i instal·la'l
   (cal permetre "instal·lar apps d'origen desconegut", com a la guia principal).

### Opció: crear una "Release" amb versió

Si vols una descàrrega fixa i endreçada per versió:

```bash
git tag v1.0
git push origin v1.0
```

Això farà que GitHub compili i **publiqui una Release** amb l'APK adjunt, a la
pestanya **Releases** del repositori.

---

## Com treballaríem els canvis a partir d'ara

El flux recomanat per anar fent millores sense trencar el que funciona:

1. **Branca per cada millora**: `git checkout -b millora-perfil-altitud`
2. Fas els canvis (o me'ls demanes i te'ls preparo).
3. Els deses: `git add -A && git commit -m "Afegeix perfil d'altitud"`
4. Els puges: `git push -u origin millora-perfil-altitud`
5. A GitHub, obres un **Pull Request** cap a `main`. Allà GitHub **compila l'APK
   d'aquella branca** automàticament, així el pots provar abans d'ajuntar-lo.
6. Si va bé, fas **Merge** del Pull Request cap a `main`.

Avantatge: `main` sempre conté una versió que compila i funciona, i cada prova té
el seu APK generat sol.

### Com treballaria jo amb tu

Com que aquí no tinc accés al teu compte de GitHub, la manera pràctica és:

- Tu em demanes una millora.
- Jo et preparo els fitxers modificats (o el zip sencer actualitzat).
- Tu els copies a la carpeta del repo i fas `commit` + `push` (o ho fas des de
  GitHub Desktop arrossegant els fitxers).
- GitHub compila l'APK i el proves.

Si en algun moment em dónes accés a un repositori (per exemple fent-lo públic o
convidant-me), llavors sí que podria fer jo els *commits* directament.

---

## Resum de comandes útils

```bash
git status                 # què ha canviat
git add -A                 # prepara tots els canvis
git commit -m "missatge"   # desa un punt de l'historial
git push                   # puja a GitHub
git pull                   # baixa canvis de GitHub
git log --oneline          # historial resumit
```
