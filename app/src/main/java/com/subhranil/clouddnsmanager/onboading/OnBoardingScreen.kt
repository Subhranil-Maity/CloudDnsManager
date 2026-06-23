package com.subhranil.clouddnsmanager.onboading

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnBoardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnBoardingViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    // We define a uniform radius for that "professional rectangular-but-curved" look
    val smoothRadius = RoundedCornerShape(8.dp)
    val primaryColor = MaterialTheme.colorScheme.primary

    // --- Error Dialog ---
    if (state.error != null) {
        BasicAlertDialog(
            onDismissRequest = { viewModel.onAction(OnBoardingIntent.DismissError) },
            properties = DialogProperties()
        ) {
            Surface(
                shape = smoothRadius,
                tonalElevation = 0.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = state.error,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }

    // --- Main Layout ---
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Enter The CloudFlare Token",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        // This centering container pushes the input field right into the middle of the screen
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(100.dp))
            OutlinedTextField(
                value = state.token,
                onValueChange = { viewModel.onAction(OnBoardingIntent.UpdateToken(it)) },
                label = { Text("Cloudflare Token") },
                shape = smoothRadius,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    // Border changes color dynamically based on focus, using your Theme's Primary
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = primaryColor.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    // Adds a thick, 2dp primary color border with curved corners
//                    .border(width = 2.dp, color = primaryColor, shape = smoothRadius)
            )
        }

        // --- Bottom Action Button ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            if (!state.isTokenVerified) {
                Button(
                    onClick = { viewModel.onAction(OnBoardingIntent.VerifyToken) },
                    enabled = !state.isTokenVerifying,
                    shape = smoothRadius,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Verify Token")
                }
            }else {
                Button(
                    onClick = { viewModel.onAction(OnBoardingIntent.Continue) },
                    enabled = state.isTokenVerified == true,
                    shape = smoothRadius,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Continue")
                }
            }
        }
    }
}