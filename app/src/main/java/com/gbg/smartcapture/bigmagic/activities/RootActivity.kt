package com.gbg.smartcapture.bigmagic.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gbg.smartcapture.bigmagic.BuildConfig
import com.gbg.smartcapture.bigmagic.viewmodel.CaptureSide
import com.gbg.smartcapture.bigmagic.compositions.FailedView
import com.gbg.smartcapture.bigmagic.compositions.FlipDocumentView
import com.gbg.smartcapture.bigmagic.compositions.LandingView
import com.gbg.smartcapture.bigmagic.compositions.PollingExhaustedView
import com.gbg.smartcapture.bigmagic.compositions.PollingView
import com.gbg.smartcapture.bigmagic.compositions.SettingsView
import com.gbg.smartcapture.bigmagic.compositions.SubmittingView
import com.gbg.smartcapture.bigmagic.compositions.VerificationResultView
import com.gbg.smartcapture.bigmagic.compositions.bits.BannerAction
import com.gbg.smartcapture.bigmagic.compositions.bits.EnterpriseBanner
import com.gbg.smartcapture.bigmagic.data.SettingsManualCaptureToggleDelayType
import com.gbg.smartcapture.bigmagic.data.SettingsSwitch
import com.gbg.smartcapture.bigmagic.util.saveJpegToGallery
import com.gbg.smartcapture.bigmagic.viewmodel.IRootViewModel
import com.gbg.smartcapture.bigmagic.viewmodel.RootViewModel
import com.gbg.smartcapture.bigmagic.viewmodel.VerificationUiState
import com.gbg.smartcapture.commons.gbgGetInsetPadding
import com.gbg.smartcapture.commons.theme.SmartCaptureUiTheme
import com.gbg.smartcapture.documentcamera.AutoCaptureToggleConfig
import com.gbg.smartcapture.documentcamera.DocumentCameraActivity
import com.gbg.smartcapture.documentcamera.DocumentProcessingState
import com.gbg.smartcapture.documentcamera.DocumentScannerConfig
import com.gbg.smartcapture.documentcamera.DocumentSide
import com.gbg.smartcapture.documentcamera.DocumentType
import kotlinx.coroutines.launch

class RootActivity : ComponentActivity() {

    private val viewModel: IRootViewModel by viewModels<RootViewModel>()
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            SmartCaptureUiTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ComposeColor.White),
                ) {
                    RootScreen()
                }
            }
        }

        observeCaptureTriggers()
        observeTransientMessages()

        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    /**
     * Handles `gbgdemo://poll[?session=vs_…]` deep links. If no `session` query parameter is
     * given, falls back to the last-created session cached in the VM.
     *
     * Useful for triggering a poll from adb without typing the session id:
     *   adb shell am start -a android.intent.action.VIEW -d "gbgdemo://poll" com.gbg.smartcapture.bigmagic
     */
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "gbgdemo" || data.host != "poll") return
        val explicit = data.getQueryParameter("session")?.trim().orEmpty()
        val sessionId = explicit.ifBlank { viewModel.lastSessionId.value.orEmpty() }
        if (sessionId.isBlank()) {
            Log.w(TAG, "gbgdemo://poll ignored — no session id and no cached last session")
            Toast.makeText(this, "No session to poll — start a verification first", Toast.LENGTH_SHORT).show()
            return
        }
        Log.i(TAG, "gbgdemo://poll session=$sessionId (explicit=${explicit.isNotBlank()})")
        viewModel.onDebugPoll(sessionId)
    }

    // --------------------- State → side-effects ---------------------

    private fun observeCaptureTriggers() {
        // SharedFlow with no replay — each request fires exactly once, even after the activity
        // is re-entered from DocumentCameraActivity, so we never double-launch.
        lifecycleScope.launch {
            viewModel.captureRequests.collect { side ->
                Log.i(TAG, "capture-request $side")
                launchCapture(
                    when (side) {
                        CaptureSide.FRONT -> DocumentSide.FRONT
                        CaptureSide.BACK -> DocumentSide.BACK
                    }
                )
            }
        }
    }

    private fun observeTransientMessages() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.transientMessages.collect { msg ->
                    Toast.makeText(this@RootActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launchCapture(side: DocumentSide) {
        val config = DocumentScannerConfig(
            autoCaptureToggleConfig = buildAutoCaptureToggle(),
            documentSide = side,
            documentType = DocumentType.ID,
        )
        val intent = DocumentCameraActivity.getIntent(this, config)
        documentCameraLauncher.launch(intent)
    }

    private fun buildAutoCaptureToggle(): AutoCaptureToggleConfig {
        val manualEnabled = viewModel.settings.manualCaptureToggle.value
        if (!manualEnabled) return AutoCaptureToggleConfig.Hide
        val delayMs = viewModel.manualCaptureToggleDelayState.value.valueMillis
        return if (delayMs <= 0L) {
            AutoCaptureToggleConfig.Show
        } else {
            AutoCaptureToggleConfig.ShowDelayed(delayMs)
        }
    }

    private val documentCameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // NOTE: `DocumentCameraActivity.latestResult` is a consume-once getter — it returns the
        // value and immediately nulls the field. Read it exactly once and bind to a local.
        val captureResult = if (result.resultCode == RESULT_OK) {
            DocumentCameraActivity.latestResult
        } else null
        Log.i(TAG, "DocumentCamera callback code=${result.resultCode} " +
                "state=${viewModel.verificationState.value::class.simpleName} " +
                "result=${captureResult?.javaClass?.simpleName}")
        when (result.resultCode) {
            RESULT_OK -> handleCaptureSuccess(captureResult)
            RESULT_CANCELED -> {
                Toast.makeText(this, "Capture cancelled", Toast.LENGTH_SHORT).show()
                viewModel.onCaptureCancelled()
            }
            else -> {
                Log.e(TAG, "DocumentCamera unexpected result code ${result.resultCode}")
                viewModel.onCaptureCancelled()
            }
        }
    }

    private fun handleCaptureSuccess(result: DocumentProcessingState.Result?) {
        val success = result as? DocumentProcessingState.Success
        if (success == null) {
            Log.w(TAG, "handleCaptureSuccess: result is $result, not Success")
            Toast.makeText(this, "Capture failed, please try again", Toast.LENGTH_SHORT).show()
            viewModel.onCaptureCancelled()
            return
        }
        val bytes = success.bytes
        val currentState = viewModel.verificationState.value
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        Log.i(TAG, "handleCaptureSuccess state=${currentState::class.simpleName} " +
                "bytes=${bytes.size} dims=${opts.outWidth}x${opts.outHeight} mime=${opts.outMimeType}")

        maybeSaveRawCaptureToGallery(bytes, currentState)

        when (currentState) {
            is VerificationUiState.AwaitingFront -> viewModel.onFrontCaptured(bytes)
            is VerificationUiState.AwaitingBack -> viewModel.onBackCaptured(bytes)
            else -> Log.w(TAG, "Capture returned but state is $currentState")
        }
    }

    private fun maybeSaveRawCaptureToGallery(bytes: ByteArray, state: VerificationUiState) {
        if (!BuildConfig.DEBUG) return
        if (!viewModel.settings.saveRawImagesToGallery.value) return
        val side = when (state) {
            is VerificationUiState.AwaitingFront -> "front"
            is VerificationUiState.AwaitingBack -> "back"
            else -> "unknown"
        }
        val filename = "smartcapture_${side}_${System.currentTimeMillis()}.jpg"
        lifecycleScope.launch { saveJpegToGallery(applicationContext, bytes, filename) }
    }

    // --------------------- Compose root ---------------------

    @Composable
    private fun RootScreen() {
        navController = rememberNavController()
        val insetPadding = LocalLayoutDirection.current.gbgGetInsetPadding()
        val state by viewModel.verificationState.collectAsState()
        val hasKey by viewModel.hasApiKey.collectAsState()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        val bannerAction = when (currentRoute) {
            Route.Settings -> BannerAction.BACK
            else -> BannerAction.SETTINGS
        }
        val bannerOnAction: () -> Unit = when (currentRoute) {
            Route.Settings -> { { navController.popBackStack() } }
            else -> { { navController.navigate(Route.Settings) } }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeColor.White)
                .padding(insetPadding)
        ) {
            EnterpriseBanner(action = bannerAction, onAction = bannerOnAction)
            NavHost(
                navController = navController,
                startDestination = Route.Main,
            ) {
                composable(Route.Main) {
                    MainRouter(
                        state = state,
                        hasKey = hasKey,
                    )
                }
                composable(Route.Settings) {
                    SettingsView(
                        viewModel = viewModel,
                        onCheckedChange = ::settingsChecked,
                        onManualCaptureToggleDelayChange = ::onManualCaptureToggleDelayChange,
                        onDebugPoll = { id ->
                            viewModel.onDebugPoll(id)
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun MainRouter(
        state: VerificationUiState,
        hasKey: Boolean,
    ) {
        when (state) {
            VerificationUiState.Idle -> LandingView(
                hasApiKey = hasKey,
                onStart = viewModel::onStart,
            )
            VerificationUiState.CreatingSession -> SubmittingView("Starting session…")
            VerificationUiState.AwaitingFront,
            is VerificationUiState.AwaitingBack -> SubmittingView("Opening camera…")
            is VerificationUiState.FlipPrompt -> FlipDocumentView(
                onContinue = viewModel::onFlipContinue,
                onCancel = viewModel::onReset,
            )
            VerificationUiState.Submitting -> SubmittingView("Submitting images…")
            is VerificationUiState.Polling -> PollingView(attempt = state.attempt)
            is VerificationUiState.PollingExhausted -> PollingExhaustedView(
                onTryAgain = viewModel::onPollRetry,
                onReset = viewModel::onReset,
            )
            is VerificationUiState.Terminal -> VerificationResultView(
                response = state.response,
                onDone = viewModel::onReset,
                onRefresh = viewModel::onRefreshTerminal,
            )
            is VerificationUiState.Failed -> FailedView(
                reason = state.reason,
                onReset = viewModel::onReset,
            )
        }
    }

    // --------------------- Settings wiring ---------------------

    private fun settingsChecked(switch: SettingsSwitch, value: Boolean) {
        viewModel.setSettingSwitch(switch, value)
    }

    private fun onManualCaptureToggleDelayChange(option: SettingsManualCaptureToggleDelayType) {
        viewModel.setManualCaptureToggleDelay(option)
    }

    private object Route {
        const val Main = "main"
        const val Settings = "settings"
    }

    companion object {
        private const val TAG = "RootActivity"
    }
}
