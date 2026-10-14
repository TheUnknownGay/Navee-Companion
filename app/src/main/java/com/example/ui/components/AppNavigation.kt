package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ElectricScooter
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.theme.NaveeCyan

data class NavItem(
    val screen: AppScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AppNavigationBar(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(AppScreen.DASHBOARD, "Cockpit", Icons.Filled.ElectricScooter, Icons.Outlined.ElectricScooter),
        NavItem(AppScreen.CONTROLS, "Vitesse", Icons.Filled.Speed, Icons.Outlined.Speed),
        NavItem(AppScreen.TELEMETRY, "Énergie", Icons.Filled.Equalizer, Icons.Outlined.Equalizer),
        NavItem(AppScreen.TRIPS, "Trajets", Icons.Filled.Route, Icons.Outlined.Route),
        NavItem(AppScreen.DIAGNOSTICS, "BLE & Diag", Icons.Filled.BugReport, Icons.Outlined.BugReport),
        NavItem(AppScreen.SETTINGS, "Réglages", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    NavigationBar(
        modifier = modifier.testTag("app_navigation_bar"),
        windowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        items.forEach { item ->
            val isSelected = currentScreen == item.screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectScreen(item.screen) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = NaveeCyan,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_${item.screen.name.lowercase()}")
            )
        }
    }
}
