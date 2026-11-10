package com.smarttv.remote.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smarttv.remote.ui.components.NavTab
import com.smarttv.remote.ui.screens.*
import com.smarttv.remote.ui.viewmodel.RemoteViewModel

object Destinations {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val WIFI_CHECK = "wifi_check"
    const val TV_DISCOVERY = "tv_discovery"
    const val PAIRING = "pairing"
    const val CONNECTION_SUCCESS = "connection_success"
    const val HOME = "home"
    const val MAIN_REMOTE = "main_remote"
    const val PLAYBACK_CONTROL = "playback_control"
    const val SKIP_DURATION = "skip_duration"
    const val TV_APPS = "tv_apps"
    const val CAST = "cast"
    const val KEYBOARD = "keyboard"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: RemoteViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.consumeUserMessage()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Destinations.SPLASH
    ) {
        // SCREEN 01: SPLASH
        composable(Destinations.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    // Read the latest state: a saved TV skips onboarding and reconnects by itself.
                    val hasPairedTv = viewModel.uiState.value.selectedDevice?.isPaired == true
                    val target = if (hasPairedTv) Destinations.MAIN_REMOTE else Destinations.WELCOME
                    navController.navigate(target) {
                        popUpTo(Destinations.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // SCREEN 02: WELCOME
        composable(Destinations.WELCOME) {
            WelcomeScreen(
                onConnectTvClick = {
                    navController.navigate(Destinations.WIFI_CHECK)
                }
            )
        }

        // SCREEN 03: WI-FI CHECK
        composable(Destinations.WIFI_CHECK) {
            WifiCheckScreen(
                wifiConnected = uiState.wifiConnected,
                wifiSsid = uiState.wifiSsid,
                onContinueClick = {
                    viewModel.startDiscovery()
                    navController.navigate(Destinations.TV_DISCOVERY)
                }
            )
        }

        // SCREEN 04: TV DISCOVERY
        composable(Destinations.TV_DISCOVERY) {
            LaunchedEffect(Unit) {
                if (uiState.discoveredDevices.isEmpty() && !uiState.isScanning) {
                    viewModel.startDiscovery()
                }
            }
            TvDiscoveryScreen(
                devices = uiState.discoveredDevices,
                selectedDevice = uiState.selectedDevice,
                isScanning = uiState.isScanning,
                isBusy = uiState.isBusy,
                errorMessage = uiState.errorMessage,
                onSelectDevice = { tv ->
                    viewModel.connectToDevice(
                        tv,
                        onCodePrompt = { navController.navigate(Destinations.PAIRING) },
                        onConnected = { navController.navigate(Destinations.CONNECTION_SUCCESS) }
                    )
                },
                onRescanClick = {
                    viewModel.startDiscovery()
                },
                onManualIpSubmit = { ip ->
                    viewModel.addManualIp(
                        ip,
                        onCodePrompt = { navController.navigate(Destinations.PAIRING) },
                        onConnected = { navController.navigate(Destinations.CONNECTION_SUCCESS) }
                    )
                }
            )
        }

        // SCREEN 05: PAIRING
        composable(Destinations.PAIRING) {
            PairingScreen(
                tvName = uiState.selectedDevice?.name ?: "No TV selected",
                pairingCode = uiState.pairingCode,
                isLoading = uiState.isPairingLoading,
                errorMessage = uiState.pairingError,
                onCodeChange = { viewModel.updatePairingCode(it) },
                onPairClick = {
                    viewModel.submitPairingCode(
                        onSuccess = {
                            navController.navigate(Destinations.CONNECTION_SUCCESS)
                        }
                    )
                },
                onBackClick = {
                    viewModel.cancelPairing()
                    navController.popBackStack()
                }
            )
        }

        // SCREEN 06: CONNECTION SUCCESS
        composable(Destinations.CONNECTION_SUCCESS) {
            ConnectionSuccessScreen(
                tvName = uiState.selectedDevice?.name ?: "No TV selected",
                tvModel = uiState.selectedDevice?.model ?: "Google TV",
                onOpenRemoteClick = {
                    navController.navigate(Destinations.MAIN_REMOTE) {
                        popUpTo(Destinations.WELCOME) { inclusive = true }
                    }
                },
                onChangeTvClick = {
                    navController.navigate(Destinations.TV_DISCOVERY)
                }
            )
        }

        // SCREEN 07: MAIN REMOTE
        // HOME: shows and movies from JioHotstar / Netflix
        composable(Destinations.HOME) {
            LaunchedEffect(Unit) {
                if (uiState.contentState is com.smarttv.remote.data.content.ContentState.Idle) viewModel.loadContent()
            }
            HomeScreen(
                tvName = uiState.selectedDevice?.name ?: "No TV selected",
                tvModel = uiState.selectedDevice?.model ?: "Google TV",
                isConnected = uiState.connectionStatus == com.smarttv.remote.data.model.ConnectionStatus.CONNECTED,
                contentState = uiState.contentState,
                tmdbKey = uiState.tmdbKey,
                onSaveKey = { viewModel.saveTmdbKey(it) },
                onRetry = { viewModel.loadContent() },
                onOpenPlatform = { viewModel.openPlatform(it) },
                onPlayItem = { viewModel.playOnTv(it) },
                onTypeTitle = { viewModel.typeTitleOnTv(it) },
                onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
                onTvSelectClick = { navController.navigate(Destinations.TV_DISCOVERY) },
                onNavigateTab = { tab -> handleTabNavigation(tab, navController) }
            )
        }

        composable(Destinations.MAIN_REMOTE) {
            MainRemoteScreen(
                tvName = uiState.selectedDevice?.name ?: "No TV selected",
                tvModel = uiState.selectedDevice?.model ?: "Google TV",
                isConnected = uiState.connectionStatus == com.smarttv.remote.data.model.ConnectionStatus.CONNECTED,
                isListeningVoice = uiState.isListeningVoice,
                easyMode = uiState.easyModeEnabled,
                onCommand = { cmd -> viewModel.sendCommand(cmd) },
                onPowerClick = { viewModel.sendPower() },
                onAppClick = { app -> viewModel.launchApp(app) },
                onVoiceClick = { viewModel.toggleVoiceListening() },
                onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
                onTvSelectClick = { navController.navigate(Destinations.TV_DISCOVERY) },
                onNavigateTab = { tab ->
                    handleTabNavigation(tab, navController)
                },
                onPlaybackClick = { navController.navigate(Destinations.PLAYBACK_CONTROL) }
            )
        }

        // SCREEN 08: PLAYBACK CONTROL
        composable(Destinations.PLAYBACK_CONTROL) {
            PlaybackControlScreen(
                tvName = uiState.selectedDevice?.name ?: "No TV selected",
                tvModel = uiState.selectedDevice?.model ?: "Google TV",
                isConnected = uiState.connectionStatus == com.smarttv.remote.data.model.ConnectionStatus.CONNECTED,
                mediaState = uiState.mediaState,
                skipConfig = uiState.skipDurationConfig,
                onPlayPause = { viewModel.togglePlayPause() },
                onSeekBackward = { viewModel.sendSeekBackward() },
                onSeekForward = { viewModel.sendSeekForward() },
                onPrevious = { viewModel.sendCommand(com.smarttv.remote.data.model.RemoteCommand.MEDIA_PREVIOUS) },
                onNext = { viewModel.sendCommand(com.smarttv.remote.data.model.RemoteCommand.MEDIA_NEXT) },
                onStop = { viewModel.sendCommand(com.smarttv.remote.data.model.RemoteCommand.MEDIA_STOP) },
                onOpenSkipSettings = { navController.navigate(Destinations.SKIP_DURATION) },
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
                onTvSelectClick = { navController.navigate(Destinations.TV_DISCOVERY) },
                onNavigateTab = { tab -> handleTabNavigation(tab, navController) }
            )
        }

        // SCREEN 09: SET SKIP DURATION
        composable(Destinations.SKIP_DURATION) {
            SetSkipDurationScreen(
                currentConfig = uiState.skipDurationConfig,
                onSaveConfig = { b, f, c -> viewModel.updateSkipDurations(b, f, c) },
                onBackClick = { navController.popBackStack() },
                onNavigateTab = { tab -> handleTabNavigation(tab, navController) }
            )
        }

        // SCREEN 10: TV APPS
        composable(Destinations.TV_APPS) {
            TvAppsScreen(
                tvName = uiState.selectedDevice?.name ?: "No TV selected",
                tvModel = uiState.selectedDevice?.model ?: "Google TV",
                isConnected = uiState.connectionStatus == com.smarttv.remote.data.model.ConnectionStatus.CONNECTED,
                apps = viewModel.filteredApps,
                selectedFilter = uiState.appFilter,
                searchQuery = uiState.searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onFilterChange = { viewModel.setAppFilter(it) },
                onAppClick = { app -> viewModel.launchApp(app) },
                onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
                onTvSelectClick = { navController.navigate(Destinations.TV_DISCOVERY) },
                onNavigateTab = { tab -> handleTabNavigation(tab, navController) }
            )
        }

        // SCREEN 11: CAST
        composable(Destinations.CAST) {
            CastScreen(
                tvName = uiState.selectedDevice?.name ?: "No TV selected",
                tvModel = uiState.selectedDevice?.model ?: "Google TV",
                isConnected = uiState.connectionStatus == com.smarttv.remote.data.model.ConnectionStatus.CONNECTED,
                castConnected = uiState.castConnected,
                castDeviceName = uiState.castDeviceName,
                castStatus = uiState.castStatus,
                routes = uiState.castRoutes,
                onStartScan = { viewModel.startCastScan() },
                onStopScan = { viewModel.stopCastScan() },
                onSelectRoute = { viewModel.selectCastRoute(it) },
                onCastFile = { uri, mime -> viewModel.castFile(uri, mime) },
                onCastScreenClick = { viewModel.castScreen() },
                onStopCast = { viewModel.stopCast() },
                onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
                onTvSelectClick = { navController.navigate(Destinations.TV_DISCOVERY) },
                onNavigateTab = { tab -> handleTabNavigation(tab, navController) }
            )
        }

        // SCREEN 12: KEYBOARD
        composable(Destinations.KEYBOARD) {
            TvKeyboardScreen(
                tvName = uiState.selectedDevice?.name ?: "No TV selected",
                tvModel = uiState.selectedDevice?.model ?: "Google TV",
                isConnected = uiState.connectionStatus == com.smarttv.remote.data.model.ConnectionStatus.CONNECTED,
                currentText = uiState.keyboardText,
                onKeyInput = { char -> viewModel.onKeyboardInput(char) },
                onBackspace = { viewModel.onKeyboardBackspace() },
                onEnter = { viewModel.onKeyboardEnter() },
                onVoiceClick = { viewModel.toggleVoiceListening() },
                onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
                onTvSelectClick = { navController.navigate(Destinations.TV_DISCOVERY) },
                onNavigateTab = { tab -> handleTabNavigation(tab, navController) }
            )
        }

        // SCREEN 13: SETTINGS
        composable(Destinations.SETTINGS) {
            SettingsScreen(
                currentDevice = uiState.selectedDevice,
                skipConfig = uiState.skipDurationConfig,
                hapticEnabled = uiState.hapticEnabled,
                easyModeEnabled = uiState.easyModeEnabled,
                onToggleHaptic = { viewModel.toggleHapticFeedback(it) },
                onToggleEasyMode = { viewModel.toggleEasyMode(it) },
                onChangeTvClick = { navController.navigate(Destinations.TV_DISCOVERY) },
                onReconnectClick = {
                    uiState.selectedDevice?.let { viewModel.connectToRemoteSession(it) }
                },
                onSkipSettingsClick = { navController.navigate(Destinations.SKIP_DURATION) },
                onBackClick = { navController.popBackStack() },
                isConnected = uiState.connectionStatus == com.smarttv.remote.data.model.ConnectionStatus.CONNECTED,
                connectionError = uiState.errorMessage,
                protocolLog = uiState.protocolLog,
                tvAwake = uiState.tvAwake,
                tmdbKey = uiState.tmdbKey,
                onSaveKey = { viewModel.saveTmdbKey(it) }
            )
        }
    }
}

private fun handleTabNavigation(tab: NavTab, navController: NavHostController) {
    val destination = when (tab) {
        NavTab.HOME -> Destinations.HOME
        NavTab.REMOTE -> Destinations.MAIN_REMOTE
        NavTab.APPS -> Destinations.TV_APPS
        NavTab.CAST -> Destinations.CAST
        NavTab.KEYBOARD -> Destinations.KEYBOARD
    }
    navController.navigate(destination) {
        popUpTo(Destinations.MAIN_REMOTE) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
