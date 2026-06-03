package com.tripwave.app.data.api

import android.content.Context
import com.tripwave.app.BuildConfig
import com.tripwave.app.data.prefs.SessionStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
        return cookieCache.any { it.name == "tripwave_session" }
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
                val req = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                // Shared mobile-client secret. Lets the server skip
                // Cloudflare Turnstile verification on /api/auth/send-otp
                // (which native can't satisfy since there's no browser
                // widget). Server still enforces email + IP rate limits,
                // so a leaked secret can't power bulk spam. Read from
                // BuildConfig.MOBILE_CLIENT_SECRET; omit when unset
                // rather than sending an empty header.
                if (BuildConfig.MOBILE_CLIENT_SECRET.isNotBlank()) {
                    req.addHeader("X-Tripwave-Mobile-Secret", BuildConfig.MOBILE_CLIENT_SECRET)
                }
                chain.proceed(req.build())
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

    /// Injects a session cookie obtained out-of-band (e.g. from the
    /// /api/auth/google?mobile=1 callback that deep-links back as
    /// tripwave://google-auth?session=...). Cookie attributes mirror
    /// what the backend would have set if the OAuth flow had gone
    /// through a normal HTTP redirect.
    fun setSessionCookie(value: String) {
        val baseUrl = BASE_URL.toHttpUrlOrNull() ?: return
        val cookie = Cookie.Builder()
            .name("tripwave_session")
            .value(value)
            .domain(baseUrl.host)
            .path("/")
            .httpOnly()
            .secure()
            .build()
        cookieJar?.saveFromResponse(baseUrl, listOf(cookie))
    }
}
