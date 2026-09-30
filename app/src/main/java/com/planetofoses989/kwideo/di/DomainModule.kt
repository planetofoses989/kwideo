package com.planetofoses989.kwideo.di

import com.planetofoses989.kwideo.domain.anime4k.Anime4KManager
import com.planetofoses989.kwideo.domain.streaming.SeriesDetector
import com.planetofoses989.kwideo.domain.streaming.StreamingMetadataRepository
import com.planetofoses989.kwideo.repository.explain.ExplainRepository
import com.planetofoses989.kwideo.repository.intro.IntroSkipRepository
import com.planetofoses989.kwideo.repository.wyzie.WyzieSearchRepository
import okhttp3.OkHttpClient
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import java.util.concurrent.TimeUnit

val domainModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    single { Anime4KManager(androidContext()) }
    single { WyzieSearchRepository(androidContext(), get(), get(), get()) }
    single { IntroSkipRepository(androidContext(), get(), get(), get(), get()) }
    single { ExplainRepository(get(), get()) }
    single { SeriesDetector(androidContext(), get()) }
    single { StreamingMetadataRepository(androidContext(), get(), get(), get()) }
}
