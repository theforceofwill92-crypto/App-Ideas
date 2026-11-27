package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Conference
import com.example.model.MarketCity
import com.example.ui.theme.*

@Composable
fun FranchiseManagementModal(
    availableCities: List<MarketCity>,
    enabledConferences: List<Conference>,
    persistedFranchiseCount: Int,
    onDismiss: () -> Unit,
    onAddFranchise: (
        franchiseName: String,
        cityName: String,
        initialValuationMillions: Long,
        conferenceOverride: Conference?,
        primaryColorHex: Long
    ) -> Pair<Boolean, String>
) {
    val unfranchisedCities = remember(availableCities) {
        availableCities.filter { !it.hasFranchise }.take(6)
    }
    var franchiseName by remember { mutableStateOf("") }
    var cityName by remember { mutableStateOf("") }
    var initialValuationText by remember { mutableStateOf("250") }
    var selectedConference by remember { mutableStateOf<Conference?>(null) }
    val clubColors = remember {
        listOf(0xFF0EA5E9L, 0xFF10B981L, 0xFFF59E0BL, 0xFFEF4444L, 0xFF8B5CF6L, 0xFFEC4899L)
    }
    var selectedColorHex by remember { mutableStateOf(clubColors.first()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val parsedValuation = initialValuationText.trim().toLongOrNull()
    val previewCity = cityName.trim().ifBlank { "City" }
    val previewName = franchiseName.trim().ifBlank { "Franchise" }
    val previewFullName = if (previewName.startsWith("$previewCity ", ignoreCase = true) &&
        previewName.length > previewCity.length + 1
    ) {
        previewName
    } else {
        "$previewCity $previewName"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("franchise_management_modal_card")
                .border(1.5.dp, GoldPrimary.copy(alpha = 0.85f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(GoldPrimary.copy(alpha = 0.28f), CyanAccent.copy(alpha = 0.18f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddBusiness,
                                contentDescription = "Franchise Management",
                                tint = GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "FRANCHISE MANAGEMENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Add New Franchise",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_close_franchise_management")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Franchise Management Modal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = CyanAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Saves directly to Room DB ($persistedFranchiseCount persisted clubs) & updates active standings.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Franchise Name
                OutlinedTextField(
                    value = franchiseName,
                    onValueChange = {
                        franchiseName = it
                        errorMessage = null
                    },
                    label = { Text("Franchise Name") },
                    placeholder = { Text("e.g., Outlaws") },
                    leadingIcon = {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = GoldPrimary)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_franchise_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // City
                OutlinedTextField(
                    value = cityName,
                    onValueChange = {
                        cityName = it
                        errorMessage = null
                    },
                    label = { Text("City") },
                    placeholder = { Text("e.g., Austin") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = CyanAccent)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_franchise_city")
                )

                if (unfranchisedCities.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(unfranchisedCities) { market ->
                            val isSelected = cityName.equals(market.name, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    cityName = market.name
                                    if (initialValuationText == "250" || initialValuationText.isBlank()) {
                                        initialValuationText = (market.expansionFeeMillions * 2L).coerceAtLeast(180L).toString()
                                    }
                                    errorMessage = null
                                },
                                label = { Text(market.name, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("chip_market_city_${market.id}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Initial Valuation ($M)
                OutlinedTextField(
                    value = initialValuationText,
                    onValueChange = { next ->
                        val digitsOnly = next.filter { it.isDigit() }
                        initialValuationText = digitsOnly
                        errorMessage = null
                    },
                    label = { Text("Initial Valuation (\$M)") },
                    placeholder = { Text("e.g., 320") },
                    leadingIcon = {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = EmeraldAccent)
                    },
                    prefix = { Text("$") },
                    suffix = { Text("M") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_franchise_initial_valuation")
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(180L, 250L, 350L, 500L).forEach { preset ->
                        val selected = parsedValuation == preset
                        FilterChip(
                            selected = selected,
                            onClick = {
                                initialValuationText = preset.toString()
                                errorMessage = null
                            },
                            label = { Text("\$${preset}M", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_valuation_preset_$preset")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Conference & Colors
                Text(
                    text = "CONFERENCE & CLUB IDENTITY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedConference == null,
                            onClick = { selectedConference = null },
                            label = { Text("Auto Geo", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    items(enabledConferences) { conf ->
                        FilterChip(
                            selected = selectedConference == conf,
                            onClick = { selectedConference = conf },
                            label = { Text(conf.shortCode, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("chip_franchise_conf_${conf.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Colors:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    clubColors.forEach { hex ->
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                                .testTag("swatch_color_${hex.toString(16)}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Preview Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(selectedColorHex).copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("franchise_management_preview")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = previewFullName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "City: $previewCity • Conference: ${selectedConference?.displayName ?: "Auto Geographic"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = EmeraldAccent.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "\$${parsedValuation ?: 0L}M",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = EmeraldAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = CrimsonAlert.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("franchise_management_error")
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = CrimsonAlert,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_cancel_franchise_management")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val valNum = parsedValuation ?: 0L
                            val (ok, msg) = onAddFranchise(
                                franchiseName,
                                cityName,
                                valNum,
                                selectedConference,
                                selectedColorHex
                            )
                            if (!ok) {
                                errorMessage = msg
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_submit_add_franchise")
                    ) {
                        Text(
                            text = "Add Franchise",
                            color = Color.Black,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
