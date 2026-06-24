package com.subhranil.clouddnsmanager.zone.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subhranil.clouddnsmanager.models.dns.DnsRecord
import com.subhranil.clouddnsmanager.models.dns.DnsRecordType

@Composable
fun DnsRecordCard(
    record: DnsRecord,
    modifier: Modifier = Modifier
) {
    val smoothRadius = RoundedCornerShape(12.dp)
    val primaryColor = MaterialTheme.colorScheme.primary

    // Map distinct color themes to record types for immediate scannability
    val typeBadgeColor = when (record.type) {
        DnsRecordType.A, DnsRecordType.AAAA -> Color(0xFFE3F2FD) to Color(0xFF1E88E5) // Blue tint
        DnsRecordType.CNAME -> Color(0xFFE8F5E9) to Color(0xFF43A047)                // Green tint
        DnsRecordType.MX, DnsRecordType.SRV -> Color(0xFFFFF3E0) to Color(0xFFFB8C00) // Orange tint
        DnsRecordType.TXT, DnsRecordType.CAA -> Color(0xFFEDE7F6) to Color(0xFF5E35B1)// Purple tint
        else -> Color(0xFFF5F5F5) to Color(0xFF616161)                                // Neutral Grey
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = primaryColor.copy(alpha = 0.15f), shape = smoothRadius),
        shape = smoothRadius,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // --- Top Row: Type Badge + Record Host Name ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = typeBadgeColor.first,
                    contentColor = typeBadgeColor.second,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = record.type.name,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = record.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Middle Section: Content / Target Value ---
            Text(
                text = "Points to:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (record.type == DnsRecordType.MX && record.priority != null) {
                    "[Priority ${record.priority}] ${record.content}"
                } else {
                    record.content
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // If complex SRV/Data options exist, render them elegantly
            record.data?.let { data ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Port: ${data.port ?: "N/A"} • Weight: ${data.weight ?: "N/A"} • Target: ${data.target ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // --- Bottom Section: TTL & Proxy Tracking ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TTL Display
                Text(
                    text = "TTL: ${if (record.ttl == 1) "Auto" else "${record.ttl}s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Cloudflare Proxy Badge Logic
                if (record.proxiable) {
                    val proxyBadgeColor = if (record.proxied) Color(0xFFF57C00) else Color(0xFF757575)
                    val proxyLabel = if (record.proxied) "Proxied" else "DNS Only"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(proxyBadgeColor, shape = RoundedCornerShape(50))
                        )
                        Text(
                            text = proxyLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = proxyBadgeColor
                            )
                        )
                    }
                }
            }
        }
    }
}