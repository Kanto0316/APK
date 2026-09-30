# Mise à jour de la colonne de référence des exports

## Ancien format

Les exports PDF et Excel présentaient dix colonnes :

`# | Date et heure | Type | Numéro client | Référence | Montant | Bonus | Frais | Solde | Statut de vérification`

La première colonne contenait le numéro de ligne et la référence de transaction était répétée dans
une colonne distincte.

## Nouveau format

Les exports PDF et Excel présentent désormais neuf colonnes, dans le même ordre :

`Réf | Date et heure | Type | Numéro client | Montant | Bonus | Frais | Solde | Statut`

La première colonne contient directement la référence de transaction. Une référence absente ou
vide est affichée sous la forme `-`. Le numéro de ligne automatique n'est plus conservé dans le
modèle d'export.

Les exports globaux et les exports de détail client utilisent tous deux la même conversion en
`ExportTransaction`, puis les mêmes `PdfExporter` et `XlsxExporter`. La modification s'applique
donc aux quatre variantes sans changer leurs filtres, leurs calculs de solde ou leurs statuts.

## Fichiers modifiés

- `app/src/main/java/com/netk/mvolatrack/export/ExportTransaction.java`
- `app/src/main/java/com/netk/mvolatrack/export/PdfExporter.java`
- `app/src/main/java/com/netk/mvolatrack/export/XlsxExporter.java`
- `app/src/main/java/com/netk/mvolatrack/MainActivity.java`
- `app/src/test/java/com/netk/mvolatrack/export/XlsxExporterTest.java`
- `app/src/androidTest/java/com/netk/mvolatrack/export/PdfExporterTest.java`
- `EXPORT_REFERENCE_COLUMN_UPDATE.md`

## Tests réalisés

- Export global PDF : génération partagée et en-têtes PDF vérifiés par le test instrumenté.
- Export global Excel : référence en première colonne, ordre des colonnes et plage du filtre vérifiés
  par les tests unitaires.
- Export détail client PDF : même modèle et même exporteur PDF que l'export global.
- Export détail client Excel : même modèle et même exporteur Excel que l'export global.
- Référence absente : affichage de `-` vérifié dans l'export Excel.
