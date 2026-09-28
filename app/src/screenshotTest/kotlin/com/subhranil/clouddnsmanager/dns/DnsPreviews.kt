package com.subhranil.clouddnsmanager.dns

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.subhranil.clouddnsmanager.dns.components.DnsRecordDetailContent
import com.subhranil.clouddnsmanager.dns.components.DnsRecordsScreen
import com.subhranil.clouddnsmanager.dns.edit.DnsEditorLoadState
import com.subhranil.clouddnsmanager.dns.edit.DnsRecordEditorContent
import com.subhranil.clouddnsmanager.dns.edit.DnsRecordEditorState
import com.subhranil.clouddnsmanager.dns.edit.DnsRecordForm
import com.subhranil.clouddnsmanager.preview.PreviewTheme
import com.subhranil.clouddnsmanager.preview.SampleData
import com.subhranil.clouddnsmanager.preview.ScreenPreview

@PreviewTest
@ScreenPreview
@Composable
fun DnsRecordsPreview() {
    PreviewTheme {
        DnsRecordsScreen(
            dnsRecords = SampleData.dnsItems,
            isLoading = false,
            onSelectRecord = {},
            onAddRecord = {},
            onRefresh = {},
            onBack = {},
        )
    }
}

/** The record details sheet body (the sheet itself is a popup, which previews can't show). */
@PreviewTest
@ScreenPreview
@Composable
fun DnsRecordDetailsPreview() {
    val item = SampleData.dnsItems.first()
    PreviewTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            DnsRecordDetailContent(
                item = item,
                noteDraft = item.note.orEmpty(),
                working = false,
                message = null,
                onNoteChange = {},
                onSaveNote = {},
                onToggleLock = {},
                onEdit = {},
                onDelete = {},
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@PreviewTest
@ScreenPreview
@Composable
fun DnsRecordEditorPreview() {
    val record = SampleData.dnsItems.first { it.record.name == "api.${SampleData.ZONE}" }.record
    PreviewTheme {
        DnsRecordEditorContent(
            state = DnsRecordEditorState(
                isNew = false,
                loadState = DnsEditorLoadState.Ready,
                zoneName = SampleData.ZONE,
                form = DnsRecordForm.fromRecord(record),
            ),
            onAction = {},
        )
    }
}
