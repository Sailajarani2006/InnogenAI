package com.innogen.aipro.di

import com.innogen.aipro.data.remote.*
import com.innogen.aipro.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds @Singleton
    abstract fun bindAIRepository(impl: AIRepositoryImpl): AIRepository

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    // Note: GitHubRepositoryImpl is injected directly (not via interface)
    // because it has OAuth-specific methods not in the interface
}
