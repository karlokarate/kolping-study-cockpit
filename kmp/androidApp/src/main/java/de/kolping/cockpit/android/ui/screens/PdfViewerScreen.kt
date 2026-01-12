package de.kolping.cockpit.android.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import java.io.File

/**
 * PdfViewerScreen - Displays PDF documents
 * Based on Issue #6 PR 5 requirements
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    filePath: String,
    fileName: String = "PDF Dokument",
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (error != null) {
                // Error state
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Fehler beim Laden",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = error ?: "Unbekannter Fehler",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onNavigateBack) {
                        Text("ZURÜCK")
                    }
                }
            } else {
                // PDF viewer
                AndroidView(
                    factory = { context ->
                        PDFView(context, null).apply {
                            try {
                                val file = File(filePath)
                                if (!file.exists()) {
                                    error = "Datei nicht gefunden: $filePath"
                                    isLoading = false
                                    return@apply
                                }
                                
                                fromFile(file)
                                    .enableSwipe(true)
                                    .swipeHorizontal(false)
                                    .enableDoubletap(true)
                                    .defaultPage(0)
                                    .enableAnnotationRendering(false)
                                    .password(null)
                                    .scrollHandle(null)
                                    .enableAntialiasing(true)
                                    .spacing(10)
                                    .onLoad { isLoading = false }
                                    .onError { throwable ->
                                        error = "Fehler beim Laden: ${throwable.message}"
                                        isLoading = false
                                    }
                                    .load()
                            } catch (e: Exception) {
                                error = "Fehler beim Initialisieren: ${e.message}"
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Loading indicator
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
