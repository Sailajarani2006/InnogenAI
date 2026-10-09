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
import com.innogen.aipro.data.local.InnoGenDatabase
import com.innogen.aipro.data.local.dao.ProjectDao
import com.innogen.aipro.data.remote.api.GitHubOAuthService
import com.innogen.aipro.data.remote.api.GitHubService
import com.innogen.aipro.data.remote.api.OpenAIService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Groq AI
    @Provides @Singleton @Named("groq")
    fun provideGroqRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.groq.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideOpenAIService(@Named("groq") retrofit: Retrofit): OpenAIService =
        retrofit.create(OpenAIService::class.java)

    // GitHub API
    @Provides @Singleton @Named("github")
    fun provideGitHubRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideGitHubService(@Named("github") retrofit: Retrofit): GitHubService =
        retrofit.create(GitHubService::class.java)

    // GitHub OAuth
    @Provides @Singleton @Named("github_oauth")
    fun provideGitHubOAuthRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://github.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideGitHubOAuthService(@Named("github_oauth") retrofit: Retrofit): GitHubOAuthService =
        retrofit.create(GitHubOAuthService::class.java)

    // Firebase
    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    // Room
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): InnoGenDatabase =
        Room.databaseBuilder(ctx, InnoGenDatabase::class.java, "innogen_db")
            .fallbackToDestructiveMigration().build()

    @Provides @Singleton
    fun provideProjectDao(db: InnoGenDatabase): ProjectDao = db.projectDao()

    // DataStore
    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore

    // Keys
    @Provides @Named("openai_key")
    fun provideGroqKey(): String = BuildConfig.GEMINI_API_KEY

    @Provides @Named("github_client_id")
    fun provideGitHubClientId(): String = BuildConfig.GITHUB_CLIENT_ID

    @Provides @Named("github_client_secret")
    fun provideGitHubClientSecret(): String = BuildConfig.GITHUB_CLIENT_SECRET
}
