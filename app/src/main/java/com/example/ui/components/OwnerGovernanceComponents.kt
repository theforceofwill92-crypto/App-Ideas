package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.BoardVoteResult
import com.example.model.OwnerArchetype
import com.example.model.OwnerBallot
import com.example.model.VoteChoice
import com.example.ui.theme.*

@Composable
fun OwnerArchetypeBadge(
    archetype: OwnerArchetype,
    modifier: Modifier = Modifier
) {
    val badgeColor = Color(archetype.badgeColorHex)
    Surface(
        color = badgeColor.copy(alpha = 0.16f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Text(
            text = archetype.shortBadge.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = badgeColor,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun BoardVotePreviewStrip(
    voteResult: BoardVoteResult,
    title: String = "BOARD OF GOVERNORS WHIP COUNT",
    modifier: Modifier = Modifier
) {
    val statusColor = if (voteResult.passed) EmeraldAccent else CrimsonAlert
    val yesFraction = if (voteResult.totalVotes > 0) {
        voteResult.yesVotes.toFloat() / voteResult.totalVotes.toFloat()
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("board_vote_preview_strip"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.HowToVote,
                        contentDescription = "Board Vote Projection",
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (voteResult.passed) "LIKELY PASSES ${voteResult.marginSummary}" else "PROJECTED VETO ${voteResult.marginSummary}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { yesFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = EmeraldAccent,
                trackColor = CrimsonAlert.copy(alpha = 0.35f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Compact owner roll-call chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                voteResult.ballots.take(6).forEach { ballot ->
                    val chipColor = if (ballot.choice == VoteChoice.APPROVE) EmeraldAccent else CrimsonAlert
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = chipColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, chipColor.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = ballot.owner.fullName.substringAfterLast(" "),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = ballot.choice.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = chipColor,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
            if (voteResult.ballots.size > 6) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "+${voteResult.ballots.size - 6} additional expansion governor(s) voting on full roll call",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun BoardOfGovernorsVoteDialog(
    voteResult: BoardVoteResult,
    onDismiss: () -> Unit,
    onExecutiveOverride: (() -> Unit)? = null
) {
    val accent = when {
        voteResult.enactedViaCommissionerOverride -> GoldPrimary
        voteResult.passed -> EmeraldAccent
        else -> CrimsonAlert
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .border(2.dp, accent, RoundedCornerShape(20.dp))
                .testTag("board_of_governors_vote_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    voteResult.enactedViaCommissionerOverride -> Icons.Default.Gavel
                                    voteResult.passed -> Icons.Default.Verified
                                    else -> Icons.Default.Block
                                },
                                contentDescription = "Board Vote Outcome",
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BOARD OF GOVERNORS ROLL CALL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = accent,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = voteResult.category.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = accent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when {
                                voteResult.enactedViaCommissionerOverride -> "OVERRIDE ${voteResult.marginSummary}"
                                voteResult.passed -> "PASSED ${voteResult.marginSummary}"
                                else -> "VETOED ${voteResult.marginSummary}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = voteResult.proposalTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = voteResult.proposalSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Vote tally gauge
                val yesPct = if (voteResult.totalVotes > 0) {
                    voteResult.yesVotes.toFloat() / voteResult.totalVotes.toFloat()
                } else 0f
                ProgressBarWithLabel(
                    label = "Majority Required (${(voteResult.totalVotes / 2) + 1} of ${voteResult.totalVotes} Owners)",
                    progress = yesPct,
                    displayValue = "${voteResult.yesVotes} Yea • ${voteResult.noVotes} Nay",
                    color = accent
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "INDIVIDUAL OWNER BALLOTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = GoldPrimary,
                    letterSpacing = 0.7.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                voteResult.ballots.forEach { ballot ->
                    OwnerBallotCard(ballot = ballot)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (!voteResult.passed && onExecutiveOverride != null) {
                    Button(
                        onClick = onExecutiveOverride,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("btn_board_executive_override")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Commissioner Executive Override (-10 Approval)",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("btn_dismiss_board_vote_dialog")
                ) {
                    Text(
                        text = if (voteResult.passed) "Close Roll Call" else "Adjust Proposal Terms",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerBallotCard(ballot: OwnerBallot) {
    val voteColor = when (ballot.choice) {
        VoteChoice.APPROVE -> EmeraldAccent
        VoteChoice.OPPOSE -> CrimsonAlert
        VoteChoice.ABSTAIN -> AmberWarning
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, voteColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = ballot.owner.fullName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        OwnerArchetypeBadge(archetype = ballot.owner.archetype)
                    }
                    Text(
                        text = "${ballot.teamName} • ${ballot.ownershipGroupName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = voteColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = ballot.choice.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = voteColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "\"${ballot.rationale}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun BoardOfGovernorsLedgerCard(
    votes: List<BoardVoteResult>,
    onSelectVote: (BoardVoteResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_board_vote_ledger")
            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HowToVote,
                        contentDescription = "Board Vote Ledger",
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BOARD OF GOVERNORS VOTE LEDGER",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary
                    )
                }
                Text(
                    text = "${votes.size} Recorded",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (votes.isEmpty()) {
                Text(
                    text = "No Board votes cast yet. Propose a rule amendment or CBA accord to record franchise owner roll-call ballots.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                votes.take(5).forEach { vote ->
                    val statusColor = when {
                        vote.enactedViaCommissionerOverride -> GoldPrimary
                        vote.passed -> EmeraldAccent
                        else -> CrimsonAlert
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onSelectVote(vote) },
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vote.proposalTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Year ${vote.year} W${vote.week} • ${vote.category.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                            Surface(
                                color = statusColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${if (vote.passed) "PASS" else "VETO"} ${vote.marginSummary}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
