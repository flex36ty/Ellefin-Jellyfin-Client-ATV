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

package com.google.jetstream.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object JellyfinNetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Add logging interceptor only in debug builds
        try {
            val loggingInterceptorClass = Class.forName("okhttp3.logging.HttpLoggingInterceptor")
            val loggingInterceptor = loggingInterceptorClass.getDeclaredConstructor().newInstance()
            val levelClass = Class.forName("okhttp3.logging.HttpLoggingInterceptor\$Level")
            val bodyLevel = levelClass.getDeclaredField("BODY").get(null)
            loggingInterceptorClass.getMethod("setLevel", levelClass).invoke(loggingInterceptor, bodyLevel)
            builder.addInterceptor(loggingInterceptor as Interceptor)
        } catch (e: ClassNotFoundException) {
            // HttpLoggingInterceptor not available (release build), skip logging
        } catch (e: Exception) {
            // Failed to set up logging, continue without it
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        
        return Retrofit.Builder()
            .baseUrl("https://jellyfin.example.com/") // Base URL will be set dynamically
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideJellyfinApiService(retrofit: Retrofit): JellyfinApiService {
        return retrofit.create(JellyfinApiService::class.java)
    }
}

/**
 * Creates a Retrofit instance with a dynamic base URL for a specific Jellyfin server
 */
fun createRetrofitForServer(serverUrl: String, okHttpClient: OkHttpClient): Retrofit {
    val contentType = "application/json".toMediaType()
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
        explicitNulls = false
        coerceInputValues = true
    }
    
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
}
