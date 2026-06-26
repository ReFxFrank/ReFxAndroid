package gg.refx.android

import gg.refx.android.core.ui.LoadState
import gg.refx.android.core.ui.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LoadState `.value` accessor and `map` semantics (§6.4). */
class LoadStateTest {

    @Test fun value_is_present_only_when_loaded() {
        assertNull(LoadState.Idle.value)
        assertNull(LoadState.Loading.value)
        assertNull(LoadState.Failed("x").value)
        assertEquals(42, LoadState.Loaded(42).value)
    }

    @Test fun map_preserves_surrounding_state() {
        assertEquals(LoadState.Idle, LoadState.Idle.map { it })
        assertTrue(LoadState.Loading.map { 1 } is LoadState.Loading)
        assertEquals("oops", (LoadState.Failed("oops").map { 1 } as LoadState.Failed).message)
        assertEquals(LoadState.Loaded("84"), LoadState.Loaded(42).map { (it * 2).toString() })
    }

    @Test fun is_loading_flag() {
        assertTrue(LoadState.Loading.isLoading)
        assertTrue(!LoadState.Loaded(1).isLoading)
    }
}
