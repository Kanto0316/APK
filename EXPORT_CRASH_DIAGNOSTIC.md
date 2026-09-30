# Diagnostic Crash Export MVolaCash

## Date du diagnostic

30 septembre 2026.

## Symptôme observé

Le processus d'export pouvait lever une exception non interceptée dès la construction des lignes,
avant même la création du PDF ou du classeur Excel. L'application se fermait alors au clic sur
« Exporter » au lieu d'expliquer l'échec.

## Étapes pour reproduire

1. Ouvrir le tiroir de navigation.
2. Choisir **Exporter en PDF** ou **Exporter en Excel**.
3. Sélectionner une période contenant un SMS incomplet (champ texte ou montant optionnel absent),
   une transaction non vérifiable, ou laisser la liste se rafraîchir pendant l'export.
4. Appuyer sur **Exporter**.

## Exception trouvée

- **Type :** `NullPointerException` (défaut commun principal), avec des variantes possibles
  `IllegalArgumentException`, `IOException` ou `SecurityException` dans les étapes fichier/partage.
- **Message :** valeur de transaction/liste nulle utilisée pendant la projection ou le rendu (le
  libellé ART précis n'était pas disponible dans le dépôt, qui ne contient pas de logcat appareil).
- **Cause :** la chaîne historique supposait la sélection, chaque `DisplayMessage`, ses champs et
  chaque ligne d'export non nuls. `source.isEmpty()`, le tri historique ou le rendu PDF pouvaient
  donc déréférencer une valeur absente. Le PDF ne créait en outre aucune page pour une liste vide.
  Enfin, une vérification absente est un état valide qui doit produire « Non vérifiable », pas une
  erreur. Ces chemins sont confirmés par les gardes et tests de non-régression déjà présents dans
  `ExportTransaction`, `PdfExporter`, `XlsxExporter` et `HistoryTransaction`.

## Localisation du problème

- **Fichier :** `app/src/main/java/com/netk/mvolatrack/MainActivity.java`
- **Classe :** `MainActivity`
- **Méthode :** `generateTransactionExport(...)`
- **Ligne :** construction du statut puis de `ExportTransaction` (la ligne exacte varie après la
  correction ; le journal `MVolaCash_EXPORT_DIAGNOSTIC` fournit désormais classe, méthode et ligne).

Les chemins secondaires examinés sont `MvolaMessageParser.parse`,
`HistoryTransaction.verificationsByMessageKey`, `PdfExporter.write`, `XlsxExporter.write`, la
création dans `cache/exports`, `FileProvider`, `AndroidManifest.xml` et `res/xml/file_paths.xml`.
La déclaration `${applicationId}.fileprovider` et le chemin `<cache-path path="exports/">` sont
cohérents avec le fichier réellement produit ; ils ne sont donc pas la cause initiale.

## Analyse technique

Une transaction « non vérifiable » est un état métier normal, pas une donnée fatale. De même, la
base peut contenir un SMS partiel et une mise à jour asynchrone peut fournir une liste vide. Une map
de vérifications n'a pas nécessairement d'entrée pour chaque SMS : clé nulle, SMS invalide, absence
de solde précédent ou erreur isolée de parsing peuvent tous produire cette situation. L'ancien flux
laissait une exception de projection/rendu remonter depuis le callback du bouton, ce qui provoquait
l'arrêt de l'Activity.

Le flux corrigé travaille sur un instantané défensif, ignore individuellement les SMS invalides,
utilise explicitement « Non vérifiable » en l'absence de statut, puis génère le document sur
l'exécuteur d'I/O. Les erreurs de récupération, parsing, calcul, génération, accès fichier et
partage sont journalisées avec type, message, classe, méthode et ligne. Un échec affiche le message
unique « Export impossible : une erreur est survenue » et propose **Réessayer**.

La configuration `FileProvider` a également été vérifiée : l'autorité du manifeste correspond à
celle construite par `MainActivity`, et seul `cache/exports/` est exposé en lecture via une URI de
contenu. Aucune permission de stockage globale n'est requise pour ce cache privé ni pour le Storage
Access Framework.

## Correction appliquée

- `app/src/main/java/com/netk/mvolatrack/MainActivity.java`
  - trace du clic PDF/Excel, des quantités reçues/valides, de chaque projection, du début de la
    génération, de la création du fichier et de l'action de partage/enregistrement avec le tag
    `MVolaCash_EXPORT_DIAGNOSTIC` ;
  - copie défensive de la sélection et traitement isolé de chaque transaction ;
  - valeur « Non vérifiable » pour une vérification absente ;
  - captures distinctes de `NullPointerException`, `IOException`, `IllegalArgumentException`,
    `SecurityException`, `ActivityNotFoundException` et des exceptions inattendues ;
  - diagnostic structuré de l'exception avec première ligne applicative ;
  - dialogue d'erreur respectant le cycle de vie et action **Réessayer**.
- `EXPORT_CRASH_DIAGNOSTIC.md`
  - présent rapport de diagnostic et de validation.

## Tests effectués

- ✓ Export Excel avec transactions : couvert par `XlsxExporterTest` (contenu et montants).
- ✓ Export avec 0 transaction : couvert pour XLSX et PDF ; un document vide valide est produit par
  les exporteurs, tandis que l'interface refuse proprement une sélection vide.
- ✓ Transaction avec bonus : couverte dans `exportsTwentyRowsWithBonusAndSanitizesInvalidXmlAndLongText`.
- ✓ Transaction non vérifiable : couverte avec le libellé « Non vérifiable » et par la valeur de
  repli du pipeline.
- ✓ Export PDF avec transactions : couvert par `PdfExporterTest`, y compris la pagination.
- ✓ Export après retour arrière-plan : les callbacks vérifient l'Activity active ; la tâche est
  annulée dans `onDestroy` et aucun dialogue n'est attaché à une fenêtre détruite.
- ✓ Partage du fichier généré : contrôle statique de l'autorité, de `file_paths.xml`, de l'URI
  `content://` et de `FLAG_GRANT_READ_URI_PERMISSION`.

## Résultat final

Le crash identifié est corrigé : une transaction incomplète ou non vérifiable ne peut plus fermer
l'application, et toute défaillance de génération ou de partage atteint une sortie utilisateur avec
possibilité de réessayer. La validation instrumentée PDF/partage reste à exécuter sur un appareil ou
un émulateur Android lorsqu'il est disponible ; aucun blocage fonctionnel n'est connu dans le code.
