# Nawflix TV

Nawflix est une version modifiée de [Nuvio TV](https://github.com/NuvioMedia/NuvioTV), créée par NuvioMedia et ses contributeurs. Ce projet indépendant est maintenu par Nawfel. Il n'est ni officiel ni affilié à NuvioMedia.

Les modifications Nawflix apportées en 2026 comprennent notamment l'identité visuelle, l'accueil Android TV, les réglages d'apparence, les optimisations pour les téléviseurs et projecteurs ciblés, ainsi que le mécanisme de mise à jour propre à Nawflix. Les notices de droits d'auteur et les attributions du projet d'origine sont conservées dans le code et dans l'application.

## Télécharger et mettre à jour

L'APK de chaque version est publié dans les [Releases de Nawflix](https://github.com/Nawfelll/Nawflix-TV/releases). Après la première installation, l'application vérifie les nouvelles releases au lancement et propose de les installer. La recherche manuelle reste disponible dans **Réglages → À propos → Mettre à jour Nawflix**. Une confirmation d'installation peut être demandée par Android.

Chaque release doit contenir l'APK et le code source correspondant au même tag. Une mise à jour conserve l'identifiant de l'application et doit être signée avec le même certificat que la version déjà installée.

## Compiler

Le projet utilise Kotlin, Jetpack Compose, TV Material 3 et Android Gradle Plugin. Il faut un JDK 21 et le SDK Android. Copiez `local.example.properties` vers `local.properties`, puis renseignez l'URL et la clé publique de l'API d'authentification ainsi que le compte et le dépôt GitHub des releases. Ne publiez jamais de clé privée, de mot de passe ou de fichier de signature.

```text
NUVIO_SUPABASE_URL=https://votre-service.example
NUVIO_SUPABASE_ANON_KEY=cle-publique
NAWFLIX_UPDATE_GITHUB_OWNER=Nawfelll
NAWFLIX_UPDATE_GITHUB_REPO=Nawflix-TV
```

```powershell
./gradlew.bat :app:assembleFullRelease
```

Une signature de distribution est nécessaire pour remplacer une version déjà installée. La configuration locale de signature n'est pas incluse dans ce dépôt.

## Licence et crédits

Nawflix est distribué sous la [GNU GPL v3.0](LICENSE), comme Nuvio TV. Le code source modifié de chaque version distribuée est disponible sous le tag correspondant à son APK. Consultez également **À propos → Licences et attributions** pour les crédits des bibliothèques et services utilisés.

Nuvio TV : [NuvioMedia/NuvioTV](https://github.com/NuvioMedia/NuvioTV). Merci aux auteurs et contributeurs du projet d'origine.
