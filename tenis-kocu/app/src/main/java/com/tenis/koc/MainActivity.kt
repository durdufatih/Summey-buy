package com.tenis.koc

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.tenis.koc.data.SessionStore
import com.tenis.koc.data.SettingsStore
import com.tenis.koc.model.Session
import com.tenis.koc.ui.HistoryScreen
import com.tenis.koc.ui.HomeScreen
import com.tenis.koc.ui.LiveSessionScreen
import com.tenis.koc.ui.SessionDetailScreen
import com.tenis.koc.ui.SettingsScreen
import com.tenis.koc.ui.TenisTheme
import com.tenis.koc.ui.VideoAnalysisScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = SessionStore(applicationContext)
        val settings = SettingsStore(applicationContext)
        setContent {
            TenisTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    TenisApp(store, settings)
                }
            }
        }
    }
}

private sealed interface Screen {
    data object Home : Screen
    data object History : Screen
    data object Settings : Screen
    data object Live : Screen
    data class Video(val uri: Uri) : Screen
    data class Detail(val id: Long) : Screen
}

@Composable
private fun TenisApp(store: SessionStore, settings: SettingsStore) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) screen = Screen.Live
        else Toast.makeText(context, "Analiz için kamera izni gerekli", Toast.LENGTH_LONG).show()
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) screen = Screen.Video(uri)
    }
    val startLive: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            screen = Screen.Live
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
    val onSessionDone: (Session?) -> Unit = { s ->
        if (s == null) {
            Toast.makeText(context, "Hiç vuruş algılanmadı. Tüm vücudun kadrajda mı?", Toast.LENGTH_LONG).show()
            screen = Screen.Home
        } else {
            store.add(s)
            screen = Screen.Detail(s.id)
        }
    }

    when (val s = screen) {
        Screen.Live -> {
            BackHandler { screen = Screen.Home }
            LiveSessionScreen(settings, onFinish = onSessionDone)
        }
        is Screen.Video -> {
            BackHandler { screen = Screen.Home }
            VideoAnalysisScreen(s.uri, settings, onDone = onSessionDone, onCancel = { screen = Screen.Home })
        }
        is Screen.Detail -> {
            val session = store.find(s.id)
            BackHandler { screen = Screen.Home }
            if (session == null) {
                LaunchedEffect(Unit) { screen = Screen.Home }
            } else {
                val previous = store.sessions.filter {
                    it.startedAt < session.startedAt && it.startedAt > session.startedAt - 14L * 24 * 3600 * 1000
                }
                SessionDetailScreen(
                    session = session,
                    previous = previous,
                    onBack = { screen = Screen.Home },
                    onDelete = { store.delete(session.id); screen = Screen.History },
                )
            }
        }
        else -> Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = s == Screen.Home,
                        onClick = { screen = Screen.Home },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Bugün") },
                    )
                    NavigationBarItem(
                        selected = s == Screen.History,
                        onClick = { screen = Screen.History },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Geçmiş") },
                    )
                    NavigationBarItem(
                        selected = s == Screen.Settings,
                        onClick = { screen = Screen.Settings },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Ayarlar") },
                    )
                }
            },
        ) { pad ->
            val m = Modifier.padding(pad)
            when (s) {
                Screen.History -> HistoryScreen(store.sessions, onOpen = { screen = Screen.Detail(it.id) }, modifier = m)
                Screen.Settings -> SettingsScreen(settings, store, modifier = m)
                else -> HomeScreen(
                    store, settings,
                    onStartLive = startLive,
                    onPickVideo = { videoPicker.launch("video/*") },
                    modifier = m,
                )
            }
        }
    }
}
