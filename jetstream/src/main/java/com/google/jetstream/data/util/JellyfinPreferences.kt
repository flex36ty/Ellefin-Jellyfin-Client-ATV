/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.jetstream.data.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JellyfinPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    var serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, DEFAULT_DEVICE_ID) ?: DEFAULT_DEVICE_ID
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var serverName: String?
        get() = prefs.getString(KEY_SERVER_NAME, null)
        set(value) = prefs.edit().putString(KEY_SERVER_NAME, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var carouselEnabled: Boolean
        get() = prefs.getBoolean(KEY_CAROUSEL_ENABLED, true) // Default to enabled
        set(value) = prefs.edit().putBoolean(KEY_CAROUSEL_ENABLED, value).apply()

    var immersiveListEnabled: Boolean
        get() = prefs.getBoolean(KEY_IMMERSIVE_LIST_ENABLED, false) // Default to disabled
        set(value) = prefs.edit().putBoolean(KEY_IMMERSIVE_LIST_ENABLED, value).apply()

    var dynamicBackgroundColorEnabled: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_BACKGROUND_COLOR_ENABLED, false) // Default to disabled
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_BACKGROUND_COLOR_ENABLED, value).apply()

    var blackBackgroundEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLACK_BACKGROUND_ENABLED, false) // Default to disabled
        set(value) = prefs.edit().putBoolean(KEY_BLACK_BACKGROUND_ENABLED, value).apply()

    var isLoggedIn: Boolean
        get() = !serverUrl.isNullOrBlank() && !accessToken.isNullOrBlank() && !userId.isNullOrBlank()
        set(value) {
            if (!value) {
                prefs.edit()
                    .remove(KEY_ACCESS_TOKEN)
                    .remove(KEY_USER_ID)
                    .remove(KEY_USERNAME)
                    .apply()
            }
        }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "jellyfin_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_SERVER_NAME = "server_name"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_CAROUSEL_ENABLED = "carousel_enabled"
        private const val KEY_IMMERSIVE_LIST_ENABLED = "immersive_list_enabled"
        private const val KEY_DYNAMIC_BACKGROUND_COLOR_ENABLED = "dynamic_background_color_enabled"
        private const val KEY_BLACK_BACKGROUND_ENABLED = "black_background_enabled"
        private const val DEFAULT_DEVICE_ID = "android_tv_client"
    }
}
