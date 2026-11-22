package com.footballlab.mobile

import java.text.Normalizer

data class ImportedMatch(val date: String, val competition: String, val home: String, val away: String, val homeScore: Int?, val awayScore: Int?)

object CsvImporter {
    private fun headerKey(value: String): String = Normalizer.normalize(value.trim().lowercase(), Normalizer.Form.NFD)
        .replace("\\p{M}".toRegex(), "").replace("[^a-z0-9]+".toRegex(), "")

    private fun parseRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '\"' && quoted && i + 1 < text.length && text[i + 1] == '\"' -> { cell.append('\"'); i++ }
                ch == '\"' -> quoted = !quoted
                (ch == ',' || ch == ';' || ch == '\\t') && !quoted -> { row.add(cell.toString().trim()); cell.setLength(0) }
                (ch == '\\n' || ch == '\\r') && !quoted -> {
                    if (ch == '\\r' && i + 1 < text.length && text[i + 1] == '\\n') i++
                    row.add(cell.toString().trim()); cell.setLength(0)
                    if (row.any { it.isNotBlank() }) rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(ch)
            }
            i++
        }
        row.add(cell.toString().trim())
        if (row.any { it.isNotBlank() }) rows.add(row)
        return rows
    }

    fun importCsv(raw: String): Pair<List<ImportedMatch>, String> {
        val rows = parseRows(raw.removePrefix("\\uFEFF"))
        if (rows.size < 2) return Pair(emptyList(), "CSV sem linhas de dados.")
        val headers = rows.first().map(::headerKey)
        fun find(vararg names: String): Int = headers.indexOfFirst { it in names.map(::headerKey) }
        val home = find("home", "hometeam", "teamhome", "mandante", "equipemandante", "casa", "timecasa")
        val away = find("away", "awayteam", "teamaway", "visitante", "equipevisitante", "fora", "timefora")
        if (home < 0 || away < 0) return Pair(emptyList(), "Cabeçalhos não reconhecidos. Necessário Home/Away ou Mandante/Visitante.")
        val date = find("date", "matchdate", "data", "datapartida")
        val comp = find("league", "competition", "championship", "liga", "campeonato", "competicao")
        val hs = find("fthg", "homegoals", "homescore", "scorehome", "golsmandante", "golscasa")
        val ascore = find("ftag", "awaygoals", "awayscore", "scoreaway", "golsvisitante", "golsfora")
        fun cell(row: List<String>, index: Int): String = if (index >= 0 && index < row.size) row[index].trim() else ""
        val result = rows.drop(1).mapNotNull { row ->
            val h = cell(row, home); val a = cell(row, away)
            if (h.isBlank() || a.isBlank()) null else ImportedMatch(cell(row, date), cell(row, comp), h, a, cell(row, hs).toIntOrNull(), cell(row, ascore).toIntOrNull())
        }
        return Pair(result, if (result.isEmpty()) "Nenhuma partida válida encontrada." else result.size.toString() + " partidas identificadas no CSV.")
    }
}