package com.hativ2.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
// HATI² Color Themes — Sanzo Wada, A Dictionary of Color Combinations (1918)
// ─────────────────────────────────────────────────────────────
//
// Each theme starts from one of Wada's numbered 3- or 4-color plates
// (`swatches`, verbatim HEX). The UI roles are derived from it by
// tools that keep WCAG contrast:
//   ink      darkest swatch, deepened toward black (≥13:1 on paper)
//   paper    lightest swatch, 92% toward white
//   accent   the saturated swatch needing the least lightening to
//            carry ink text (≥6:1); CTA fills, focal pills
//   hero / categoryFood / tertiary   light tints of the plate
//   muted / divider / disabled       ink blended into paper
//   dark*    accent and tints re-balanced for the #121212 surface
//
// Status colors (green = owed to you, pink/red = you owe) and the
// named volume pastels are deliberately NOT themed: they carry
// meaning or are stored as user data.
// ─────────────────────────────────────────────────────────────

@Immutable
data class HatiPalette(
    val id: String,
    val name: String,
    val wadaNumber: Int?,
    val swatches: List<Color>,
    val ink: Color,
    val paper: Color,
    val accent: Color,
    val tertiary: Color,
    val surfaceVariant: Color,
    val hero: Color,
    val categoryFood: Color,
    val muted: Color,
    val divider: Color,
    val disabled: Color,
    val darkAccent: Color,
    val darkTertiary: Color,
    val darkHero: Color,
    val darkCategoryFood: Color,
)

object HatiPalettes {
    val Classic = HatiPalette(
        id = "classic", name = "Classic", wadaNumber = null,
        swatches = listOf(Color(0xFFFEF08A), Color(0xFFBFDBFE), Color(0xFFBBF7D0), Color(0xFF000000)),
        ink = Color(0xFF000000), paper = Color(0xFFFFFFFF), accent = Color(0xFFFEF08A),
        tertiary = Color(0xFFBFDBFE), surfaceVariant = Color(0xFFE5E7EB),
        hero = Color(0xFFFFF5D6), categoryFood = Color(0xFFFFC9A8),
        muted = Color(0xFF6B7280), divider = Color(0xFFD1D5DB), disabled = Color(0xFF9CA3AF),
        darkAccent = Color(0xFFFFF3B0), darkTertiary = Color(0xFFCCE5FF),
        darkHero = Color(0xFF3A3422), darkCategoryFood = Color(0xFFB37A5A),
    )

    val Persimmon = HatiPalette(
        id = "persimmon", name = "Persimmon", wadaNumber = 338,
        swatches = listOf(Color(0xFFA10B2B), Color(0xFFFFAB00), Color(0xFF00592E), Color(0xFFBFABCC)),
        ink = Color(0xFF003119), paper = Color(0xFFFFF8EB), accent = Color(0xFFFFAB00),
        tertiary = Color(0xFFD99CA9), surfaceVariant = Color(0xFFFFEDC7),
        hero = Color(0xFFFFE7B8), categoryFood = Color(0xFFD99DAA),
        muted = Color(0xFF597763), divider = Color(0xFFCCD0C1), disabled = Color(0xFF99A997),
        darkAccent = Color(0xFFFFB31A), darkTertiary = Color(0xFFE4BAC3),
        darkHero = Color(0xFF46340E), darkCategoryFood = Color(0xFF6F0D22),
    )

    val Lantern = HatiPalette(
        id = "lantern", name = "Lantern", wadaNumber = 335,
        swatches = listOf(Color(0xFF740909), Color(0xFFFF8C00), Color(0xFF2619D1), Color(0xFF1B3644)),
        ink = Color(0xFF172E3A), paper = Color(0xFFFFF6EB), accent = Color(0xFFFF8C00),
        tertiary = Color(0xFFA7A2EC), surfaceVariant = Color(0xFFFFE6C7),
        hero = Color(0xFFFFDFB8), categoryFood = Color(0xFFA8A3ED),
        muted = Color(0xFF616E72), divider = Color(0xFFD1CEC7), disabled = Color(0xFFA2A6A4),
        darkAccent = Color(0xFFFF981A), darkTertiary = Color(0xFFC1BEF2),
        darkHero = Color(0xFF462D0E), darkCategoryFood = Color(0xFF1F178E),
    )

    val Vermilion = HatiPalette(
        id = "vermilion", name = "Vermilion", wadaNumber = 144,
        swatches = listOf(Color(0xFFB319AB), Color(0xFFFF5200), Color(0xFF000000)),
        ink = Color(0xFF000000), paper = Color(0xFFFFF1EB), accent = Color(0xFFFF5200),
        tertiary = Color(0xFFDD98D9), surfaceVariant = Color(0xFFFFD9C7),
        hero = Color(0xFFFFCFB8), categoryFood = Color(0xFFD580D1),
        muted = Color(0xFF706A67), divider = Color(0xFFCCC1BC), disabled = Color(0xFF99918D),
        darkAccent = Color(0xFFFF7D40), darkTertiary = Color(0xFFE7B7E5),
        darkHero = Color(0xFF46200E), darkCategoryFood = Color(0xFF7B1775),
    )

    val Moss = HatiPalette(
        id = "moss", name = "Moss", wadaNumber = 270,
        swatches = listOf(Color(0xFFE62E73), Color(0xFFF5F5B8), Color(0xFF58771E), Color(0xFF328E13)),
        ink = Color(0xFF23300C), paper = Color(0xFFFEFEF9), accent = Color(0xFF84BB71),
        tertiary = Color(0xFFF4A1C0), surfaceVariant = Color(0xFFFDFDEF),
        hero = Color(0xFFDDECD7), categoryFood = Color(0xFFF18CB2),
        muted = Color(0xFF697258), divider = Color(0xFFD2D5CA), disabled = Color(0xFFA7AC9A),
        darkAccent = Color(0xFF90C280), darkTertiary = Color(0xFFF7BDD3),
        darkHero = Color(0xFF2B3727), darkCategoryFood = Color(0xFF9C2451),
    )

    val Fern = HatiPalette(
        id = "fern", name = "Fern", wadaNumber = 304,
        swatches = listOf(Color(0xFF681916), Color(0xFFFFB852), Color(0xFF7E8743), Color(0xFF00D973)),
        ink = Color(0xFF581513), paper = Color(0xFFFFF9F1), accent = Color(0xFF00D973),
        tertiary = Color(0xFFFFDFB1), surfaceVariant = Color(0xFFFFEFD9),
        hero = Color(0xFFB8F4D8), categoryFood = Color(0xFFFFD8A0),
        muted = Color(0xFF936561), divider = Color(0xFFDECCC5), disabled = Color(0xFFBC9E98),
        darkAccent = Color(0xFF1ADD81), darkTertiary = Color(0xFFFFE9C9),
        darkHero = Color(0xFF0E3E27), darkCategoryFood = Color(0xFFAC7E3C),
    )

    val Spring = HatiPalette(
        id = "spring", name = "Spring", wadaNumber = 347,
        swatches = listOf(Color(0xFF99B333), Color(0xFF33FF7D), Color(0xFF0057BA), Color(0xFFB875EB)),
        ink = Color(0xFF002C5D), paper = Color(0xFFEFFFF5), accent = Color(0xFF33FF7D),
        tertiary = Color(0xFF8CB3E0), surfaceVariant = Color(0xFFD2FFE2),
        hero = Color(0xFFC6FFDB), categoryFood = Color(0xFF8CB3E0),
        muted = Color(0xFF547692), divider = Color(0xFFBFD5D6), disabled = Color(0xFF8FAAB8),
        darkAccent = Color(0xFF47FF8A), darkTertiary = Color(0xFFAFCAE9),
        darkHero = Color(0xFF19462A), darkCategoryFood = Color(0xFF063F7F),
    )

    val Lagoon = HatiPalette(
        id = "lagoon", name = "Lagoon", wadaNumber = 172,
        swatches = listOf(Color(0xFFC2612C), Color(0xFF008AA1), Color(0xFF3400A3)),
        ink = Color(0xFF2A0082), paper = Color(0xFFEBF6F7), accent = Color(0xFF59B3C2),
        tertiary = Color(0xFFB5A2DD), surfaceVariant = Color(0xFFC7E5EA),
        hero = Color(0xFFD1EAEE), categoryFood = Color(0xFFB8A6DF),
        muted = Color(0xFF735DAF), divider = Color(0xFFC4C5E0), disabled = Color(0xFF9D93C9),
        darkAccent = Color(0xFF6ABBC8), darkTertiary = Color(0xFFCBBEE8),
        darkHero = Color(0xFF223539), darkCategoryFood = Color(0xFF280670),
    )

    val Patina = HatiPalette(
        id = "patina", name = "Patina", wadaNumber = 244,
        swatches = listOf(Color(0xFFB08699), Color(0xFFC74300), Color(0xFF5C8A73), Color(0xFF008AA1)),
        ink = Color(0xFF501B00), paper = Color(0xFFF9F5F7), accent = Color(0xFF66B9C7),
        tertiary = Color(0xFFE6AA8C), surfaceVariant = Color(0xFFEEE4E9),
        hero = Color(0xFFD4EBEF), categoryFood = Color(0xFFE3A180),
        muted = Color(0xFF8B6756), divider = Color(0xFFD7CAC5), disabled = Color(0xFFB59E94),
        darkAccent = Color(0xFF75C0CC), darkTertiary = Color(0xFFEDC4AF),
        darkHero = Color(0xFF24373A), darkCategoryFood = Color(0xFF883206),
    )

    val Harbor = HatiPalette(
        id = "harbor", name = "Harbor", wadaNumber = 194,
        swatches = listOf(Color(0xFFFA2B00), Color(0xFFFFCFC4), Color(0xFF4F8FE6)),
        ink = Color(0xFF570F00), paper = Color(0xFFFFFBFA), accent = Color(0xFF7BABEC),
        tertiary = Color(0xFFFDA08C), surfaceVariant = Color(0xFFFFF4F2),
        hero = Color(0xFFDAE7FA), categoryFood = Color(0xFFFC8A73),
        muted = Color(0xFF926258), divider = Color(0xFFDECCC8), disabled = Color(0xFFBC9D96),
        darkAccent = Color(0xFF88B3EE), darkTertiary = Color(0xFFFDBCAF),
        darkHero = Color(0xFF293442), darkCategoryFood = Color(0xFFA92206),
    )

    val Wisteria = HatiPalette(
        id = "wisteria", name = "Wisteria", wadaNumber = 269,
        swatches = listOf(Color(0xFFB85E00), Color(0xFF730F1F), Color(0xFF9C52F2), Color(0xFF000000)),
        ink = Color(0xFF000000), paper = Color(0xFFF7F1FE), accent = Color(0xFFAB6CF4),
        tertiary = Color(0xFFDFB78C), surfaceVariant = Color(0xFFE9D9FC),
        hero = Color(0xFFE7D6FC), categoryFood = Color(0xFFD8A673),
        muted = Color(0xFF6D6A70), divider = Color(0xFFC6C1CB), disabled = Color(0xFF949198),
        darkAccent = Color(0xFFBC89F6), darkTertiary = Color(0xFFE9CCAF),
        darkHero = Color(0xFF342644), darkCategoryFood = Color(0xFF7E4306),
    )

    val Peony = HatiPalette(
        id = "peony", name = "Peony", wadaNumber = 277,
        swatches = listOf(Color(0xFFFF4DC9), Color(0xFFB319AB), Color(0xFF718600), Color(0xFF06004F)),
        ink = Color(0xFF06004F), paper = Color(0xFFFFF1FB), accent = Color(0xFFFF4DC9),
        tertiary = Color(0xFFBFC98C), surfaceVariant = Color(0xFFFFD8F3),
        hero = Color(0xFFFFCDF0), categoryFood = Color(0xFFB1BC73),
        muted = Color(0xFF6C6395), divider = Color(0xFFCDC1D8), disabled = Color(0xFF9B90B6),
        darkAccent = Color(0xFFFF68D1), darkTertiary = Color(0xFFD2D9AF),
        darkHero = Color(0xFF461F3A), darkCategoryFood = Color(0xFF505D06),
    )

    val Coral = HatiPalette(
        id = "coral", name = "Coral", wadaNumber = 341,
        swatches = listOf(Color(0xFFFF616B), Color(0xFF328E13), Color(0xFF172713), Color(0xFFA6E6DB)),
        ink = Color(0xFF172713), paper = Color(0xFFF8FDFC), accent = Color(0xFFFF7981),
        tertiary = Color(0xFFA3CC95), surfaceVariant = Color(0xFFEBFAF7),
        hero = Color(0xFFFFD9DC), categoryFood = Color(0xFF8EC17D),
        muted = Color(0xFF667265), divider = Color(0xFFCBD2CD), disabled = Color(0xFF9EA79F),
        darkAccent = Color(0xFFFF868E), darkTertiary = Color(0xFFBEDBB5),
        darkHero = Color(0xFF46292A), darkCategoryFood = Color(0xFF276313),
    )

    val all: List<HatiPalette> = listOf(
        Classic, Persimmon, Lantern, Vermilion, Moss, Fern, Spring,
        Lagoon, Patina, Harbor, Wisteria, Peony, Coral,
    )

    fun byId(id: String?): HatiPalette = all.firstOrNull { it.id == id } ?: Classic
}

// Snapshot state so the legacy color constants in Color.kt can resolve the
// active theme from any context (composition, draw lambdas, plain functions)
// and still trigger recomposition/redraw when the user switches themes.
// Single global: the app hosts one HatiV2Theme at a time.
internal object ActivePalette {
    var current: HatiPalette by mutableStateOf(HatiPalettes.Classic)
}
