# SMS Tracker

Application Android locale qui importe et conserve les SMS dans une base Room.

## Sauvegarde locale

L’écran principal contient une section de sauvegarde avec la date du dernier export et les actions **Sauvegarder** et **Restaurer**.

- Le premier export demande un mot de passe d’au moins huit caractères. Le fichier `Download/SmsTracker/sms-history.smsbackup` est chiffré avec AES-256-GCM ; la clé est dérivée du mot de passe avec PBKDF2-HMAC-SHA-256 et un sel aléatoire. Les expéditeurs et contenus ne sont jamais écrits en clair.
- Le mot de passe retenu pour les exports automatiques est lui-même protégé par Android Keystore. Une sauvegarde automatique est déclenchée après la réception ou l’import de SMS.
- Le fichier est placé dans le stockage partagé (`MediaStore.Downloads` sur Android 10 et versions ultérieures), et n’est donc pas supprimé avec l’application. Le mot de passe, lui, n’est pas exporté : Android supprimant la clé Keystore lors d’une désinstallation, il doit être saisi une fois dans le dialogue ouvert automatiquement après une réinstallation. Si la clé est encore disponible, la restauration s’effectue sans interaction.
- Au premier lancement, l’application distingue une installation neuve, une restauration en attente et une installation déjà configurée. Elle recherche le fichier avant l’import de la boîte Android et lance la restauration automatiquement. Une restauration manuelle reste disponible à tout moment. Les doublons sont ignorés par l’index unique Room.
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
