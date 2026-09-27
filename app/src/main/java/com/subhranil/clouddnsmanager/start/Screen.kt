package com.subhranil.clouddnsmanager.start

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Initial placeholder destination. The system splash screen stays up until MainActivity
 * replaces this destination, so it's intentionally blank to avoid flashing a spinner.
 */
@Composable
fun StartScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )
}
