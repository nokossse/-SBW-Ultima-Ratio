# [SBW] Ultima Ratio — 0.8.0

Passe de finition complète du mod, sans nouveau véhicule. Base : 0.7.4 (tag `v0.7.4`, branche `main`).
Tout le travail est sur la branche `release/0.8`, tag `v0.8.0`.

Pour revenir en 0.7.4 : `git checkout main` (ou `git checkout v0.7.4`).
Pour adopter la 0.8.0 : `git checkout main` puis `git merge release/0.8`.

Bilan : 55 fichiers touchés, environ +880 / −1350 lignes (le code a maigri tout en gagnant des fonctions).

---

## 1. Bugs corrigés

### Gameplay
- **HADES pouvait devenir un SAM automatique.** Le HADES héritait du MAMBA : Shift + clic droit activait le mode auto, et le lanceur tirait ses missiles balistiques sur les avions, les monstres et les missiles ennemis. Le mode automatique est maintenant réservé au MAMBA (`MambaEntity.hasAutoMode()`, désactivé par `HadesEntity`). Shift + clic droit sur le HADES retrouve le comportement normal de SBW.
- **Pilotage depuis la GCS : le clic droit utilisait l'objet tenu.** En pilotant un drone depuis la station au sol, un clic droit mangeait la nourriture, bandait l'arc, etc. tenu en main. Le clic droit ne fait plus rien d'autre que basculer le moniteur, et seulement si c'est bien un moniteur VSR qui est tenu.
- **Moniteur VSR : délier pouvait voler le drone d'un coéquipier.** Accroupi + clic droit effaçait le contrôleur du drone même si un autre joueur l'avait repris depuis une GCS. Le contrôleur n'est maintenant effacé que s'il s'agit bien du joueur qui délie.
- **Moniteur VSR : crash possible sur NBT corrompu.** `UUID.fromString` était appelé sans protection dans `use()` et `inventoryTick()` : un moniteur au NBT abîmé (commande, éditeur, ancien format) faisait planter le serveur. Lecture sécurisée centralisée (`VsrMonitorItem.getLinkedUuid`), réutilisée par `VsrRemote`. Un lien illisible est simplement oublié.
- **Radars ARABEL / NG : le radar arrêtait de transmettre après un rechargement.** Le joueur qui avait déployé le radar n'était pas sauvegardé ; un camion radar déployé sans personne à bord ne transmettait plus rien après un redémarrage du serveur ou un rechargement de chunk. Il est maintenant sauvegardé (`RadarActivator`).
- **Radars : message trompeur sans énergie.** Tenter de déployer un radar sans énergie affichait « INACTIF ». Il affiche maintenant « Pas assez d'énergie pour déployer le radar » en rouge.
- **SAMP/T NG : conflit avec Simple Radar Systems.** La protection du Shift + clic droit (qui empêche SRS d'ouvrir son interface) ne couvrait que l'ARABEL. Elle couvre maintenant les deux camions radar (`RadarInteractEvents`).
- **Serval LAD aveugle aux drones du mod.** La tourelle anti-drones automatique ignorait les VSR700 et MQ-9 ennemis (ce ne sont pas des `DroneEntity` SBW). Elle les engage maintenant quand ils volent et n'appartiennent pas au camp du propriétaire (opérateur, équipe, dernier pilote).
- **5 véhicules absents de l'onglet créatif de SBW.** Rafale, Tigre, AS350, VSR700 et MQ-9 n'étaient pas déclarés comme conteneurs SBW. Les 20 véhicules y sont maintenant.
- **Recette du Moniteur VSR disparue.** Elle avait été supprimée par erreur dans le commit `976dae6` ; le moniteur n'était plus obtenable qu'en créatif alors que les descriptions demandent de l'utiliser. Restaurée (Moniteur SBW + Rare Material Pack, sans forme).
- **ASTER-30, MICA et METEOR hors du tag `superbwarfare:aa_missile`.** Les modificateurs de dégâts des véhicules contre les missiles sol-air/air-air (`@#superbwarfare:aa_missile`) ne s'appliquaient pas à nos missiles. Ajoutés au tag.
- **Icônes de la carte tactique.** Le HADES apparaissait comme une défense aérienne (`Type: AA`) et le CAESAR comme un char (`Type: Tank`). Les deux sont maintenant `Artillery`, comme le THUNDART.
- **GCS : extraction à la main.** La station au sol n'était pas dans le tag `mineable/pickaxe` (contrairement au GF300) : elle se cassait à la vitesse de la main. Corrigé.
- **GCS : seconde moitié posée aussi côté client.** `setPlacedBy` posait la seconde moitié sur les deux côtés ; seul le serveur le fait maintenant (comme le lit vanilla).

### Performances / stabilité
- **Tigre : une entité Kh-39 créée à chaque image.** Le rendu du missile sur le pylône construisait une nouvelle entité à chaque frame et pour chaque Tigre visible. L'entité factice est maintenant créée une fois par niveau et réutilisée.
- **Image radar des missiles (`RadarPicture`) : fuite mémoire.** Les pistes n'étaient purgées que si un MAMBA en mode auto lisait l'image ; sans MAMBA, elles s'accumulaient toute la session. Purge aussi à chaque rapport radar, et oubli des pistes laissées par un monde précédent (solo : quitter un monde puis en charger un autre).
- **MAMBA en mode auto : recherche allégée.** La recherche de cibles (une requête d'entités sur 2000 × 2000 blocs) tournait à chaque tick quand rien n'était engagé ; elle tourne maintenant un tick sur deux.
- **CAESAR : donnée synchronisée inutile supprimée.** `STAB_BLEND` était calculé et envoyé aux clients à chaque tick de stabilisation sans jamais être lu.
- **Laser DH (VSR700 / MQ-9) : logs de debug.** Le raycast Distant Horizons écrivait une ligne INFO toutes les 2 s tant que le laser était actif. Passé en DEBUG.
- **Réseau : sens des paquets imposé.** Chaque message déclare maintenant sa direction (client → serveur ou serveur → client) : un client ne peut plus envoyer un paquet prévu pour le client, et inversement. Les identifiants de messages sont inchangés (compatible avec un serveur 0.7.4 côté protocole).

---

## 2. Fonctions terminées

- **Liste de drones de la GCS défilante.** Au-delà de 10 drones, les suivants n'étaient jamais listés (raccourci assumé dans le code). La molette fait maintenant défiler la liste, avec l'indication « 11-20 / 27 » sous le titre.
- **Info-bulles d'utilisation.**
  - Moniteur VSR non lié : comment lier, prendre/lâcher les commandes, délier.
  - Station de contrôle au sol et Thales GF300 : à quoi ils servent et comment les utiliser (nouvelle classe `DescribedBlockItem`).
- **Sous-titres** pour les quatre sons du mod (tir du CAESAR, départ d'ASTER-30, mortier, canon du Rafale).
- **Onglet créatif** : l'icône est maintenant la caisse du CAESAR avec l'icône du véhicule (au lieu de la caisse SBW générique).
- **Métadonnées du mod** : description à jour (elle ne parlait encore que du CAESAR), ligne de crédits, description du resource pack.

---

## 3. Descriptions réécrites (EN + FR)

Toutes les descriptions de véhicules ont été relues contre les JSON et le code, puis réécrites dans un style commun (ce que c'est en vrai, puis comment ça se joue). Erreurs factuelles corrigées :

- ARABEL : portée annoncée 500 blocs, réelle 1000 (config `arabelRadarRange`). SAMP/T NG : annoncée 1000, réelle 2000 (`samptNgRadarRange`). Les deux précisent maintenant « par défaut (configurable) ».
- MQ-9 : « 4 Hellfire verrouillés par la caméra » → 2 Hellfire qui suivent l'axe de la caméra (sans verrouillage, depuis la 0.7.2). Ajout de la carte tactique et de la portée de liaison (5000 blocs).
- VSR700 : la description ne mentionnait ni les Mistral, ni le laser, ni le radar, ni la GCS, ni la portée de liaison (2500 blocs).
- Serval LAD : présenté comme une version « appui-feu » ; c'est la version de lutte anti-drones.
- VAB P : « jusqu'à huit fantassins » → quatre (6 places dans le JSON). Serval : composition de l'équipage précisée.
- CAESAR : suppression de la portée « plus de 10 000 blocs » (fausse, environ 2 500 avec la vitesse et la gravité du JSON) ; ajout de la stabilisation automatique, du rechargement réel (~7 s) et de l'indicateur d'artillerie.
- MAMBA : cycle de déploiement, mode automatique, propriétaire, image radar partagée.
- HADES : coquille (« nowdays ») et texte remplacé par une vraie description.
- VBMR : mention des fumigènes tirés par le tireur ; MEPAC : tir uniquement à l'arrêt, indicateur d'artillerie.

Français : vouvoiement partout (la GCS tutoyait), « Moniteur VSR » au lieu de « VSR Monitor », « Crédit » accentué, « un tourelleau », « véhicule blindé multi-rôles » pour le VBMR, « marchepied » pour les marches arrière du Serval.

---

## 4. Code nettoyé / réécrit

- **Camions radar : une seule classe.** `MambaRadarEntity` et `SampTNgEntity` étaient deux copies de 185 lignes. Elles héritent maintenant de `RadarVehicleEntity` (déploiement, énergie, animation, antenne) et ne font plus que 10 lignes chacune. Les sauvegardes existantes restent compatibles (`RadarDeployed`).
- **Renderers de camions : une seule base.** La direction (roues, essieu avant, volant) était copiée dans 6 renderers. Nouvelle base `TruckRenderer` (`driveTruck`, `hideBones`) utilisée par Kerax, MAMBA, HADES, ARABEL, SAMP/T NG et THUNDART. `SampTNgRenderer` et `MambaRadarRenderer` fusionnés en `RadarTruckRenderer` (seul le nom du bone d'antenne change).
- **Registres compactés.** `ModEntities` : deux helpers (`vehicle`, `projectile`) au lieu de 26 blocs identiques, et une liste unique `ModEntities.VEHICLES` utilisée par l'onglet créatif et par l'enregistrement des conteneurs SBW (c'est ce qui corrige les 5 véhicules manquants).
- **MAMBA / HADES** : la longueur du tube devient une méthode (`siloLength()`) au lieu d'un `beforeShoot` dupliqué dans le HADES.
- **Code mort retiré** : `MambaEntity.isReadyToFire`, NBT `Deployed` / `Undeployed` jamais relus, surcharge `MobileRadar.tick(VehicleEntity)` jamais appelée, `CaesarEntity.getStabBlend`.
- **Commentaires** : tous les commentaires restés en français dans le code (GF300, radar mobile, ASTER-30, menu GF300…) traduits en anglais comme le reste du code ; commentaires faux corrigés (portée auto du MAMBA, « place 3 (index 2) » du radar, « Kh-39 » du menu VSR700 qui contient des Mistral).
- **Mixins** : préfixe `ultimaratio$` partout (`ur$hideCrate` renommé) ; `SeekToolBuilderMixin` cible la classe directement (supprime l'avertissement du processeur Mixin).
- **Noms complets inutiles** (`com.nokosse.ultimaratio...` en plein code) remplacés par des imports.
- **Dépôt** : `tools/__pycache__/*.pyc` (binaire d'un script qui n'existe plus) retiré du suivi ; `.gitignore` complété (`__pycache__`, `_sbw_extract`, `_tmp_cls`, `run-*`, `/data`). Les dossiers non suivis n'ont pas été supprimés.

---

## 5. Version

- `mod_version` : 0.7.4 → **0.8.0** (`gradle.properties`).
- Jar : `build/libs/ultimaratio-0.8.0.jar`.

## 6. Vérifications faites

- `gradlew build` : OK.
- `gradlew runClient` (Kotlin for Forge + SBW chargés par le build) directement dans une copie du dernier monde de dev (`run/saves/UR 0.8 test`, les mondes d'origine ne sont pas touchés) : le jeu démarre, le joueur rejoint le monde, la sauvegarde automatique passe. Aucune erreur ni avertissement venant d'Ultima Ratio, aucun mixin en échec. Le monde contenait un MAMBA et un Rafale enregistrés avec une version précédente : ils sont relus et resauvegardés sans erreur (Forge signale seulement le passage 0.7.2-hotfix → 0.8.0).

## 7. Non vérifié en jeu

Ces points compilent et se chargent, mais n'ont pas été joués manuellement :
- vol du MQ-9 (déjà noté comme jamais testé en vol avant la 0.8) ;
- Serval LAD contre un VSR700 / MQ-9 ennemi (nécessite deux équipes) ;
- défilement de la GCS avec plus de 10 drones ;
- conflit SRS sur le SAMP/T NG (SRS absent de l'environnement de dev).
