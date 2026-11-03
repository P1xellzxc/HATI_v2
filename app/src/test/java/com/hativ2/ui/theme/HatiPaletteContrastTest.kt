package com.hativ2.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class HatiPaletteContrastTest {

    private fun channel(c: Float): Double =
        if (c <= 0.04045f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun assertContrast(palette: HatiPalette, role: String, fg: Color, bg: Color, min: Double) {
        val ratio = contrast(fg, bg)
        assertTrue("${palette.id}: $role contrast $ratio < $min", ratio >= min)
    }

    @Test
    fun `every palette keeps text legible in light mode`() {
        HatiPalettes.all.forEach { p ->
            assertContrast(p, "ink on paper", p.ink, p.paper, 7.0)
            assertContrast(p, "ink on accent button", p.ink, p.accent, 4.5)
            assertContrast(p, "ink on hero pill", p.ink, p.hero, 4.5)
            assertContrast(p, "ink on selected chip", p.ink, p.categoryFood, 4.5)
            assertContrast(p, "ink on tertiary", p.ink, p.tertiary, 4.5)
            assertContrast(p, "muted text on paper", p.muted, p.paper, 4.5)
        }
    }

    @Test
    fun `every palette keeps text legible in dark mode`() {
        HatiPalettes.all.forEach { p ->
            assertContrast(p, "dark accent on surface", p.darkAccent, DarkSurface, 7.0)
            assertContrast(p, "on-surface text on dark hero", DarkOnSurface, p.darkHero, 4.5)
        }
    }

    @Test
    fun `palette ids are unique and byId falls back to Classic`() {
        val ids = HatiPalettes.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(HatiPalettes.Classic, HatiPalettes.byId("no-such-theme"))
        assertEquals(HatiPalettes.Classic, HatiPalettes.byId(null))
    }

    @Test
    fun `wada palettes carry their source plate`() {
        HatiPalettes.all.filter { it != HatiPalettes.Classic }.forEach { p ->
            val n = p.wadaNumber
            assertTrue("${p.id} has no plate number", n != null && n in 1..348)
            assertTrue("${p.id} should be a 3- or 4-color plate", p.swatches.size in 3..4)
        }
    }
}
