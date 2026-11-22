package com.footballlab.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    val tabs = listOf("Início", "Jogos", "Métricas", "Filtros", "Backtest")
    MaterialTheme(colorScheme = darkColorScheme(
        background = Bg, surface = Panel, primary = Accent, secondary = Accent
    )) {
        Scaffold(containerColor = Bg, bottomBar = {
            NavigationBar(containerColor = Panel) {
                tabs.forEach { label ->
                    NavigationBarItem(selected = tab == label, onClick = { tab = label },
                        icon = { Text(when(label) { "Início" -> "⌂"; "Jogos" -> "⚽"; "Métricas" -> "▤"; "Filtros" -> "▽"; else -> "↗" }) },
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
                    "Jogos" -> PlaceholderScreen("Jogos", "Seu histórico de partidas ficará armazenado localmente.")
                    "Métricas" -> PlaceholderScreen("Métricas", "Crie e gerencie suas próprias métricas.")
                    "Filtros" -> PlaceholderScreen("Filtros", "Combine métricas para montar estratégias.")
                    else -> PlaceholderScreen("Backtest", "Teste estratégias no histórico e investigue os resultados.")
                }
            }
        }
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
