package com.example.calorietracker.data.cloud

import com.example.calorietracker.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Signed-in Supabase user. */
data class CloudSession(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    /** Epoch seconds. */
    val expiresAt: Long
)

class CloudException(message: String) : Exception(message)

/**
 * Account and cloud backup on Supabase (email + password auth, one JSON backup
 * row per user in the `backups` table, protected by row-level security).
 * The project URL and the public anon key come from BuildConfig; see README.
 */
class CloudSync {
    private val url = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val json = "application/json".toMediaType()
    private val gson = Gson()
    private val client = OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build()

    val isConfigured: Boolean get() = url.isNotBlank() && anonKey.isNotBlank()

    /** Returns a session, or null when the project requires e-mail confirmation first. */
    suspend fun signUp(email: String, password: String): CloudSession? {
        val body = call("POST", "/auth/v1/signup", null, JsonObject().apply {
            addProperty("email", email); addProperty("password", password)
        })
        return if (body.has("access_token")) session(body) else null
    }

    suspend fun signIn(email: String, password: String): CloudSession =
        session(call("POST", "/auth/v1/token?grant_type=password", null, JsonObject().apply {
            addProperty("email", email); addProperty("password", password)
        }))

    /** Exchanges a Google ID token (with the raw nonce whose hash Google signed) for a Supabase session. */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String): CloudSession =
        session(call("POST", "/auth/v1/token?grant_type=id_token", null, JsonObject().apply {
            addProperty("provider", "google"); addProperty("id_token", idToken); addProperty("nonce", rawNonce)
        }))

    val googleClientId: String get() = BuildConfig.GOOGLE_WEB_CLIENT_ID
    val googleEnabled: Boolean get() = isConfigured && googleClientId.isNotBlank()

    suspend fun refresh(s: CloudSession): CloudSession =
        session(call("POST", "/auth/v1/token?grant_type=refresh_token", null, JsonObject().apply {
            addProperty("refresh_token", s.refreshToken)
        }))

    /** Refreshes the token if it expires within a minute. */
    suspend fun fresh(s: CloudSession): CloudSession =
        if (s.expiresAt - System.currentTimeMillis() / 1000 < 60) refresh(s) else s

    suspend fun signOut(s: CloudSession) {
        runCatching { call("POST", "/auth/v1/logout", s.accessToken, JsonObject()) }
    }

    /** Upserts the user's backup row. */
    suspend fun upload(s: CloudSession, backupJson: String) {
        val row = JsonObject().apply {
            addProperty("user_id", s.userId)
            add("data", JsonParser.parseString(backupJson))
            addProperty("updated_at", java.time.Instant.now().toString())
        }
        call(
            "POST", "/rest/v1/backups?on_conflict=user_id", s.accessToken, row,
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal")
        )
    }

    /** The stored backup JSON and its time, or null if the user has none yet. */
    suspend fun download(s: CloudSession): Pair<String, String>? {
        val text = raw("GET", "/rest/v1/backups?select=data,updated_at&user_id=eq.${s.userId}", s.accessToken, null)
        val rows = JsonParser.parseString(text).asJsonArray
        val row = rows.firstOrNull()?.asJsonObject ?: return null
        return row.get("data").toString() to row.get("updated_at").asString
    }

    private fun session(o: JsonObject): CloudSession {
        val user = o.getAsJsonObject("user")
        return CloudSession(
            userId = user.get("id").asString,
            email = user.get("email")?.asString.orEmpty(),
            accessToken = o.get("access_token").asString,
            refreshToken = o.get("refresh_token").asString,
            expiresAt = o.get("expires_at")?.asLong ?: (System.currentTimeMillis() / 1000 + (o.get("expires_in")?.asLong ?: 3600))
        )
    }

    private suspend fun call(
        method: String, path: String, token: String?, body: JsonObject?, extraHeaders: Map<String, String> = emptyMap()
    ): JsonObject {
        val text = raw(method, path, token, body, extraHeaders)
        return if (text.isBlank()) JsonObject() else JsonParser.parseString(text).let { if (it.isJsonObject) it.asJsonObject else JsonObject() }
    }

    private suspend fun raw(
        method: String, path: String, token: String?, body: JsonObject?, extraHeaders: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        if (!isConfigured) throw CloudException("Облако не настроено в этой сборке.")
        val request = Request.Builder()
            .url(url + path)
            .header("apikey", anonKey)
            // Publishable keys (sb_publishable_…) go only in `apikey`; Authorization carries the
            // signed-in user's JWT. Legacy JWT anon keys still work as a bearer for anonymous calls.
            .apply {
                val bearer = token ?: anonKey.takeIf { !it.startsWith("sb_") }
                if (bearer != null) header("Authorization", "Bearer $bearer")
            }
            .apply { extraHeaders.forEach { (k, v) -> header(k, v) } }
            .method(method, body?.let { gson.toJson(it).toRequestBody(json) })
            .build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw CloudException("Нет подключения к интернету.")
        }
        response.use {
            val text = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw CloudException(errorMessage(it.code, text))
            text
        }
    }

    private fun errorMessage(code: Int, body: String): String {
        val msg = runCatching {
            val o = JsonParser.parseString(body).asJsonObject
            (o.get("error_description") ?: o.get("msg") ?: o.get("message"))?.asString
        }.getOrNull().orEmpty()
        return when {
            msg.contains("Invalid login credentials", true) -> "Неверный email или пароль."
            msg.contains("already registered", true) -> "Такой email уже зарегистрирован — войди."
            msg.contains("Email not confirmed", true) -> "Подтверди email по ссылке из письма и войди снова."
            msg.contains("Password should be", true) -> "Пароль слишком короткий — минимум 6 символов."
            msg.contains("rate limit", true) || code == 429 -> "Слишком много попыток. Подожди немного."
            code == 401 || code == 403 -> "Сессия истекла — войди заново."
            else -> "Ошибка сервера ($code). ${msg.take(120)}"
        }
    }
}
