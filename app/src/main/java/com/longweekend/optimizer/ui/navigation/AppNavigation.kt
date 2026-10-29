package com.longweekend.optimizer.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.compose.ui.res.stringResource
import com.longweekend.optimizer.R
import com.longweekend.optimizer.model.Opportunity
import com.longweekend.optimizer.ui.screens.findbreak.FindABreakScreen
import com.longweekend.optimizer.ui.screens.holidays.HolidayCalendarScreen
import com.longweekend.optimizer.ui.screens.onboarding.OnboardingScreen
import com.longweekend.optimizer.ui.screens.plandetails.PlanDetailsScreen
import com.longweekend.optimizer.ui.screens.profile.ProfileScreen
import com.longweekend.optimizer.ui.screens.saved.SavedPlansScreen
import com.longweekend.optimizer.ui.screens.suggestedbreaks.SuggestedBreaksScreen
import com.longweekend.optimizer.ui.theme.*
import com.longweekend.optimizer.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Discover : Screen("discover")
    object Suggested : Screen("suggested")
    object Detail : Screen("detail")
    object Saved : Screen("saved")
    object Profile : Screen("profile")
    object Holidays : Screen("holidays")
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel
) {
    val onboardingDone by viewModel.onboardingDone.collectAsState()
    if (!onboardingDone) {
        OnboardingScreen(viewModel = viewModel)
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val savedPlans by viewModel.savedPlans.collectAsState()
    val selectedOpportunity by viewModel.selectedOpportunity.collectAsState()

    val showBottomBar = currentRoute in listOf(Screen.Discover.route, Screen.Holidays.route, Screen.Saved.route)

    val openDetail: (Opportunity) -> Unit = { opp ->
        viewModel.selectOpportunity(opp)
        navController.navigate(Screen.Detail.route)
    }
    val openProfile: () -> Unit = {
        navController.navigate(Screen.Profile.route) { launchSingleTop = true }
    }

    Scaffold(
        // Each screen's top bar pads for the status bar itself; this scaffold only handles the bottom
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SurfaceLowest,
                    tonalElevation = 8.dp,
                ) {
                    val isDiscover = currentRoute == Screen.Discover.route
                    NavigationBarItem(
                        selected = isDiscover,
                        onClick = {
                            navController.navigate(Screen.Discover.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isDiscover) Icons.Default.Explore else Icons.Outlined.Explore,
                                contentDescription = stringResource(R.string.nav_discover),
                                tint = if (isDiscover) TealPrimary else TextOnSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                stringResource(R.string.nav_discover),
                                color = if (isDiscover) TealPrimary else TextOnSurfaceVariant,
                                fontWeight = if (isDiscover) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = TealPrimaryFixed.copy(alpha = 0.5f)
                        )
                    )

                    val isHolidays = currentRoute == Screen.Holidays.route
                    NavigationBarItem(
                        selected = isHolidays,
                        onClick = {
                            navController.navigate(Screen.Holidays.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isHolidays) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                                contentDescription = stringResource(R.string.cd_nav_holidays),
                                tint = if (isHolidays) TealPrimary else TextOnSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                stringResource(R.string.nav_holidays),
                                color = if (isHolidays) TealPrimary else TextOnSurfaceVariant,
                                fontWeight = if (isHolidays) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = TealPrimaryFixed.copy(alpha = 0.5f)
                        )
                    )

                    val isSaved = currentRoute == Screen.Saved.route
                    NavigationBarItem(
                        selected = isSaved,
                        onClick = {
                            navController.navigate(Screen.Saved.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (savedPlans.isNotEmpty()) {
                                        Badge(
                                            containerColor = AmberSecondary,
                                            contentColor = AmberOnSecondary
                                        ) {
                                            Text("${savedPlans.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = stringResource(R.string.nav_saved),
                                    tint = if (isSaved) TealPrimary else TextOnSurfaceVariant
                                )
                            }
                        },
                        label = {
                            Text(
                                stringResource(R.string.nav_saved),
                                color = if (isSaved) TealPrimary else TextOnSurfaceVariant,
                                fontWeight = if (isSaved) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = TealPrimaryFixed.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Discover.route,
            // Consume what this scaffold already padded so the screens' own scaffolds don't pad it again
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(Screen.Discover.route) {
                FindABreakScreen(
                    viewModel = viewModel,
                    onNavigateToSuggested = {
                        navController.navigate(Screen.Suggested.route)
                    },
                    onNavigateToDetail = openDetail,
                    onNavigateToProfile = openProfile
                )
            }

            composable(Screen.Suggested.route) {
                SuggestedBreaksScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToDetail = openDetail,
                    onNavigateToProfile = openProfile
                )
            }

            composable(Screen.Detail.route) {
                val opp = selectedOpportunity
                if (opp != null) {
                    PlanDetailsScreen(
                        opportunity = opp,
                        viewModel = viewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                } else {
                    // Navigation is a side effect; it must not run during composition
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            composable(Screen.Holidays.route) {
                HolidayCalendarScreen(
                    viewModel = viewModel,
                    onNavigateToProfile = openProfile
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Saved.route) {
                SavedPlansScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = openDetail,
                    onNavigateToProfile = openProfile,
                    onNavigateToDiscover = {
                        navController.navigate(Screen.Discover.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
