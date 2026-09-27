package com.example.calorietracker.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.cloud.CloudSession
import com.example.calorietracker.graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class AccountStatus {
    object Idle : AccountStatus()
    data class Busy(val text: String) : AccountStatus()
    data class Done(val text: String) : AccountStatus()
    data class Failed(val text: String) : AccountStatus()
}

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = application.graph
    val cloudConfigured = graph.cloud.isConfigured
    val googleEnabled = graph.cloud.googleEnabled

    val session: StateFlow<CloudSession?> = graph.settings.cloudSession
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val lastSync: StateFlow<Long> = graph.settings.cloudLastSync
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val autoSync: StateFlow<Boolean> = graph.settings.cloudAuto
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _status = MutableStateFlow<AccountStatus>(AccountStatus.Idle)
    val status: StateFlow<AccountStatus> = _status.asStateFlow()

    /** After sign-in: does the cloud already hold a backup? null while unknown. */
    private val _cloudHasData = MutableStateFlow<Boolean?>(null)
    val cloudHasData: StateFlow<Boolean?> = _cloudHasData.asStateFlow()

    private fun run(busy: String, then: (() -> Unit)? = null, block: suspend () -> String) {
        viewModelScope.launch {
            _status.value = AccountStatus.Busy(busy)
            _status.value = try {
                AccountStatus.Done(block()).also { then?.invoke() }
            } catch (e: Exception) {
                AccountStatus.Failed(e.message ?: "Что-то пошло не так")
            }
        }
    }

    private suspend fun signedIn(s: CloudSession) {
        graph.settings.setCloudSession(s)
        _cloudHasData.value = try { graph.cloud.download(s) != null } catch (e: Exception) { false }
    }

    fun signIn(email: String, password: String) = run("Вход…") {
        signedIn(graph.cloud.signIn(email.trim(), password))
        "Вход выполнен. Можно загрузить данные из облака или сохранить текущие."
    }

    /** Google sign-in needs an Activity context for the system account picker. */
    fun signInWithGoogle(activityContext: android.content.Context) = run("Вход через Google…") {
        val token = com.example.calorietracker.data.cloud.GoogleSignIn.requestToken(activityContext, graph.cloud.googleClientId)
        val s = graph.cloud.signInWithGoogle(token.idToken, token.rawNonce)
        signedIn(s)
        "Вход выполнен: ${s.email}. Можно загрузить данные из облака или сохранить текущие."
    }

    fun signUp(email: String, password: String) = run("Создаю аккаунт…") {
        val s = graph.cloud.signUp(email.trim(), password)
        if (s == null) {
            "Аккаунт создан. Подтверди email по ссылке из письма и войди."
        } else {
            graph.settings.setCloudSession(s)
            _cloudHasData.value = false
            graph.syncUp()?.let { throw IllegalStateException(it) }
            "Аккаунт создан, данные сохранены в облако."
        }
    }

    fun upload() = run("Сохраняю в облако…") {
        graph.syncUp()?.let { throw IllegalStateException(it) }
        "Данные сохранены в облако."
    }

    fun download(then: (() -> Unit)? = null) = run("Загружаю из облака…", then) {
        val s = graph.cloud.fresh(session.value ?: throw IllegalStateException("Не выполнен вход"))
        graph.settings.setCloudSession(s)
        val (json, _) = graph.cloud.download(s) ?: throw IllegalStateException("В облаке ещё нет сохранённых данных.")
        graph.backup.restore(graph.backup.fromJson(json))
        graph.settings.setCloudLastSync(System.currentTimeMillis())
        "Данные загружены из облака."
    }

    fun signOut() = run("Выход…") {
        session.value?.let { graph.cloud.signOut(it) }
        graph.settings.setCloudSession(null)
        _cloudHasData.value = null
        "Вы вышли. Данные на телефоне остались."
    }

    fun setAutoSync(value: Boolean) {
        viewModelScope.launch { graph.settings.setCloudAuto(value) }
    }

    fun exportTo(uri: Uri, includePhotos: Boolean) = run("Сохраняю файл…") {
        withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                graph.backup.writeZip(it, includePhotos)
            } ?: throw IllegalStateException("Не удалось открыть файл")
        }
        "Резервная копия сохранена."
    }

    fun importFrom(uri: Uri) = run("Восстанавливаю…") {
        withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { graph.backup.restoreZip(it) }
                ?: throw IllegalStateException("Не удалось открыть файл")
        }
        "Данные восстановлены из файла."
    }
}
