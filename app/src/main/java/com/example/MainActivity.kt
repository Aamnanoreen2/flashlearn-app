package com.example

import android.os.Bundle
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.db.FlashMasterDatabase
import com.example.data.repository.FlashcardRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FlashMasterViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room DB and Repository
        val db = FlashMasterDatabase.getDatabase(applicationContext)
        val repository = FlashcardRepository(db)

        // Instantiate ViewModel
        val viewModel = ViewModelProvider(
            this,
            FlashMasterViewModelFactory(repository, application)
        )[FlashMasterViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainAppLayout(viewModel = viewModel)
            }
        }
    }
}

class FlashMasterViewModelFactory(
    private val repository: FlashcardRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FlashMasterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FlashMasterViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class MainNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : MainNavItem("home", "Home", Icons.Default.Home)
    object Decks : MainNavItem("decks", "Decks", Icons.Default.Layers)
    object Study : MainNavItem("study", "Study", Icons.Default.School)
    object Statistics : MainNavItem("statistics", "Stats", Icons.Default.BarChart)
    object Settings : MainNavItem("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainAppLayout(viewModel: FlashMasterViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom navigation in study activities or quizzes to maximize visual space
    val showBottomBar = remember(currentRoute) {
        currentRoute in listOf(
            MainNavItem.Home.route,
            MainNavItem.Decks.route,
            MainNavItem.Study.route,
            MainNavItem.Statistics.route,
            MainNavItem.Settings.route
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("app_bottom_navigation")
                ) {
                    val navItems = listOf(
                        MainNavItem.Home,
                        MainNavItem.Decks,
                        MainNavItem.Study,
                        MainNavItem.Statistics,
                        MainNavItem.Settings
                    )
                    
                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        // Pop up to the start destination of the graph to
                                        // avoid building up a large stack of destinations
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination when
                                        // reselecting the same item
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(text = item.label) },
                            modifier = Modifier.testTag("nav_tab_${item.route}")
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Tab 1: Home
            composable(MainNavItem.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onCreateDeckClick = {
                        navController.navigate(MainNavItem.Decks.route)
                    },
                    onContinueStudyingClick = { deck ->
                        navController.navigate("deck_details/${deck.id}")
                    },
                    onReviewMistakesClick = {
                        navController.navigate(MainNavItem.Study.route)
                    }
                )
            }

            // Tab 2: Decks Catalog & Management
            composable(MainNavItem.Decks.route) {
                DecksScreen(
                    viewModel = viewModel,
                    onDeckClick = { deck ->
                        navController.navigate("deck_details/${deck.id}")
                    }
                )
            }

            // Tab 3: Study Launchpad Center
            composable(MainNavItem.Study.route) {
                StudyScreen(
                    viewModel = viewModel,
                    onStartStudy = { deck, cards ->
                        viewModel.startStudySession(deck, cards)
                        navController.navigate("active_study")
                    }
                )
            }

            // Tab 4: Diagnostics Analytics
            composable(MainNavItem.Statistics.route) {
                StatisticsScreen(viewModel = viewModel)
            }

            // Tab 5: Settings Preferences
            composable(MainNavItem.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }

            // Sub-Screen: Deck Details (Flashcards inside specific Deck)
            composable(
                route = "deck_details/{deckId}",
                arguments = listOf(navArgument("deckId") { type = NavType.IntType })
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getInt("deckId") ?: -1
                val decksList by viewModel.decks.collectAsState()
                val targetDeck = decksList.find { it.id == deckId }

                if (targetDeck != null) {
                    DeckDetailsScreen(
                        deck = targetDeck,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onStartStudy = { deck, cards ->
                            viewModel.startStudySession(deck, cards)
                            navController.navigate("active_study")
                        },
                        onStartAiQuiz = { deck ->
                            navController.navigate("ai_quiz/${deck.id}")
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }

            // Sub-Screen: Active Study Play Cards (Grade levels, Timers, Focus & gestures)
            composable("active_study") {
                StudySessionScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.endStudySession()
                        navController.popBackStack()
                    }
                )
            }

            // Sub-Screen: AI Assistant MultiChoice Quiz powered by Gemini Rest client
            composable(
                route = "ai_quiz/{deckId}",
                arguments = listOf(navArgument("deckId") { type = NavType.IntType })
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getInt("deckId") ?: -1
                val decksList by viewModel.decks.collectAsState()
                val targetDeck = decksList.find { it.id == deckId }

                if (targetDeck != null) {
                    AiQuizScreen(
                        deck = targetDeck,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
