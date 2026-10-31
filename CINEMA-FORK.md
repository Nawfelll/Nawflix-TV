# Cinéma — édition personnelle pour Google TV

Fork local de NuvioMedia/NuvioTV, licence GPL-3.0 conservée. Ce projet n'est pas une application officielle Stremio et n'est pas affilié à Apple.

## Modifications

- Identifiant Android distinct : `fr.nawfel.cinema`.
- Accueil natif Kotlin/Compose : grand décor, dégradés de contraste, rangées translucides, sélection blanche à la télécommande.
- Navigation horizontale : accueil, découverte, bibliothèque, recherche et réglages.
- Réutilisation des écrans de détails, extensions, sous-titres, lecteurs et suivi local de NuvioTV.
- Import ponctuel des extensions Stremio par son API officielle. Aucun mot de passe ni jeton de compte n'est sauvegardé par cet import.
- Mises à jour intégrées désactivées pour éviter le remplacement par NuvioTV officiel.

## Limites de cette première version

- L'import Stremio concerne les extensions, pas la bibliothèque ni l'historique. La progression créée dans Cinéma est locale.
- Les rangées translucides utilisent une surface teintée, sans flou temps réel coûteux.
- La lecture dépend des sources configurées, de leur disponibilité et des codecs du matériel.
- Les autres écrans conservent principalement l'apparence de NuvioTV.
- Accueil affiche sur le XGIMI connecte. La lecture video reste a valider separement.

## Compilation

JDK 21 et Android SDK 36. Créer `local.properties` avec `sdk.dir` pointant vers le SDK installé.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\gradlew.bat :app:assembleFullDebug
```

Le build de développement utilise la clé Android debug locale. Conserver cette clé pour installer les futures versions sans réinstallation. Pour une diffusion durable, créer une clé de signature dédiée.

Sources amont : https://github.com/NuvioMedia/NuvioTV

Client API Stremio consulté : https://github.com/Stremio/stremio-api-client

Commit amont : d063618d20978553c5310162793cd2f26b3cd1bb

## Performances et son (TV 1,4 Go de RAM)

- **Mode potato** (`core/perf/NawflixPerformance.kt`) : activé automatiquement si la RAM totale est <= ~2,3 Go ou si l'appareil est `low_ram`. Réglable dans Réglages > Avancé. Il raccourcit les fondus, rend les panneaux opaques, supprime le zoom au focus, plafonne les images à 720p en RGB565, réduit les caches Coil, coupe les bandes-annonces automatiques, le mode ambiant, JankStats et Sentry.
- **Accueil** : les dégradés de contraste sont incorporés au backdrop au décodage (`CinemaScrimTransformation`) : une seule image opaque au lieu de 4+ couches plein écran. Le changement de hero est différé tant que le focus bouge.
- **Sons** (`core/sound/UiSounds.kt`) : navigation, onglets, validation, retour et son d'intro. Générés par `scripts/generate_ui_sounds.py` (numpy, aucun échantillon externe). Coupés pendant la lecture, désactivables dans Réglages > Avancé.
- **Logo** : `nawflix_mark.png` / `nawflix_logo.png`, fond transparent, en haut à gauche.
- **Updater** : désactivé pour de bon (`AppFeaturePolicy.inAppUpdatesEnabled = false`), il proposait les versions officielles de NuvioTV.
- Test de perf : installer un build **release** puis `adb shell cmd package compile -m speed-profile -f fr.nawfel.cinema`. Un APK debug est bien plus lent.
- `res/raw/isrgrootx1.pem` : racine Let's Encrypt X1 exportée du magasin Windows (empreinte SHA-256 vérifiée). L'entrée X2 a été retirée de `network_security_config.xml`.
