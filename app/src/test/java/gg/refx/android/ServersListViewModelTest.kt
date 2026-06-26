package gg.refx.android

import gg.refx.android.core.network.Page
import gg.refx.android.core.network.PageMeta
import gg.refx.android.core.ui.LoadState
import gg.refx.android.data.api.ServersApi
import gg.refx.android.data.model.CommandRequest
import gg.refx.android.data.model.LiveStats
import gg.refx.android.data.model.PowerRequest
import gg.refx.android.data.model.Server
import gg.refx.android.data.model.ServerState
import gg.refx.android.data.repo.ServersRepository
import gg.refx.android.feature.servers.ServersListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServersListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun server(id: String, state: ServerState = ServerState.RUNNING) =
        Server(id = id, shortId = id, name = "srv-$id", state = state)

    private class FakeApi(val pages: Map<Int, Page<Server>>) : ServersApi {
        override suspend fun list(page: Int, pageSize: Int, q: String?): Page<Server> =
            pages[page] ?: Page()
        override suspend fun get(id: String): Server = error("unused")
        override suspend fun power(id: String, body: PowerRequest) = error("unused")
        override suspend fun command(id: String, body: CommandRequest) = error("unused")
        override suspend fun stats(id: String): LiveStats = error("unused")
    }

    private fun vmWith(pages: Map<Int, Page<Server>>): ServersListViewModel {
        val api = FakeApi(pages)
        return ServersListViewModel(ServersRepository(apiProvider = { api }))
    }

    @Test fun loads_first_page_and_reports_hasMore() = runTest(dispatcher.scheduler) {
        val vm = vmWith(
            mapOf(1 to Page(data = listOf(server("a"), server("b")), meta = PageMeta(page = 1, totalPages = 2))),
        )
        advanceUntilIdle()
        val s = vm.state.value
        assertTrue(s.state is LoadState.Loaded)
        assertEquals(2, s.state.value?.size)
        assertTrue(s.hasMore)
    }

    @Test fun next_page_appends_and_dedupes_overlapping_ids() = runTest(dispatcher.scheduler) {
        val vm = vmWith(
            mapOf(
                1 to Page(data = listOf(server("a"), server("b")), meta = PageMeta(page = 1, totalPages = 2)),
                // page 2 re-includes "b" (server set reordered between fetches).
                2 to Page(data = listOf(server("b"), server("c")), meta = PageMeta(page = 2, totalPages = 2)),
            ),
        )
        advanceUntilIdle()
        vm.loadNextPage()
        advanceUntilIdle()

        val items = vm.state.value.state.value!!
        assertEquals(listOf("a", "b", "c"), items.map { it.id }) // "b" not duplicated
        assertFalse(vm.state.value.hasMore)
    }

    @Test fun attention_count_flags_problem_states() = runTest(dispatcher.scheduler) {
        val vm = vmWith(
            mapOf(
                1 to Page(
                    data = listOf(
                        server("a", ServerState.RUNNING),
                        server("b", ServerState.CRASHED),
                        server("c", ServerState.PENDING_PAYMENT),
                        server("d", ServerState.SUSPENDED),
                    ),
                    meta = PageMeta(page = 1, totalPages = 1),
                ),
            ),
        )
        advanceUntilIdle()
        assertEquals(3, vm.state.value.attentionCount) // crashed + pending_payment + suspended
    }
}
