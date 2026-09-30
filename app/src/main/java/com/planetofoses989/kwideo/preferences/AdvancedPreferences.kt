package com.planetofoses989.kwideo.preferences

import com.planetofoses989.kwideo.BuildConfig
import com.planetofoses989.kwideo.preferences.preference.PreferenceStore

class AdvancedPreferences(
  preferenceStore: PreferenceStore,
) {
  val mpvConfStorageUri = preferenceStore.getString("mpv_conf_storage_location_uri")
  val mpvConf = preferenceStore.getString("mpv.conf")
  val inputConf = preferenceStore.getString("input.conf")

  val verboseLogging = preferenceStore.getBoolean("verbose_logging", BuildConfig.BUILD_TYPE != "release")

  val enabledStatisticsPage = preferenceStore.getInt("enabled_stats_page", 0)

  val enableRecentlyPlayed = preferenceStore.getBoolean("enable_recently_played", true)

  val enableScripts = preferenceStore.getBoolean("enable_scripts", true)
  val disabledScripts = preferenceStore.getStringSet("disabled_scripts", emptySet())
  val customScriptButtons = preferenceStore.getString("custom_script_buttons", "[]")
}
