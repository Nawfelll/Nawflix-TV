# Nawflix TV — guide de démarrage développeur

Fork de [NuvioTV](https://github.com/NuvioMedia/NuvioTV) (GPL-3.0) pour Android TV / Google TV. Kotlin, Jetpack Compose TV, Media3.
Cible de test principale : Xiaomi MiTV (Android 11, **1,4 Go de RAM**, 1280×720, ABI `armeabi-v7a`). Lire aussi `CINEMA-FORK.md`.

## Prérequis
- **JDK 21** (Temurin conseillé)
- **Android SDK 36** + build-tools 36.0.0 + platform-tools (`adb`)
- ~8 Go de disque pour Gradle et ses dépendances (première compilation : 15 à 20 min)

## Configuration
1. Copier `local.example.properties` en `local.properties` et ajouter :
   ```properties
   sdk.dir=C\:/chemin/vers/Android/Sdk
   NUVIO_SUPABASE_URL=...
   NUVIO_SUPABASE_ANON_KEY=...
   ```
   `NUVIO_SUPABASE_URL` et `NUVIO_SUPABASE_ANON_KEY` sont la configuration **publique** du client web Nuvio : elle est servie sur
   `https://nuvio.tv/config.js` (champs `supabaseUrl` et `supabaseAnonKey`). Sans elle, la connexion au compte Nuvio (donc la
   synchro des extensions/plugins, donc la lecture) ne fonctionne pas.
2. Ne jamais commiter `local.properties` ni un mot de passe de compte.

## Compiler
```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21'
.\gradlew.bat :app:assembleFullDebug        # build de dev
# Release (R8 + profil de démarrage), signé avec la clé debug locale :
$env:CI_USE_DEBUG_SIGNING = 'true'
.\gradlew.bat :app:assembleFullRelease
```
APK : `app/build/outputs/apk/full/{debug,release}/`.

## Installer et tester sur une TV
```powershell
adb connect <ip-de-la-tv>:5555      # activer le débogage ADB réseau dans les options développeur
adb install -r app\build\outputs\apk\full\release\app-full-release.apk
adb shell cmd package compile -m speed-profile -f fr.nawfel.cinema
```
Toujours tester les performances avec un build **release** : un APK debug est beaucoup plus lent.
Mesures utiles : `adb shell dumpsys gfxinfo fr.nawfel.cinema` (images saccadées, temps GPU) et
`adb shell setprop debug.hwui.overdraw show` puis relancer l'app (visualisation de l'overdraw).

## Où se trouve quoi
| Sujet | Fichier |
|---|---|
| Mode potato (appareils faibles, auto selon la RAM) | `app/src/main/java/com/nuvio/tv/core/perf/NawflixPerformance.kt` |
| Accueil façon plateforme de streaming | `ui/screens/home/CinemaHomeContent.kt` |
| Dégradés incorporés au fond (un seul calque GPU) | `ui/components/CinemaScrimTransformation.kt` |
| Barre du haut + logo | `CinemaScaffold.kt` |
| Sons d'interface | `core/sound/UiSounds.kt`, générés par `scripts/generate_ui_sounds.py` |
| Connexion Nuvio (email + mot de passe, QR en alternative) | `ui/screens/account/AuthQrSignInScreen.kt` |
| Mises à jour intégrées (désactivées volontairement) | `app/src/full/.../core/build/AppFeaturePolicy.kt` |

## Règles de conception retenues
- Contenu d'abord : pas de panneaux, les rangées flottent sur le fond.
- Le focus est le seul signal : cadre blanc net ; pas de zoom en mode potato.
- Marges constantes de 56 dp ; une seule couleur d'accent (rouge du logo `#D7263D`).
- Perf : peu de calques plein écran, images à 720p en RGB565 sur appareil faible, animations lues en phase de dessin.

## Pièges connus
- Les noms de ressources `res/raw` doivent être uniques toutes extensions confondues.
- `res/raw/isrgrootx1.pem` est requis par `network_security_config.xml`.
- Une liste verticale plein écran enveloppe les boutons placés dessous : la recherche de focus « bas » ne les trouve plus. Utiliser `focusProperties { down = ... }`.
