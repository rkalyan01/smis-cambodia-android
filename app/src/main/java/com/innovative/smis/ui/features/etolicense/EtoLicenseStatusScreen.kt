package com.innovative.smis.ui.features.etolicense


import com.innovative.smis.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.innovative.smis.data.model.response.EtoLicenseData
import com.innovative.smis.data.model.response.RenewalHistoryItem
import com.innovative.smis.data.model.response.TerminationHistoryItem
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtoLicenseStatusScreen(
    navController: NavController,
    viewModel: EtoLicenseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_eto_license_status), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            isRefreshing = uiState.isLoading,
            state = rememberPullToRefreshState(),
            onRefresh = viewModel::loadData
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading && uiState.etoList.isEmpty()) {
                     // Only show center spinner if list is empty, otherwise PullToRefreshBox handles it
                     // Actually PullToRefreshBox shows indicator too. But we might want initial loading indicator.
                     // The logic below is for when the list is invalid/empty/error.
                } 
                
                if (uiState.error != null && !uiState.isLoading) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadData() }, modifier = Modifier.padding(top = 16.dp)) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                } else if (uiState.etoList.isEmpty() && !uiState.isLoading) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Business, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.message_no_records_found),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = uiState.message ?: stringResource(R.string.message_no_contracts_found_eto),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { viewModel.loadData() }, modifier = Modifier.padding(top = 16.dp)) {
                            Text(stringResource(R.string.action_refresh))
                        }
                    }
                } else if (!uiState.etoList.isEmpty()) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.etoList) { eto ->
                            EtoLicenseCard(eto, uiState.message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EtoLicenseCard(eto: EtoLicenseData, message: String? = null) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = eto.companyName ?: stringResource(R.string.label_unknown_eto),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.label_license_number_format, eto.licenseNumber ?: "N/A"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = stringResource(R.string.action_expand),
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Basic Info Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    LabelValueItem(stringResource(R.string.label_contract_start), eto.contractStartDate)
                    Spacer(modifier = Modifier.height(8.dp))
                    LabelValueItem(stringResource(R.string.label_pbc_contract_id), eto.pbcContractId)
                }
                Column(modifier = Modifier.weight(1f)) {
                    // Highlight Expiration Date
                    Text(
                        text = stringResource(R.string.label_expiration_date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = eto.contractExpirationDate ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error // Red to indicate urgency
                    )
                }
            }

            // Warning Message Banner (inside card, below PBC Contract ID)
            if (message != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3CD),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCC00))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFF856404),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF856404)
                        )
                    }
                }
            }

            // Expanded Details (Renewal & Termination Tables)
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {

                    // Show only if data exists as per requirement
                    val hasRenewals = eto.renewalHistory.isNotEmpty()
                    val hasTerminations = eto.terminationHistory.isNotEmpty()

                    if (hasRenewals || hasTerminations) {
                        Text(
                            text = stringResource(R.string.label_renewal_termination_details),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (hasRenewals) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.label_renewal_details),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                                HorizontalDivider()
                                // Table Header
                                Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                                    Text(stringResource(R.string.label_prev_exp), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(stringResource(R.string.label_renew_date), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(stringResource(R.string.label_new_exp), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                }
                                // Table Rows
                                eto.renewalHistory.forEach { history ->
                                    HorizontalDivider(thickness = 0.5.dp)
                                    Row(Modifier.padding(8.dp)) {
                                        Text(history.prevExpirationDate ?: "-", fontSize = 11.sp, modifier = Modifier.weight(1f))
                                        Text(history.renewDate ?: "-", fontSize = 11.sp, modifier = Modifier.weight(1f))
                                        Text(history.newExpirationDate ?: "-", fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    if (hasTerminations) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.label_termination_details),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(8.dp)
                                )
                                HorizontalDivider()
                                // Table Header
                                Row(Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)).padding(8.dp)) {
                                    Text(stringResource(R.string.label_date), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
                                    Text(stringResource(R.string.label_by), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
                                    Text(stringResource(R.string.label_reason), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
                                }
                                // Table Rows
                                eto.terminationHistory.forEach { history ->
                                    HorizontalDivider(thickness = 0.5.dp)
                                    Row(Modifier.padding(8.dp)) {
                                        Text(history.terminationDate ?: "-", fontSize = 11.sp, modifier = Modifier.weight(0.8f))
                                        Text(history.terminatedBy ?: "-", fontSize = 11.sp, modifier = Modifier.weight(0.8f))
                                        Text(history.terminationCause ?: "-", fontSize = 11.sp, modifier = Modifier.weight(1.4f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }

                    if (!hasRenewals && !hasTerminations) {
                        Text(
                            text = stringResource(R.string.message_no_history),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LabelValueItem(label: String, value: String?) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value ?: "N/A",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}