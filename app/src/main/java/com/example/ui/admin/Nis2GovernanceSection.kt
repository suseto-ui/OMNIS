package com.example.ui.admin

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accessibility.OmnisAccessibilityHelper.accessibleHeading
import com.example.security.nis2.AssetInventoryManager
import com.example.security.nis2.Nis2RiskGovernanceManager
import com.example.security.nis2.SecurityEvent
import com.example.security.nis2.SiemSecurityDispatcher

@Composable
fun Nis2GovernanceSection() {
    val riskSummary by Nis2RiskGovernanceManager.riskSummary.collectAsState()
    val siemEvents by SiemSecurityDispatcher.events.collectAsState()
    val siemCount by SiemSecurityDispatcher.siemTransmittedCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("nis2_governance_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "NIS2 & Zero Trust Governance",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.accessibleHeading("NIS2 a Zero Trust správa rizik")
            )
            Text(
                text = "Architektura rizik a ochrana statutárních orgánů před sankcemi dle zákona o kybernetické bezpečnosti.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Karta hodnocení rizik statutárních orgánů
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Risk Shield",
                        tint = if (riskSummary.penaltyRiskScore < 0.2f) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Statutární hodnocení odpovědnosti",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stav: ${riskSummary.governanceStatus}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (riskSummary.penaltyRiskScore < 0.2f) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = riskSummary.statutoryOfficerWarning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBadge(label = "OIDC Pokrytí", value = "${riskSummary.oidcMigrationProgressPct.toInt()}%")
                    MetricBadge(label = "Plošné MFA", value = "${riskSummary.mfaEnforcementCoveragePct.toInt()}%")
                    MetricBadge(label = "SIEM Přenosy", value = "$siemCount")
                }
            }
        }

        // 2. Chráněná aktiva a NÚKIB soupis
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Soupis chráněných aktiv (NÚKIB Asset Inventory)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                AssetInventoryManager.protectedAssets.forEach { asset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = asset.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "${asset.type} • ${asset.securityZone}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(
                            text = asset.criticality,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (asset.criticality == "CRITICAL") Color(0xFFEF4444) else Color(0xFF3B82F6)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }

        // 3. SBOM - Dodavatelský řetězec a zranitelnosti třetích stran
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SBOM Dodavatelský řetězec (Software Bill of Materials)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                AssetInventoryManager.supplyChainSbom.forEach { sbom ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "${sbom.componentName} (v${sbom.version})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = "Dodavatel: ${sbom.vendorOrSupplier} • ${sbom.license}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(
                            text = sbom.vulnerabilityStatus,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        // 4. SIEM Realtime Security Event Log
        Text(
            text = "SIEM Asynchronní záznamy událostí (${siemEvents.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (siemEvents.isEmpty()) {
            Text(
                text = "Žádné zaznamenané bezpečnostní anomálie. Systém je nominální.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        } else {
            siemEvents.take(10).forEach { event ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = event.eventType, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text(text = event.severity, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (event.severity == "CRITICAL") Color.Red else Color.DarkGray)
                        }
                        Text(text = event.details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
