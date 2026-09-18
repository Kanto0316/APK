# SMS Tracker

Application Android locale qui capture les nouveaux SMS reçus et les conserve dans une base Room privée et indépendante de la boîte SMS du téléphone.

## Sauvegarde locale

L’écran principal contient une section de sauvegarde avec la date du dernier export et les actions **Sauvegarder** et **Restaurer**.

- Le premier export demande un mot de passe d’au moins huit caractères. Le fichier `Download/SmsTracker/sms-history.smsbackup` est chiffré avec AES-256-GCM ; la clé est dérivée du mot de passe avec PBKDF2-HMAC-SHA-256 et un sel aléatoire. Les expéditeurs et contenus ne sont jamais écrits en clair.
- Le mot de passe retenu pour les exports automatiques est lui-même protégé par Android Keystore. Une sauvegarde automatique est déclenchée après la réception d’un SMS.
- Le fichier est placé dans le stockage partagé (`MediaStore.Downloads` sur Android 10 et versions ultérieures), et n’est donc pas supprimé avec l’application. Le mot de passe, lui, n’est pas exporté : Android supprimant la clé Keystore lors d’une désinstallation, il doit être saisi une fois dans le dialogue ouvert automatiquement après une réinstallation. Si la clé est encore disponible, la restauration s’effectue sans interaction.
- Au premier lancement, l’application distingue une installation neuve, une restauration en attente et une installation déjà configurée. Elle ne lit jamais la boîte SMS Android : sans sauvegarde restaurée, l’historique est donc vide. Une restauration manuelle reste disponible à tout moment. Les doublons sont ignorés par l’index unique Room.
- Le format contient ses propres versions de fichier et de schéma. Les fichiers tronqués, altérés, issus d’une version future ou ouverts avec un mauvais mot de passe sont refusés sans modifier les données existantes.

Sur Android 6 à 9, l’accès au dossier public de téléchargement nécessite l’autorisation de stockage demandée par l’application.

## Scénario de validation sur appareil

1. Installer l’APK et accorder les autorisations SMS (et stockage sur Android 6 à 9).
2. Recevoir un SMS et vérifier sa présence dans la liste.
3. Appuyer sur **Sauvegarder**, choisir un mot de passe et contrôler que la date affichée est actualisée.
4. Désinstaller l’application sans supprimer le fichier présent dans `Download/SmsTracker`.
5. Réinstaller et lancer l’application : saisir le même mot de passe dans le dialogue de restauration affiché automatiquement.
6. Vérifier que le SMS réapparaît et qu’une nouvelle réception actualise automatiquement la sauvegarde.

Le projet ne dépend d’aucun serveur ni de Firebase.

## Réception en arrière-plan

`SmsReceiver` est déclaré dans le manifeste : il reçoit `SMS_RECEIVED` sans que l’écran soit
ouvert, y compris après retrait de l’application des récents. Son travail asynchrone reste actif
avec `goAsync()` jusqu’à la fin de l’insertion Room. Après un redémarrage,
`BootReceiver` réactive explicitement ce composant ; aucun service n’est nécessaire. Android ne livre toutefois aucune diffusion
à une application arrêtée de force par l’utilisateur tant qu’elle n’a pas été relancée.

La capture est clairement signalée comme inactive si `RECEIVE_SMS` n’est pas accordée. Suivi SMS
ne lit jamais l’historique du fournisseur `Telephony.Sms` : seuls les nouveaux messages livrés au
receiver (ou ceux provenant d’une sauvegarde explicitement restaurée) figurent dans Room.

### Scénarios de validation de la capture

Pour chaque étape, envoyer un **nouveau** SMS, puis ouvrir Suivi SMS et vérifier qu’il apparaît une
seule fois dans la liste :

1. application ouverte au premier plan ;
2. application fermée avec le bouton Retour ;
3. application retirée des applications récentes ;
4. téléphone verrouillé lors de la réception, puis déverrouillé pour contrôler la liste ;
5. téléphone redémarré et session utilisateur déverrouillée.

La section temporaire de diagnostic affiche les jalons persistants « Dernier SMS capturé en
arrière-plan » et « Dernière insertion Room ». Elle permet de distinguer immédiatement un problème
de diffusion d'un échec de base de données. Logcat détaille l'action, le nombre de segments, les
champs extraits et le résultat de l'insertion ; filtrer avec `SmsReceiver`.

Refuser ensuite l’autorisation SMS : l’écran doit afficher « Capture SMS : INACTIVE », aucun SMS
reçu pendant ce refus ne doit être importé rétrospectivement. Réaccorder l’autorisation et vérifier
uniquement avec un nouveau SMS. Les traces `SmsReceiver` et `SmsBootReceiver` dans Logcat permettent
de confirmer respectivement la réception, l’insertion Room et la réactivation au démarrage.
