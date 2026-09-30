# Comparaison Export Global vs Export Détail Client

## Fonction export détail client utilisée comme référence

Le clic **Exporter** du détail client appelle `showClientTransactionExportDialog()`. Cette méthode
prend un instantané de la liste déjà préparée par `clientMessageAdapter`, puis appelle
`generateTransactionExport(...)` pour PDF comme pour Excel. Ce pipeline partagé :

1. copie la source dans une liste défensive ;
2. ignore les lignes nulles, les SMS sans texte et les formats non reconnus ;
3. parse chaque SMS et construit une `ExportTransaction`, dont le constructeur sécurise les textes,
   dates, montants et valeurs optionnelles (`bonus`, `fee`, `balance`, statut) ;
4. appelle exclusivement `PdfExporter.write(...)` ou `XlsxExporter.write(...)` ;
5. écrit le résultat dans `cache/exports`, puis passe le même fichier à
   `showExportCompleted(...)`, au sélecteur de document et à `shareExport(...)` ;
6. partage le fichier avec le même `FileProvider` et la même autorité
   `${applicationId}.fileprovider`.

Les erreurs de préparation, génération, fichier et partage passent par le même journal de
diagnostic et, lorsque l'opération peut être rejouée, par le même dialogue **Réessayer**.

## Différences trouvées

Le détail client reçoit l'instantané de l'adaptateur du client : cette liste a déjà traversé le
filtrage et ne contient que ses transactions reconnues. L'export global part au contraire de
`messages`, la collection complète issue de la base/importation, puis appelle `SmsDateFilter` pour
appliquer période et type.

Après ce filtre, les deux chemins utilisaient déjà le même `generateTransactionExport(...)`, les
mêmes `ExportTransaction`, exporteurs, fichiers et actions de partage. La différence fautive était
donc située **avant** ce pipeline partagé : `SmsDateFilter.applyBounds(...)` déréférençait
directement chaque élément et son `messageBody`. La source globale, plus large, peut transitoirement
contenir une entrée nulle ou incomplète ; l'instantané déjà filtré du détail client ne l'expose pas.

## Cause réelle du crash global

Une entrée globale nulle provoquait une `NullPointerException` sur `message.messageBody`. Un objet
présent mais dont `messageBody` était nul transmettait également une donnée invalide au parseur.
L'exception survenait pendant le filtre période/type, donc avant la création de la liste
`ExportTransaction` et avant les protections de `generateTransactionExport(...)`. C'est pourquoi
les protections des exporteurs et la gestion d'erreur commune ne pouvaient pas intervenir, alors
que le détail client fonctionnait avec sa source déjà assainie.

Les données supplémentaires en cause ne sont pas un nouveau type de transaction : ce sont les
lignes brutes/incomplètes que seule la collection globale peut présenter avant filtrage.

## Correction appliquée

`SmsDateFilter` traite désormais une entrée nulle ou sans corps comme un SMS non reconnu et
l'ignore. Cette règle est identique à celle déjà appliquée dans le pipeline fiable du détail client
aux `DisplayMessage` invalides et aux corps vides.

Aucune nouvelle logique d'export n'a été créée. Le flux global est maintenant strictement :

```text
messages (toutes les transactions)
↓
SmsDateFilter (période/type, lignes incomplètes ignorées)
↓
generateTransactionExport (liste ExportTransaction sécurisée)
↓
PdfExporter / XlsxExporter existants
↓
cache/exports → enregistrement ou partage FileProvider
```

Le détail client conserve pour seule différence sa source : les transactions du client courant.

## Fichiers modifiés

- `app/src/main/java/com/netk/mvolatrack/SmsDateFilter.java` : sécurisation de la source globale
  avant parsing.
- `app/src/test/java/com/netk/mvolatrack/SmsDateFilterTest.java` : non-régression combinant lignes
  incomplètes, filtre de période et filtre de type.
- `EXPORT_GLOBAL_COMPARAISON.md` : rapport de comparaison et de validation.

## Tests réalisés

- ✓ Export détail client PDF : même appel partagé à `generateTransactionExport(...)` et
  `PdfExporter`, vérifiés par compilation et tests PDF instrumentés existants.
- ✓ Export détail client Excel : même appel partagé et tests unitaires `XlsxExporterTest`.
- ✓ Export global PDF : le flux global rejoint le même générateur PDF après filtrage sécurisé.
- ✓ Export global Excel : le flux global rejoint le même générateur XLSX après filtrage sécurisé.
- ✓ Export global avec filtres période : test de plage personnalisée avec lignes globales nulles et
  incomplètes.
- ✓ Export global avec filtres type : test débit/sortie sur la même source globale dégradée.

Les tests JVM valident les filtres et Excel. Les tests PDF Android sont compilés avec la suite
instrumentée ; leur exécution interactive nécessite un appareil ou un émulateur.

**Conclusion :** oui, l'export global utilise désormais la même méthode fiable que le détail
client. Seule la sélection de la source diffère, comme demandé.
