# Rapport de création — SMS Tracker V1

## Résumé

L'application de démonstration est devenue un outil local de suivi des SMS financiers. Elle reconnaît les messages de solde, bonus, recharge, paiement et transaction, puis affiche les données enregistrées sans les transmettre à un service distant.

## Fichiers créés ou modifiés

- `AndroidManifest.xml` : permissions et déclaration du récepteur SMS.
- `MainActivity.java`, `activity_main.xml`, `item_transaction.xml` : tableau de bord et liste.
- `sms/SmsReceiver.java`, `sms/SmsParser.java` : réception et analyse.
- `database/Transaction.java`, `TransactionDao.java`, `AppDatabase.java` : stockage Room.
- `repository/TransactionRepository.java` : accès asynchrone aux données.
- `notification/NotificationHelper.java` : canal silencieux et notification locale.
- `SmsParserTest.java`, `TransactionDaoTest.java` : tests d'analyse et d'insertion.

## Permissions

- `RECEIVE_SMS` est demandée à l'exécution et permet uniquement la réception.
- `POST_NOTIFICATIONS` est demandée sur Android 13 et versions ultérieures.
- Le receiver exige `BROADCAST_SMS` de son émetteur afin que seul le système puisse l'activer.

Aucune permission Internet, contacts, localisation ou envoi de SMS n'est déclarée.

## Architecture et fonctionnement

À la réception de `android.provider.Telephony.SMS_RECEIVED`, les parties du SMS sont assemblées. `SmsParser` recherche un type supporté ainsi qu'un montant et sa devise (`Ar` ou `MGA`). La date du SMS et son expéditeur sont conservés. Une transaction valide est insérée sur un exécuteur dédié dans la base Room `sms-tracker.db`, puis une notification silencieuse est affichée.

L'activité observe Room avec `LiveData`. Elle présente le dernier solde, le dernier bonus et les 100 transactions les plus récentes (date, type, montant et expéditeur). Toutes les données restent dans le stockage privé de l'application.

## Tests

Les tests unitaires couvrent le solde, le bonus, la recharge, le paiement, la transaction, les montants avec ou sans espaces, les décimales, les devises Ar/MGA, ainsi que la conservation de la date et de l'expéditeur. Un test instrumenté utilise une base Room en mémoire pour vérifier l'insertion et la relecture par le DAO.

## Limites connues

- Seuls les messages contenant explicitement un mot-clé supporté, un montant et `Ar`/`MGA` sont enregistrés ; les formulations propres à un opérateur pourront nécessiter de nouvelles règles.
- La notification dépend de l'autorisation utilisateur sur Android 13+, mais l'enregistrement continue si elle est refusée.
- La désactivation de l'autorisation SMS empêche la réception automatique.
- Cette V1 ne propose ni export, ni sauvegarde cloud, ni synchronisation.
