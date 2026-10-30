package com.longweekend.optimizer.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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

    // Suggestions belong to the Discover tab, so the bar stays; plan details and settings are focused screens
    val showBottomBar = currentRoute in listOf(Screen.Discover.route, Screen.Suggested.route, Screen.Holidays.route, Screen.Saved.route)

    // Switching tabs keeps each tab's place; tapping the tab you're on goes back to its start
    val openTab: (String, Boolean) -> Unit = { route, isCurrent ->
        if (isCurrent) {
            navController.popBackStack(route, inclusive = false)
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

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
                // Flat bar with a hairline on top, matching the cards
                val divider = CardBorder
                NavigationBar(
                    containerColor = SurfaceLowest,
                    tonalElevation = 0.dp,
                    modifier = Modifier.drawBehind {
                        drawLine(divider, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                    }
                ) {
                    val isDiscover = currentRoute == Screen.Discover.route || currentRoute == Screen.Suggested.route
                    NavigationBarItem(
                        selected = isDiscover,
                        onClick = { openTab(Screen.Discover.route, isDiscover) },
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
                        onClick = { openTab(Screen.Holidays.route, isHolidays) },
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
                        onClick = { openTab(Screen.Saved.route, isSaved) },
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
                .consumeWindowInsets(innerPadding),
            // The library default is a 700 ms crossfade, which makes every tap feel sluggish
            enterTransition = { fadeIn(tween(SCREEN_FADE_IN_MS)) },
            exitTransition = { fadeOut(tween(SCREEN_FADE_OUT_MS)) },
            popEnterTransition = { fadeIn(tween(SCREEN_FADE_IN_MS)) },
            popExitTransition = { fadeOut(tween(SCREEN_FADE_OUT_MS)) }
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
                        },
                        onOpenSaved = { openTab(Screen.Saved.route, false) }
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

private const val SCREEN_FADE_IN_MS = 220
private const val SCREEN_FADE_OUT_MS = 120
