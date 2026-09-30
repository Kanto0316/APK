# Menu d’export de l’écran Messages

## Ancien emplacement de l’export

Les actions **Exporter en PDF** et **Exporter en Excel** se trouvaient dans la section
**DONNÉES** du Navigation Drawer. Elles lançaient l’export global depuis le menu latéral.

## Nouvel emplacement de l’export

Un bouton Overflow vertical (`⋮`) est maintenant affiché à droite du champ de recherche de
l’écran **Messages**, sur la même ligne et avec le même alignement vertical. Il reste visible,
y compris lorsque la liste est vide.

Un appui ouvre un `PopupMenu` Android contenant :

- **Exporter en PDF** ;
- **Exporter en Excel**.

Les deux options sont désactivées lorsqu’un export est déjà en cours ou lorsque la liste
visible ne contient aucune transaction. Elles sont activées dès qu’au moins une transaction
est affichée. Les actions PDF et Excel ont été retirées du Navigation Drawer, qui conserve
les paramètres, la sécurité, la sauvegarde/restauration, l’état des permissions, l’aide et
l’à-propos.

## Logique utilisée

L’export prend un instantané défensif de la liste exacte fournie par l’adapter de Messages.
Cette liste a donc déjà reçu les filtres de période, de type et de recherche. Aucune boîte de
dialogue de filtrage intermédiaire n’est affichée.

Le flux partagé avec l’export du détail client est utilisé :

1. instantané de la liste affichée (`adapter.snapshot()`) ;
2. nouvelle copie défensive avant traitement ;
3. exclusion des lignes nulles, des SMS sans texte et des formats non reconnus ;
4. conversion en `ExportTransaction`, avec champs optionnels sécurisés et statut absent
   libellé **Non vérifiable** ;
5. génération par `PdfExporter` ou `XlsxExporter` ;
6. création du fichier dans le cache puis proposition d’enregistrement ou de partage.

Une liste vide n’est jamais transmise aux exporters. Les erreurs de préparation, PDF, XLSX
ou d’écriture restent interceptées sans fermer l’application et affichent :
**« Export impossible : une erreur est survenue »**.

## Fichiers modifiés

- `app/src/main/res/layout/activity_main.xml` : ligne recherche + bouton Overflow adaptative ;
- `app/src/main/res/menu/navigation_drawer_menu.xml` : suppression des deux exports de
  transactions du drawer ;
- `app/src/main/java/com/netk/mvolatrack/MainActivity.java` : `PopupMenu`, disponibilité des
  options et branchement au pipeline d’export partagé ;
- `EXPORT_MESSAGES_MENU.md` : présent rapport.

## Tests réalisés

- compilation Android debug (`assembleDebug`) et tests unitaires JVM
  (`testDebugUnitTest`) lancés ensemble (non exécutables dans l’environnement de validation,
  qui ne fournit pas de SDK Android) ;
- contrôle statique des ressources et des références aux anciennes entrées du drawer ;
- vérification du flux : liste filtrée affichée → instantané → copie défensive →
  `ExportTransaction` → `PdfExporter`/`XlsxExporter`.

## Résultat final

Le bouton `⋮` est visible à droite de la recherche sur l’écran Messages. Il propose directement
les exports PDF et Excel sur les transactions actuellement visibles, sans nouveau filtre. Les
options empêchent un export vide et les fonctions générales restent seules dans le drawer.
