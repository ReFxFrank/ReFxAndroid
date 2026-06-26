package gg.refx.android

import gg.refx.android.data.model.GameTemplateRef
import gg.refx.android.data.model.Server
import gg.refx.android.feature.servers.ServerSection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Section applicability gating (parity spec §8). */
class ServerSectionTest {

    private fun server(slug: String?, supportsWorkshop: Boolean? = null) = Server(
        id = "s", shortId = "s", name = "n",
        template = GameTemplateRef(slug = slug, supportsWorkshop = supportsWorkshop),
    )

    @Test fun minecraft_sections_applicable_for_minecraft() {
        val s = server("minecraft-java")
        val sections = ServerSection.applicableFor(s)
        assertTrue(sections.contains(ServerSection.MINECRAFT))
        assertTrue(sections.contains(ServerSection.MODS))
        assertTrue(sections.contains(ServerSection.MODPACKS))
        assertTrue(sections.contains(ServerSection.CONSOLE))
        assertFalse(sections.contains(ServerSection.VOICE))
    }

    @Test fun voice_server_hides_console_and_minecraft() {
        val s = server("teamspeak")
        val sections = ServerSection.applicableFor(s)
        assertTrue(sections.contains(ServerSection.VOICE))
        assertFalse(sections.contains(ServerSection.CONSOLE))
        assertFalse(sections.contains(ServerSection.MINECRAFT))
    }

    @Test fun workshop_gated_by_supports_workshop() {
        assertTrue(ServerSection.applicableFor(server("gmod", supportsWorkshop = true)).contains(ServerSection.WORKSHOP))
        assertFalse(ServerSection.applicableFor(server("gmod", supportsWorkshop = false)).contains(ServerSection.WORKSHOP))
    }

    @Test fun generic_server_shows_core_sections() {
        val sections = ServerSection.applicableFor(server("valheim"))
        assertTrue(sections.contains(ServerSection.CONSOLE))
        assertTrue(sections.contains(ServerSection.FILES))
        assertTrue(sections.contains(ServerSection.BACKUPS))
        assertFalse(sections.contains(ServerSection.MINECRAFT))
    }
}
