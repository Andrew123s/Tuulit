package app.tuulit.data

import app.tuulit.data.remote.ApiClient
import app.tuulit.data.remote.ApiError
import app.tuulit.data.remote.TokenSource
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs under Robolectric because org.json is an Android framework class. */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ApiClientTest {
    private val server = MockWebServer()
    private var token: String? = "id-token"
    private val tokens = object : TokenSource {
        override suspend fun idToken() = token
        override suspend fun appCheckToken() = "app-check"
    }

    @Before fun start() = server.start()
    @After fun stop() = server.shutdown()

    private fun client() = ApiClient(tokens, "http://127.0.0.1:${server.port}")

    @Test fun sendsBearerAndAppCheckHeaders() = runTest {
        server.enqueue(MockResponse().setBody("""{"text":"hi"}"""))
        val r = client().post("/v1/ai", JSONObject().put("tool", "chat"))
        assertEquals("hi", r.getString("text"))
        val req = server.takeRequest()
        assertEquals("Bearer id-token", req.getHeader("Authorization"))
        assertEquals("app-check", req.getHeader("X-Firebase-AppCheck"))
        assertTrue(req.body.readUtf8().contains("\"tool\":\"chat\""))
    }

    @Test fun mapsErrors() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"Provider unavailable"}"""))
        expect<ApiError.RateLimited> { client().post("/x", JSONObject()) }
        expect<ApiError.NotSignedIn> { client().post("/x", JSONObject()) }
        try { client().post("/x", JSONObject()); fail() } catch (e: ApiError.Server) { assertEquals("Provider unavailable", e.message) }
    }

    @Test fun refusesWithoutTokenOrUrl() = runTest {
        token = null
        expect<ApiError.NotSignedIn> { client().post("/x", JSONObject()) }
        expect<ApiError.NotConfigured> { ApiClient(tokens, "").post("/x", JSONObject()) }
        assertEquals(0, server.requestCount)
    }

    private suspend inline fun <reified E : ApiError> expect(block: () -> Unit) {
        try { block(); fail("expected ${E::class.simpleName}") } catch (e: ApiError) { assertTrue("got $e", e is E) }
    }
}
