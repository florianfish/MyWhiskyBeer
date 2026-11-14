# Design — « cave à la lueur »

L'ambiance d'un bar à whisky ou d'une cave : sombre, chaude, avec deux accents qui rappellent les liquides. L'appli se distingue des apps génériques en violet Material par une identité propre (couleurs dynamiques Material You désactivées).

## Couleurs

| Rôle | Sombre (défaut) | Clair |
|---|---|---|
| Fond | `#17120E` bois brûlé | `#FAF4EA` papier |
| Texte | `#F3E6D3` parchemin | `#241B14` encre |
| **Whisky** (primary) | `#E0A458` ambre | `#9A5418` |
| **Bière** (secondary) | `#E8C468` paille | `#7A5E00` |
| Accent ponctuel (tertiary) | `#C9704A` cuivre | `#9C4A26` |

Chaque élément lié à un produit prend la couleur de son type : liseré des cartes, badge, jauge, note.
Le mode sombre est le mode naturel de l'appli (on note souvent le soir) ; le mode clair suit le système.

## Typographie

- **Titres et notes** : serif, esprit étiquette de bouteille. Provisoirement la serif système ; à remplacer par une police embarquée (Fraunces ou Playfair Display).
- **Texte courant** : sans-serif système, pour la lisibilité.
- Libellés de type en petites capitales espacées (`WHISKY`, `BIÈRE`).

## Éléments signature

- **Jauge-verre** (`GlassGauge`) : la note remplit un verre au lieu d'afficher des étoiles. Tumbler pour le whisky, verre évasé avec col de mousse pour la bière. Animée au changement de valeur.
- **Cartes façon étiquette** : liseré vertical de la couleur du type, nom en serif, arômes en pastilles.
- **États vides** : deux verres vides côte à côte.

## Idées pour la suite

- **Roue d'arômes** interactive pour la saisie : familles en anneau intérieur, arômes à l'extérieur, un appui pour cocher, un appui long pour l'intensité.
- **Radar du profil aromatique** sur la fiche produit et dans les statistiques.
- **Curseur de note « verser »** : on fait glisser le doigt vers le haut pour remplir le verre, avec retour haptique tous les 0,5 point (bière) ou tous les 5 points (whisky).
- **Photo en en-tête** de la fiche produit, avec un dégradé vers le fond sombre ; couleur d'accent éventuellement extraite de la photo (Palette).
- **Icône** : provisoire (tumbler ambré), à retravailler — par exemple un verre mi-ambre mi-paille.
