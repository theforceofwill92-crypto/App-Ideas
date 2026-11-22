package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FranchiseIdentityEngine
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GoldPrimary

@Composable
fun FranchiseIdentityCustomizer(
    cityName: String,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    onRandomizeNickname: () -> Unit,
    primaryColorHex: Long,
    secondaryColorHex: Long,
    onColorsChanged: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // 0 = editing Primary Color, 1 = editing Secondary Color
    var activeSlot by remember { mutableIntStateOf(0) }
    val palette = FranchiseIdentityEngine.brandColorPalette
    val primaryName = FranchiseIdentityEngine.colorNameFor(primaryColorHex)
    val secondaryName = FranchiseIdentityEngine.colorNameFor(secondaryColorHex)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Live Dual-Color Franchise Crest Preview
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(primaryColorHex).copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_franchise_crest_preview")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TeamBadge(
                    cityName = cityName,
                    nickname = nickname.ifBlank { "Club" },
                    colorHex = primaryColorHex,
                    secondaryColorHex = secondaryColorHex,
                    subtitle = "$primaryName • $secondaryName",
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val (randP, randS) = FranchiseIdentityEngine.generateRandomColorPair(
                            currentPrimaryHex = primaryColorHex,
                            currentSecondaryHex = secondaryColorHex
                        )
                        onColorsChanged(randP, randS)
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("btn_randomize_colors")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Randomize 2 Team Colors",
                        tint = GoldPrimary
                    )
                }
            }
        }

        // Nickname Input + Realistic Random Nickname Generator Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                label = { Text("Franchise Nickname") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_franchise_nickname")
            )
            FilledTonalButton(
                onClick = onRandomizeNickname,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = GoldPrimary.copy(alpha = 0.18f),
                    contentColor = GoldPrimary
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("btn_randomize_nickname")
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = "Generate Realistic Random Nickname",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Random",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // 2-Color Selector Header & Slot Switcher (Primary vs Secondary)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TEAM COLORS (${palette.size} OPTIONS • PICK 2)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (activeSlot == 0) "Editing Primary" else "Editing Secondary",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColorSlotChip(
                    slotTitle = "1. Primary",
                    colorName = primaryName,
                    colorHex = primaryColorHex,
                    isSelected = activeSlot == 0,
                    onClick = { activeSlot = 0 },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("slot_primary_color")
                )
                ColorSlotChip(
                    slotTitle = "2. Secondary",
                    colorName = secondaryName,
                    colorHex = secondaryColorHex,
                    isSelected = activeSlot == 1,
                    onClick = { activeSlot = 1 },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("slot_secondary_color")
                )
            }

            // 24 Distinct Color Options (4 rows x 6 swatches)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grid_franchise_24_colors"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                palette.chunked(6).forEachIndexed { rowIdx, rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowColors.forEachIndexed { colIdx, colorOption ->
                            val swatchIndex = rowIdx * 6 + colIdx
                            val isPrimary = colorOption.hex == primaryColorHex
                            val isSecondary = colorOption.hex == secondaryColorHex
                            val textOnSwatch = if (FranchiseIdentityEngine.isLightColor(colorOption.hex)) {
                                Color(0xFF0F172A)
                            } else {
                                Color.White
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (activeSlot == 0) {
                                            if (colorOption.hex == secondaryColorHex) {
                                                // Swap so Primary & Secondary always stay 2 different colors
                                                onColorsChanged(colorOption.hex, primaryColorHex)
                                            } else {
                                                onColorsChanged(colorOption.hex, secondaryColorHex)
                                            }
                                        } else {
                                            if (colorOption.hex == primaryColorHex) {
                                                // Swap so Primary & Secondary always stay 2 different colors
                                                onColorsChanged(secondaryColorHex, colorOption.hex)
                                            } else {
                                                onColorsChanged(primaryColorHex, colorOption.hex)
                                            }
                                        }
                                    }
                                    .semantics {
                                        contentDescription = colorOption.name
                                    }
                                    .testTag("brand_color_swatch_$swatchIndex"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorOption.hex))
                                        .border(
                                            width = if (isPrimary || isSecondary) 3.dp else 1.dp,
                                            color = when {
                                                isPrimary -> GoldPrimary
                                                isSecondary -> CyanAccent
                                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isPrimary || isSecondary) {
                                        Text(
                                            text = if (isPrimary) "1" else "2",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = textOnSwatch,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSlotChip(
    slotTitle: String,
    colorName: String,
    colorHex: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) GoldPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (isSelected) 1.6.dp else 1.dp,
            color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(colorHex))
                    .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slotTitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = colorName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
