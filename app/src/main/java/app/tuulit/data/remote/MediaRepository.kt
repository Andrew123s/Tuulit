package app.tuulit.data.remote

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Voice-journal recordings. They are kept in app-private storage. When the person is signed in
 * and chooses "Keep a backup", they are uploaded to a private Cloudflare R2 bucket through a
 * short-lived pre-signed URL issued by the Worker. No R2 credential exists in the app.
 */
class MediaRepository(private val context: Context, private val api: ApiClient) {

    private val dir: File get() = File(context.filesDir, "voice").apply { mkdirs() }

    fun localFile(epochDay: Long): File = File(dir, "voice_$epochDay.m4a")
    fun hasLocal(epochDay: Long): Boolean = localFile(epochDay).let { it.exists() && it.length() > 0 }

    /** Uploads the day's recording and returns its R2 object key. */
    suspend fun backup(epochDay: Long): String {
        val file = localFile(epochDay)
        require(file.exists()) { "No recording for that day." }
        val res = api.post("/v1/media/upload-url", JSONObject().put("kind", "voice").put("contentType", "audio/mp4").put("bytes", file.length()))
        api.putSigned(res.getString("url"), file, "audio/mp4")
        return res.getString("key")
    }

    /** Downloads a backed-up recording to this device (e.g. after restoring on a new phone). */
    suspend fun restore(epochDay: Long, key: String): File {
        val res = api.post("/v1/media/download-url", JSONObject().put("key", key))
        val out = localFile(epochDay)
        api.download(res.getString("url"), out)
        return out
    }

    suspend fun deleteRemote(key: String) {
        api.post("/v1/media/delete", JSONObject().put("key", key))
    }

    fun deleteLocal(epochDay: Long) { localFile(epochDay).delete() }

    fun deleteAllLocal() { dir.listFiles()?.forEach { it.delete() } }
}
