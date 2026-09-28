package com.subhranil.clouddnsmanager.zone

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.subhranil.clouddnsmanager.preview.PreviewTheme
import com.subhranil.clouddnsmanager.preview.SampleData
import com.subhranil.clouddnsmanager.preview.ScreenPreview

@PreviewTest
@ScreenPreview
@Composable
fun ZoneHubPreview() {
    PreviewTheme {
        ZoneHubContent(state = ZoneHubState(zoneName = SampleData.ZONE), onAction = {})
    }
}
