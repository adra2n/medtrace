package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yy.medtrace.R
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.ui.components.MemberSelector
import com.yy.medtrace.ui.components.TrendSection
import com.yy.medtrace.ui.components.buildSeries
import com.yy.medtrace.ui.state.SelectedMemberHolder
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.viewmodel.TrendsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    viewModel: TrendsViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val members = uiState.members
    val records = uiState.records
    val error = uiState.error
    val selectedMemberId = SelectedMemberHolder.selectedMemberId.value
    val dateFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadMembers()
    }

    LaunchedEffect(selectedMemberId) {
        selectedMemberId?.let { viewModel.loadRecords(selectedMemberId, 365L) }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = stringResource(R.string.trends_title),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.trends_cd_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                if (members.isNotEmpty()) {
                    MemberSelector(
                        members = members,
                        selectedMemberId = selectedMemberId,
                        onSelect = { member ->
                            scope.launch { SelectedMemberHolder.select(member.id, viewModel.database) }
                        },
                        emptyHint = stringResource(R.string.trends_empty_members)
                    )
                }
            }

            error?.let { msg ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text(stringResource(R.string.trends_btn_close))
                            }
                        }
                    }
                }
            }

            item {
                val series = remember(records) { buildSeries(records) }
                TrendSection(
                    series = series,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.large,
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                    elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.trends_record_stats),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${records.size}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.trends_stat_total),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val recentCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    records.count {
                                        it.onsetTime?.isAfter(LocalDateTime.now().minusDays(30)) ?: false
                                    }
                                } else 0
                                Text(
                                    text = "$recentCount",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.trends_stat_recent_30days),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${records.sumOf { it.medItems.size }}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.trends_stat_medications),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.trends_recent_records),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (records.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.large,
                        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.trends_no_records),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(records.take(10), key = { it.id }) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.large,
                        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = record.diagnosis.ifBlank { stringResource(R.string.trends_diagnosis_unfilled) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = record.onsetTime?.format(dateFormatter) ?: stringResource(R.string.trends_time_unknown),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (record.medItems.isNotEmpty()) {
                                Surface(
                                    shape = AppShapes.small,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.trends_med_count, record.medItems.size),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
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
