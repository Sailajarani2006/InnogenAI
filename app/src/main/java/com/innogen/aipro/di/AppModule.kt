package com.innogen.aipro.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.innogen.aipro.BuildConfig
import com.innogen.aipro.data.local.DatabaseMigrations
import com.innogen.aipro.data.local.InnoGenDatabase
import com.innogen.aipro.data.local.dao.ProjectDao
import com.innogen.aipro.data.remote.api.GitHubService
import com.innogen.aipro.data.remote.api.OpenAIService
import com.innogen.aipro.utils.DatabaseKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "innogen_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── OkHttp ────────────────────────────────────────────────────────────────

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // FIX-05 (HIGH-001): Certificate pinning prevents MITM attacks even if a
        // rogue CA is installed on the device. Pins are SHA-256 hashes of the
        // public key (SPKI). Always include a backup pin from the CA chain.
        //
        // To get pin hashes (run on a machine with openssl):
        //   openssl s_client -connect api.groq.com:443 2>/dev/null \
        //     | openssl x509 -pubkey -noout \
        //     | openssl pkey -pubin -outform der \
        //     | openssl dgst -sha256 -binary | base64
        //
        // IMPORTANT: Update these pins whenever the certificate is rotated.
        // Set a monitoring alert 30 days before certificate expiry.
        val certificatePinner = CertificatePinner.Builder()
            // Groq AI API
            .add("api.groq.com", "sha256/REPLACE_WITH_GROQ_LEAF_PIN_HASH_BASE64=")
            .add("api.groq.com", "sha256/REPLACE_WITH_GROQ_BACKUP_PIN_HASH_BASE64=") // CA backup
            // GitHub API
            .add("api.github.com", "sha256/REPLACE_WITH_GITHUB_LEAF_PIN_HASH_BASE64=")
            .add("api.github.com", "sha256/REPLACE_WITH_GITHUB_BACKUP_PIN_HASH_BASE64=") // CA backup
            .build()

        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .addInterceptor(HttpLoggingInterceptor().apply {
                // FIX-01 (HIGH-002): Never log request/response bodies in release builds.
                // BODY logging exposes Authorization headers (API keys, GitHub PATs) to Logcat.
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // ── Groq AI (OpenAI Compatible) ──────────────────────────────────────────

    @Provides @Singleton @Named("groq")
    fun provideGroqRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.groq.com/openai/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideOpenAIService(@Named("groq") retrofit: Retrofit): OpenAIService =
        retrofit.create(OpenAIService::class.java)

    // ── Retrofit – GitHub ─────────────────────────────────────────────────────

    @Provides @Singleton @Named("github")
    fun provideGitHubRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideGitHubService(@Named("github") retrofit: Retrofit): GitHubService =
        retrofit.create(GitHubService::class.java)

    // ── Firebase ──────────────────────────────────────────────────────────────

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    // ── Room DB ───────────────────────────────────────────────────────────────

    @Provides @Singleton
    fun provideDatabase(
        @ApplicationContext ctx: Context,
        keyManager: DatabaseKeyManager   // FIX-09 (MED-005): injected key manager
    ): InnoGenDatabase {
        // Build SQLCipher passphrase from the securely stored key
        val passphrase = keyManager.getDatabaseKey()
        val factory    = SupportFactory(SQLiteDatabase.getBytes(passphrase))
        passphrase.fill('\u0000') // zero-out passphrase from memory immediately

        return Room.databaseBuilder(ctx, InnoGenDatabase::class.java, "innogen_db")
            .openHelperFactory(factory)                   // encrypt with SQLCipher
            .addMigrations(DatabaseMigrations.MIGRATION_1_2) // explicit — no data loss
            .build()
    }

    @Provides @Singleton
    fun provideProjectDao(db: InnoGenDatabase): ProjectDao = db.projectDao()

    // ── DataStore ─────────────────────────────────────────────────────────────

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        ctx.dataStore

    // ── API Key ───────────────────────────────────────────────────────────────

    @Provides @Named("openai_key")
    fun provideOpenAIKey(): String = BuildConfig.OPENAI_API_KEY
}
