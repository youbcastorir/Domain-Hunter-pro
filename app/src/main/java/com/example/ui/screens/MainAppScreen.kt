@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAppScreen(
    viewModel: DomainHunterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    val scope = rememberCoroutineScope()

    // Interactive custom toast feedback banner
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(key1 = true) {
        viewModel.uiEventMessage.collectLatest { msg ->
            toastMessage = msg
            kotlinx.coroutines.delay(2500)
            toastMessage = null
        }
    }

    // Interactive notification listener
    var showNotifAlert by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(key1 = true) {
        viewModel.simulatedNotification.collectLatest { log ->
            showNotifAlert = log
            kotlinx.coroutines.delay(4000)
            showNotifAlert = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (onboardingCompleted) {
                BottomNavBar(currentScreen = currentScreen) { target ->
                    viewModel.navigateTo(target)
                }
            }
        },
        containerColor = BackgroundNavy
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BackgroundNavy,
                            Color(0xFF04060E)
                        )
                    )
                )
        ) {
            // Navigation Flow Renderer
            AnimatedContent(
                targetState = if (!onboardingCompleted) "Onboarding" else currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith
                            fadeOut(animationSpec = tween(180))
                },
                label = "SniperNavigation"
            ) { state ->
                when (state) {
                    "Onboarding" -> OnboardingView(viewModel)
                    is Screen.Dashboard -> DashboardView(viewModel)
                    is Screen.SmartScanner -> SmartScannerView(viewModel)
                    is Screen.ScanResults -> ScanResultsView(viewModel, (state as Screen.ScanResults).filtersJson)
                    is Screen.FullAnalysis -> FullAnalysisView(viewModel, (state as Screen.FullAnalysis).domain, (state as Screen.FullAnalysis).source)
                    is Screen.Watchlist -> WatchlistView(viewModel)
                    is Screen.SmartAlerts -> SmartAlertsView(viewModel)
                    is Screen.BulkScanner -> BulkScannerView(viewModel)
                    is Screen.MarketIntelligence -> MarketIntelligenceView(viewModel)
                    is Screen.PortfolioTracker -> PortfolioTrackerView(viewModel)
                    is Screen.Settings -> SettingsView(viewModel)
                }
            }

            // Realtime alert popup
            AnimatedVisibility(
                visible = showNotifAlert != null,
                enter = slideInVertically(initialOffsetY = { -200 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -200 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, WarningOrange.copy(alpha = 0.8f)),
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WarningOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = WarningOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BOT MATCH DISPATCHED",
                                color = WarningOrange,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = showNotifAlert ?: "",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // High priority animated custom toast banner
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                Surface(
                    color = CardNavy.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f)),
                    tonalElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Notification",
                            tint = PositiveGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = toastMessage ?: "",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// 5-Tab adaptive Bottom Navigation
@Composable
fun BottomNavBar(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit
) {
    Surface(
        color = CardNavy,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                TabItem("Home", Icons.Default.Dashboard, Screen.Dashboard, listOf(Screen.Dashboard::class)),
                TabItem("Scanner", Icons.Default.Search, Screen.SmartScanner, listOf(Screen.SmartScanner::class, Screen.ScanResults::class, Screen.FullAnalysis::class, Screen.BulkScanner::class)),
                TabItem("Watchlist", Icons.Default.Bookmark, Screen.Watchlist, listOf(Screen.Watchlist::class)),
                TabItem("Intelligence", Icons.Default.TrendingUp, Screen.MarketIntelligence, listOf(Screen.MarketIntelligence::class)),
                TabItem("Assets", Icons.Default.Folder, Screen.PortfolioTracker, listOf(Screen.PortfolioTracker::class, Screen.SmartAlerts::class, Screen.Settings::class))
            )

            tabs.forEach { tab ->
                val isSelected = tab.classes.any { it == currentScreen::class }
                val tintColor by animateColorAsState(
                    targetValue = if (isSelected) ElectricBlue else TextSecondary,
                    label = "TabIconTint"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTabSelected(tab.target) }
                        .padding(vertical = 4.dp)
                        .testTag("tab_${tab.label.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = tintColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = tintColor,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

data class TabItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val target: Screen,
    val classes: List<kotlin.reflect.KClass<out Screen>>
)

// Canvas Sniper Scope Premium Logo Header Drawing
@Composable
fun ConsoleHeader(
    title: String,
    subtitle: String,
    viewModel: DomainHunterViewModel,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Draw Sniper Scope decorative reticle element on load
            Canvas(
                modifier = Modifier
                    .size(28.dp)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = size.width * 0.35f

                // Draw outer grid circle
                drawCircle(
                    color = ElectricBlue,
                    radius = radius,
                    style = Stroke(width = 2.dp.toPx())
                )
                // Draw crosshairs
                drawLine(
                    color = ElectricBlue,
                    start = Offset(cx - radius - 4f, cy),
                    end = Offset(cx + radius + 4f, cy),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = ElectricBlue,
                    start = Offset(cx, cy - radius - 4f),
                    end = Offset(cx, cy + radius + 4f),
                    strokeWidth = 1.5.dp.toPx()
                )
                // Draw center focal target pin
                drawCircle(
                    color = WarningOrange,
                    radius = 2.dp.toPx()
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            IconButton(onClick = { viewModel.navigateTo(Screen.Settings) }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// SCREEN 1: FIRST TURN GUEST ONBOARDING (Bypass and Setup)
@Composable
fun OnboardingView(viewModel: DomainHunterViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        // Scope Brand Avatar
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ElectricBlue.copy(alpha = 0.15f))
                .border(2.dp, ElectricBlue, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(45.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawCircle(color = ElectricBlue, radius = cx * 0.45f, style = Stroke(2.5.dp.toPx()))
                drawLine(color = ElectricBlue, start = Offset(0f, cy), end = Offset(size.width, cy), strokeWidth = 2.dp.toPx())
                drawLine(color = ElectricBlue, start = Offset(cx, 0f), end = Offset(cx, size.height), strokeWidth = 2.dp.toPx())
                drawCircle(color = WarningOrange, radius = 5f)
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Domain Sniper Pro",
            color = TextPrimary,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "AI-Driven Auction Scouter & Portfolio Sniper",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = WarningOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QUICK ACTIVATE MODULES", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Activate real-time scouters. The local persistence engine monitors GoDaddy & Namecheap feeds automatically in sleep cycles.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.triggerVibration()
                viewModel.completeOnboarding()
            },
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("onboard_activate_btn")
        ) {
            Text("Launch Sniper Console", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// SCREEN 2: HOME DASHBOARD (Live Stats & Quick Scoute Card feeds)
@Composable
fun DashboardView(viewModel: DomainHunterViewModel) {
    val scans by viewModel.scannedDomains.collectAsState()
    val watchlist by viewModel.watchlistDomains.collectAsState()
    val portfolio by viewModel.portfolioDomains.collectAsState()
    val alerts by viewModel.smartAlerts.collectAsState()

    val totalScanned = scans.size
    val goldTierFound = scans.count { it.grade == "S" || it.grade == "A" }
    val watchlistCount = watchlist.size
    val alertsActive = alerts.count { it.enabled }

    var selectedQuickFilter by remember { mutableStateOf("All") } // All / Under $10 / Under $30 / Expired / Auction

    val filteredScans = remember(scans, selectedQuickFilter) {
        scans.filter {
            when (selectedQuickFilter) {
                "Under $10" -> it.price <= 10.0
                "Under $30" -> it.price <= 30.0
                "Expired" -> it.suggestedNiches.contains("Expired", ignoreCase = true) || (1..10).random() > 4 // expired mock
                "Auction" -> it.price > 50.0
                else -> true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        ConsoleHeader(
            title = "Domain Sniper Pro",
            subtitle = "S-Tier Domain Scout Systems",
            viewModel = viewModel
        )

        // Live stats grid cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stat 1: Scanned
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("SCANNED", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$totalScanned", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Today log", color = TextSecondary, fontSize = 9.sp)
                }
            }
            // Stat 2: S/A found
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ELITE (S/A)", color = WarningOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$goldTierFound", color = WarningOrange, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Found today", color = TextSecondary, fontSize = 9.sp)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stat 3: Watchlist
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("WATCHLIST", color = ElectricBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$watchlistCount", color = ElectricBlue, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Saved assets", color = TextSecondary, fontSize = 9.sp)
                }
            }
            // Stat 4: Alerts
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ALERTS", color = PositiveGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$alertsActive", color = PositiveGreen, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Active bots", color = TextSecondary, fontSize = 9.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large Pulse FAB scanner shortcut
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElectricBlue.copy(alpha = 0.1f))
                    .border(1.5.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .clickable { viewModel.navigateTo(Screen.SmartScanner) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Scan",
                        tint = ElectricBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("LAUNCH SMART SCANNER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Configure premium extension & metric filters", color = TextSecondary, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = ElectricBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick filter buttons feed
        Text(
            text = "QUICK FILTER FINDS",
            color = AccentCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickFilters = listOf("All", "Under $10", "Under $30", "Expired", "Auction")
            items(quickFilters) { filter ->
                val selected = selectedQuickFilter == filter
                Surface(
                    color = if (selected) ElectricBlue else CardNavy,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (selected) Color.Transparent else BorderColor),
                    modifier = Modifier.clickable { selectedQuickFilter = filter }
                ) {
                    Text(
                        text = filter,
                        color = if (selected) Color.White else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent high-scoring finds feed
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT OUTSTANDING SCANS",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Bulk scan",
                color = ElectricBlue,
                fontSize = 11.sp,
                modifier = Modifier.clickable { viewModel.navigateTo(Screen.BulkScanner) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredScans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No local history matching.", color = TextSecondary, fontSize = 12.sp)
                    Text("Press 'Scanner' tab to scour live dropping domains", color = TextSecondary, fontSize = 11.sp)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredScans.take(15).forEach { item ->
                    DomainCompactCard(domain = item, onClick = {
                        viewModel.selectedDomainName.value = item.domainName
                        viewModel.analysisCameFrom.value = "scanned"
                        viewModel.navigateTo(Screen.FullAnalysis(item.domainName, "scanned"))
                    }, onWatchlistToggle = {
                        viewModel.toggleWatchlist(item)
                    })
                }
            }
        }
    }
}

// Compact Domain Card UI component
@Composable
fun DomainCompactCard(
    domain: ScannedDomain,
    onClick: () -> Unit,
    onWatchlistToggle: () -> Unit
) {
    val tierColor = when (domain.grade) {
        "S" -> WarningOrange
        "A" -> PositiveGreen
        "B" -> ElectricBlue
        "C" -> TextSecondary
        else -> CriticalRed
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardNavy),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left score circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tierColor.copy(alpha = 0.12f))
                    .border(1.5.dp, tierColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = domain.grade,
                        color = tierColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${domain.overallScore}",
                        color = TextPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = domain.domainName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricBadge(label = "DA:${domain.da}")
                    MetricBadge(label = "${domain.ageYears}y old")
                    if (domain.waybackTraffic) {
                        Surface(
                            color = PositiveGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "TRAFFIC",
                                color = PositiveGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Right price & book
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${domain.price.toInt()}",
                    color = PositiveGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = onWatchlistToggle,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Store",
                        tint = ElectricBlue
                    )
                }
            }
        }
    }
}

@Composable
fun MetricBadge(label: String) {
    Surface(
        color = CardNavy.copy(alpha = 0.5f),
        border = BorderStroke(0.6.dp, BorderColor),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

// SCREEN 3: SMART SCANNER SCREEN (Filter settings & triggers)
@Composable
fun SmartScannerView(viewModel: DomainHunterViewModel) {
    val modelExtensions by viewModel.extensionsChecked.collectAsState()
    val maxPrice by viewModel.maxPriceFilter.collectAsState()
    val only30 by viewModel.onlyUnder30.collectAsState()
    val dropsOnly by viewModel.neverRegistered.collectAsState()
    val minScore by viewModel.minDomainScore.collectAsState()
    val minDA by viewModel.minDAFilter.collectAsState()
    val backlinkInput by viewModel.minBacklinksInput.collectAsState()
    val ageVal by viewModel.minAgeDropdown.collectAsState()
    val types by viewModel.domainTypesChecked.collectAsState()
    val keyword by viewModel.keywordFilterInput.collectAsState()
    val nicheSelected by viewModel.nicheDropdownSelected.collectAsState()
    val maxLen by viewModel.maxLengthSlider.collectAsState()
    val cleanHyphen by viewModel.noHyphensToggle.collectAsState()
    val cleanNum by viewModel.noNumbersToggle.collectAsState()

    val isScanning by viewModel.isScanning.collectAsState()

    var showNicheDrop by remember { mutableStateOf(false) }
    var showAgeDrop by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        ConsoleHeader(
            title = "Smart Scanner",
            subtitle = "Surgical target feeds configuration",
            viewModel = viewModel
        )

        // Bulk and Alerts shortcuts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.navigateTo(Screen.BulkScanner) },
                colors = ButtonDefaults.buttonColors(containerColor = CardNavy),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bulk Upload", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.navigateTo(Screen.SmartAlerts) },
                colors = ButtonDefaults.buttonColors(containerColor = CardNavy),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = PositiveGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Smart Alerts", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Card containing controls
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Extensions Checked Row
                Text("ACQUISITION EXTENSIONS", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                val extList = listOf(".com", ".ai", ".io", ".co", ".net", ".org", ".xyz")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    extList.forEach { ext ->
                        val checked = modelExtensions.contains(ext)
                        FilterChip(
                            selected = checked,
                            onClick = {
                                viewModel.triggerVibration()
                                val current = modelExtensions.toMutableList()
                                if (checked) current.remove(ext) else current.add(ext)
                                viewModel.extensionsChecked.value = current
                            },
                            label = { Text(ext, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricBlue,
                                selectedLabelColor = Color.White,
                                containerColor = BackgroundNavy,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = checked,
                                borderColor = BorderColor,
                                selectedBorderColor = Color.Transparent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Price limit slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("MAX REGISTRY PRICE", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$${maxPrice.toInt()}", color = PositiveGreen, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
                Slider(
                    value = maxPrice,
                    onValueChange = { viewModel.maxPriceFilter.value = it },
                    valueRange = 10f..500f,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricBlue,
                        activeTrackColor = ElectricBlue,
                        inactiveTrackColor = BackgroundNavy
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = only30,
                        onCheckedChange = { viewModel.onlyUnder30.value = it },
                        colors = CheckboxDefaults.colors(checkedColor = ElectricBlue)
                    )
                    Text("Limit to Closeout deals (under $30)", color = TextPrimary, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quality threshold sliders
                Text("MINIMUM QUALITY OVERALL SCORE", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Overall Score floor", color = TextSecondary, fontSize = 11.sp)
                    Text("$minScore", color = WarningOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Slider(
                    value = minScore.toFloat(),
                    onValueChange = { viewModel.minDomainScore.value = it.toInt() },
                    valueRange = 20f..95f,
                    colors = SliderDefaults.colors(
                        thumbColor = WarningOrange,
                        activeTrackColor = WarningOrange,
                        inactiveTrackColor = BackgroundNavy
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("MINIMUM DOMAIN AUTHORITY (DA)", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Moz DA floor", color = TextSecondary, fontSize = 11.sp)
                    Text("$minDA", color = ElectricBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Slider(
                    value = minDA.toFloat(),
                    onValueChange = { viewModel.minDAFilter.value = it.toInt() },
                    valueRange = 0f..80f,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricBlue,
                        activeTrackColor = ElectricBlue,
                        inactiveTrackColor = BackgroundNavy
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Keyword filters
                Text("KEYWORD PATTERN", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { viewModel.keywordFilterInput.value = it },
                    placeholder = { Text("E.g. paid, secure, neural") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = BackgroundNavy,
                        unfocusedContainerColor = BackgroundNavy
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Max length & Clean filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("MAX CHARACTERS", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$maxLen chars", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = maxLen.toFloat(),
                    onValueChange = { viewModel.maxLengthSlider.value = it.toInt() },
                    valueRange = 4f..25f,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricBlue,
                        activeTrackColor = ElectricBlue,
                        inactiveTrackColor = BackgroundNavy
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = cleanHyphen,
                        onCheckedChange = { viewModel.noHyphensToggle.value = it },
                        colors = CheckboxDefaults.colors(checkedColor = ElectricBlue)
                    )
                    Text("No Hyphens (-)", color = TextPrimary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Checkbox(
                        checked = cleanNum,
                        onCheckedChange = { viewModel.noNumbersToggle.value = it },
                        colors = CheckboxDefaults.colors(checkedColor = ElectricBlue)
                    )
                    Text("No Numbers (0-9)", color = TextPrimary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Start Scan core pulsing action Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ElectricBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("SCROUNGING REGISTRAR REGISTRIES...", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        viewModel.runDomainScanner {
                            // finished callback
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("scan_trigger_btn")
                ) {
                    Icon(imageVector = Icons.Default.Radar, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Scour Drop Feeds", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
        }
    }
}

// SCREEN 4: DISPATCH RESULTS FEEDS
@Composable
fun ScanResultsView(viewModel: DomainHunterViewModel, filtersJson: String) {
    val scans by viewModel.scannedDomains.collectAsState()
    val sortOpt by viewModel.sortOption.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    val sortedList = remember(scans, sortOpt) {
        val list = scans.toMutableList()
        when (sortOpt) {
            "Price" -> list.sortBy { it.price }
            "Age" -> list.sortByDescending { it.ageYears }
            "Backlinks" -> list.sortByDescending { it.backlinks }
            "DA" -> list.sortByDescending { it.da}
            else -> list.sortByDescending { it.overallScore }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        ConsoleHeader(
            title = "Scan Results",
            subtitle = "Found target candidates",
            viewModel = viewModel
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.SmartScanner) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
        }

        // Filters summaries row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "MATCHED ${sortedList.size} DOMAIN LEADS",
                color = AccentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Sort button
            Box {
                Button(
                    onClick = { showSortMenu = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CardNavy),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BorderColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Sort: $sortOpt", color = TextPrimary, fontSize = 10.sp)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(CardNavy)
                ) {
                    listOf("Score", "Price", "Age", "Backlinks", "DA").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = TextPrimary) },
                            onClick = {
                                viewModel.sortOption.value = option
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (sortedList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching items found. Relax your criteria levels.", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedList) { domain ->
                    DomainLeadsCard(domain = domain, onFullAnalysis = {
                        viewModel.selectedDomainName.value = domain.domainName
                        viewModel.analysisCameFrom.value = "scanned"
                        viewModel.navigateTo(Screen.FullAnalysis(domain.domainName, "scanned"))
                    }, onWatchlistToggle = {
                        viewModel.toggleWatchlist(domain)
                    })
                }
            }
        }
    }
}

@Composable
fun DomainLeadsCard(
    domain: ScannedDomain,
    onFullAnalysis: () -> Unit,
    onWatchlistToggle: () -> Unit
) {
    val tierColor = when (domain.grade) {
        "S" -> WarningOrange
        "A" -> PositiveGreen
        "B" -> ElectricBlue
        "C" -> TextSecondary
        else -> CriticalRed
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardNavy),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tier grade badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(tierColor.copy(alpha = 0.12f))
                        .border(1.5.dp, tierColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(domain.grade, color = tierColor, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("${domain.overallScore}", color = TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(domain.domainName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(domain.suggestedNiches, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Text(
                    text = "$${domain.price.toInt()}",
                    color = PositiveGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lower metrics pills row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricPill(label = "DA: ${domain.da}")
                MetricPill(label = "BL: ${domain.backlinks}")
                MetricPill(label = "Age: ${domain.ageYears}y")
                if (domain.waybackTraffic) {
                    MetricPill(label = "WEB: Yes", color = PositiveGreen)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onWatchlistToggle,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bookmark", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onFullAnalysis,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(imageVector = Icons.Default.Troubleshoot, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analyze Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun MetricPill(label: String, color: Color = TextSecondary) {
    Surface(
        color = BackgroundNavy.copy(alpha = 0.6f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.6.dp, BorderColor)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// SCREEN 5: EXTRA BRAINABILITY METRIC DETAIL PAGE
@Composable
fun FullAnalysisView(viewModel: DomainHunterViewModel, domainName: String, cameFrom: String) {
    val scans by viewModel.scannedDomains.collectAsState()
    val watchlist by viewModel.watchlistDomains.collectAsState()

    val matchedScanned = scans.find { it.domainName == domainName }
    
    // Fallback Mock generator for manual detail entry bypass safety
    val domainObj = matchedScanned ?: ScannedDomain(
        domainName = domainName, overallScore = 78, grade = "A", price = 150.0, extension = ".com",
        da = 24, backlinks = 120, tf = 14, cf = 16, ageYears = 6, waybackTraffic = true,
        suggestedNiches = "SaaS, Automation, AI Devs", similarSoldDomains = "synclabs.com sold for $4200",
        buyVerdictReason = "Pronouncable name keyword combo; Highly memorable exact fit; Healthy backlink DA indicators.",
        riskFactors = "Check trademarks prior to bulk launch"
    )

    val tierColor = when (domainObj.grade) {
        "S" -> WarningOrange
        "A" -> PositiveGreen
        "B" -> ElectricBlue
        "C" -> TextSecondary
        else -> CriticalRed
    }

    val context = LocalContext.current
    val clip = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        ConsoleHeader(
            title = domainObj.domainName,
            subtitle = "Surgical Metric Analytics",
            viewModel = viewModel
        ) {
            IconButton(onClick = {
                val backDest = if (cameFrom == "watchlist") Screen.Watchlist else Screen.ScanResults("")
                viewModel.navigateTo(backDest)
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
        }

        // Circular sweep meter gauge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Circle Canvas Arc metric drawing
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 7.dp.toPx()
                        // grey track
                        drawCircle(
                            color = BorderColor,
                            style = Stroke(width = strokePx)
                        )
                        // accent sweep arc
                        drawArc(
                            color = tierColor,
                            startAngle = -90f,
                            sweepAngle = (domainObj.overallScore / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${domainObj.overallScore}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(text = "${domainObj.grade} Grade", color = tierColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("REGISTRY VALUE:", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$${domainObj.price.toInt()}", color = PositiveGreen, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // 1. Quality scoring breakdowns
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("QUALITY ANALYSIS BREAKDOWN", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                // Metric A: SEO
                MetricBar(label = "SEO Domain Authority (DA)", score = domainObj.da, maxVal = 100, barColor = ElectricBlue)
                // Metric B: Backlinks
                MetricBar(label = "Trust Flow / Majestic (TF)", score = domainObj.tf, maxVal = 50, barColor = PositiveGreen)
                // Metric C: Age years
                MetricBar(label = "Citation Flow (CF)", score = domainObj.cf, maxVal = 50, barColor = WarningOrange)

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL BACKLINKS", color = TextSecondary, fontSize = 10.sp)
                        Text("${domainObj.backlinks}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("WAYBACK FIRST AGE", color = TextSecondary, fontSize = 10.sp)
                        Text("${domainObj.ageYears} Years registered", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Wave Brandability Claude/Gemini generated
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AI BRANDABILITY AUDIT", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PRONOUNCEABILITY", color = TextSecondary, fontSize = 9.sp)
                        Text(if (domainObj.overallScore >= 60) "High" else "Moderate", color = PositiveGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MEMORABILITY", color = TextSecondary, fontSize = 9.sp)
                        Text(if (domainObj.overallScore >= 75) "Outstanding" else "Average", color = WarningOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("ESTIMATED TARGET MARKET NICHES", color = TextSecondary, fontSize = 9.sp)
                Text(domainObj.suggestedNiches, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(10.dp))
                Text("HISTORICAL COMPARATIVE FLIPS", color = TextSecondary, fontSize = 9.sp)
                Text(domainObj.similarSoldDomains, color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 3. Gemini verdict points
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Recommend, contentDescription = null, tint = WarningOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GEMINI SNIPER AI VERDICT", color = WarningOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Display bullets
                val bulletPoints = domainObj.buyVerdictReason.split(";")
                bulletPoints.forEach { pt ->
                    if (pt.trim().isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("•", color = WarningOrange, modifier = Modifier.padding(end = 8.dp), fontWeight = FontWeight.Bold)
                            Text(pt.trim(), color = TextPrimary, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("SUGGESTED RESALE VALUE", color = TextSecondary, fontSize = 9.sp)
                        Text("$${domainObj.suggestedResalePrice.toInt()}", color = PositiveGreen, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("VERDICT ACTION", color = TextSecondary, fontSize = 9.sp)
                        Text(domainObj.verdict, color = tierColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large actions checkout Buy links opening godaddy / check
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    clip.setText(AnnotatedString(domainObj.domainName))
                    Toast.makeText(context, "Copied registrar address!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CardNavy),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Name", color = TextPrimary, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    viewModel.toggleWatchlist(domainObj)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.2f)
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                val saved = watchlist.any { it.domainName == domainObj.domainName }
                Text(if (saved) "Unbookmark" else "Save Target", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MetricBar(label: String, score: Int, maxVal: Int, barColor: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextSecondary, fontSize = 10.sp)
            Text("$score/$maxVal", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Progress bar container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(BackgroundNavy)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((score.toFloat() / maxVal).coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

// SCREEN 6: WATCHLIST SCREEN
@Composable
fun WatchlistView(viewModel: DomainHunterViewModel) {
    val items by viewModel.watchlistDomains.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        ConsoleHeader(
            title = "Watchlist Bookmarks",
            subtitle = "Tracked Dropping Targets",
            viewModel = viewModel
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${items.size} SAVED NOMINAL ASSETS",
                color = AccentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (items.isNotEmpty()) {
                Text(
                    text = "Clear All",
                    color = CriticalRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.clearWatchlist() }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No domains bookmarked yet.", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { domain ->
                    // Watchlist card showing price drops
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardNavy),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.selectedDomainName.value = domain.domainName
                            viewModel.analysisCameFrom.value = "watchlist"
                            viewModel.navigateTo(Screen.FullAnalysis(domain.domainName, "watchlist"))
                        }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(ElectricBlue.copy(alpha = 0.12f))
                                        .border(1.5.dp, ElectricBlue, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(domain.grade, color = ElectricBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(domain.domainName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("DA: ${domain.da} • ${domain.ageYears}y old", color = TextSecondary, fontSize = 11.sp)
                                }
                                
                                // Price drops logic
                                val priceDropSim = false // can mock drops
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, tint = PositiveGreen, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("$${domain.lastCheckedPrice.toInt()}", color = PositiveGreen, fontSize = 15.sp, fontWeight = FontWeight.Black)
                                    }
                                    Text("Reg limit basis", color = TextSecondary, fontSize = 9.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Days saved: ${domain.daysSaved}", color = TextSecondary, fontSize = 10.sp)
                                Text(
                                    text = "Delete",
                                    color = CriticalRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { viewModel.deleteFromWatchlist(domain.domainName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// SCREEN 7: SMART ALERTS CONFIGURATIONS
@Composable
fun SmartAlertsView(viewModel: DomainHunterViewModel) {
    val alerts by viewModel.smartAlerts.collectAsState()

    val alertName by viewModel.alertName.collectAsState()
    val alertKeyword by viewModel.alertKeyword.collectAsState()
    val alertExt by viewModel.alertExtensionsSelected.collectAsState()
    val alertMaxPrice by viewModel.alertMaxPrice.collectAsState()
    val alertMinScore by viewModel.alertMinScore.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        ConsoleHeader(
            title = "Smart Alerts Bot",
            subtitle = "Custom trigger filters",
            viewModel = viewModel
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.PortfolioTracker) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
        }

        // Alert builder card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("CREATE NEW DISPATCH BOT ALERT", color = WarningOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = alertName,
                    onValueChange = { viewModel.alertName.value = it },
                    label = { Text("Alert Name Identifier", fontSize = 11.sp) },
                    placeholder = { Text("AI Short Names bot") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = alertKeyword,
                        onValueChange = { viewModel.alertKeyword.value = it },
                        label = { Text("Keyword Pattern", fontSize = 11.sp) },
                        placeholder = { Text("E.g. secure") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = alertExt,
                        onValueChange = { viewModel.alertExtensionsSelected.value = it },
                        label = { Text("Extensions", fontSize = 11.sp) },
                        placeholder = { Text(".com,.ai") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (alertMaxPrice > 0) alertMaxPrice.toInt().toString() else "",
                        onValueChange = { viewModel.alertMaxPrice.value = it.toFloatOrNull() ?: 0f },
                        label = { Text("Max Cost ($)", fontSize = 11.sp) },
                        placeholder = { Text("150") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = alertMinScore.toString(),
                        onValueChange = { viewModel.alertMinScore.value = it.toIntOrNull() ?: 50 },
                        label = { Text("Min Score threshold", fontSize = 11.sp) },
                        placeholder = { Text("70") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.triggerVibration()
                        viewModel.saveAlert(alertName, alertKeyword, alertExt, alertMaxPrice.toDouble(), alertMinScore, "expired,auction")
                        viewModel.alertName.value = ""
                        viewModel.alertKeyword.value = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddAlert, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Arm Sniper Bot", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Active alerts triggers
        Text(
            text = "ACTIVE CRITERIA TRIGGERS",
            color = AccentCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )

        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching critera triggers active yet.", color = TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                alerts.forEach { alert ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardNavy),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (alert.enabled) PositiveGreen else TextSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(alert.alertName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Switch(
                                    checked = alert.enabled,
                                    onCheckedChange = { viewModel.toggleAlertEnabled(alert.id, it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PositiveGreen, checkedTrackColor = PositiveGreen.copy(alpha = 0.3f))
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Criteria: '${alert.keywordPattern}' in ${alert.extensionFilter} under $${alert.maxPrice.toInt()} (score > ${alert.minScore})", color = TextSecondary, fontSize = 11.sp)
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Last Triggered: ${alert.lastTriggeredDate}", color = WarningOrange, fontSize = 10.sp)
                                Text(
                                    text = "Delete",
                                    color = CriticalRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { viewModel.deleteAlert(alert) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// SCREEN 8: BULK POWER USER SCANNER MODULE
@Composable
fun BulkScannerView(viewModel: DomainHunterViewModel) {
    val processMethod by viewModel.bulkInputMethod.collectAsState()
    val textBulkInput by viewModel.bulkInputText.collectAsState()

    val bulkIsProcessing by viewModel.bulkIsProcessing.collectAsState()
    val bulkProgress by viewModel.bulkProgress.collectAsState()
    val processed by viewModel.bulkProcessedCount.collectAsState()
    val totalCount by viewModel.bulkTotalCount.collectAsState()
    val results by viewModel.bulkResultsList.collectAsState()

    val context = LocalContext.current
    val clip = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        ConsoleHeader(
            title = "Bulk Port Scanners",
            subtitle = "Power User Batch Analytics",
            viewModel = viewModel
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.SmartScanner) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
        }

        // Input switch rows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val methods = listOf("text", "generate")
            methods.forEach { m ->
                val selected = processMethod == m
                Surface(
                    color = if (selected) ElectricBlue else CardNavy,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.bulkInputMethod.value = m }
                ) {
                    Text(
                        text = if (m == "text") "Paste Domains" else "Keywords Combo Generator",
                        color = if (selected) Color.White else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (processMethod == "text") {
                    Text("PASTE EXPIRED LISTING LINES (ONE PER LINE)", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textBulkInput,
                        onValueChange = { viewModel.bulkInputText.value = it },
                        placeholder = { Text("E.g.\nsynclabs.ai\npaywell.io\nfitbrand.co") },
                        minLines = 4,
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("PERMUTATIONS KEYWORDS GENERATOR", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    var kwText by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = kwText,
                        onValueChange = { kwText = it },
                        placeholder = { Text("Enter keywords comma-spaced: pay,secure,ai") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.generateBulkKeywordsList(kwText, listOf(".com", ".ai", ".io"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generate Combs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (bulkIsProcessing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Scoring batch: $processed/$totalCount", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        // Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(BackgroundNavy)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(bulkProgress)
                                    .clip(CircleShape)
                                    .background(ElectricBlue)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.triggerVibration()
                            viewModel.runBulkScanner(textBulkInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Initiate Bulk valuation", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (results.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("BATCH VALUATION RUNS (${results.size})", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Export CSV",
                    color = ElectricBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        val csvDump = viewModel.exportAllCsv()
                        clip.setText(AnnotatedString(csvDump))
                        Toast.makeText(context, "Copied CSV to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                results.forEach { item ->
                    DomainCompactCard(domain = item, onClick = {
                        viewModel.selectedDomainName.value = item.domainName
                        viewModel.analysisCameFrom.value = "scanned"
                        viewModel.navigateTo(Screen.FullAnalysis(item.domainName, "scanned"))
                    }, onWatchlistToggle = {
                        viewModel.toggleWatchlist(item)
                    })
                }
            }
        }
    }
}

// SCREEN 9: MARKET INTELLIGENCE SCREEN
@Composable
fun MarketIntelligenceView(viewModel: DomainHunterViewModel) {
    val insights by viewModel.marketInsights.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        ConsoleHeader(
            title = "Market Intelligence",
            subtitle = "Daily DNA Auction Indices",
            viewModel = viewModel
        )

        // Hottest niches list
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("HOTTEST ACCREDITED NICHES TODAY", color = WarningOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                viewModel.trendingNiches.forEach { n ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(WarningOrange)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(n, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Custom drawn canvas bar chart representing average sales rates
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AVERAGE FLIP VALUE BY EXTENSION ($)", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    
                    val mapVals = viewModel.avgPriceByExtension.values.toList()
                    val keys = viewModel.avgPriceByExtension.keys.toList()
                    val maxVal = mapVals.maxOrNull()?.toFloat() ?: 5000f

                    val barsCount = mapVals.size
                    val spacing = w / barsCount

                    mapVals.forEachIndexed { i, price ->
                        val barH = (price / maxVal) * h * 0.8f
                        val barW = spacing * 0.45f
                        val x = i * spacing + (spacing * 0.25f)
                        val y = h - barH

                        drawRoundRect(
                            color = ElectricBlue,
                            topLeft = Offset(x, y),
                            size = Size(barW, barH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    viewModel.avgPriceByExtension.forEach { (ext, price) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(ext, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("$${price}", color = TextSecondary, fontSize = 7.sp)
                        }
                    }
                }
            }
        }

        // DNJournal sales lists
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("RECORDED PUBLIC TRANSACTIONS (DNJOURNAL)", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                viewModel.topRecentSales.forEach { sale ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(sale.domain, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(sale.channel, color = TextSecondary, fontSize = 9.sp)
                        }
                        Text("$${sale.price.toInt()}", color = PositiveGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // AI Advice insights
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = WarningOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GEMINI ALGORITHMIC INTELLIGENCE", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))

                insights.forEach { ins ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("•", color = WarningOrange, modifier = Modifier.padding(end = 8.dp))
                        Text(ins, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

// SCREEN 10: PORTFOLIO OWNED TRACKER SCREEN (Calculator & Logs)
@Composable
fun PortfolioTrackerView(viewModel: DomainHunterViewModel) {
    val ownedList by viewModel.portfolioDomains.collectAsState()

    val totalInvested = ownedList.sumOf { it.buyPrice }
    val totalEstimatedSell = ownedList.sumOf { it.targetSellPrice }
    val averageROI = if (totalInvested > 0) {
        ((totalEstimatedSell - totalInvested) / totalInvested * 100).toInt()
    } else 0

    // Adder fields
    val portName by viewModel.portName.collectAsState()
    val portBuyPrice by viewModel.portBuyPrice.collectAsState()
    val portTargetPrice by viewModel.portTargetPrice.collectAsState()
    val portListingPlatform by viewModel.portListingPlatform.collectAsState()

    var showAddForm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        ConsoleHeader(
            title = "Portfolio Holdings",
            subtitle = "Asset valuation tracking indices",
            viewModel = viewModel
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.SmartAlerts) }) {
                Icon(Icons.Default.AddAlert, contentDescription = null, tint = WarningOrange)
            }
        }

        // Visual aggregate ROI summary
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("OWNED PORTFOLIO STATS BASIS", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("CAPITAL INVESTED", color = TextSecondary, fontSize = 10.sp)
                        Text("$${totalInvested.toInt()}", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("POTENTIAL RETURN", color = TextSecondary, fontSize = 10.sp)
                        Text("$${totalEstimatedSell.toInt()}", color = PositiveGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("ESTIMATED ROI", color = TextSecondary, fontSize = 10.sp)
                        Text("$averageROI%", color = WarningOrange, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Toggle add domain form
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { showAddForm = !showAddForm },
                colors = ButtonDefaults.buttonColors(containerColor = if (showAddForm) CardNavy else ElectricBlue),
                shape = RoundedCornerShape(10.dp),
                border = if (showAddForm) BorderStroke(1.dp, BorderColor) else null,
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Icon(imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (showAddForm) "Close Form" else "Register Acquired Domain", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (showAddForm) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ADD COMPLETED PURCHASE", color = WarningOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = portName,
                        onValueChange = { viewModel.portName.value = it },
                        label = { Text("Domain purchased") },
                        placeholder = { Text(" E.g. fitlabs.co") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = portBuyPrice,
                            onValueChange = { viewModel.portBuyPrice.value = it },
                            label = { Text("Buy Price ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = portTargetPrice,
                            onValueChange = { viewModel.portTargetPrice.value = it },
                            label = { Text("Target Sell ($)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = portListingPlatform,
                        onValueChange = { viewModel.portListingPlatform.value = it },
                        label = { Text("Listing Platform") },
                        placeholder = { Text("Afternic BIN, Sedo, GoDaddy") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val priceB = portBuyPrice.toDoubleOrNull() ?: 12.0
                            val targetB = portTargetPrice.toDoubleOrNull() ?: 2400.0
                            viewModel.savePortfolioDomain(
                                domain = portName, buyPrice = priceB, buyDate = "",
                                platform = "Default", targetPrice = targetB, listing = portListingPlatform,
                                status = "Listed"
                            )
                            viewModel.portName.value = ""
                            viewModel.portBuyPrice.value = ""
                            viewModel.portTargetPrice.value = ""
                            viewModel.portListingPlatform.value = ""
                            showAddForm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PositiveGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Asset to Database", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Owned asset lists
        Text(
            text = "OWNED PORTFOLIO CHECKLIST (${ownedList.size})",
            color = AccentCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )

        if (ownedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No portfolios logged. Tap register to start tracking.", color = TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ownedList.forEach { asset ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardNavy),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(asset.domainName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                // Status badge
                                val bgS = when (asset.status) {
                                    "Sold" -> PositiveGreen.copy(alpha = 0.15f)
                                    "Listed" -> ElectricBlue.copy(alpha = 0.15f)
                                    else -> TextSecondary.copy(alpha = 0.15f)
                                }
                                val fgS = when (asset.status) {
                                    "Sold" -> PositiveGreen
                                    "Listed" -> ElectricBlue
                                    else -> TextSecondary
                                }
                                Surface(color = bgS, shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        asset.status.uppercase(),
                                        color = fgS,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("ACQUISITION COST", color = TextSecondary, fontSize = 8.sp)
                                    Text("$${asset.buyPrice.toInt()}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("TARGET SALE PRICE", color = TextSecondary, fontSize = 8.sp)
                                    Text("$${asset.targetSellPrice.toInt()}", color = PositiveGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("LISTING FORUMS", color = TextSecondary, fontSize = 8.sp)
                                    Text(asset.currentListingPlatform, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (asset.status != "Sold") {
                                    Text(
                                        text = "Mark as Sold",
                                        color = PositiveGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            viewModel.markPortfolioSold(asset.id, asset.targetSellPrice)
                                        }
                                    )
                                } else {
                                    Text("Sold date: ${asset.soldDate}", color = PositiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Text(
                                    text = "Delete",
                                    color = CriticalRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { viewModel.deletePortfolio(asset.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// SCREEN 11: SETTINGS PREFERENCES WINDOW
@Composable
fun SettingsView(viewModel: DomainHunterViewModel) {
    val keyMoz by viewModel.keyMoz.collectAsState()
    val keyMajestic by viewModel.keyMajestic.collectAsState()
    val keyNamecheap by viewModel.keyNamecheap.collectAsState()
    val keyGoDaddy by viewModel.keyGoDaddy.collectAsState()
    val scanPref by viewModel.defaultScanPreference.collectAsState()
    val maxResults by viewModel.maxResultsSlider.collectAsState()
    val morningAuto by viewModel.morningScanSchedule.collectAsState()
    val priceDrop by viewModel.notificationPriceDrop.collectAsState()
    val sFound by viewModel.notificationSFound.collectAsState()

    val context = LocalContext.current
    val clip = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        ConsoleHeader(
            title = "Sniper Preferences",
            subtitle = "Surgical Settings",
            viewModel = viewModel
        )

        // API Setup card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("CREDENTIAL API SECRETS (PRO KEYBOARD)", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = keyMoz,
                    onValueChange = { viewModel.keyMoz.value = it },
                    label = { Text("Moz SEO API key") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = keyMajestic,
                    onValueChange = { viewModel.keyMajestic.value = it },
                    label = { Text("Majestic Flow API key") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = keyNamecheap,
                        onValueChange = { viewModel.keyNamecheap.value = it },
                        label = { Text("Namecheap API") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = keyGoDaddy,
                        onValueChange = { viewModel.keyGoDaddy.value = it },
                        label = { Text("GoDaddy Key") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderColor),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Notification configurations
        Card(
            colors = CardDefaults.cardColors(containerColor = CardNavy),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("BACKGROUND DEPLOYMENT ALARMS", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily auto drop scout (08:00 AM)", color = TextPrimary, fontSize = 12.sp)
                    Switch(checked = morningAuto, onCheckedChange = { viewModel.morningScanSchedule.value = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Trigger alarms on Bookmarked price drops", color = TextPrimary, fontSize = 12.sp)
                    Switch(checked = priceDrop, onCheckedChange = { viewModel.notificationPriceDrop.value = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Trigger push on Gold-tier S finds", color = TextPrimary, fontSize = 12.sp)
                    Switch(checked = sFound, onCheckedChange = { viewModel.notificationSFound.value = it })
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Trigger updates & Wipes
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    keyboard?.hide()
                    viewModel.updateSettings(
                        moz = keyMoz, majestic = keyMajestic,
                        namecheap = keyNamecheap, godaddy = keyGoDaddy,
                        scanPref = scanPref, maxRes = maxResults,
                        morningAuto = morningAuto, priceDrop = priceDrop, sFound = sFound
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text("Save Preference credentials", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        viewModel.clearCache()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardNavy),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Purge Cache", color = TextPrimary, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        viewModel.wipeDatabase()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardNavy),
                    border = BorderStroke(1.dp, CriticalRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Hard Wipe DB", color = CriticalRed, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Domain Sniper Pro • Version 1.0.4 rM33 (Native Android Build)\nGoogle AI Studio Power Engine",
                color = TextSecondary,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
