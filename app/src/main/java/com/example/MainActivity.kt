package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.chat.ChatScreen
import com.example.ui.compare.CompareScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.editor.EditorScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NawaDevStudioTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.DevStudioViewModel
import kotlinx.coroutines.delay

enum class NavScreen(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    CHAT("AI Chat", Icons.Default.Chat),
    COMPARE("Compare", Icons.Default.CompareArrows),
    EDITOR("Editor", Icons.Default.Code),
    SETTINGS("API Setup", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: DevStudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NawaDevStudioTheme {
                DevStudioApp(viewModel)
            }
        }
    }
}

@Composable
fun DevStudioApp(viewModel: DevStudioViewModel) {
    var currentScreen by remember { mutableStateOf(NavScreen.DASHBOARD) }
    val bannerMessage by viewModel.statusBanner.collectAsState()

    // Auto dismiss banner after 3 seconds
    LaunchedEffect(bannerMessage) {
        if (bannerMessage != null) {
            delay(3000)
            viewModel.dismissBanner()
        }
    }

    // Android back button handling
    BackHandler(enabled = currentScreen != NavScreen.DASHBOARD) {
        currentScreen = NavScreen.DASHBOARD
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground),
        containerColor = CyberBackground,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        CyberBorder,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav")
            ) {
                NavScreen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanPrimary,
                            selectedTextColor = CyanPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CyanPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                NavScreen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { currentScreen = NavScreen.CHAT },
                    onNavigateToCompare = { currentScreen = NavScreen.COMPARE },
                    onNavigateToEditor = { currentScreen = NavScreen.EDITOR },
                    onNavigateToSettings = { currentScreen = NavScreen.SETTINGS }
                )
                NavScreen.CHAT -> ChatScreen(
                    viewModel = viewModel,
                    onNavigateToCompare = { currentScreen = NavScreen.COMPARE },
                    onNavigateToEditor = { currentScreen = NavScreen.EDITOR }
                )
                NavScreen.COMPARE -> CompareScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { currentScreen = NavScreen.SETTINGS }
                )
                NavScreen.EDITOR -> EditorScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { currentScreen = NavScreen.CHAT }
                )
                NavScreen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }

            // Notification / Feedback Banner
            AnimatedVisibility(
                visible = bannerMessage != null,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Surface(
                    color = Color(0xFF0F2035),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.6f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.dismissBanner() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = bannerMessage ?: "",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
