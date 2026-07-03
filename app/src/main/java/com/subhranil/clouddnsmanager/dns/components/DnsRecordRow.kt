package com.subhranil.clouddnsmanager.dns.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import com.subhranil.clouddnsmanager.models.dns.DnsRecordType

@Composable
fun DnsRecordRow(
    record: DnsRecord,
    modifier: Modifier = Modifier
) {
    // Elegant Muted Badges for Dev-centric UI
    val (badgeBg, badgeText) = when (record.type) {
        DnsRecordType.A, DnsRecordType.AAAA -> Color(0xFFE8F0FE) to Color(0xFF1A73E8)
        DnsRecordType.CNAME -> Color(0xFFE6F4EA) to Color(0xFF137333)
        DnsRecordType.MX, DnsRecordType.SRV -> Color(0xFFFEF7E0) to Color(0xFFB06000)
        DnsRecordType.TXT -> Color(0xFFF1F3F4) to Color(0xFF3C4043)
        else -> Color(0xFFF8F9FA) to Color(0xFF5F6368)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Column 1: Type Badge (Fixed width for tabular alignment)
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.width(64.dp)
            ) {
                Text(
                    color = badgeText,
                    text = record.type.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Column 2: Name & Content (Core details Stacked)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (record.type == DnsRecordType.MX && record.priority != null) {
                        "Priority ${record.priority} → ${record.content}"
                    } else {
                        record.content
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Column 3: Proxy Status Indicator
            if (record.proxiable) {
                val proxyColor = if (record.proxied) Color(0xFFF47B20) else Color(0xFF9CA3AF) // Cloudflare Orange vs Grey
                val proxyLabel = if (record.proxied) "Proxied" else "DNS Only"

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.width(85.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(proxyColor, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = proxyLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = proxyColor
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(85.dp))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}