package com.subhranil.clouddnsmanager.onboading

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

private const val SUPPORTING_INFO_TEXT =
    "Provide an API token with Zone read permissions to synchronize your infrastructure dashboard. Make sure that the Token has the required permissions."

private const val SUCCESS_INFO_TEXT =
    "Your Cloudflare API token has been successfully validated. You can now proceed to manage your infrastructure domains."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnBoardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnBoardingViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val smoothRadius = RoundedCornerShape(8.dp)
    val primaryColor = MaterialTheme.colorScheme.primary

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Dynamic Typography Content based on State
    val headlineText = if (state.isTokenVerified) "Successfully Verified" else "Enter The CloudFlare Token"
    val subText = if (state.isTokenVerified) SUCCESS_INFO_TEXT else SUPPORTING_INFO_TEXT

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
                // --- LANDSCAPE MODE: Split Screen Layout ---
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
                            text = headlineText,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = subText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Right Column: Input Box & Button Layout
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.End
                    ) {
                        TokenInputField(
                            state = state,
                            smoothRadius = smoothRadius,
                            primaryColor = primaryColor,
                            onValueChange = { viewModel.onAction(OnBoardingIntent.UpdateToken(it)) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        ActionButtonGroup(
                            state = state,
                            smoothRadius = smoothRadius,
                            primaryColor = primaryColor,
                            modifier = Modifier.width(200.dp),
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
                        text = headlineText,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    TokenInputField(
                        state = state,
                        smoothRadius = smoothRadius,
                        primaryColor = primaryColor,
                        onValueChange = { viewModel.onAction(OnBoardingIntent.UpdateToken(it)) }
                    )
                }

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

// --- Reusable Core Token Text Input Field ---
@Composable
private fun TokenInputField(
    state: OnBoardingState,
    smoothRadius: RoundedCornerShape,
    primaryColor: Color,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = state.token,
        onValueChange = onValueChange,
        label = { Text("API Token") },
        placeholder = { Text("Paste your token here...") },
        shape = smoothRadius,
        singleLine = true,
        // Using readOnly ensures the data stream stays alive, but blocks user input
        readOnly = state.isTokenVerifying || state.isTokenVerified,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = primaryColor,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            // Visually dampens the text color slightly when it's locked/read-only
            focusedTextColor = if (state.isTokenVerified) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// --- Reusable Button Group Handling State Actions ---
@Composable
private fun ActionButtonGroup(
    state: OnBoardingState,
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
                    color = MaterialTheme.colorScheme.onPrimary, // Mapped to primary contrast color
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