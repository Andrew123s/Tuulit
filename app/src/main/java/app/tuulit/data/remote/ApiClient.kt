package app.tuulit.data.remote

import app.tuulit.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Errors from the Tuulit API, each with copy that can be shown to the person as-is. */
sealed class ApiError(message: String) : Exception(message) {
    object NotConfigured : ApiError("Tuulit's server isn't set up in this build yet (TUULIT_API_BASE_URL is missing).")
    object NotSignedIn : ApiError("Sign in to use this.")
    object Offline : ApiError("You're offline. Your logs are safe on this phone.")
    object RateLimited : ApiError("That's a lot of questions in a short time. Try again in a little while.")
    class Server(message: String) : ApiError(message)
}

/** Supplies the Firebase ID token and App Check token for each request. */
interface TokenSource {
    suspend fun idToken(): String?
    suspend fun appCheckToken(): String?
}

/**
 * Thin client for the Tuulit Cloudflare Worker. The Worker holds every secret (AI provider,
 * Brevo, R2, Firebase service account); the app only ever sends a short-lived Firebase ID token.
 */
class ApiClient(
    private val tokens: TokenSource,
    private val baseUrl: String = BuildConfig.TUULIT_API_BASE_URL,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    val isConfigured: Boolean get() = baseUrl.startsWith("https://") || baseUrl.startsWith("http://10.0.2.2") || baseUrl.startsWith("http://127.0.0.1")

    suspend fun post(path: String, body: JSONObject, authenticated: Boolean = true): JSONObject = withContext(Dispatchers.IO) {
        if (!isConfigured) throw ApiError.NotConfigured
        val builder = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .post(body.toString().toRequestBody(JSON))
        if (authenticated) {
            val token = tokens.idToken() ?: throw ApiError.NotSignedIn
            builder.header("Authorization", "Bearer $token")
        }
        tokens.appCheckToken()?.let { builder.header("X-Firebase-AppCheck", it) }
        val response = try {
            http.newCall(builder.build()).execute()
        } catch (e: IOException) {
            throw ApiError.Offline
        }
        response.use { r ->
            val text = r.body?.string().orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            when {
                r.isSuccessful -> json
                r.code == 401 || r.code == 403 -> throw ApiError.NotSignedIn
                r.code == 429 -> throw ApiError.RateLimited
                else -> throw ApiError.Server(json.optString("error").ifEmpty { "Something went wrong on our side (${r.code}). Please try again." })
            }
        }
    }

    /** Uploads a file to a pre-signed R2 URL issued by the Worker. */
    suspend fun putSigned(url: String, file: File, contentType: String) = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).put(file.asRequestBody(contentType.toMediaType())).build()
        val r = try { http.newCall(req).execute() } catch (e: IOException) { throw ApiError.Offline }
        r.use { if (!it.isSuccessful) throw ApiError.Server("Upload failed (${it.code}).") }
    }

    suspend fun download(url: String, into: File) = withContext(Dispatchers.IO) {
        val r = try { http.newCall(Request.Builder().url(url).build()).execute() } catch (e: IOException) { throw ApiError.Offline }
        r.use { resp ->
            if (!resp.isSuccessful) throw ApiError.Server("Download failed (${resp.code}).")
            into.outputStream().use { out -> resp.body?.byteStream()?.copyTo(out) }
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
