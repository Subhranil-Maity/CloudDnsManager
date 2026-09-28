package com.subhranil.clouddnsmanager.security

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.subhranil.clouddnsmanager.lock.LockScreenContent
import com.subhranil.clouddnsmanager.lock.LockScreenState
import com.subhranil.clouddnsmanager.preview.PreviewTheme
import com.subhranil.clouddnsmanager.preview.ScreenPreview
import com.subhranil.clouddnsmanager.security.settings.SecuritySettingsContent
import com.subhranil.clouddnsmanager.security.settings.SecuritySettingsState
import com.subhranil.clouddnsmanager.security.setup.PinSetupContent
import com.subhranil.clouddnsmanager.security.setup.PinSetupState
import com.subhranil.clouddnsmanager.security.setup.PinSetupStep

@PreviewTest
@ScreenPreview
@Composable
fun LockScreenPreview() {
    PreviewTheme {
        LockScreenContent(
            state = LockScreenState(pin = "12", biometricsEnabled = true),
            onAction = {},
            canUseBiometrics = true,
            onUseBiometrics = {},
        )
    }
}

@PreviewTest
@ScreenPreview
@Composable
fun PinSetupPreview() {
    PreviewTheme {
        PinSetupContent(
            state = PinSetupState(changing = false, step = PinSetupStep.Confirm, pin = "1234"),
            onAction = {},
        )
    }
}

@PreviewTest
@ScreenPreview
@Composable
fun SecuritySettingsPreview() {
    PreviewTheme {
        SecuritySettingsContent(
            state = SecuritySettingsState(isPinSet = true, biometricsEnabled = true, biometricsAvailable = true),
            onAction = {},
            onEnableBiometrics = {},
        )
    }
}
