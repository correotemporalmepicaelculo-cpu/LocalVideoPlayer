package com.lucas.videoplayer.presentation.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucas.videoplayer.data.repository.VideoRepository
import com.lucas.videoplayer.domain.model.Video
import com.lucas.videoplayer.data.scanner.VideoScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _videos = MutableLiveData<List<Video>>()
    val videos: LiveData<List<Video>> = _videos

    private val _isScanning = MutableLiveData<Boolean>(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _scanProgress = MutableLiveData<String>("")
    val scanProgress: LiveData<String> = _scanProgress

    private val _selectedFolders = MutableLiveData<List<String>>(emptyList())
    val selectedFolders: LiveData<List<String>> = _selectedFolders

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadSelectedFolders()
        observeVideos()
    }

    private fun observeVideos() {
        viewModelScope.launch {
            videoRepository.allVideos.collect { videoList ->
                _videos.postValue(videoList)
            }
        }
    }

    private fun loadSelectedFolders() {
        // TODO: Cargar carpetas guardadas en SharedPreferences
        // Por ahora usa carpeta de descargas por defecto
        val downloadsPath = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        ).absolutePath
        _selectedFolders.postValue(listOf(downloadsPath))
    }

    fun requestFolderPermission(activity: Activity, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        activity.startActivityForResult(intent, requestCode)
    }

    fun onFolderSelected(uri: Uri) {
        val path = getPathFromUri(uri)
        if (path != null) {
            val current = _selectedFolders.value ?: emptyList()
            if (!current.contains(path)) {
                _selectedFolders.postValue(current + path)
                saveSelectedFolders(current + path)
                startScan(current + path)
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        // Para SAF, necesitamos usar DocumentFile para acceder
        // Por simplicidad, retornamos el path si es file://
        return if (uri.scheme == "file") {
            uri.path
        } else {
            // Para content:// URIs, usamos el path del documento
            uri.path?.let { "/storage/emulated/0$it" } // Aproximación
        }
    }

    private fun saveSelectedFolders(folders: List<String>) {
        // TODO: Guardar en SharedPreferences
    }

    fun startScan(folders: List<String>? = null) {
        val foldersToScan = folders ?: _selectedFolders.value ?: return
        if (foldersToScan.isEmpty()) return

        _isScanning.postValue(true)
        _scanProgress.postValue("Escaneando carpetas...")

        viewModelScope.launch {
            videoRepository.scanFolders(foldersToScan, object : VideoScanner.ScanCallback {
                override fun onVideoFound(video: com.lucas.videoplayer.data.local.entity.VideoEntity) {
                    _scanProgress.postValue("Encontrado: ${video.displayName}")
                }

                override fun onScanComplete(totalFound: Int) {
                    _isScanning.postValue(false)
                    _scanProgress.postValue("Escaneo completado: $totalFound videos encontrados")
                }

                override fun onError(error: String) {
                    _isScanning.postValue(false)
                    _error.postValue(error)
                    _scanProgress.postValue("Error: $error")
                }
            })
        }
    }

    fun refreshVideos() {
        startScan()
    }
}