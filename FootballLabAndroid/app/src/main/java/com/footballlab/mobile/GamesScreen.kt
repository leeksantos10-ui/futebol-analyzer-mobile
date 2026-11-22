package com.footballlab.mobile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GamesScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { MatchDatabase(context) }
    var search by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Importe um arquivo CSV com colunas Mandante/Home e Visitante/Away.") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            val (matches, parseMessage) = CsvImporter.importCsv(raw)
            if (matches.isNotEmpty()) {
                val (added, duplicates) = db.insertAll(matches)
                message = "$parseMessage\n$added inseridos • $duplicates duplicados ignorados."
                refresh++
            } else message = parseMessage
        } catch (e: Exception) { message = "Erro ao importar: ${e.localizedMessage ?: "arquivo inválido"}" }
    }
    val games = remember(search, refresh) { db.all(search) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151E1B)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Base de partidas", color = Color(0xFF35D07F), fontWeight = FontWeight.Bold)
                Text("Total armazenado: ${db.count()}", color = Color.White)
                Button(onClick = { picker.launch(arrayOf("text/*", "application/vnd.ms-excel", "text/csv")) }, modifier = Modifier.fillMaxWidth()) { Text("Importar CSV") }
                OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Buscar time, campeonato ou data") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(message, color = Color(0xFF9BA9A2), style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("Partidas (até 500)", color = Color(0xFF35D07F), fontWeight = FontWeight.Bold)
        if (games.isEmpty()) Text("Nenhuma partida encontrada.", color = Color(0xFF9BA9A2))
        else games.forEach { game ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151E1B)), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(game.date.ifBlank { "Data não informada" }, color = Color(0xFF9BA9A2), style = MaterialTheme.typography.labelSmall)
                        Text("${game.home}  ${game.homeScore?.toString() ?: "-"} x ${game.awayScore?.toString() ?: "-"}  ${game.away}", color = Color.White, fontWeight = FontWeight.SemiBold)
                        if (game.competition.isNotBlank()) Text(game.competition, color = Color(0xFF9BA9A2), style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { db.delete(game.id); refresh++ }) { Text("Excluir", color = Color(0xFFFF7B7B)) }
                }
            }
        }
    }
}
