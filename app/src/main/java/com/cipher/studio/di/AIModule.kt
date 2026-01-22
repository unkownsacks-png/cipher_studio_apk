package com.cipher.studio.di

import com.cipher.studio.data.service.GenerativeAIServiceImpl
import com.cipher.studio.domain.service.GenerativeAIService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AIModule {

    @Binds
    @Singleton
    abstract fun bindGenerativeAIService(
        impl: GenerativeAIServiceImpl
    ): GenerativeAIService
}