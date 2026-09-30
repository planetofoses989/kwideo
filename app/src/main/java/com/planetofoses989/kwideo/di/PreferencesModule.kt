package com.planetofoses989.kwideo.di

import com.planetofoses989.kwideo.database.MpviumDatabase
import com.planetofoses989.kwideo.preferences.AdvancedPreferences
import com.planetofoses989.kwideo.preferences.AppearancePreferences
import com.planetofoses989.kwideo.preferences.AudioPreferences
import com.planetofoses989.kwideo.preferences.BrowserPreferences
import com.planetofoses989.kwideo.preferences.DecoderPreferences
import com.planetofoses989.kwideo.preferences.FoldersPreferences
import com.planetofoses989.kwideo.preferences.GesturePreferences
import com.planetofoses989.kwideo.preferences.PlayerPreferences
import com.planetofoses989.kwideo.preferences.SettingsManager
import com.planetofoses989.kwideo.preferences.SubtitlesPreferences
import com.planetofoses989.kwideo.preferences.preference.AndroidPreferenceStore
import com.planetofoses989.kwideo.preferences.preference.PreferenceStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val PreferencesModule =
  module {
    single { AndroidPreferenceStore(androidContext()) }.bind(PreferenceStore::class)

    single { AppearancePreferences(get()) }
    singleOf(::PlayerPreferences)
    singleOf(::GesturePreferences)
    singleOf(::DecoderPreferences)
    singleOf(::SubtitlesPreferences)
    singleOf(::AudioPreferences)
    singleOf(::AdvancedPreferences)
    single { BrowserPreferences(get(), androidContext()) }
    singleOf(::FoldersPreferences)
    singleOf(::SettingsManager)
  }
