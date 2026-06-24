package com.subhranil.clouddnsmanager.selectzones

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subhranil.clouddnsmanager.onboading.OnBoardingIntent
import com.subhranil.clouddnsmanager.selectzones.components.ZonesScreen
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectZoneScreen(
    modifier: Modifier = Modifier,
    viewModel: SelectZoneViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle();
    // --- Error Dialog ---
    val smoothRadius = RoundedCornerShape(8.dp)
    val primaryColor = MaterialTheme.colorScheme.primary
    if (state.error != null) {
        BasicAlertDialog(
            onDismissRequest = { viewModel.onAction(SelectZoneIntent.DismissError) },
            properties = DialogProperties()
        ) {
            Surface(
                shape = smoothRadius,
                tonalElevation = 0.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = state.error!!,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
    val context = LocalContext.current
    ZonesScreen(
        zones = state.zones,
        onZoneClick = {
//            Toast.makeText(context,  "Pressed", Toast.LENGTH_SHORT,).show()
            viewModel.onAction(SelectZoneIntent.SelectZone(it.id))
        },
        modifier = Modifier.fillMaxSize(),
        isLoading = state.loading
    )
}