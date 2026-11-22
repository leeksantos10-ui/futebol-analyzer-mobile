package com.footballlab.mobile

import java.text.Normalizer

data class ImportedMatch(val date: String, val competition: String, val home: String, val away: String, val homeScore: Int?, val awayScore: Int?)

object CsvImporter {
    private fun key(v: String) = Normalizer.normalize(v.trim().lowercase(), Normalizer.Form.NFD).replace("\\p{M}".toRegex(), "").replace("[^a-z0-9]+".toRegex(), "")
    private fun rows(text: String): List<List<String>> {
        val out = mutableListOf<List<String>>(); var row = mutableListOf<String>(); val cell = StringBuilder(); var quoted = false; var i=0
        while(i<text.length) { val c=text[i]; when {
            c=='"' && quoted && i+1<text.length && text[i+1]=='"' -> { cell.append('"'); i++ }
            c=='"' -> quoted=!quoted
            (c==',' || c==';' || c=='\\t') && !quoted -> { row.add(cell.toString().trim()); cell.setLength(0) }
            (c=='\\n' || c=='\\r') && !quoted -> { if(c=='\\r' && i+1<text.length && text[i+1]=='\\n') i++; row.add(cell.toString().trim()); cell.setLength(0); if(row.any{it.isNotBlank()})out.add(row); row= mutableListOf() }
            else -> cell.append(c)
        }; i++ }
        row.add(cell.toString().trim()); if(row.any{it.isNotBlank()})out.add(row); return out
    }
    fun importCsv(raw: String): Pair<List<ImportedMatch>, String> {
        val parsed=rows(raw.removePrefix("\\uFEFF")); if(parsed.size<2)return Pair(emptyList(),"CSV sem linhas de dados.")
        val headers=parsed.first().map(::key)
        fun find(vararg names:String)=headers.indexOfFirst{it in names.map(::key)}
        val h=find("home","hometeam","teamhome","mandante","equipemandante","casa","timecasa"); val a=find("away","awayteam","teamaway","visitante","equipevisitante","fora","timefora")
        if(h<0||a<0)return Pair(emptyList(),"Cabeçalhos não reconhecidos: informe Mandante/Home e Visitante/Away.")
        val d=find("date","matchdate","data","datapartida"); val l=find("league","competition","championship","liga","campeonato","competicao")
        val hs=find("fthg","homegoals","homescore","scorehome","golsmandante","golscasa"); val ascore=find("ftag","awaygoals","awayscore","scoreaway","golsvisitante","golsfora")
        fun value(row:List<String>,idx:Int)=if(idx>=0&&idx<row.size)row[idx].trim() else ""
        val matches=parsed.drop(1).mapNotNull{row->val home=value(row,h);val away=value(row,a);if(home.isBlank()||away.isBlank())null else ImportedMatch(value(row,d),value(row,l),home,away,value(row,hs).toIntOrNull(),value(row,ascore).toIntOrNull())}
        return Pair(matches,if(matches.isEmpty())"Nenhuma partida válida encontrada." else "${matches.size} partidas identificadas.")
    }
}
