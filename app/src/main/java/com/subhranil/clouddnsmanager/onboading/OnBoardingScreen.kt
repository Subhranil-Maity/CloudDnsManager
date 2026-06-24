package com.subhranil.clouddnsmanager.onboading

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnBoardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnBoardingViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val smoothRadius = RoundedCornerShape(8.dp)
    val primaryColor = MaterialTheme.colorScheme.primary

    // Detect screen orientation to switch dynamically between portrait and landscape layouts
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // --- Modern Clean Alert Dialog ---
    if (state.error != null) {
        BasicAlertDialog(
            onDismissRequest = { viewModel.onAction(OnBoardingIntent.DismissError) },
            properties = DialogProperties()
        ) {
            Surface(
                shape = smoothRadius,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Authentication Error",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.onAction(OnBoardingIntent.DismissError) }) {
                            Text("Dismiss", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // --- Main Layout Container ---
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            if (isLandscape) {
                // --- LANDSCAPE MODE: Split Screen Grid System ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Branding Headers
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enter The CloudFlare Token",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Provide an API token with Zone read permissions to synchronize your infrastructure dashboard.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Right Column: Input Box & Localized Action Button Layout
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.End
                    ) {
                        OutlinedTextField(
                            value = state.token,
                            onValueChange = { viewModel.onAction(OnBoardingIntent.UpdateToken(it)) },
                            label = { Text("API Token") },
                            placeholder = { Text("Paste your token here...") },
                            shape = smoothRadius,
                            singleLine = true,
                            enabled = !state.isTokenVerifying && !state.isTokenVerified,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Button fits perfectly in the bottom right of the input column block
                        ActionButtonGroup(
                            state = state,
                            smoothRadius = smoothRadius,
                            primaryColor = primaryColor,
                            modifier = Modifier.width(200.dp), // Maintain tight layout proportions in horizontal view
                            onAction = viewModel::onAction
                        )
                    }
                }
            } else {
                // --- PORTRAIT MODE: Top-to-Bottom Flow Layout ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Enter The CloudFlare Token",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = "Provide an API token with Zone read permissions to synchronize your infrastructure dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = state.token,
                        onValueChange = { viewModel.onAction(OnBoardingIntent.UpdateToken(it)) },
                        label = { Text("API Token") },
                        placeholder = { Text("Paste your token here...") },
                        shape = smoothRadius,
                        singleLine = true,
                        enabled = !state.isTokenVerifying && !state.isTokenVerified,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Global Bottom Action Placement
                ActionButtonGroup(
                    state = state,
                    smoothRadius = smoothRadius,
                    primaryColor = primaryColor,
                    modifier = Modifier.fillMaxWidth(),
                    onAction = viewModel::onAction
                )
            }
        }
    }
}

// --- Reusable Button Group to prevent code duplication across states ---
@Composable
private fun ActionButtonGroup(
    state: OnBoardingState, // Adjust to exactly match your real state instance object name
    smoothRadius: RoundedCornerShape,
    primaryColor: Color,
    onAction: (OnBoardingIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isTokenVerified) {
        Button(
            onClick = { onAction(OnBoardingIntent.VerifyToken) },
            enabled = !state.isTokenVerifying && state.token.isNotBlank(),
            shape = smoothRadius,
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = modifier.height(50.dp)
        ) {
            if (state.isTokenVerifying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text("Verify Token", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    } else {
        Button(
            onClick = { onAction(OnBoardingIntent.Continue) },
            shape = smoothRadius,
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = modifier.height(50.dp)
        ) {
            Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}