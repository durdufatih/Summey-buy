package com.tenis.koc.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tenis.koc.model.Fault
import com.tenis.koc.model.Session
import com.tenis.koc.model.StrokeResult
import com.tenis.koc.model.StrokeType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Antrenmanları cihazda JSON dosyasında saklar. */
class SessionStore(context: Context) {
    private val file = File(context.filesDir, "sessions.json")

    var sessions by mutableStateOf(load())
        private set

    fun add(s: Session) {
        sessions = (sessions + s).sortedByDescending { it.startedAt }
        save()
    }

    fun delete(id: Long) {
        sessions = sessions.filterNot { it.id == id }
        save()
    }

    fun clear() {
        sessions = emptyList()
        save()
    }

    fun find(id: Long): Session? = sessions.firstOrNull { it.id == id }

    private fun load(): List<Session> = try {
        if (!file.exists()) emptyList() else parse(file.readText()).sortedByDescending { it.startedAt }
    } catch (e: Exception) {
        emptyList()
    }

    private fun save() {
        val arr = JSONArray()
        for (s in sessions) {
            val strokes = JSONArray()
            for (st in s.strokes) {
                strokes.put(
                    JSONObject()
                        .put("t", st.timeMs)
                        .put("type", st.type.name)
                        .put("score", st.score)
                        .put("speed", st.speed.toDouble())
                        .put("faults", JSONArray(st.faults.map { it.name }))
                )
            }
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("startedAt", s.startedAt)
                    .put("durationMs", s.durationMs)
                    .put("source", s.source)
                    .put("strokes", strokes)
            )
        }
        file.writeText(arr.toString())
    }

    private fun parse(text: String): List<Session> {
        val arr = JSONArray(text)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val sa = o.getJSONArray("strokes")
            val strokes = (0 until sa.length()).mapNotNull { j ->
                val so = sa.getJSONObject(j)
                val type = runCatching { StrokeType.valueOf(so.getString("type")) }.getOrNull()
                    ?: return@mapNotNull null
                val fa = so.optJSONArray("faults") ?: JSONArray()
                val faults = (0 until fa.length()).mapNotNull { k ->
                    runCatching { Fault.valueOf(fa.getString(k)) }.getOrNull()
                }
                StrokeResult(
                    timeMs = so.getLong("t"),
                    type = type,
                    score = so.getInt("score"),
                    faults = faults,
                    speed = so.optDouble("speed", 0.0).toFloat(),
                )
            }
            Session(
                id = o.getLong("id"),
                startedAt = o.getLong("startedAt"),
                durationMs = o.getLong("durationMs"),
                source = o.optString("source", "Kamera"),
                strokes = strokes,
            )
        }
    }
}

/** Kullanıcı ayarları ve günlük plan işaretleri. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _left = mutableStateOf(prefs.getBoolean("left", false))
    var leftHanded: Boolean
        get() = _left.value
        set(v) { _left.value = v; prefs.edit().putBoolean("left", v).apply() }

    private val _voice = mutableStateOf(prefs.getBoolean("voice", true))
    var voice: Boolean
        get() = _voice.value
        set(v) { _voice.value = v; prefs.edit().putBoolean("voice", v).apply() }

    private val _sens = mutableStateOf(prefs.getInt("sensitivity", 2))
    var sensitivity: Int
        get() = _sens.value
        set(v) { _sens.value = v; prefs.edit().putInt("sensitivity", v).apply() }

    private val _checks = mutableStateOf(prefs.getStringSet("checks", emptySet())?.toSet() ?: emptySet())

    fun isChecked(key: String): Boolean = key in _checks.value

    fun toggle(key: String) {
        val cur = _checks.value
        val next = if (key in cur) cur - key else cur + key
        _checks.value = next
        prefs.edit().putStringSet("checks", next).apply()
    }
}
