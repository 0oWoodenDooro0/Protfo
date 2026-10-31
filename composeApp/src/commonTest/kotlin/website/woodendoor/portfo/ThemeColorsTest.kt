package website.woodendoor.portfo

import androidx.compose.ui.graphics.Color
import website.woodendoor.portfo.ui.theme.PortfoDarkColorScheme
import website.woodendoor.portfo.ui.theme.PortfoLightColorScheme
import website.woodendoor.portfo.ui.theme.parseHexColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ThemeColorsTest {

    @Test
    fun testLightColorSchemeDefaults() {
        assertEquals(Color(0xFF006874), PortfoLightColorScheme.primary)
        assertEquals(Color(0xFFFFFFFF), PortfoLightColorScheme.onPrimary)
        assertEquals(Color(0xFFF8FAFB), PortfoLightColorScheme.surface)
        assertEquals(Color(0xFF191C1D), PortfoLightColorScheme.onSurface)
    }

    @Test
    fun testDarkColorSchemeDefaults() {
        assertEquals(Color(0xFF4FD8EB), PortfoDarkColorScheme.primary)
        assertEquals(Color(0xFF00363D), PortfoDarkColorScheme.onPrimary)
        assertEquals(Color(0xFF0E1415), PortfoDarkColorScheme.surface)
        assertEquals(Color(0xFFE1E3E3), PortfoDarkColorScheme.onSurface)
    }

    @Test
    fun testLightAndDarkSchemesAreDistinct() {
        assertNotEquals(PortfoLightColorScheme.primary, PortfoDarkColorScheme.primary)
        assertNotEquals(PortfoLightColorScheme.surface, PortfoDarkColorScheme.surface)
        assertNotEquals(PortfoLightColorScheme.onSurface, PortfoDarkColorScheme.onSurface)
    }

    @Test
    fun testParseHexColor() {
        assertEquals(Color(0xFFFF0000), parseHexColor("#FF0000"))
        assertEquals(Color(0xFF00FF00), parseHexColor("00FF00"))
        assertEquals(Color(0x80123456), parseHexColor("#80123456"))
        assertEquals(Color.Gray, parseHexColor("invalid"))
    }
}
