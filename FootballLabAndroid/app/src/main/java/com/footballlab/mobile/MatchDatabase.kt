package com.footballlab.mobile

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class StoredMatch(val id: Long, val date: String, val competition: String, val home: String, val away: String, val homeScore: Int?, val awayScore: Int?)

class MatchDatabase(context: Context) : SQLiteOpenHelper(context, "football_lab.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE matches (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, competition TEXT NOT NULL, home TEXT NOT NULL, away TEXT NOT NULL, home_score INTEGER, away_score INTEGER, unique_key TEXT NOT NULL UNIQUE)")
        db.execSQL("CREATE INDEX matches_date_idx ON matches(date)")
        db.execSQL("CREATE INDEX matches_teams_idx ON matches(home, away)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    private fun uniqueKey(m: ImportedMatch) = listOf(m.date.trim().lowercase(),m.home.trim().lowercase(),m.away.trim().lowercase()).joinToString("|")
    fun insertAll(matches: List<ImportedMatch>): Pair<Int,Int> {
        val db=writableDatabase; var added=0; var skipped=0
        db.beginTransaction()
        try { matches.forEach { m ->
            val values=ContentValues().apply { put("date",m.date);put("competition",m.competition);put("home",m.home);put("away",m.away); if(m.homeScore==null)putNull("home_score") else put("home_score",m.homeScore);if(m.awayScore==null)putNull("away_score") else put("away_score",m.awayScore);put("unique_key",uniqueKey(m)) }
            val id=db.insertWithOnConflict("matches",null,values,SQLiteDatabase.CONFLICT_IGNORE); if(id==-1L)skipped++ else added++
        };db.setTransactionSuccessful() } finally { db.endTransaction() }
        return Pair(added,skipped)
    }
    fun all(search: String = ""): List<StoredMatch> {
        val out=mutableListOf<StoredMatch>(); val q="%"+search.trim()+"%"
        readableDatabase.query("matches",null,if(search.isBlank())null else "home LIKE ? OR away LIKE ? OR competition LIKE ? OR date LIKE ?",if(search.isBlank())null else arrayOf(q,q,q,q),null,null,"date DESC, id DESC","500").use { c ->
            while(c.moveToNext()) out.add(StoredMatch(c.getLong(c.getColumnIndexOrThrow("id")),c.getString(c.getColumnIndexOrThrow("date")),c.getString(c.getColumnIndexOrThrow("competition")),c.getString(c.getColumnIndexOrThrow("home")),c.getString(c.getColumnIndexOrThrow("away")),if(c.isNull(c.getColumnIndexOrThrow("home_score")))null else c.getInt(c.getColumnIndexOrThrow("home_score")),if(c.isNull(c.getColumnIndexOrThrow("away_score")))null else c.getInt(c.getColumnIndexOrThrow("away_score"))))
        }; return out
    }
    fun count(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM matches",null).use{if(it.moveToFirst())it.getInt(0)else 0}
    fun delete(id: Long) { writableDatabase.delete("matches","id=?",arrayOf(id.toString())) }
}
