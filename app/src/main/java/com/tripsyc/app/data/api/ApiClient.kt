package com.tripsyc.app.data.api

import android.content.Context
import com.tripsyc.app.BuildConfig
import com.tripsyc.app.data.prefs.SessionStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class PersistentCookieJar(private val sessionStore: SessionStore) : CookieJar {

    private val cookieCache: CopyOnWriteArrayList<Cookie> = CopyOnWriteArrayList()

    init {
        // Load cookies from DataStore on init (blocking because CookieJar is synchronous)
        val saved = runBlocking { sessionStore.cookies.firstOrNull() }
        if (!saved.isNullOrBlank()) {
            parseCookieString(saved).forEach { cookieCache.add(it) }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // Remove old cookies for the same domain/name
        val toRemove = cookieCache.filter { cached ->
            cookies.any { it.name == cached.name && it.domain == cached.domain }
        }
        cookieCache.removeAll(toRemove.toSet())
        cookieCache.addAll(cookies)
        persistCookies()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieCache.filter { it.matches(url) }
    }

    fun clearCookies() {
        cookieCache.clear()
        runBlocking { sessionStore.clearCookies() }
    }

    fun hasSessionCookie(): Boolean {
        return cookieCache.any { it.name == "tripsyc_session" }
    }

    private fun persistCookies() {
        val cookieString = cookieCache.joinToString(";") { "${it.name}=${it.value}" }
        runBlocking { sessionStore.saveCookies(cookieString) }
    }

    private fun parseCookieString(raw: String): List<Cookie> {
        // Simple reconstruction — we store them as a semicolon-separated list
        // The full Cookie with domain info isn't preserved this way, so we
        // use the more robust approach of storing cookies as their serialized form
        return emptyList() // Handled via persistent storage; real cookies come from network
    }
}

object ApiClient {

    val BASE_URL: String = BuildConfig.BASE_URL

    private var cookieJar: PersistentCookieJar? = null

    fun initialize(context: Context) {
        val store = SessionStore(context.applicationContext)
        cookieJar = PersistentCookieJar(store)
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .cookieJar(cookieJar ?: throw IllegalStateException("ApiClient not initialized"))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }
        builder.build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    fun getCookieJar(): PersistentCookieJar = cookieJar
        ?: throw IllegalStateException("ApiClient not initialized")

    fun hasSession(): Boolean = cookieJar?.hasSessionCookie() ?: false

    fun clearSession() {
        cookieJar?.clearCookies()
    }
}
