package app.tuulit.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Database(
    entities = [
        CycleEntity::class, DailyLogEntity::class, PainLogEntity::class, ComfortItemEntity::class,
        CareCompletionEntity::class, AchievementEntity::class, GameScoreEntity::class, AiMessageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TuulitDatabase : RoomDatabase() {
    abstract fun dao(): TuulitDao

    companion object {
        private const val NAME = "tuulit.db"

        /** Opens the SQLCipher-encrypted database. The passphrase is wrapped by an Android Keystore key. */
        fun open(context: Context): TuulitDatabase {
            System.loadLibrary("sqlcipher")
            // The key is 256 random bits, so it is passed to SQLCipher as a raw key ("x'hex'"),
            // which skips the passphrase key-derivation that would otherwise slow every open.
            val raw = DatabaseKey.passphrase(context)
            val rawKey = ("x'" + raw.joinToString("") { "%02x".format(it) } + "'").toByteArray()
            return Room.databaseBuilder(context.applicationContext, TuulitDatabase::class.java, NAME)
                .openHelperFactory(SupportOpenHelperFactory(rawKey))
                .build()
        }

        /** Unencrypted in-memory database for tests. */
        fun inMemory(context: Context): TuulitDatabase =
            Room.inMemoryDatabaseBuilder(context, TuulitDatabase::class.java).allowMainThreadQueries().build()

        fun deleteFiles(context: Context) {
            context.deleteDatabase(NAME)
        }
    }
}

/**
 * Generates a random 32-byte database passphrase once, encrypts it with an AES-GCM key that never
 * leaves the Android Keystore, and stores only the ciphertext in no-backup preferences.
 */
internal object DatabaseKey {
    private const val ALIAS = "tuulit_db_key"
    private const val PREFS = "tuulit_db_key_store"
    private const val KEY_BLOB = "wrapped"

    fun passphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_BLOB, null)
        if (stored != null) {
            runCatching { return unwrap(Base64.decode(stored, Base64.NO_WRAP)) }
            // The keystore key was lost (e.g. device restore). The old database cannot be read.
            context.deleteDatabase("tuulit.db")
        }
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_BLOB, Base64.encodeToString(wrap(raw), Base64.NO_WRAP)).apply()
        return raw
    }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun wrap(raw: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, keystoreKey())
        return c.iv + c.doFinal(raw)
    }

    private fun unwrap(blob: ByteArray): ByteArray {
        val iv = blob.copyOfRange(0, 12)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
        return c.doFinal(blob.copyOfRange(12, blob.size))
    }
}
