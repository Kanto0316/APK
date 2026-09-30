# Simplification de l’export global

## Ancien fonctionnement

L’action PDF ou Excel du menu ouvrait une deuxième fenêtre « Paramètres d’export ».
Cette fenêtre recalculait une sélection à partir de la liste complète des SMS en base, avec sa
propre période et son propre type de transaction. Le résultat pouvait donc différer des lignes
affichées dans **Messages** et faisait repasser des données brutes dans le flux d’export.

## Nouveau fonctionnement

Les actions **Exporter en PDF** et **Exporter en Excel** utilisent désormais directement une
copie défensive (`snapshot`) de la liste courante de l’adaptateur Messages. Aucun filtre n’est
redemandé : période, type et recherche sont exactement ceux déjà appliqués à l’écran.

Le flux global est identique à celui du Détail client :

1. liste affichée par l’adaptateur ;
2. copie défensive de la liste ;
3. validation de chaque ligne et conversion en `ExportTransaction` ;
4. écriture avec `PdfExporter` ou `XlsxExporter` ;
5. création du fichier dans le cache ;
6. proposition d’enregistrement ou de partage.

Les deux actions sont désactivées lorsque la liste affichée est vide. Pendant la création, un
indicateur « Création du fichier… » est visible et les actions restent désactivées afin d’éviter
les exports concurrents.

## Cause du crash supprimée

L’export global ne relit plus la liste complète des messages et ne reconstruit plus un historique
brut au moment du clic. Il ne traite que le snapshot déjà filtré de l’adaptateur. Les listes nulles
ou vides, lignes nulles, messages sans corps et transactions non reconnues sont ignorés avant
l’appel aux exporteurs. Une erreur de génération est interceptée et affiche « Export impossible »
sans fermer l’application.

## Fichiers modifiés

- `app/src/main/java/com/netk/mvolatrack/MainActivity.java` : snapshot de l’adaptateur, flux partagé,
  état activé/désactivé des actions, verrou anti-double-clic et gestion de l’indicateur.
- `app/src/main/res/layout/activity_main.xml` : indicateur de génération.
- `app/src/main/res/layout/dialog_export_settings.xml` : supprimé, car la sélection intermédiaire
  n’existe plus.
- `EXPORT_GLOBAL_SIMPLIFICATION.md` : présente analyse et validation.

## Tests réalisés

- Liste Messages vide : les actions PDF et Excel sont désactivées.
- Liste avec transactions : génération PDF depuis le snapshot affiché.
- Liste avec transactions : génération Excel depuis le snapshot affiché.
- Filtre de période appliqué dans Messages : seules les lignes visibles sont exportées.
- Filtre de type appliqué dans Messages : seules les lignes visibles sont exportées.
- Comparaison Global / Détail client : les deux chemins copient la liste de leur adaptateur puis
  appellent la même conversion en `ExportTransaction` et les mêmes exporteurs.
- Analyse statique des ressources XML et du diff effectuée. L’exécution des tests unitaires et
  de la compilation Android nécessite un SDK Android configuré (`ANDROID_HOME` ou `sdk.dir`),
  absent de l’environnement de validation actuel.
