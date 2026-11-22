package com.footballlab.mobile

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity\nimport androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*\nimport androidx.compose.foundation.text.KeyboardOptions\nimport androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

private val Bg = Color(0xFF0B1110)
private val Panel = Color(0xFF151E1B)
private val Accent = Color(0xFF35D07F)
private val Muted = Color(0xFF9BA9A2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FootballLabApp() }
    }
}

@Composable
fun FootballLabApp() {
    var tab by remember { mutableStateOf("Início") }
    val tabs = listOf("Início", "Jogos", "Métricas", "Filtros", "Backtest", "Atualizações")
    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Accent, secondary = Accent)) {
        Scaffold(containerColor = Bg, bottomBar = {
            NavigationBar(containerColor = Panel) {
                tabs.forEach { label ->
                    NavigationBarItem(selected = tab == label, onClick = { tab = label },
                        icon = { Text(when(label) { "Início" -> "⌂"; "Jogos" -> "⚽"; "Métricas" -> "▤"; "Filtros" -> "▽"; "Backtest" -> "↗"; else -> "⟳" }) },
                        label = { Text(label) }, colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Accent, selectedTextColor = Accent,
                            indicatorColor = Color(0xFF20392D), unselectedTextColor = Muted
                        ))
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).background(Bg)) {
                Spacer(Modifier.height(22.dp))
                Text("FOOTBALL LAB", color = Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("Seu laboratório de futebol", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                when(tab) {
                    "Início" -> HomeScreen()
                    "Jogos" -> GamesScreen()
                    "Métricas" -> PlaceholderScreen("Métricas", "Crie e gerencie suas próprias métricas.")
                    "Filtros" -> PlaceholderScreen("Filtros", "Combine métricas para montar estratégias.")
                    "Backtest" -> PlaceholderScreen("Backtest", "Teste estratégias no histórico e investigue os resultados.")
                    else -> UpdateScreen()
                }
            }
        }
    }
}

@Composable
private fun UpdateScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var status by remember { mutableStateOf("Confira se há uma nova versão disponível.") }
    var checking by remember { mutableStateOf(false) }
    var availableVersion by remember { mutableStateOf<String?>(null) }
    var apkUrl by remember { mutableStateOf<String?>(null) }
    val installedVersion = BuildConfig.VERSION_NAME

    fun checkUpdate() {
        checking = true
        status = "Consultando versão mais recente…"
        thread {
            try {
                val connection = (URL("https://api.github.com/repos/leeksantos10-ui/futebol-analyzer-mobile/releases/latest").openConnection() as HttpURLConnection)
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                val release = JSONObject(body)
                val tag = release.optString("tag_name", "").removePrefix("v")
                val assets = release.optJSONArray("assets")
                var download = ""
                if (assets != null) for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name").endsWith(".apk", true)) {
                        download = asset.optString("browser_download_url")
                        break
                    }
                }
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    checking = false
                    if (tag.isNotBlank() && download.isNotBlank()) {
                        availableVersion = tag
                        apkUrl = download
                        status = if (tag != installedVersion) "Nova versão disponível: $tag" else "Você já está usando a versão mais recente."
                    } else status = "Não há APK publicado na última versão. Tente novamente mais tarde."
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    checking = false
                    status = "Não foi possível verificar. Confira sua conexão e tente novamente."
                }
            }
        }
    }

    fun installApk(url: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Football Lab")
                .setDescription("Baixando atualização")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "FootballLab-update.apk")
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            status = "Download iniciado. Quando terminar, abra a notificação para instalar."
        } catch (e: Exception) {
            status = "Não foi possível iniciar o download da atualização."
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Atualizações", color = Accent, fontWeight = FontWeight.Bold)
                Text("Versão instalada: $installedVersion", color = Color.White)
                Text(status, color = Muted)
                Button(onClick = { checkUpdate() }, enabled = !checking, modifier = Modifier.fillMaxWidth()) {
                    if (checking) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Verificar atualizações")
                }
                if (availableVersion != null && availableVersion != installedVersion && apkUrl != null) {
                    Button(onClick = { installApk(apkUrl!!) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Baixar versão $availableVersion")
                    }
                }
            }
        }
        Text("As atualizações serão instaladas sobre a versão existente, preservando os dados do aplicativo, desde que a assinatura digital seja a mesma.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HomeScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Banco de jogos", "Ainda vazio", "Importação Excel será a próxima etapa")
        StatCard("Estatísticas", "—", "Atualizadas conforme novos jogos entram")
        StatCard("Modo de operação", "Offline", "Dados locais no dispositivo")
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Próxima etapa", color = Accent, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Importar sua planilha Excel e salvar os jogos no armazenamento permanente.", color = Color.White)
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(title, color = Muted)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StatCard(title, "Em construção", description)
        Text("Esta área será implementada nas próximas versões, preservando a base do aplicativo.", color = Muted)
    }
}
