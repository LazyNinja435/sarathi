package com.sarathi.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sarathi.app.SarathiApp
import com.sarathi.app.data.UserPreferences
import com.sarathi.app.ui.components.SacredBackground
import com.sarathi.app.ui.screens.BlessingScreen
import com.sarathi.app.ui.screens.ChatScreen
import com.sarathi.app.ui.screens.DharmaScreen
import com.sarathi.app.ui.screens.FeelScreen
import com.sarathi.app.ui.screens.NameScreen
import com.sarathi.app.ui.screens.SettingsScreen
import com.sarathi.app.ui.screens.SplashScreen
import com.sarathi.app.ui.screens.ToneScreen
import com.sarathi.app.ui.screens.VerseScreen
import com.sarathi.app.viewmodel.ChatViewModel
import com.sarathi.app.viewmodel.DharmaViewModel
import com.sarathi.app.viewmodel.FeelViewModel
import com.sarathi.app.viewmodel.OnboardingViewModel
import com.sarathi.app.viewmodel.SettingsViewModel
import com.sarathi.app.viewmodel.VerseViewModel
import kotlinx.coroutines.flow.first

@Composable
fun SarathiNavGraph(
    chatViewModel: ChatViewModel,
) {
    val app = LocalContext.current.applicationContext as SarathiApp
    val navController = rememberNavController()
    var startRoute by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val done = app.userPreferencesRepository.preferences.first().onboardingComplete
        startRoute = if (done) Routes.CHAT else Routes.SPLASH
    }

    val sr = startRoute
    if (sr == null) {
        SacredBackground(Modifier.fillMaxSize()) {}
        return
    }

    key(sr) {
        NavHost(
            navController = navController,
            startDestination = sr,
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onBegin = { navController.navigate(Routes.NAME) },
                )
            }
            composable(Routes.NAME) {
                val vm: OnboardingViewModel = viewModel(factory = app.viewModelFactory)
                NameScreen(
                    onOfferName = { name ->
                        vm.saveName(name)
                        navController.navigate(Routes.TONE)
                    },
                )
            }
            composable(Routes.TONE) {
                val vm: OnboardingViewModel = viewModel(factory = app.viewModelFactory)
                ToneScreen(
                    onContinue = { tone ->
                        vm.saveTone(tone)
                        navController.navigate(Routes.BLESSING)
                    },
                )
            }
            composable(Routes.BLESSING) {
                val vm: OnboardingViewModel = viewModel(factory = app.viewModelFactory)
                val prefs by app.userPreferencesRepository.preferences.collectAsStateWithLifecycle(
                    UserPreferences(),
                )
                BlessingScreen(
                    name = prefs.userName,
                    onEnterChariot = {
                        vm.completeOnboarding()
                        navController.navigate(Routes.CHAT) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.CHAT) {
                ChatScreen(
                    chatViewModel = chatViewModel,
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(Routes.VERSE) {
                val vm: VerseViewModel = viewModel(factory = app.viewModelFactory)
                VerseScreen(
                    verseViewModel = vm,
                    onBack = { navController.popBackStack() },
                    onReflectWithVerse = { v ->
                        chatViewModel.prefillAndSend(
                            "Please reflect with me on ${v.referenceLabel}: ${v.translation}",
                        )
                        navController.popBackStack(Routes.CHAT, inclusive = false)
                    },
                )
            }
            composable(Routes.FEEL) {
                val vm: FeelViewModel = viewModel(factory = app.viewModelFactory)
                FeelScreen(
                    feelViewModel = vm,
                    onBack = { navController.popBackStack() },
                    onContinue = { emotion ->
                        chatViewModel.prefillAndSend("I feel ${emotion.label.lowercase()}.")
                        navController.popBackStack(Routes.CHAT, inclusive = false)
                    },
                )
            }
            composable(Routes.DHARMA) {
                val vm: DharmaViewModel = viewModel(factory = app.viewModelFactory)
                DharmaScreen(
                    dharmaViewModel = vm,
                    onReflectWithKrishna = { note ->
                        chatViewModel.prefillAndSend("What duty am I avoiding? I wrote: $note")
                        navController.popBackStack(Routes.CHAT, inclusive = false)
                    },
                    onMenu = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = app.viewModelFactory)
                SettingsScreen(
                    settingsViewModel = vm,
                    onBack = { navController.popBackStack() },
                    onResetOnboarding = {
                        navController.navigate(Routes.SPLASH) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
