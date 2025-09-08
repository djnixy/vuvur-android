package com.example.vuvur

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vuvur.screens.GalleryScreen
import com.example.vuvur.screens.GalleryViewModel
import com.example.vuvur.screens.RandomScreen
import com.example.vuvur.screens.SettingsScreen
import com.example.vuvur.screens.ViewerScreen
import com.example.vuvur.ui.theme.VuvurTheme
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Gallery : Screen("gallery", "Gallery", Icons.Default.Home)
    data object Random : Screen("random", "Random", Icons.Default.Shuffle)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Viewer : Screen("viewer/{startIndex}", "Viewer", Icons.Default.Home)
}

val menuItems = listOf(
    Screen.Gallery,
    Screen.Random,
    Screen.Settings
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VuvurTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val galleryViewModel: GalleryViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val route = currentDestination?.route
    val isRandom = route == Screen.Random.route
    val isViewer = route?.startsWith("viewer") == true
    val gesturesEnabled = !isRandom && !isViewer

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Spacer(Modifier.height(12.dp))
                menuItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (!isRandom && !isViewer) {
                    TopAppBar(
                        title = {
                            val currentTitle = menuItems.find { item ->
                                currentDestination?.hierarchy?.any { it.route == item.route } == true
                            }?.label ?: "Vuvur"
                            Text(currentTitle)
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Gallery.route,
                modifier = if (!isRandom && !isViewer) Modifier.padding(innerPadding) else Modifier.fillMaxSize()
            ) {
                composable(Screen.Gallery.route) {
                    GalleryScreen(
                        viewModel = galleryViewModel,
                        onImageClick = { index ->
                            navController.navigate("viewer/$index")
                        }
                    )
                }
                composable(Screen.Random.route) { RandomScreen(navController = navController) }
                composable(Screen.Settings.route) { SettingsScreen() }

                composable(
                    route = Screen.Viewer.route,
                    arguments = listOf(navArgument("startIndex") { type = NavType.IntType })
                ) { backStackEntry ->
                    ViewerScreen(
                        viewModel = galleryViewModel,
                        startIndex = backStackEntry.arguments?.getInt("startIndex") ?: 0,
                        navController = navController
                    )
                }
            }
        }
    }
}