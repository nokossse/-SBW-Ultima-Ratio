# [SBW] Ultima Ratio — 0.8.0

Passe de finition complète du mod, sans nouveau véhicule. Base : 0.7.4 (tag `v0.7.4`, branche `main`).
Tout le travail est sur la branche `release/0.8`, tag `v0.8.0`.

Pour revenir en 0.7.4 : `git checkout main` (ou `git checkout v0.7.4`).
Pour adopter la 0.8.0 : `git checkout main` puis `git merge release/0.8`.

Bilan : 60 fichiers touchés, environ +945 / −1730 lignes depuis la 0.7.4.

---

## 1. Retraits demandés

- **Moniteur VSR supprimé.** C'était un objet créatif de debug. L'item, sa classe, son modèle, ses textes et toute la logique de liaison sont retirés. Les drones (VSR700, MQ-9) se pilotent uniquement depuis la station de contrôle au sol (GCS).
  - Supprimé : `VsrMonitorItem`, le modèle `vsr_monitor`, la propriété d'item « linked », l'entrée de l'onglet créatif, les messages « drone lié / délié / déjà lié / moniteur occupé / drone d'une autre équipe ».
  - `Vsr700Entity` : plus de liaison par clic droit avec le moniteur (le clic droit sur un drone redevient celui de SBW), plus de suivi des moniteurs liés. La coupure de liaison (crash, hors de portée, drone retiré) prévient toujours le contrôleur.
  - `VsrRemote` : la GCS est la seule source de pilotage. La touche « Descendre » de SBW relâche toujours le drone.
  - `VsrMonitorInteractEvents` devient `RemotePilotInteractEvents` : tant qu'un joueur pilote depuis la GCS, son corps n'utilise aucun objet, n'interagit avec aucun bloc ni aucune entité, ne mine pas et n'attaque pas. Ce blocage est fait côté serveur aussi, plus seulement côté client.
  - Un moniteur VSR présent dans un ancien monde disparaîtra au chargement (objet qui n'existe plus).
- **Simple Radar Systems retiré.** Le mod est abandonné : la dépendance optionnelle `srs` est retirée de `mods.toml`, ainsi que l'événement qui interceptait Shift + clic droit sur les camions radar pour passer avant l'interface de SRS. Le Shift + clic droit des radars ARABEL / NG passe maintenant directement par l'entité.
- **Serval LAD : uniquement les petits drones.** La tourelle automatique est faite contre les drones FPV et les essaims, pas contre les UAV : le ciblage des VSR700 / MQ-9 ajouté pendant cette passe est retiré, le comportement est celui de la 0.7.4.

---

## 2. Bugs corrigés

### Gameplay
- **HADES pouvait devenir un SAM automatique.** Le HADES héritait du MAMBA : Shift + clic droit activait le mode auto, et le lanceur tirait ses missiles balistiques sur les avions, les monstres et les missiles ennemis. Le mode automatique est maintenant réservé au MAMBA (`MambaEntity.hasAutoMode()`, désactivé par `HadesEntity`). Shift + clic droit sur le HADES retrouve le comportement normal de SBW.
- **MAMBA et Serval LAD : seul le propriétaire pouvait basculer le mode automatique.** Comme la CIWS de SBW, le premier joueur à faire Shift + clic droit en devenait le propriétaire, et ses coéquipiers ne pouvaient plus l'activer ni le couper (le clic ne faisait rien). Maintenant, le propriétaire et toute son équipe (scoreboard) peuvent le basculer, même quand le propriétaire est déconnecté ; un match à mort par équipe de SBW (tout le monde ennemi) reste respecté. Les autres joueurs reçoivent « Seule l'équipe du propriétaire peut le basculer ». Le propriétaire reste la référence ami/ennemi du mode auto (nouvelle classe `OwnerTeam`).
- **Pilotage depuis la GCS : le clic droit utilisait l'objet tenu.** En pilotant un drone depuis la station au sol, un clic droit mangeait la nourriture, bandait l'arc, etc. tenu en main. Les clics ne font plus rien au corps du pilote, côté client comme côté serveur.
- **Radars ARABEL / NG : le radar arrêtait de transmettre après un rechargement.** Le joueur qui avait déployé le radar n'était pas sauvegardé ; un camion radar déployé sans personne à bord ne transmettait plus rien après un redémarrage du serveur ou un rechargement de chunk. Il est maintenant sauvegardé (`RadarActivator`).
- **Radars : message trompeur sans énergie.** Tenter de déployer un radar sans énergie affichait « INACTIF ». Il affiche maintenant « Pas assez d'énergie pour déployer le radar » en rouge.
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
- **Réseau : sens des paquets imposé.** Chaque message déclare maintenant sa direction (client → serveur ou serveur → client) : un client ne peut plus envoyer un paquet prévu pour le client, et inversement. Les identifiants de messages sont inchangés.

---

## 3. Fonctions terminées

- **Liste de drones de la GCS défilante.** Au-delà de 10 drones, les suivants n'étaient jamais listés (raccourci assumé dans le code). La molette fait maintenant défiler la liste, avec l'indication « 11-20 / 27 » sous le titre.
- **Info-bulles d'utilisation** pour la station de contrôle au sol et le Thales GF300 : à quoi ils servent et comment les utiliser (nouvelle classe `DescribedBlockItem`).
- **Sous-titres** pour les quatre sons du mod (tir du CAESAR, départ d'ASTER-30, mortier, canon du Rafale).
- **Onglet créatif** : l'icône est maintenant la caisse du CAESAR avec l'icône du véhicule (au lieu de la caisse SBW générique).
- **Métadonnées du mod** : description à jour (elle ne parlait encore que du CAESAR), ligne de crédits, description du resource pack.

---

## 4. Descriptions réécrites (EN + FR)

Toutes les descriptions de véhicules ont été relues contre les JSON et le code, puis réécrites dans un style commun (ce que c'est en vrai, puis comment ça se joue). Erreurs factuelles corrigées :

- ARABEL : portée annoncée 500 blocs, réelle 1000 (config `arabelRadarRange`). SAMP/T NG : annoncée 1000, réelle 2000 (`samptNgRadarRange`). Les deux précisent maintenant « par défaut (configurable) ».
- MQ-9 : « 4 Hellfire verrouillés par la caméra » → 2 Hellfire qui suivent l'axe de la caméra (sans verrouillage, depuis la 0.7.2). Pilotage depuis la GCS, carte tactique et portée de liaison (5000 blocs) ajoutés.
- VSR700 : la description ne mentionnait ni les Mistral, ni le laser, ni le radar, ni la GCS, ni la portée de liaison (2500 blocs).
- Serval LAD : présenté comme une version « appui-feu » ; c'est la version de lutte anti-drones, contre les petits drones FPV et les essaims.
- VAB P : « jusqu'à huit fantassins » → quatre (6 places dans le JSON). Serval : composition de l'équipage précisée.
- CAESAR : suppression de la portée « plus de 10 000 blocs » (fausse, environ 2 500 avec la vitesse et la gravité du JSON) ; ajout de la stabilisation automatique, du rechargement réel (~7 s) et de l'indicateur d'artillerie.
- MAMBA : cycle de déploiement, mode automatique, propriétaire, image radar partagée.
- HADES : coquille (« nowdays ») et texte remplacé par une vraie description.
- VBMR : mention des fumigènes tirés par le tireur ; MEPAC : tir uniquement à l'arrêt, indicateur d'artillerie.

Français : vouvoiement partout (la GCS tutoyait), « Crédit » accentué, « un tourelleau », « véhicule blindé multi-rôles » pour le VBMR, « marchepied » pour les marches arrière du Serval.

---

## 5. Code nettoyé / réécrit

- **Camions radar : une seule classe.** `MambaRadarEntity` et `SampTNgEntity` étaient deux copies de 185 lignes. Elles héritent maintenant de `RadarVehicleEntity` (déploiement, énergie, animation, antenne) et ne font plus que 10 lignes chacune. Les sauvegardes existantes restent compatibles (`RadarDeployed`).
- **Renderers de camions : une seule base.** La direction (roues, essieu avant, volant) était copiée dans 6 renderers. Nouvelle base `TruckRenderer` (`driveTruck`, `hideBones`) utilisée par Kerax, MAMBA, HADES, ARABEL, SAMP/T NG et THUNDART. `SampTNgRenderer` et `MambaRadarRenderer` fusionnés en `RadarTruckRenderer` (seul le nom du bone d'antenne change).
- **Registres compactés.** `ModEntities` : deux helpers (`vehicle`, `projectile`) au lieu de 26 blocs identiques, et une liste unique `ModEntities.VEHICLES` utilisée à la fois par l'onglet créatif d'UR et par l'enregistrement des conteneurs SBW (les 20 véhicules y sont déclarés au même endroit).
- **MAMBA / HADES** : la longueur du tube devient une méthode (`siloLength()`) au lieu d'un `beforeShoot` dupliqué dans le HADES.
- **Code mort retiré** : `MambaEntity.isReadyToFire`, NBT `Deployed` / `Undeployed` jamais relus, surcharge `MobileRadar.tick(VehicleEntity)` jamais appelée, `CaesarEntity.getStabBlend`.
- **Commentaires** : tous les commentaires restés en français dans le code (GF300, radar mobile, ASTER-30, menu GF300…) traduits en anglais comme le reste du code ; commentaires faux corrigés (portée auto du MAMBA, « place 3 (index 2) » du radar, « Kh-39 » du menu VSR700 qui contient des Mistral).
- **Mixins** : préfixe `ultimaratio$` partout (`ur$hideCrate` renommé) ; `SeekToolBuilderMixin` cible la classe directement (supprime l'avertissement du processeur Mixin).
- **Noms complets inutiles** (`com.nokosse.ultimaratio...` en plein code) remplacés par des imports.
- **Dépôt** : `tools/__pycache__/*.pyc` (binaire d'un script qui n'existe plus) retiré du suivi ; `.gitignore` complété (`__pycache__`, `_sbw_extract`, `_tmp_cls`, `run-*`, `/data`). Les dossiers non suivis n'ont pas été supprimés.

---

## 6. Version

- `mod_version` : 0.7.4 → **0.8.0** (`gradle.properties`).
- Jar : `build/libs/ultimaratio-0.8.0.jar`.

## 7. Vérifications faites

- `gradlew build` : OK.
- `gradlew runClient` (Kotlin for Forge + SBW chargés par le build) directement dans une copie du dernier monde de dev (`run/saves/UR 0.8 test`, les mondes d'origine ne sont pas touchés) : le jeu démarre, le joueur rejoint le monde, la sauvegarde automatique passe. Aucune erreur ni avertissement venant d'Ultima Ratio, aucun mixin en échec. Le monde contenait un MAMBA et un Rafale enregistrés avec une version précédente : ils sont relus et resauvegardés sans erreur. Vérifié une seconde fois après le retrait du Moniteur VSR et de SRS.

## 8. Non vérifié en jeu

Ces points compilent et se chargent, mais n'ont pas été joués manuellement :
- vol du MQ-9 (déjà noté comme jamais testé en vol avant la 0.8) ;
- pilotage d'un drone depuis la GCS après le retrait du moniteur ;
- défilement de la GCS avec plus de 10 drones.
