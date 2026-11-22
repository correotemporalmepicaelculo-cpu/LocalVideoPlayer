package com.lucas.videoplayer.presentation.main

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observers
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lucas.videoplayer.R
import com.lucas.videoplayer.databinding.ActivityMainBinding
import com.lucas.videoplayer.presentation.adapter.VideoAdapter
import com.lucas.videoplayer.presentation.player.PlayerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by androidx.lifecycle.viewModels()
    private val videoAdapter = VideoAdapter { video ->
        openPlayer(video)
    }

    private val REQUEST_FOLDER_PICKER = 1001
    private val PERMISSION_REQUEST_CODE = 1002

    // Activity Result Launchers
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val path = getPathFromTreeUri(uri)
                if (path != null) {
                    viewModel.onFolderSelected(Uri.parse(path))
                }
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.startScan()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        checkPermissionsAndScan()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = videoAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewModel.videos.observe(this) { videos ->
            videoAdapter.submitList(videos)
            binding.emptyView.isVisible = videos.isEmpty()
            binding.recyclerView.isVisible = videos.isNotEmpty()
        }

        viewModel.isScanning.observe(this) { isScanning ->
            binding.progressBar.isVisible = isScanning
            binding.toolbar.menu?.findItem(R.id.action_scan)?.isVisible = !isScanning
            binding.toolbar.menu?.findItem(R.id.action_stop_scan)?.isVisible = isScanning
        }

        viewModel.scanProgress.observe(this) { progress ->
            binding.scanProgressText.text = progress
            binding.scanProgressText.isVisible = progress.isNotBlank()
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkPermissionsAndScan() {
        if (hasStoragePermission()) {
            viewModel.startScan()
        } else {
            requestStoragePermission()
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES))
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.storage_permission_explanation)
            .setPositiveButton(R.string.settings) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun openPlayer(video: Video) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_VIDEO_PATH, video.path)
            putExtra(PlayerActivity.EXTRA_VIDEO_TITLE, video.displayName)
        }
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_folder -> {
                openFolderPicker()
                true
            }
            R.id.action_scan -> {
                viewModel.startScan()
                true
            }
            R.id.action_stop_scan -> {
                // VideoScanner no tiene cancel() público, se ignora por ahora
                true
            }
            R.id.action_settings -> {
                // TODO: Abrir SettingsActivity
                true
            }
            R.id.action_playlists -> {
                // TODO: Abrir PlaylistActivity
                true
            }
            R.id.action_history -> {
                // TODO: Abrir HistoryActivity
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        folderPickerLauncher.launch(intent)
    }

    private fun getPathFromTreeUri(uri: Uri): String? {
        // Convertir URI de árbol a path real
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val authority = uri.authority

        return when (authority) {
            "com.android.externalstorage.documents" -> {
                val parts = docId.split(":")
                if (parts.size == 2) {
                    val type = parts[0]
                    val path = parts[1]
                    when (type) {
                        "primary" -> Environment.getExternalStorageDirectory().absolutePath + "/" + path
                        "home" -> Environment.getExternalStorageDirectory().absolutePath + "/" + path
                        else -> "/storage/$type/$path"
                    }
                } else {
                    null
                }
            }
            "com.android.providers.media.documents" -> {
                // MediaStore URI
                val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
                val selection = "${MediaStore.Files.FileColumns._ID}=?"
                val selectionArgs = arrayOf(docId.split(":").last())
                contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
                    } else null
                }
            }
            else -> null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}