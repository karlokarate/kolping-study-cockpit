package de.kolping.cockpit.android.sync

import android.util.Log
import de.kolping.cockpit.android.storage.FileStorageManager
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import java.io.File

/**
 * Download manager for fetching files with progress tracking
 * Supports parallel downloads, resume capability, and progress callbacks
 */
class DownloadManager(
    private val fileStorage: FileStorageManager,
    private val maxConcurrentDownloads: Int = 3
) {
    companion object {
        private const val TAG = "DownloadManager"
        private const val BUFFER_SIZE = 8192
    }
    
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000 // 2 minutes per file
            connectTimeoutMillis = 30_000
        }
        
        followRedirects = true
    }
    
    private val downloadSemaphore = Semaphore(maxConcurrentDownloads)
    
    /**
     * Download result containing file information
     */
    data class DownloadResult(
        val file: File,
        val sizeBytes: Long,
        val url: String,
        val success: Boolean,
        val error: Exception? = null
    )
    
    /**
     * Download a single file with progress callback
     * 
     * @param url URL to download from
     * @param directory Target directory
     * @param fileName Target file name
     * @param onProgress Optional progress callback (bytesDownloaded, totalBytes)
     * @param resumeIfExists If true and file exists, resume download
     * @return DownloadResult with file info and status
     */
    suspend fun downloadFile(
        url: String,
        directory: File,
        fileName: String,
        onProgress: ((Long, Long) -> Unit)? = null,
        resumeIfExists: Boolean = true
    ): DownloadResult = withContext(Dispatchers.IO) {
        downloadSemaphore.acquire()
        try {
            val targetFile = File(directory, fileName)
            var existingSize = 0L
            
            // Check if file exists and get size for resume
            if (resumeIfExists && targetFile.exists()) {
                existingSize = targetFile.length()
                Log.d(TAG, "File exists (${existingSize} bytes), attempting resume: $fileName")
            }
            
            // Get file size
            val headResponse = client.head(url)
            val totalSize = headResponse.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
            
            // Check if already fully downloaded
            if (existingSize > 0 && existingSize == totalSize) {
                Log.d(TAG, "File already fully downloaded: $fileName")
                onProgress?.invoke(totalSize, totalSize)
                return@withContext DownloadResult(
                    file = targetFile,
                    sizeBytes = totalSize,
                    url = url,
                    success = true
                )
            }
            
            // Download with resume support
            val response: HttpResponse = client.get(url) {
                if (resumeIfExists && existingSize > 0) {
                    header(HttpHeaders.Range, "bytes=$existingSize-")
                }
            }
            
            if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.PartialContent) {
                throw Exception("Download failed: ${response.status}")
            }
            
            // Ensure directory exists
            directory.mkdirs()
            
            // Download and write to file
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesDownloaded = existingSize
            
            // Open file in append mode if resuming, otherwise overwrite
            val fileOutputStream = if (resumeIfExists && existingSize > 0) {
                java.io.FileOutputStream(targetFile, true)
            } else {
                java.io.FileOutputStream(targetFile, false)
            }
            
            fileOutputStream.use { output ->
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        onProgress?.invoke(bytesDownloaded, totalSize)
                    }
                }
            }
            
            Log.d(TAG, "Downloaded successfully: $fileName ($bytesDownloaded bytes)")
            
            DownloadResult(
                file = targetFile,
                sizeBytes = bytesDownloaded,
                url = url,
                success = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: $fileName - ${e.message}", e)
            DownloadResult(
                file = File(directory, fileName),
                sizeBytes = 0L,
                url = url,
                success = false,
                error = e
            )
        } finally {
            downloadSemaphore.release()
        }
    }
    
    /**
     * Download multiple files in parallel
     * 
     * @param downloads List of (url, directory, fileName) tuples
     * @param onProgress Optional progress callback for each file
     * @param onFileComplete Optional callback when each file completes
     * @return List of download results
     */
    suspend fun downloadFiles(
        downloads: List<Triple<String, File, String>>,
        onProgress: ((String, Long, Long) -> Unit)? = null,
        onFileComplete: ((DownloadResult) -> Unit)? = null
    ): List<DownloadResult> = coroutineScope {
        downloads.map { (url, directory, fileName) ->
            async {
                val result = downloadFile(
                    url = url,
                    directory = directory,
                    fileName = fileName,
                    onProgress = { downloaded, total ->
                        onProgress?.invoke(fileName, downloaded, total)
                    }
                )
                onFileComplete?.invoke(result)
                result
            }
        }.awaitAll()
    }
    
    /**
     * Cancel all ongoing downloads
     */
    fun cancelAll() {
        client.close()
    }
    
    fun close() {
        client.close()
    }
}
