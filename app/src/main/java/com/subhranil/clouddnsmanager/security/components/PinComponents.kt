package com.subhranil.clouddnsmanager.security.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.subhranil.clouddnsmanager.security.AppLockRepository
import com.subhranil.clouddnsmanager.security.PinHasher
import com.subhranil.clouddnsmanager.security.PinResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Six separate digit boxes backed by one hidden numeric text field, so the system
 * keyboard, paste and backspace all work. Entered digits are shown as dots.
 */
@Composable
fun PinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    autoFocus: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }

    BasicTextField(
        // Pin the cursor to the end so digits are always appended / removed in order
        value = TextFieldValue(value, selection = TextRange(value.length)),
        onValueChange = { input ->
            onValueChange(input.text.filter { it.isDigit() }.take(PinHasher.PIN_LENGTH))
        },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .semantics {
                contentDescription = "PIN, ${value.length} of ${PinHasher.PIN_LENGTH} digits entered"
            },
        decorationBox = {
            // The inner text field is intentionally not drawn: the boxes are the visuals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                repeat(PinHasher.PIN_LENGTH) { index ->
                    PinDigitBox(
                        filled = index < value.length,
                        active = focused && enabled && index == value.length,
                        isError = isError,
                        enabled = enabled,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        },
    )

    if (autoFocus && enabled) {
        LaunchedEffect(Unit) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }
}

@Composable
private fun PinDigitBox(
    filled: Boolean,
    active: Boolean,
    isError: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val borderColor = when {
        isError -> colors.error
        active -> colors.primary
        filled -> colors.onSurfaceVariant
        else -> colors.outlineVariant
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .widthIn(max = 52.dp)
            .aspectRatio(0.85f)
            .alpha(if (enabled) 1f else 0.5f)
            .border(
                width = if (active || isError) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            ),
    ) {
        if (filled) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(colors.onSurface, CircleShape)
            )
        }
    }
}

/** Seconds remaining until [untilMs], ticking once per second; 0 when no cooldown is active. */
@Composable
fun rememberLockoutSecondsLeft(untilMs: Long): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(untilMs) {
        now = System.currentTimeMillis()
        while (now < untilMs) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    return ((untilMs - now + 999) / 1_000).coerceAtLeast(0)
}

/**
 * Asks for the existing app PIN (used by the AuthGate). Verifies through [repository] so
 * wrong attempts count towards the shared cooldown.
 */
@Composable
fun PinEntryDialog(
    title: String,
    message: String,
    repository: AppLockRepository,
    onVerified: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lockedUntil by remember { mutableLongStateOf(0L) }
    var verifying by remember { mutableStateOf(false) }
    val secondsLeft = rememberLockoutSecondsLeft(lockedUntil)

    LaunchedEffect(Unit) { lockedUntil = repository.lockoutUntilMs() }

    fun submit(candidate: String) {
        if (verifying || candidate.length < PinHasher.PIN_LENGTH) return
        verifying = true
        scope.launch {
            when (val result = repository.verifyPin(candidate)) {
                PinResult.Success -> onVerified()
                is PinResult.Wrong -> {
                    pin = ""
                    error = "Incorrect PIN. ${result.attemptsBeforeLockout} attempts left before a cooldown."
                }
                is PinResult.LockedOut -> {
                    pin = ""
                    error = null
                    lockedUntil = result.untilMs
                }
            }
            verifying = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                PinInputField(
                    value = pin,
                    onValueChange = {
                        pin = it
                        error = null
                        if (it.length == PinHasher.PIN_LENGTH) submit(it)
                    },
                    enabled = secondsLeft == 0L && !verifying,
                    isError = error != null,
                )
                val status = when {
                    secondsLeft > 0 -> "Too many attempts. Try again in ${secondsLeft}s."
                    else -> error
                }
                if (status != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(status, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { submit(pin) },
                enabled = pin.length == PinHasher.PIN_LENGTH && secondsLeft == 0L && !verifying,
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
