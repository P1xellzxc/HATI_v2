---
title: "Wada themes"
tags: ["hati/design"]
---

13 color themes. Twelve come from Sanzo Wada's *A Dictionary of Color Combinations* (1918); roles (ink, paper, accent, tints) are derived under WCAG contrast rules. Source: `app/src/main/java/com/hativ2/ui/theme/HatiPalette.kt` (the web build reads the same file).

| Theme | Plate | Wada swatches | Accent | Ink |
|---|---|---|---|---|
| Classic | original | `#FEF08A` `#BFDBFE` `#BBF7D0` `#000000` | `#FEF08A` | `#000000` |
| Persimmon | No. 338 | `#A10B2B` `#FFAB00` `#00592E` `#BFABCC` | `#FFAB00` | `#003119` |
| Lantern | No. 335 | `#740909` `#FF8C00` `#2619D1` `#1B3644` | `#FF8C00` | `#172E3A` |
| Vermilion | No. 144 | `#B319AB` `#FF5200` `#000000` | `#FF5200` | `#000000` |
| Moss | No. 270 | `#E62E73` `#F5F5B8` `#58771E` `#328E13` | `#84BB71` | `#23300C` |
| Fern | No. 304 | `#681916` `#FFB852` `#7E8743` `#00D973` | `#00D973` | `#581513` |
| Spring | No. 347 | `#99B333` `#33FF7D` `#0057BA` `#B875EB` | `#33FF7D` | `#002C5D` |
| Lagoon | No. 172 | `#C2612C` `#008AA1` `#3400A3` | `#59B3C2` | `#2A0082` |
| Patina | No. 244 | `#B08699` `#C74300` `#5C8A73` `#008AA1` | `#66B9C7` | `#501B00` |
| Harbor | No. 194 | `#FA2B00` `#FFCFC4` `#4F8FE6` | `#7BABEC` | `#570F00` |
| Wisteria | No. 269 | `#B85E00` `#730F1F` `#9C52F2` `#000000` | `#AB6CF4` | `#000000` |
| Peony | No. 277 | `#FF4DC9` `#B319AB` `#718600` `#06004F` | `#FF4DC9` | `#06004F` |
| Coral | No. 341 | `#FF616B` `#328E13` `#172713` `#A6E6DB` | `#FF7981` | `#172713` |

Checked by `HatiPaletteContrastTest` (Android) and `web/tests/contrast.js` (web). Back to [[Design language]]
