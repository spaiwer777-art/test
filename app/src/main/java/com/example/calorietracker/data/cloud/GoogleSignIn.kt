package com.example.calorietracker.data.cloud

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.security.SecureRandom

/** Result of the system Google account picker: an ID token plus the raw nonce for Supabase. */
data class GoogleToken(val idToken: String, val rawNonce: String)

/**
 * Shows Android's "Sign in with Google" sheet (Credential Manager). The hashed
 * nonce goes to Google and ends up inside the ID token; Supabase checks it
 * against the raw nonce, so a stolen token can't be replayed.
 */
object GoogleSignIn {
    suspend fun requestToken(activityContext: Context, webClientId: String): GoogleToken {
        val raw = ByteArray(24).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        val hashed = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setNonce(hashed)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw CloudException("Вход отменён.")
        } catch (e: NoCredentialException) {
            throw CloudException("На телефоне нет Google-аккаунта. Добавь его в настройках Android.")
        }
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleToken(GoogleIdTokenCredential.createFrom(credential.data).idToken, raw)
        }
        throw CloudException("Google не вернул данные для входа.")
    }
}
