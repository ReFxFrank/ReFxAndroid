package gg.refx.android

import gg.refx.android.core.network.RefxJson
import gg.refx.android.data.model.AppNotification
import gg.refx.android.data.model.TicketDetail
import gg.refx.android.data.model.TicketPriority
import gg.refx.android.data.model.TicketState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Support + notification decoding (parity spec §3). */
class SupportAndNotificationDecodingTest {

    @Test fun ticket_detail_with_messages_and_staff_author() {
        val json = """{"id":"t","number":42,"subject":"Help","state":"PENDING_AGENT","priority":"HIGH",
            "messages":[
              {"id":"m1","body":"Hi","author":{"id":"u1","email":"me@x.com"}},
              {"id":"m2","body":"On it","author":{"id":"s1","firstName":"Sam","globalRole":"SUPPORT"}}
            ]}"""
        val detail = RefxJson.decodeFromString(TicketDetail.serializer(), json)
        assertEquals(TicketState.PENDING_AGENT, detail.state)
        assertEquals(TicketPriority.HIGH, detail.priority)
        assertEquals(2, detail.messages.size)
        assertFalse(detail.messages[0].author!!.isStaff)
        assertTrue(detail.messages[1].author!!.isStaff)
        assertEquals("Sam", detail.messages[1].author!!.displayName)
    }

    @Test fun notification_read_state() {
        val unread = RefxJson.decodeFromString(AppNotification.serializer(), """{"id":"n1","title":"Hello"}""")
        assertFalse(unread.isRead)
        val read = RefxJson.decodeFromString(
            AppNotification.serializer(),
            """{"id":"n2","title":"Hi","readAt":"2024-01-01T00:00:00Z"}""",
        )
        assertTrue(read.isRead)
    }
}
