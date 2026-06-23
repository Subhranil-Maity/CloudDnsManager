package com.subhranil.clouddnsmanager.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun StartScreen(
    modifier: Modifier = Modifier,
    viewModel: StartViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.loading) {
            CircularProgressIndicator()
        } else {
            Text("If you see this, something is seriously wrong. you were never meant to see this")
        }
    }
}