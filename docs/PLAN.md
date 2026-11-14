# Plan

## 1. Modèle de données

### Produit (`Product`)
Tronc commun :
- `id`, `type` (BEER | WHISKY | OTHER)
- `name`, `producer` (brasserie / distillerie / embouteilleur)
- `country`, `region`
- `abv` (degré, %)
- `barcode` (optionnel)
- `photoPath`
- `tags` (libres)
- `createdAt`

Champs bière (`BeerDetails`) :
- `style` (IPA, NEIPA, stout, lager, saison, sour…)
- `ibu` (amertume, optionnel)
- `color` (blonde, ambrée, brune, noire…)
- `format` (bouteille, canette, pression)

Champs whisky (`WhiskyDetails`) :
- `category` (single malt, blended malt, blend, bourbon, rye, single grain…)
- `age` (années, optionnel) / `vintage`
- `caskType` (ex-bourbon, sherry, porto, vin…)
- `peated` (non / léger / fort), `ppm` optionnel
- `caskStrength`, `chillFiltered`, `naturalColor`
- `bottler` (officiel ou indépendant)

### Dégustation (`Tasting`)
- `id`, `productId`, `date`, `place`
- `rating` : bière sur 5 (demi-points), whisky sur 100, stockés en interne sur 100
- `nose`, `palate`, `finish` : texte libre + arômes cochés
- `aromas` : liste de références vers le référentiel d'arômes
- `servedAs` (whisky : sec, goutte d'eau, glace ; bière : verre, température)
- `price` (optionnel), `comment`
- `photoPath` (optionnel)

### Cave (`CellarItem`)
- `productId`, `status` (EN_STOCK | ENTAMÉE | VIDE | WISHLIST)
- `fillLevel` (whisky : niveau restant)
- `purchaseDate`, `purchasePrice`, `shop`

### Référentiel d'arômes (`Aroma`)
Arborescence par famille, différente pour bière et whisky :
- Whisky : fruité, floral, céréales, boisé/vanillé, fumé/tourbé, épicé, sherry, iodé/marin, soufré…
- Bière : houblon (agrumes, résineux, tropical), malt (biscuit, caramel, torréfié), levure (banane, clou de girofle, funky), acidité…

## 2. Écrans

1. **Accueil** : dernières dégustations + bouton « Nouvelle dégustation » bien visible
2. **Nouvelle dégustation** : choisir ou créer le produit (recherche, scan, photo), puis noter
3. **Fiche produit** : infos, photo, historique des dégustations, note moyenne, statut en cave
4. **Liste / recherche** : filtres par type, style, région, note, pays, tag
5. **Cave** : en stock, entamées, wishlist
6. **Statistiques** (V2) : profils aromatiques, styles préférés, carte des pays
7. **Réglages** : échelles de notation, export/import

## 3. Roadmap

### V1 — MVP
- [x] CI GitHub Actions (build + tests, APK en artefact, release signée sur tag)
- [x] Projet Android (Compose, Room, Hilt)
- [x] Création de produits bière & whisky (modification et recherche texte à faire)
- [x] Dégustation : note (curseur « verser »), nez/bouche/finale, commentaire, date, lieu
- [x] Sélection d'arômes depuis le référentiel (roue avec intensités)
- [x] Photo depuis l'appareil photo ou la galerie
- [x] Liste filtrable par type (recherche texte à faire)
- [ ] Export / import JSON (sauvegarde)

### V2 — Confort
- [ ] Scan code-barres (ML Kit) + pré-remplissage depuis Open Food Facts (bières)
- [ ] OCR de l'étiquette pour pré-remplir nom / degré / âge
- [x] Gestion de la cave et de la wishlist
- [ ] Statistiques globales (le radar par produit est fait)
- [ ] Comparer deux dégustations côte à côte

### V3 — Bonus
- [ ] Synchro / sauvegarde auto-hébergée (sur mon infra)
- [ ] Widget écran d'accueil « dernière dégustation »
- [ ] Mode soirée dégustation (plusieurs produits notés à la suite, à l'aveugle)
- [ ] Suggestions « ça devrait te plaire » basées sur mes notes
- [ ] Autres spiritueux (rhum, gin, cognac…)

## 4. Points d'attention

- **Base de produits** : pas de base complète à intégrer. Saisie manuelle assistée, avec autocomplétion sur ce que j'ai déjà saisi (producteurs, styles, distilleries).
- **Rapidité de saisie** : valeurs par défaut intelligentes (date du jour, dernier lieu utilisé), champs avancés repliés.
- **Échelles de notes** : stocker sur 100 en interne et afficher selon le type, pour pouvoir changer d'échelle sans migration.
- **Photos** : compresser et redimensionner à l'enregistrement pour garder une base légère.
