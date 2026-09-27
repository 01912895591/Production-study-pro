package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProductionStudy
import com.example.data.model.StudyRow
import com.example.ui.components.ManualEditDialog
import com.example.ui.components.SignaturePadDialog
import com.example.ui.excel.ExcelExporter
import com.example.ui.pdf.PdfExporter
import com.example.ui.signature.SignatureHelper
import com.example.ui.viewmodel.ProductionStudyViewModel
import com.example.ui.viewmodel.SortOption
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductionStudyScreen(viewModel: ProductionStudyViewModel) {
    val localContext = LocalContext.current
    val activeStudy by viewModel.activeStudy.collectAsStateWithLifecycle()
    val savedStudies by viewModel.savedStudies.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val runningRow by viewModel.activeTimingRow.collectAsStateWithLifecycle()
    val runningCol by viewModel.activeTimingCol.collectAsStateWithLifecycle()

    val activeRowIndex by viewModel.activeRowIndex.collectAsStateWithLifecycle()

    // Dialog sheets
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDeveloperDialog by remember { mutableStateOf(false) }
    var showSignatureField by remember { mutableStateOf<String?>(null) } // Name of signature field needing sign
    var showManualEditCell by remember { mutableStateOf<Pair<Int, Int>?>(null) } // row to col code
    var showDeleteConfirmStudy by remember { mutableStateOf<ProductionStudy?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Production Study Tool",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (activeStudy != null) "Draft auto-saved to local storage" else "IE Department - Apparel & Assembly Pro",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (activeStudy != null) {
                        IconButton(onClick = { viewModel.closeActiveStudy() }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close study", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "App logo", modifier = Modifier.padding(start = 12.dp, end = 8.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAboutDeveloperDialog = true },
                        modifier = Modifier.testTag("action_about_dev")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "About Developer",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.testTag("action_help_info")
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "How to use app", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    if (activeStudy != null) {
                        IconButton(
                            onClick = {
                                activeStudy?.let {
                                    PdfExporter.generateAndSharePdf(localContext, it)
                                }
                            },
                            modifier = Modifier.testTag("action_share_pdf")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share PDF as A4", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        val isStudyActive = activeStudy != null
        AnimatedContent(
            targetState = isStudyActive,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            },
            modifier = Modifier.padding(innerPadding)
        ) { active ->
            if (!active) {
                // DASHBOARD SCREEN
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth(0.96f)
                            .testTag("search_studies_input")
                            .padding(bottom = 12.dp),
                        placeholder = { 
                            Text(
                                text = "Search by Buyer, Style, Operator...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Sorting and Info Row (Responsive Sorting Box)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total: ${savedStudies.size} ${if (savedStudies.size == 1) "study" else "studies"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                        
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            val currentSort by viewModel.sortBy.collectAsStateWithLifecycle()
                            
                            AssistChip(
                                onClick = { expanded = true },
                                label = { Text("Sort: ${currentSort.displayName}") },
                                leadingIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = "Sort Icon", modifier = Modifier.size(16.dp)) },
                                trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown Arrow", modifier = Modifier.size(16.dp)) }
                            )
                            
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                SortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.displayName) },
                                        onClick = {
                                            viewModel.sortBy.value = option
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Studies list
                    if (savedStudies.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Ready to Begin Study",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Tap the plus button to create a new Production Study",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("study_list"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = savedStudies,
                                key = { it.id },
                                contentType = { "study_card" }
                            ) { item ->
                                val onSelect = remember(item.id) { { viewModel.loadStudy(item.id) } }
                                val onDelete = remember(item.id) { { showDeleteConfirmStudy = item } }
                                StudyCard(
                                    id = item.id,
                                    buyer = item.buyer,
                                    lineNo = item.lineNo,
                                    style = item.style,
                                    operationName = item.operationName,
                                    operatorName = item.operatorName,
                                    date = item.date,
                                    onSelect = onSelect,
                                    onDelete = onDelete
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Giant Start Button
                    Button(
                        onClick = { viewModel.createNewStudy() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("start_new_study_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Production Study", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // ACTIVE TIME STUDY METHOD WORKFLOW
                val currentStudy = activeStudy
                if (currentStudy != null) {
                    ActiveStudyView(
                        study = currentStudy,
                        viewModel = viewModel,
                        activeRowIndex = activeRowIndex,
                        onChangeActiveRow = { viewModel.updateActiveRowIndex(it) },
                        onSignatureClick = { field -> showSignatureField = field },
                        onCellLongClick = { r, c -> showManualEditCell = Pair(r, c) },
                        runningRow = runningRow,
                        runningCol = runningCol
                    )
                }
            }
        }
    }

    // Modal Sheet: Signature drawing pad
    showSignatureField?.let { field ->
        val title = when (field) {
            "worker" -> "Worker Signature"
            "supervisor" -> "Line Supervisor Signature"
            "chief" -> "Line Chief Signature"
            "apm" -> "APM / Floor Incharge Signature"
            "manager" -> "Production Manager Signature"
            "ie" -> "IE Executive Signature"
            else -> "Signature"
        }
        SignaturePadDialog(
            title = title,
            onDismiss = { showSignatureField = null },
            onSave = { base64 ->
                viewModel.saveSignature(field, base64)
                showSignatureField = null
            }
        )
    }

    // Modal Sheet: Manual Cell override
    showManualEditCell?.let { cell ->
        val cellTitle = getColLabel(cell.second)
        val curVal = viewModel.getCellValue(cell.first, cell.second)
        ManualEditDialog(
            cellTitle = "Row ${cell.first} - $cellTitle",
            initialValue = curVal,
            onDismiss = { showManualEditCell = null },
            onSave = { updatedTime ->
                viewModel.manuallySetCellValue(cell.first, cell.second, updatedTime)
                showManualEditCell = null
            }
        )
    }

    // Delete confirmation Dialog
    showDeleteConfirmStudy?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmStudy = null },
            title = { Text("Delete production study?") },
            text = { Text("This will permanently remove the study sheet for buyer '${item.buyer}', style '${item.style}'. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudy(item)
                        showDeleteConfirmStudy = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("delete_study_confirm_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmStudy = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modern Step-by-Step Help & Instruction Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "App Instructions Guide",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Follow these step-by-step instructions to perform a successful production time study:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    listOf(
                        Triple("1", "Create & Open Study", "Tap \"New Study\" on the dashboard, input details (Buyer, Style, Operator Name, Target, SMV) and tap \"Create Study\"."),
                        Triple("2", "Select Cell to Track", "In the active study, tap on any empty grid cell under Cycle Time (Col 1-10), Effective, or Non-Effective activities."),
                        Triple("3", "Use Stopwatch Timer", "Double-tap the selected cell to start or stop the real-time stopwatch. Alternatively, tap the corresponding button in the bottom stopwatch panel (e.g. \"Start/Stop\", \"Col 1\", \"Machine\")."),
                        Triple("4", "Manual Correction", "Made a mistake? Long-press any cell in the grid to manually key in a value using the manual edit pop-up."),
                        Triple("5", "Non-Effective Delays", "Use the dedicated \"Machine\" button for Machine Jam / breakdown delays. Other delay options like Thread break, Needle break, Waiting, or Rework are also mapped to their CTA buttons."),
                        Triple("6", "Collect Signatures", "Scroll down to the Signatures section and tap on the respective role (Worker, Line Supervisor, APM, etc.) to sign on-screen via the drawing pad."),
                        Triple("7", "Export to PDF Sheet", "Tap the Share icon at the top header or \"Share A4 PDF Sheet\" at the bottom. This generates a standardized A4 Excel-formatted Production Study Sheet PDF.")
                    ).forEach { (step, title, description) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                )
                            }
                            Column(modifier = Modifier.weight(1f).padding(top = 2.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (step != "7") {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    modifier = Modifier.testTag("help_dialog_ok_button")
                ) {
                    Text("Got It")
                }
            }
        )
    }

    // About Developer Dialog
    if (showAboutDeveloperDialog) {
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showAboutDeveloperDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "About Developer",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Developed by",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Engr. M. A. Qaiyum Talukder",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = "For any information, guidance, or tutorial support, please feel free to contact:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Contact Rows
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        uriHandler.openUri("tel:+8801912895591")
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Phone (Tap to Call)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+8801912895591",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        uriHandler.openUri("mailto:maqaiyumtalukder@gmail.com")
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Email (Tap to Send)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "maqaiyumtalukder@gmail.com",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDeveloperDialog = false },
                    modifier = Modifier.testTag("about_dialog_ok_button")
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun StudyCard(
    id: Long,
    buyer: String,
    style: String,
    lineNo: String,
    operationName: String,
    operatorName: String,
    date: String,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("study_card_${id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (buyer.isNotEmpty()) buyer else "Unknown Buyer",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Line ${lineNo.ifEmpty { "N/A" }}") }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Style: ${style.ifEmpty { "Unspecified" }} | Op: ${operationName.ifEmpty { "Unspecified" }}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = operatorName.ifEmpty { "No name" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_study_${id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Study",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveStudyView(
    study: ProductionStudy,
    viewModel: ProductionStudyViewModel,
    activeRowIndex: Int,
    onChangeActiveRow: (Int) -> Unit,
    onSignatureClick: (String) -> Unit,
    onCellLongClick: (Int, Int) -> Unit,
    runningRow: Int?,
    runningCol: Int?
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val rowMap = remember(study.rows) { study.rows.associateBy { it.rowId } }
    val runningTimeFlow = viewModel.runningElapsedSecs

    val onCellClick = remember(viewModel, onChangeActiveRow, haptic) {
        { r: Int, c: Int ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            when (c) {
                in 0..9 -> com.example.ui.audio.AudioAlertsManager.playCaptureTone()
                10, 11 -> com.example.ui.audio.AudioAlertsManager.playEffectiveTone()
                in 12..18 -> com.example.ui.audio.AudioAlertsManager.playNonEffectiveTone()
            }
            viewModel.handleCellClick(r, c)
            onChangeActiveRow(r)
        }
    }
    val onCellLongClickRemembered = remember(onCellLongClick, haptic) {
        { r: Int, c: Int ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCellLongClick(r, c)
        }
    }
    val onCellDoubleClickRemembered = remember(viewModel, haptic) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            com.example.ui.audio.AudioAlertsManager.playCaptureTone()
            viewModel.handleCellDoubleClick()
        }
    }

    // Toggle states for responsive collapsible sections on constrained screen heights, collected from viewModel
    val headerExpanded by viewModel.headerExpanded.collectAsStateWithLifecycle()
    val sheetExpanded by viewModel.sheetExpanded.collectAsStateWithLifecycle()
    val stopwatchExpanded by viewModel.stopwatchExpanded.collectAsStateWithLifecycle()
    val metricsExpanded by viewModel.metricsExpanded.collectAsStateWithLifecycle()
    val hourlyExpanded by viewModel.hourlyExpanded.collectAsStateWithLifecycle()
    val signaturesExpanded by viewModel.signaturesExpanded.collectAsStateWithLifecycle()
    val remarksExpanded by viewModel.remarksExpanded.collectAsStateWithLifecycle()
    var showTotalTimeEditDialog by remember { mutableStateOf(false) }
    var manualTotalTimeText by remember { mutableStateOf("") }

    var smvInputText by remember(study.id) {
        mutableStateOf(if (study.smv > 0.0) study.smv.toString() else "")
    }
    var prevBestInputText by remember(study.id) {
        mutableStateOf(if (study.previousBestAchieved > 0.0) study.previousBestAchieved.toString() else "")
    }

    LaunchedEffect(study.id, study.smv) {
        val currentVal = smvInputText.toDoubleOrNull() ?: 0.0
        if (currentVal != study.smv) {
            smvInputText = if (study.smv > 0.0) study.smv.toString() else ""
        }
    }
    LaunchedEffect(study.id, study.previousBestAchieved) {
        val currentVal = prevBestInputText.toDoubleOrNull() ?: 0.0
        if (currentVal != study.previousBestAchieved) {
            prevBestInputText = if (study.previousBestAchieved > 0.0) study.previousBestAchieved.toString() else ""
        }
    }

    var completedRowsLastSeen by remember(study.id) {
        mutableStateOf(study.rows.filter { r -> r.cycleTimes.all { it > 0.0 } }.map { it.rowId }.toSet())
    }

    LaunchedEffect(study.rows) {
        val currentCompleted = study.rows.filter { r -> r.cycleTimes.all { it > 0.0 } }.map { it.rowId }.toSet()
        val newlyCompleted = currentCompleted - completedRowsLastSeen
        if (newlyCompleted.contains(activeRowIndex)) {
            if (activeRowIndex < 20) {
                onChangeActiveRow(activeRowIndex + 1)
            }
        }
        completedRowsLastSeen = currentCompleted
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Status indicator: Draft Auto-Saved
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Auto Save Status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Draft auto-saved locally. Safe from browser refresh.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Section 1: Study Metadata Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateHeaderExpanded(!headerExpanded) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "1. HEADER INFORMATION",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!headerExpanded) {
                                val infoText = listOf(
                                    study.buyer.ifEmpty { "No Buyer" },
                                    study.style.ifEmpty { "No Style" },
                                    study.lineNo.ifEmpty { "No Line" }
                                ).joinToString(" | ")
                                Text(
                                    text = infoText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (headerExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (headerExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = headerExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 0: Factory Name
                            ProductionStudyTextField(
                                value = study.factoryName,
                                onValueChange = { viewModel.updateMetadata(factoryName = it) },
                                label = "Factory Name",
                                leadingIcon = Icons.Default.Build,
                                testTag = "meta_factory_name"
                            )

                            // Row 1: Buyer & Style
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProductionStudyTextField(
                                    value = study.buyer,
                                    onValueChange = { viewModel.updateMetadata(buyer = it) },
                                    label = "Buyer",
                                    modifier = Modifier.weight(1f),
                                    testTag = "meta_buyer",
                                    leadingIcon = Icons.Default.Person
                                )
                                ProductionStudyTextField(
                                    value = study.style,
                                    onValueChange = { viewModel.updateMetadata(style = it) },
                                    label = "Style",
                                    modifier = Modifier.weight(1f),
                                    testTag = "meta_style",
                                    leadingIcon = Icons.Default.Info
                                )
                            }

                            // Row 2: Line No, Unit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProductionStudyTextField(
                                    value = study.lineNo,
                                    onValueChange = { viewModel.updateMetadata(lineNo = it) },
                                    label = "Line No",
                                    modifier = Modifier.weight(1f),
                                    testTag = "meta_line",
                                    leadingIcon = Icons.AutoMirrored.Filled.List
                                )
                                ProductionStudyTextField(
                                    value = study.unit,
                                    onValueChange = { viewModel.updateMetadata(unit = it) },
                                    label = "Unit",
                                    modifier = Modifier.weight(1f),
                                    testTag = "meta_unit",
                                    leadingIcon = Icons.Default.Home
                                )
                            }

                            // Row 3: Date, Study By
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProductionStudyTextField(
                                    value = study.date,
                                    onValueChange = { viewModel.updateMetadata(date = it) },
                                    label = "Date",
                                    modifier = Modifier.weight(1f),
                                    testTag = "meta_date",
                                    leadingIcon = Icons.Default.DateRange
                                )
                                ProductionStudyTextField(
                                    value = study.studyBy,
                                    onValueChange = { viewModel.updateMetadata(studyBy = it) },
                                    label = "Study By",
                                    modifier = Modifier.weight(1f),
                                    testTag = "meta_study_by",
                                    leadingIcon = Icons.Default.Edit
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Full width: Operation Name
                            ProductionStudyTextField(
                                value = study.operationName,
                                onValueChange = { viewModel.updateMetadata(operationName = it) },
                                label = "Operation Name",
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "meta_operation",
                                leadingIcon = Icons.Default.Build
                            )

                            // Full width: Worker Name / No.
                            ProductionStudyTextField(
                                value = study.operatorName,
                                onValueChange = { viewModel.updateMetadata(operatorName = it) },
                                label = "Worker Name / No.",
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "meta_operator",
                                leadingIcon = Icons.Default.Face
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Row 4: SMV & Prev Best Achieved side-by-side
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProductionStudyTextField(
                                    value = smvInputText,
                                    onValueChange = { input ->
                                        // Allow only digits and decimal points
                                        val filtered = input.filter { it.isDigit() || it == '.' }
                                        val dotCount = filtered.count { it == '.' }
                                        val cleanInput = if (dotCount > 1) {
                                            val firstDotIndex = filtered.indexOf('.')
                                            filtered.filterIndexed { index, char -> char != '.' || index == firstDotIndex }
                                        } else {
                                            filtered
                                        }
                                        smvInputText = cleanInput
                                        val parseString = if (cleanInput.startsWith(".")) "0$cleanInput" else cleanInput
                                        val parsed = parseString.toDoubleOrNull()
                                        if (parsed != null) {
                                            viewModel.updateMetadata(smv = parsed)
                                        } else if (cleanInput.isEmpty() || cleanInput == "." || cleanInput.all { it == '0' || it == '.' }) {
                                            viewModel.updateMetadata(smv = 0.0)
                                        }
                                    },
                                    label = "SMV",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(0.9f),
                                    testTag = "meta_smv",
                                    leadingIcon = Icons.Default.Settings
                                )
                                ProductionStudyTextField(
                                    value = prevBestInputText,
                                    onValueChange = { input ->
                                        // Allow only digits and decimal points
                                        val filtered = input.filter { it.isDigit() || it == '.' }
                                        val dotCount = filtered.count { it == '.' }
                                        val cleanInput = if (dotCount > 1) {
                                            val firstDotIndex = filtered.indexOf('.')
                                            filtered.filterIndexed { index, char -> char != '.' || index == firstDotIndex }
                                        } else {
                                            filtered
                                        }
                                        prevBestInputText = cleanInput
                                        val parseString = if (cleanInput.startsWith(".")) "0$cleanInput" else cleanInput
                                        val parsed = parseString.toDoubleOrNull()
                                        if (parsed != null) {
                                            viewModel.updateMetadata(previousBestAchieved = parsed)
                                        } else if (cleanInput.isEmpty() || cleanInput == "." || cleanInput.all { it == '0' || it == '.' }) {
                                            viewModel.updateMetadata(previousBestAchieved = 0.0)
                                        }
                                    },
                                    label = "Prev Best (pcs/Hr)",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1.1f),
                                    testTag = "meta_prev_best",
                                    leadingIcon = Icons.Default.Star
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Elegant, high-fidelity Timeline & Session Duration Section
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.04f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Title line with icon & auto recalc indicator
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                             Icon(
                                                 imageVector = Icons.Default.PlayArrow, // Timeline / Play symbol
                                                 contentDescription = null,
                                                 tint = MaterialTheme.colorScheme.primary,
                                                 modifier = Modifier.size(16.dp)
                                             )
                                             Text(
                                                 text = "STUDY TIMELINE & DURATION",
                                                 style = MaterialTheme.typography.labelMedium,
                                                 fontWeight = FontWeight.Bold,
                                                 color = MaterialTheme.colorScheme.primary
                                             )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "AUTO CALC ACTIVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Side-by-side Start & End Times (With empty label to prevent squeezing/outline text wrapper vertical stretching)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Start Time
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Start Time",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                            ProductionStudyTextField(
                                                 value = study.startTime,
                                                 onValueChange = { viewModel.updateMetadata(startTime = it) },
                                                 label = "", // Empty to disable vertical label wrapping
                                                 modifier = Modifier.fillMaxWidth(),
                                                 testTag = "meta_start_time",
                                                 singleLine = true,
                                                 trailingIcon = Icons.Default.Refresh,
                                                 onTrailingIconClick = { viewModel.updateMetadata(startTime = viewModel.getCurrentFormattedTime()) },
                                                 onClickField = { viewModel.updateMetadata(startTime = viewModel.getCurrentFormattedTime()) }
                                            )
                                        }

                                        // End Time
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "End Time",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                            ProductionStudyTextField(
                                                 value = study.endTime,
                                                 onValueChange = { viewModel.updateMetadata(endTime = it) },
                                                 label = "", // Empty to disable vertical label wrapping
                                                 modifier = Modifier.fillMaxWidth(),
                                                 testTag = "meta_end_time",
                                                 singleLine = true,
                                                 keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                 trailingIcon = Icons.Default.Refresh,
                                                 onTrailingIconClick = { viewModel.updateMetadata(endTime = viewModel.getCurrentFormattedTime()) },
                                                 onClickField = { viewModel.updateMetadata(endTime = viewModel.getCurrentFormattedTime()) }
                                            )
                                        }
                                    }

                                    // Total Study Duration Highlight Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                             .background(
                                                 color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f),
                                                 shape = RoundedCornerShape(8.dp)
                                             )
                                             .border(
                                                 width = 1.dp,
                                                 color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                                 shape = RoundedCornerShape(8.dp)
                                             )
                                             .clickable {
                                                 manualTotalTimeText = if (study.totalStudyTime > 0.0) study.totalStudyTime.toString() else ""
                                                 showTotalTimeEditDialog = true
                                             }
                                             .padding(vertical = 8.dp, horizontal = 12.dp),
                                         horizontalArrangement = Arrangement.SpaceBetween,
                                         verticalAlignment = Alignment.CenterVertically
                                    ) {
                                         Column {
                                             Text(
                                                 text = "Total Study Time",
                                                 style = MaterialTheme.typography.labelMedium,
                                                 fontWeight = FontWeight.SemiBold,
                                                 color = MaterialTheme.colorScheme.primary
                                             )
                                             Text(
                                                 text = "total time = end time - start time",
                                                 style = MaterialTheme.typography.bodySmall,
                                                 color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                             )
                                         }

                                         Row(
                                             verticalAlignment = Alignment.CenterVertically,
                                             horizontalArrangement = Arrangement.spacedBy(6.dp)
                                         ) {
                                             Text(
                                                 text = "${String.format(java.util.Locale.US, "%.1f", study.totalStudyTime)} mins",
                                                 style = MaterialTheme.typography.titleMedium,
                                                 fontWeight = FontWeight.Black,
                                                 color = MaterialTheme.colorScheme.primary
                                             )
                                             Icon(
                                                 imageVector = Icons.Default.Edit,
                                                 contentDescription = "Edit Manual Override",
                                                 tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                 modifier = Modifier.size(16.dp)
                                             )
                                         }
                                    }
                                }
                            }

                            // Render manual total time override pop-up
                            if (showTotalTimeEditDialog) {
                                 AlertDialog(
                                     onDismissRequest = { showTotalTimeEditDialog = false },
                                     title = { Text("Edit Total Study Time") },
                                     text = {
                                         Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                             Text("Enter manual total duration in minutes:", style = MaterialTheme.typography.bodyMedium)
                                             OutlinedTextField(
                                                 value = manualTotalTimeText,
                                                 onValueChange = { manualTotalTimeText = it },
                                                 keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                 singleLine = true,
                                                 modifier = Modifier.fillMaxWidth(),
                                                 placeholder = { Text("e.g. 30.0") }
                                             )
                                         }
                                     },
                                     confirmButton = {
                                         TextButton(
                                             onClick = {
                                                 val mins = manualTotalTimeText.toDoubleOrNull() ?: 0.0
                                                 viewModel.updateMetadata(totalStudyTime = mins)
                                                 showTotalTimeEditDialog = false
                                             }
                                         ) {
                                             Text("Save")
                                         }
                                     },
                                     dismissButton = {
                                         TextButton(onClick = { showTotalTimeEditDialog = false }) {
                                             Text("Cancel")
                                         }
                                     }
                                 )
                            }


                        }
                    }
                }
            }
        }

        // Section 2: physical time-study matrix layout (Overview grid)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateSheetExpanded(!sheetExpanded) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "2. CYCLE TIME SHEET OVERVIEW",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (sheetExpanded) "Scroll Horizontally/Vertically. Hold cell to manually edit." else "Expand to view all 20 rows of cycles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (sheetExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (sheetExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = sheetExpanded) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            // Scrollable container with fixed max height constraint to fit beautifully on compact phone displays
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .heightIn(max = 220.dp)
                                    ) {
                                        // Column Headers Row
                                        item {
                                            Row(modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer)) {
                                                GridHeaderCell("Row", 45)
                                                (1..10).forEach { GridHeaderCell("C$it", 50) }
                                                GridHeaderCell("Bundle\n(Eff)", 75)
                                                GridHeaderCell("Bobbin\n(Eff)", 75)
                                                GridHeaderCell("Thread\n(Non)", 75)
                                                GridHeaderCell("Needle\n(Non)", 75)
                                                GridHeaderCell("Mach.\n(Non)", 75)
                                                GridHeaderCell("Wait\n(Non)", 75)
                                                GridHeaderCell("Rework\n(Non)", 75)
                                                GridHeaderCell("Pers.\n(Non)", 75)
                                                GridHeaderCell("Other\n(Non)", 75)
                                            }
                                        }

                                        // 20 Body Rows
                                        items(
                                            count = 20,
                                            key = { it }
                                        ) { index ->
                                            val rId = index + 1
                                            val rowObj = rowMap[rId] ?: StudyRow(rowId = rId)
                                            val isSelectedRow = rId == activeRowIndex
                                            val isRowRunning = runningRow == rId
                                            
                                            StudyGridRow(
                                                rowObj = rowObj,
                                                isSelectedRow = isSelectedRow,
                                                runningCol = runningCol,
                                                isRowRunning = isRowRunning,
                                                onRowClick = onChangeActiveRow,
                                                onCellClick = onCellClick,
                                                onCellDoubleClick = onCellDoubleClickRemembered,
                                                onCellLongClick = onCellLongClickRemembered,
                                                runningTimeFlow = runningTimeFlow
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

        // Section 3: STOPWATCH CONTROL COMMAND DOCK & COMMAND PAD (Crucial for mobile execution)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Stopwatch header & Active information
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "3. STOPWATCH INPUT COMMAND DOCK",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.clickable { viewModel.updateStopwatchExpanded(!stopwatchExpanded) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Active Row: ",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = activeRowIndex.toString(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                // Tiny row switch indicators
                                IconButton(
                                    onClick = { if (activeRowIndex > 1) onChangeActiveRow(activeRowIndex - 1) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Prev row", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                IconButton(
                                    onClick = { if (activeRowIndex < 20) onChangeActiveRow(activeRowIndex + 1) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Next row", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }

                        // Giant ticking number display (safeguarded against vertical squish with wrap layout)
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(2.dp).wrapContentWidth()
                        ) {
                            RunningTimeText(
                                runningRow = runningRow,
                                runningTimeFlow = runningTimeFlow
                            )
                        }
                    }

                    AnimatedVisibility(visible = stopwatchExpanded) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            if (runningRow != null && runningCol != null) {
                                val activeLabel = "Ticking Row $runningRow - ${getColLabel(runningCol)}"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = activeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            com.example.ui.audio.AudioAlertsManager.playCaptureTone()
                                            viewModel.stopTimerAndRecordEndTime()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("btn_stop_timer")
                                    ) {
                                        Text("Stop Timer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // 10 CYCLES BUTTONS (Productive timing)
                            Text("CYCLE TIME SELECTION:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Simple grid for 10 cycles, beautifully reactive and padded
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    (0..4).forEach { colCode ->
                                        CycleStopwatchButton(
                                            label = "C${colCode + 1}",
                                            isActive = runningRow == activeRowIndex && runningCol == colCode,
                                            isFilled = viewModel.getCellValue(activeRowIndex, colCode) > 0.0,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                com.example.ui.audio.AudioAlertsManager.playCaptureTone()
                                                viewModel.handleCellClick(activeRowIndex, colCode)
                                            }
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    (5..9).forEach { colCode ->
                                        CycleStopwatchButton(
                                            label = "C${colCode + 1}",
                                            isActive = runningRow == activeRowIndex && runningCol == colCode,
                                            isFilled = viewModel.getCellValue(activeRowIndex, colCode) > 0.0,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                com.example.ui.audio.AudioAlertsManager.playCaptureTone()
                                                viewModel.handleCellClick(activeRowIndex, colCode)
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // EFFECTIVE & NON EFFECTIVE COMMAND BUTTONS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Effective card columns (Bundle, Bobbin) - utilizing material styling properly
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text("EFFECTIVE:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    CycleStopwatchButton(
                                        label = "Bundle (Eff)",
                                        isActive = runningCol == 10,
                                        isFilled = viewModel.getCellValue(activeRowIndex, 10) > 0.0,
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        activeColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp),
                                        onClick = {
                                            com.example.ui.audio.AudioAlertsManager.playEffectiveTone()
                                            viewModel.handleCellClick(activeRowIndex, 10)
                                        }
                                    )
                                    CycleStopwatchButton(
                                        label = "Bobbin (Eff)",
                                        isActive = runningCol == 11,
                                        isFilled = viewModel.getCellValue(activeRowIndex, 11) > 0.0,
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        activeColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            com.example.ui.audio.AudioAlertsManager.playEffectiveTone()
                                            viewModel.handleCellClick(activeRowIndex, 11)
                                        }
                                    )
                                }

                                // Non effective activities (Thread break, needle break... etc) - utilizing proper theme colors to ensure readability in dark/light mode
                                Column(modifier = Modifier.weight(3.5f)) {
                                    Text("NON-EFFECTIVE:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    val listNonEff = listOf(
                                        Triple("Thread", 12, "Thread Break"),
                                        Triple("Needle", 13, "Needle Break"),
                                        Triple("Machine", 14, "Machine"),
                                        Triple("Wait", 15, "Waiting / Floor"),
                                        Triple("Rework", 16, "Rework"),
                                        Triple("Fatig", 17, "Personal / Fatigue"),
                                        Triple("Other", 18, "Others")
                                    )
                                    
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp), 
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listNonEff.forEach { triple ->
                                            CycleStopwatchButton(
                                                label = triple.first,
                                                isActive = runningCol == triple.second,
                                                isFilled = viewModel.getCellValue(activeRowIndex, triple.second) > 0.0,
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                activeColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.widthIn(max = 78.dp),
                                                onClick = {
                                                    com.example.ui.audio.AudioAlertsManager.playNonEffectiveTone()
                                                    viewModel.handleCellClick(activeRowIndex, triple.second)
                                                }
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

        // Section 4: AUTOMATIC CALCULATIONS RESULTS PANEL (Adaptive & Collapsible)
        item {
            val calc = remember(study) { calculateMetrics(study) }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateMetricsExpanded(!metricsExpanded) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "4. AUTOMATIC CALCULATION METRICS",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!metricsExpanded) {
                                Text(
                                    text = "Target Imp: ${String.format(Locale.US, "%.1f pcs", calc.targetImprovement)} | Error: ${String.format(Locale.US, "%.1f%%", calc.errorPercent)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (metricsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (metricsExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = metricsExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CalculationResultRow("Total Productive Time", String.format(Locale.US, "%.1f seconds", calc.cycleTimesSum), isHighlight = true)
                            CalculationResultRow("Average Capacity Time", String.format(Locale.US, "%.1f seconds (per cycle)", calc.avgCapacityTime))
                            CalculationResultRow("Capacity Target", String.format(Locale.US, "%.1f pcs/Hr", calc.capacityTarget))
                            CalculationResultRow("Target Improvement", String.format(Locale.US, "%.1f pcs", calc.targetImprovement))
                            CalculationResultRow("Improvement %", String.format(Locale.US, "%.2f %%", calc.improvementPercent))
                            CalculationResultRow("Total Effective Time (Bundle+Bob)", String.format(Locale.US, "%.1f s", calc.totalEffective))
                            CalculationResultRow("Total Non productive Time", String.format(Locale.US, "%.1f seconds", calc.totalNonProductiveTime))
                            CalculationResultRow("Total Observe Time", String.format(Locale.US, "%.1f seconds", calc.totalObserveTime))
                            CalculationResultRow("Error %", String.format(Locale.US, "%.2f %%", calc.errorPercent), isAlert = calc.errorPercent > 10.0 || calc.errorPercent < -10.0)
                        }
                    }
                }
            }
        }

        // Section 5: Hourly output tracking inputs post study (8 Hours)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateHourlyExpanded(!hourlyExpanded) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "5. HOURLY PRODUCTION AFTER STUDY",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val totalHourProd = study.hourlyProduction.sum()
                            Text(
                                text = if (hourlyExpanded) "Input pieces produced for hours 1 to 8" else "Total Post Study Production: $totalHourProd pcs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (hourlyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (hourlyExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = hourlyExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = "Input pieces produced in hours 1 to 8 to identify post-study metrics:",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                (0..7).forEach { index ->
                                    val hourVal = study.hourlyProduction.getOrNull(index) ?: 0
                                    var inputText by remember(study.id, index) {
                                        mutableStateOf(if (hourVal > 0) hourVal.toString() else "")
                                    }
                                    LaunchedEffect(study.id, hourVal) {
                                        val currentParsed = inputText.toIntOrNull() ?: 0
                                        if (currentParsed != hourVal) {
                                            inputText = if (hourVal > 0) hourVal.toString() else ""
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.width(90.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Hour ${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = inputText,
                                            onValueChange = { newValue ->
                                                val filtered = newValue.filter { it.isDigit() }
                                                inputText = filtered
                                                val parsedInt = filtered.toIntOrNull() ?: 0
                                                viewModel.updateHourlyProduction(index, parsedInt)
                                            },
                                            placeholder = { Text("-") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                            singleLine = true,
                                            modifier = Modifier.testTag("hourly_prod_$index")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 6: IE signatures fields
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateSignaturesExpanded(!signaturesExpanded) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "6. SIGNATURE AUTHORIZATIONS",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val signedCount = listOf(
                                study.workerSignature, study.lineSupervisorSignature, study.lineChiefSignature,
                                study.apmFloorInchargeSignature, study.productionMgrSignature, study.ieExecutiveSignature
                            ).count { it.isNotEmpty() }
                            Text(
                                text = if (signaturesExpanded) "Draw and save legal signatures" else "Authorized Sigs: $signedCount of 6 signed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (signaturesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (signaturesExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = signaturesExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SignatureWidget("Worker Signature", study.workerSignature, modifier = Modifier.weight(1f), onClick = { onSignatureClick("worker") }, onClear = { viewModel.clearSignature("worker") })
                                SignatureWidget("Line Supervisor", study.lineSupervisorSignature, modifier = Modifier.weight(1f), onClick = { onSignatureClick("supervisor") }, onClear = { viewModel.clearSignature("supervisor") })
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SignatureWidget("Line Chief", study.lineChiefSignature, modifier = Modifier.weight(1f), onClick = { onSignatureClick("chief") }, onClear = { viewModel.clearSignature("chief") })
                                SignatureWidget("APM/Floor Incharge", study.apmFloorInchargeSignature, modifier = Modifier.weight(1f), onClick = { onSignatureClick("apm") }, onClear = { viewModel.clearSignature("apm") })
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SignatureWidget("Production Mgr", study.productionMgrSignature, modifier = Modifier.weight(1f), onClick = { onSignatureClick("manager") }, onClear = { viewModel.clearSignature("manager") })
                                SignatureWidget("IE Executive", study.ieExecutiveSignature, modifier = Modifier.weight(1f), onClick = { onSignatureClick("ie") }, onClear = { viewModel.clearSignature("ie") })
                            }
                        }
                    }
                }
            }
        }

        // Section 7: Remarks Input Comment
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateRemarksExpanded(!remarksExpanded) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "7. FIELD WORKS REMARKS",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (remarksExpanded) "Study Remarks / Action Items" else if (study.remarks.isNotEmpty()) study.remarks else "No remarks entered",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (remarksExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (remarksExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = remarksExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            OutlinedTextField(
                                value = study.remarks,
                                onValueChange = { viewModel.updateMetadata(remarks = it) },
                                label = { Text("Study Remarks / Action Items") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .testTag("meta_remarks"),
                                maxLines = 4
                            )
                        }
                    }
                }
            }
        }

        // Generate export and sharing buttons dynamically responsive across mobile viewports
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Share PDF
                Button(
                    onClick = { PdfExporter.generateAndSharePdf(viewModel.getApplication(), study) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_share_export_pdf_footer"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Share PDF",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Button 2: Share Excel
                Button(
                    onClick = { ExcelExporter.generateAndShareExcel(context, study) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_share_export_excel_footer"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Share Excel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Button 3: Share Text
                Button(
                    onClick = {
                        val cycleTimesSum = study.rows.flatMap { it.cycleTimes }.filter { it > 0.0 }.sum()
                        val totalProduced = study.calculatedTotalProducedPcs
                        val previousBest = study.previousBestAchieved
                        val targetImprovement = totalProduced.toDouble() - previousBest
                        val improvementPercent = if (totalProduced > 0) {
                            (targetImprovement / totalProduced.toDouble()) * 100.0
                        } else {
                            0.0
                        }
                        val avgCapacityTime = if (totalProduced > 0) cycleTimesSum / totalProduced else 0.0

                        val whatsappSummary = """
*=== 📋 PRODUCTION STUDY SUMMARY ===*

🏭 *Factory:* ${study.factoryName.ifEmpty { "N/A" }}
👤 *Operator:* ${study.operatorName.ifEmpty { "N/A" }}
👗 *Style:* ${study.style.ifEmpty { "N/A" }}
🔧 *Operation:* ${study.operationName.ifEmpty { "N/A" }}
📅 *Date:* ${study.date}
⏱️ *Total Study Time:* ${study.totalStudyTime} mins

*📊 PERFORMANCE STATS SUMMARY:*
----------------------------------------
🔴 *Previous Best Achieved:* *${String.format(Locale.US, "%.1f", previousBest)} pcs/Hr*
🟢 *Total Produced After Study:* *${totalProduced} pcs*
🟢 *Improvement:* *${String.format(Locale.US, "%.1f", targetImprovement)} pcs*
🟢 *Improvement %:* *${String.format(Locale.US, "%.2f%%", improvementPercent)}*
⚡ *Avg. Capacity Time:* ${String.format(Locale.US, "%.1f secs", avgCapacityTime)}
🎯 *Capacity Target:* ${String.format(Locale.US, "%.1f pcs/Hr", study.calculatedCapacityTarget)}

📈 *Hourly Production List:*
${study.hourlyProduction.mapIndexed { index, i -> "• Hour ${index + 1}: ${if (i > 0) "$i pcs" else "-"}" }.joinToString("\n")}

📝 *Remarks:* ${study.remarks.ifEmpty { "None" }}
                        """.trimIndent()

                        try {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, whatsappSummary)
                            }
                            val chooser = Intent.createChooser(intent, "Share WhatsApp Text Summary:")
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(chooser)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_share_whatsapp_text"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // WhatsApp Green
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Share Text",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Low-level helper components
@Composable
fun GridHeaderCell(label: String, widthDp: Int) {
    Box(
        modifier = Modifier
            .size(width = widthDp.dp, height = 38.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 11.sp
        )
    }
}

@Composable
fun RunningTimeText(
    runningRow: Int?,
    runningTimeFlow: StateFlow<Double>
) {
    val currentTimerValue by runningTimeFlow.collectAsStateWithLifecycle()
    Text(
        text = if (runningRow != null) String.format(Locale.US, "%.1f s", currentTimerValue) else "0.0 s",
        style = MaterialTheme.typography.titleLarge,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        color = if (runningRow != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
    )
}

@Composable
fun TickingCellText(
    runningTimeFlow: StateFlow<Double>
) {
    val activeValue by runningTimeFlow.collectAsStateWithLifecycle()
    Text(
        text = String.format(Locale.US, "%.1f", activeValue),
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        color = Color.Red,
        textAlign = TextAlign.Center
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimeGridCell(
    value: Double,
    isTiming: Boolean,
    width: Int,
    textColor: Color = Color.Unspecified,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
    runningTimeFlow: StateFlow<Double>? = null
) {
    val haptic = LocalHapticFeedback.current
    // Standard timing highlight color
    val animBgColor = if (isTiming) {
        Color(255, 215, 215)
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(width = width.dp, height = 34.dp)
            .background(animBgColor)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
                onDoubleClick = if (onDoubleClick != null) {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDoubleClick()
                    }
                } else null
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isTiming && runningTimeFlow != null) {
            TickingCellText(runningTimeFlow = runningTimeFlow)
        } else {
            val emptyString = if (value > 0.0) String.format(Locale.US, "%.1f", value) else ""
            Text(
                text = emptyString,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StudyGridRow(
    rowObj: com.example.data.model.StudyRow,
    isSelectedRow: Boolean,
    runningCol: Int?,
    isRowRunning: Boolean,
    onRowClick: (Int) -> Unit,
    onCellClick: (Int, Int) -> Unit,
    onCellDoubleClick: () -> Unit,
    onCellLongClick: (Int, Int) -> Unit,
    runningTimeFlow: StateFlow<Double>
) {
    Row(
        modifier = Modifier
            .background(if (isSelectedRow) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
            .clickable { onRowClick(rowObj.rowId) }
    ) {
        // Row index cell
        Box(
            modifier = Modifier
                .size(width = 45.dp, height = 34.dp)
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rowObj.rowId.toString(),
                fontWeight = if (isSelectedRow) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelectedRow) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
        }

        // Cycle cells 1..10
        (0..9).forEach { cIdx ->
            val valTime = rowObj.cycleTimes[cIdx]
            val isCurrentTiming = isRowRunning && runningCol == cIdx
            
            TimeGridCell(
                value = valTime,
                isTiming = isCurrentTiming,
                width = 50,
                onClick = { onCellClick(rowObj.rowId, cIdx) },
                onDoubleClick = onCellDoubleClick,
                onLongClick = { onCellLongClick(rowObj.rowId, cIdx) },
                runningTimeFlow = runningTimeFlow
            )
        }

        // Effective: Bundle 10, Bobbin 11
        TimeGridCell(
            value = rowObj.effectiveBundleHandling,
            isTiming = isRowRunning && runningCol == 10,
            width = 75,
            textColor = MaterialTheme.colorScheme.primary,
            onClick = { onCellClick(rowObj.rowId, 10) },
            onDoubleClick = onCellDoubleClick,
            onLongClick = { onCellLongClick(rowObj.rowId, 10) },
            runningTimeFlow = runningTimeFlow
        )
        TimeGridCell(
            value = rowObj.effectiveBobbinChange,
            isTiming = isRowRunning && runningCol == 11,
            width = 75,
            textColor = MaterialTheme.colorScheme.primary,
            onClick = { onCellClick(rowObj.rowId, 11) },
            onDoubleClick = onCellDoubleClick,
            onLongClick = { onCellLongClick(rowObj.rowId, 11) },
            runningTimeFlow = runningTimeFlow
        )

        // Non effective: 12..18
        (12..18).forEach { colCode ->
            val valTimeNon = when (colCode) {
                12 -> rowObj.nonEffectiveThreadBreakage
                13 -> rowObj.nonEffectiveNeedleBreakage
                14 -> rowObj.nonEffectiveMachineBreakdown
                15 -> rowObj.nonEffectiveWaitingForWork
                16 -> rowObj.nonEffectiveRework
                17 -> rowObj.nonEffectivePersonalFatigue
                18 -> rowObj.nonEffectiveOthers
                else -> 0.0
            }
            val isCurrentTimingNon = isRowRunning && runningCol == colCode
            
            TimeGridCell(
                value = valTimeNon,
                isTiming = isCurrentTimingNon,
                width = 75,
                textColor = MaterialTheme.colorScheme.error,
                onClick = { onCellClick(rowObj.rowId, colCode) },
                onDoubleClick = onCellDoubleClick,
                onLongClick = { onCellLongClick(rowObj.rowId, colCode) },
                runningTimeFlow = runningTimeFlow
            )
        }
    }
}

@Composable
fun CycleStopwatchButton(
    label: String,
    isActive: Boolean,
    isFilled: Boolean,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    activeColor: Color = MaterialTheme.colorScheme.error,
    contentColor: Color? = null,
    activeContentColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val actualContentColor = if (isActive) {
        activeContentColor
    } else {
        contentColor ?: if (isFilled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    }

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) activeColor else if (isFilled) color.copy(alpha = 0.85f) else color,
            contentColor = actualContentColor
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = modifier
            .height(44.dp)
            .border(
                width = if (isActive) 2.dp else if (isFilled) 1.5.dp else 1.dp,
                color = if (isActive) Color.White else if (isFilled) actualContentColor.copy(alpha = 0.8f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CalculationResultRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    isAlert: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isHighlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else if (isAlert) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isAlert) MaterialTheme.colorScheme.error else if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SignatureWidget(
    title: String,
    base64Image: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFFCFCFC))
                .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (base64Image.isNotEmpty()) {
                val bitmapValue = remember(base64Image) {
                    SignatureHelper.decodeBase64ToBitmap(base64Image)
                }
                if (bitmapValue != null) {
                    Image(
                        bitmap = bitmapValue.asImageBitmap(),
                        contentDescription = "Signed image for $title",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(imageVector = Icons.Default.Create, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Tap to Sign", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (base64Image.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onClear,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("Clear sign", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// Label resolver for columns Code values
fun getColLabel(code: Int): String {
    return when (code) {
        in 0..9 -> "Cycle ${code + 1}"
        10 -> "Bundle handling"
        11 -> "Bobbin change"
        12 -> "Thread break"
        13 -> "Needle break"
        14 -> "Machine breakdown"
        15 -> "Waiting for work"
        16 -> "Rework"
        17 -> "Personal / Fatigue"
        18 -> "Others"
        else -> "Unknown"
    }
}

// Mathematical calculation classes for production metrics
data class StudyCalculations(
    val cycleTimesSum: Double = 0.0,
    val avgCapacityTime: Double = 0.0,
    val capacityTarget: Double = 0.0,
    val targetImprovement: Double = 0.0,
    val improvementPercent: Double = 0.0,
    val totalEffective: Double = 0.0,
    val totalNonProductiveTime: Double = 0.0,
    val totalObserveTime: Double = 0.0,
    val errorPercent: Double = 0.0
)

fun calculateMetrics(study: ProductionStudy): StudyCalculations {
    var cycleTimesSum = 0.0
    var bundleSum = 0.0
    var bobbinSum = 0.0
    var nonProductiveSum = 0.0

    study.rows.forEach { row ->
        cycleTimesSum += row.cycleTimes.sum()
        bundleSum += row.effectiveBundleHandling
        bobbinSum += row.effectiveBobbinChange
        nonProductiveSum += row.nonEffectiveThreadBreakage +
                row.nonEffectiveNeedleBreakage +
                row.nonEffectiveMachineBreakdown +
                row.nonEffectiveWaitingForWork +
                row.nonEffectiveRework +
                row.nonEffectivePersonalFatigue +
                row.nonEffectiveOthers
    }

    val totalEffective = cycleTimesSum + bundleSum + bobbinSum
    val totalObserveTime = totalEffective + nonProductiveSum

    val totalProducePcs = study.calculatedTotalProducedPcs.toDouble()
    val avgCapacityTime = if (totalProducePcs > 0) cycleTimesSum / totalProducePcs else 0.0

    // Target Improvement = Total Production during study - Previous Best achieved
    val targetImprovement = totalProducePcs - study.previousBestAchieved
    val improvementPercent = if (totalProducePcs > 0.0) {
        (targetImprovement / totalProducePcs) * 100.0
    } else {
        0.0
    }

    val manualTotalTimeSecs = study.totalStudyTime * 60.0
    val errorPercent = if (manualTotalTimeSecs > 0) {
        ((manualTotalTimeSecs - totalObserveTime) / manualTotalTimeSecs) * 100.0
    } else {
        0.0
    }

    return StudyCalculations(
        cycleTimesSum = cycleTimesSum,
        avgCapacityTime = avgCapacityTime,
        capacityTarget = study.calculatedCapacityTarget,
        targetImprovement = targetImprovement,
        improvementPercent = improvementPercent,
        totalEffective = totalEffective,
        totalNonProductiveTime = nonProductiveSum,
        totalObserveTime = totalObserveTime,
        errorPercent = errorPercent
    )
}

@Composable
fun ProductionStudyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    onClickField: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current

    // Auto-focus the next field logically by default
    val finalKeyboardOptions = if (keyboardOptions.imeAction == ImeAction.Default) {
        keyboardOptions.copy(imeAction = ImeAction.Next)
    } else {
        keyboardOptions
    }

    val finalKeyboardActions = if (keyboardActions == KeyboardActions.Default) {
        KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
            onDone = { focusManager.clearFocus() }
        )
    } else {
        keyboardActions
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .onFocusChanged { focusState ->
                if (focusState.isFocused && value.isEmpty()) {
                    onClickField?.invoke()
                }
            },
        singleLine = singleLine,
        keyboardOptions = finalKeyboardOptions,
        keyboardActions = finalKeyboardActions,
        shape = RoundedCornerShape(10.dp),
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingIcon = trailingIcon?.let {
            {
                IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                    Icon(
                        imageVector = it,
                        contentDescription = "Auto Fill",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}
