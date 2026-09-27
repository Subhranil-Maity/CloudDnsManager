package com.subhranil.clouddnsmanager.security

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LockState {
    /** Still reading whether a PIN is set; render nothing sensitive yet. */
    data object Checking : LockState
    data object Locked : LockState
    data object Unlocked : LockState
}

/**
 * Decides when the app is locked: on cold start, and after it has been in the
 * background for longer than [RELOCK_AFTER_MS]. Only applies while a PIN is set.
 */
class AppLockManager(
    private val repository: AppLockRepository,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _lockState = MutableStateFlow<LockState>(LockState.Checking)
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    private var pinSet = false

    /** elapsedRealtime is monotonic, so changing the device clock can't skip the re-lock. */
    private var backgroundedAt: Long? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch {
            repository.isPinSet.collect { isSet ->
                pinSet = isSet
                when {
                    // Cold start: lock straight away if a PIN exists
                    _lockState.value == LockState.Checking ->
                        _lockState.value = if (isSet) LockState.Locked else LockState.Unlocked
                    // PIN removed (or wiped by logout): nothing left to lock with
                    !isSet -> _lockState.value = LockState.Unlocked
                    // PIN newly set: stay unlocked for the current session
                }
            }
        }
    }

    fun unlock() {
        _lockState.value = LockState.Unlocked
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart(owner: LifecycleOwner) {
        val since = backgroundedAt ?: return
        backgroundedAt = null
        if (pinSet && SystemClock.elapsedRealtime() - since > RELOCK_AFTER_MS) {
            _lockState.value = LockState.Locked
        }
    }

    companion object {
        const val RELOCK_AFTER_MS = 60_000L
    }
}
