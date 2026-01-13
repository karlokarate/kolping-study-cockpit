package de.kolping.cockpit.mapping.android.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.kolping.cockpit.mapping.android.JsBridge
import de.kolping.cockpit.mapping.android.RecorderController
import de.kolping.cockpit.mapping.android.SessionStore
import de.kolping.cockpit.mapping.android.web.RecorderWebViewClient
import de.kolping.cockpit.mapping.core.CaptureFilters
import de.kolping.cockpit.mapping.core.Event

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RecorderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val controller = remember { RecorderController() }
    val sessionStore = remember { SessionStore(context) }
    
    var chainName by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf("") }
    var eventCount by remember { mutableIntStateOf(0) }
    var redactEnabled by remember { mutableStateOf(true) }
    var captureJson by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showParentDialog by remember { mutableStateOf(false) }

    val chainPoints = remember { mutableStateListOf<de.kolping.cockpit.mapping.core.ChainPoint>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recorder") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier.fillMaxSize().padding(padding).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chainName,
                onValueChange = { chainName = it },
                label = { Text("Chain Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (chainName.isNotBlank()) {
                            controller.createChain(chainName)
                            statusMessage = "Chain '$chainName' created"
                        }
                    },
                    enabled = chainName.isNotBlank() && !isRecording
                ) { Text("Create Chain") }
                
                Button(
                    onClick = {
                        if (!isRecording) {
                            controller.startSession(
                                targetUrl = currentUrl.ifBlank { null },
                                filters = CaptureFilters(redact = redactEnabled)
                            )
                            isRecording = true
                            statusMessage = "Recording started"
                        } else {
                            controller.stopSession()
                            isRecording = false
                            chainPoints.clear()
                            chainPoints.addAll(controller.getChainPoints())
                            statusMessage = "Recording stopped - ${controller.eventCount} events"
                        }
                    },
                    enabled = controller.currentChainName != null
                ) { Text(if (isRecording) "Stop" else "Record") }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = redactEnabled, onCheckedChange = { redactEnabled = it })
                    Text("Redact")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = captureJson, onCheckedChange = { captureJson = it })
                    Text("Capture JSON")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (currentUrl.isNotBlank()) {
                            controller.saveTargetUrl(currentUrl)
                            statusMessage = "Target URL saved"
                        }
                    },
                    enabled = currentUrl.isNotBlank()
                ) { Text("Save Target URL") }
                
                Button(
                    onClick = { showParentDialog = true },
                    enabled = currentUrl.isNotBlank() && controller.currentChainName != null
                ) { Text("Add Point") }
                
                Button(
                    onClick = {
                        val bundle = controller.export()
                        val ts = System.currentTimeMillis()
                        val path = sessionStore.writeBundle(bundle, "export_$ts")
                        statusMessage = "Exported to $path"
                    },
                    enabled = !isRecording
                ) { Text("Export") }
            }

            if (statusMessage.isNotBlank()) {
                Text(statusMessage, style = MaterialTheme.typography.bodySmall)
            }
            
            Text("Events: $eventCount | URL: $currentUrl", style = MaterialTheme.typography.bodySmall)

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        
                        val bridge = JsBridge(
                            onNetworkRequest = { event ->
                                controller.addNetworkRequest(event)
                                eventCount = controller.eventCount
                            },
                            onNetworkResponse = { event ->
                                controller.addNetworkResponse(event)
                                eventCount = controller.eventCount
                            },
                            onClick = { event ->
                                controller.addClickEvent(event)
                                eventCount = controller.eventCount
                            }
                        )
                        addJavascriptInterface(bridge, "RecorderBridge")
                        
                        webViewClient = RecorderWebViewClient(
                            isRecordingProvider = { isRecording },
                            onUrlChanged = { url ->
                                currentUrl = url
                                if (isRecording) {
                                    controller.addNavigationEvent(url, Event.Phase.FINISHED)
                                    eventCount = controller.eventCount
                                }
                            }
                        )
                        loadUrl("https://portal.kolping-hochschule.de/my/")
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize().weight(1f)
            )
        }

        if (showParentDialog) {
            AlertDialog(
                onDismissRequest = { showParentDialog = false },
                title = { Text("Select Parent Node") },
                text = {
                    Column {
                        Text("Add point at: $currentUrl")
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            controller.addChainPoint("Point", currentUrl, null)
                            chainPoints.clear()
                            chainPoints.addAll(controller.getChainPoints())
                            showParentDialog = false
                            statusMessage = "Point added (no parent)"
                        }) { Text("No Parent (Root)") }
                        chainPoints.forEach { point ->
                            TextButton(onClick = {
                                controller.addChainPoint("Point", currentUrl, point.id)
                                chainPoints.clear()
                                chainPoints.addAll(controller.getChainPoints())
                                showParentDialog = false
                                statusMessage = "Point added under ${point.name}"
                            }) { Text(point.name) }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showParentDialog = false }) { Text("Cancel") } }
            )
        }
    }
}
