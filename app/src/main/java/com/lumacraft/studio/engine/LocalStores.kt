package com.lumacraft.studio.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.lumacraft.studio.model.Adjustments
import com.lumacraft.studio.model.AppSettings
import com.lumacraft.studio.model.AiState
import com.lumacraft.studio.model.BrushMode
import com.lumacraft.studio.model.BrushStroke
import com.lumacraft.studio.model.CurveChannel
import com.lumacraft.studio.model.DrawStroke
import com.lumacraft.studio.model.EmojiLayer
import com.lumacraft.studio.model.EditState
import com.lumacraft.studio.model.ExportFormat
import com.lumacraft.studio.model.FrameStyle
import com.lumacraft.studio.model.GeoState
import com.lumacraft.studio.model.HealSpot
import com.lumacraft.studio.model.HslZone
import com.lumacraft.studio.model.PresetItem
import com.lumacraft.studio.model.RecentItem
import com.lumacraft.studio.model.SplitTone
import com.lumacraft.studio.model.TapFix
import com.lumacraft.studio.model.TextLayer
import com.lumacraft.studio.model.defaultHslZones

// ------------------------------------------------------------------ JSON (de)serialization

object StateJson {

    private val ADJ_KEYS = listOf(
        "exposure", "brightness", "contrast", "highlights", "shadows", "whites", "blacks",
        "saturation", "vibrance", "temperature", "tint", "hueShift", "sharpness", "clarity",
        "texture", "dehaze", "fade", "vignette", "grain", "noiseReduction"
    )

    fun toJson(s: EditState): String {
        val o = JSONObject()
        val a = JSONObject()
        val adj = listOf(
            s.adjust.exposure, s.adjust.brightness, s.adjust.contrast, s.adjust.highlights,
            s.adjust.shadows, s.adjust.whites, s.adjust.blacks, s.adjust.saturation,
            s.adjust.vibrance, s.adjust.temperature, s.adjust.tint, s.adjust.hueShift,
            s.adjust.sharpness, s.adjust.clarity, s.adjust.texture, s.adjust.dehaze,
            s.adjust.fade, s.adjust.vignette, s.adjust.grain, s.adjust.noiseReduction
        )
        ADJ_KEYS.forEachIndexed { i, k -> a.put(k, adj[i].toDouble()) }
        o.put("adjust", a)

        o.put(
            "split",
            JSONArray(
                listOf(
                    s.split.shadowHue, s.split.shadowSat, s.split.highlightHue,
                    s.split.highlightSat, s.split.balance
                ).map { it.toDouble() }
            )
        )

        val cur = JSONObject()
        listOf(CurveChannel.MASTER, CurveChannel.RED, CurveChannel.GREEN, CurveChannel.BLUE).forEach { ch ->
            cur.put(ch.name, JSONArray(s.curves[ch]?.map { it.toDouble() } ?: listOf(0.0, 0.25, 0.5, 0.75, 1.0)))
        }
        o.put("curves", cur)

        val hsl = JSONArray()
        s.hsl.forEach { z -> hsl.put(JSONArray(listOf(z.hue, z.sat, z.lum).map { it.toDouble() })) }
        o.put("hsl", hsl)

        o.put(
            "wheels",
            JSONArray(
                listOf(
                    s.shadowWheel.x, s.shadowWheel.y, s.midWheel.x, s.midWheel.y,
                    s.highWheel.x, s.highWheel.y
                ).map { it.toDouble() }
            )
        )
        o.put("filterId", s.filterId ?: "")
        o.put("filterIntensity", s.filterIntensity.toDouble())

        val g = JSONObject()
        g.put("aspect", s.geo.aspect)
        g.put("straighten", s.geo.straighten.toDouble())
        g.put("kv", s.geo.keystoneV.toDouble())
        g.put("kh", s.geo.keystoneH.toDouble())
        g.put("turns", s.geo.quarterTurns)
        g.put("flipH", s.geo.flipH)
        g.put("flipV", s.geo.flipV)
        o.put("geo", g)

        o.put("skinSmooth", s.skinSmooth.toDouble())
        o.put("naturalEnhance", s.naturalEnhance.toDouble())

        fun spots(list: List<HealSpot>): JSONArray {
            val arr = JSONArray()
            list.forEach { arr.put(JSONArray(listOf(it.x, it.y, it.r).map { v -> v.toDouble() })) }
            return arr
        }
        fun taps(list: List<TapFix>): JSONArray {
            val arr = JSONArray()
            list.forEach { arr.put(JSONArray(listOf(it.x, it.y, it.r).map { v -> v.toDouble() })) }
            return arr
        }
        o.put("healSpots", spots(s.healSpots))
        o.put("redEye", taps(s.redEyeSpots))
        o.put("teeth", taps(s.teethSpots))

        val strokes = JSONArray()
        s.brushStrokes.forEach { st ->
            val arr = JSONArray()
            arr.put(st.mode.ordinal)
            arr.put(st.radius.toDouble())
            st.points.forEach { p -> arr.put(p.first.toDouble()); arr.put(p.second.toDouble()) }
            strokes.put(arr)
        }
        o.put("brushStrokes", strokes)

        val ai = JSONObject()
        ai.put("bgBlur", s.ai.bgBlur.toDouble())
        ai.put("subjectPop", s.ai.subjectPop.toDouble())
        ai.put("bgDim", s.ai.bgDim.toDouble())
        ai.put("cutout", s.ai.cutout)
        ai.put("bgColor", s.ai.bgColor ?: -1L)
        ai.put("showMask", s.ai.showMask)
        ai.put("maskInvert", s.ai.maskInvert)
        o.put("ai", ai)

        val texts = JSONArray()
        s.textLayers.forEach { t ->
            texts.put(
                JSONArray(
                    listOf(
                        t.text, t.fontIdx, t.sizeRel, t.color, t.bold, t.italic,
                        t.centered, t.x, t.y, t.opacity
                    )
                )
            )
        }
        o.put("texts", texts)

        val emojis = JSONArray()
        s.emojiLayers.forEach { e -> emojis.put(JSONArray(listOf(e.emoji, e.sizeRel, e.x, e.y))) }
        o.put("emojis", emojis)

        val draws = JSONArray()
        s.drawStrokes.forEach { d ->
            val arr = JSONArray()
            arr.put(d.color)
            arr.put(d.widthRel.toDouble())
            d.points.forEach { p -> arr.put(p.first.toDouble()); arr.put(p.second.toDouble()) }
            draws.put(arr)
        }
        o.put("draws", draws)

        o.put("frame", s.frame.ordinal)
        o.put("watermark", s.watermark)
        return o.toString()
    }

    fun fromJson(raw: String): EditState? = try {
        val o = JSONObject(raw)
        val aObj = o.optJSONObject("adjust") ?: JSONObject()
        val vals = ADJ_KEYS.map { aObj.optDouble(it, 0.0).toFloat() }
        val adjust = Adjustments(
            exposure = vals[0], brightness = vals[1], contrast = vals[2], highlights = vals[3],
            shadows = vals[4], whites = vals[5], blacks = vals[6], saturation = vals[7],
            vibrance = vals[8], temperature = vals[9], tint = vals[10], hueShift = vals[11],
            sharpness = vals[12], clarity = vals[13], texture = vals[14], dehaze = vals[15],
            fade = vals[16], vignette = vals[17], grain = vals[18], noiseReduction = vals[19]
        )

        val splitArr = o.optJSONArray("split")
        val split = if (splitArr != null && splitArr.length() == 5) {
            SplitTone(
                splitArr.getDouble(0).toFloat(), splitArr.getDouble(1).toFloat(),
                splitArr.getDouble(2).toFloat(), splitArr.getDouble(3).toFloat(),
                splitArr.getDouble(4).toFloat()
            )
        } else SplitTone()

        val cur = JSONObject()
        val curves = mapOf(
            CurveChannel.MASTER to readCurve(o.optJSONObject("curves")?.optJSONArray("MASTER")),
            CurveChannel.RED to readCurve(o.optJSONObject("curves")?.optJSONArray("RED")),
            CurveChannel.GREEN to readCurve(o.optJSONObject("curves")?.optJSONArray("GREEN")),
            CurveChannel.BLUE to readCurve(o.optJSONObject("curves")?.optJSONArray("BLUE"))
        )

        val baseZones = defaultHslZones()
        val hslArr = o.optJSONArray("hsl")
        val hsl = if (hslArr != null && hslArr.length() == baseZones.size) {
            baseZones.mapIndexed { i, z ->
                val arr = hslArr.getJSONArray(i)
                z.copy(hue = arr.getDouble(0).toFloat(), sat = arr.getDouble(1).toFloat(), lum = arr.getDouble(2).toFloat())
            }
        } else baseZones

        val wArr = o.optJSONArray("wheels")
        val wheels = if (wArr != null && wArr.length() == 6) {
            listOf(
                com.lumacraft.studio.model.WheelGrade(wArr.getDouble(0).toFloat(), wArr.getDouble(1).toFloat()),
                com.lumacraft.studio.model.WheelGrade(wArr.getDouble(2).toFloat(), wArr.getDouble(3).toFloat()),
                com.lumacraft.studio.model.WheelGrade(wArr.getDouble(4).toFloat(), wArr.getDouble(5).toFloat())
            )
        } else {
            listOf(
                com.lumacraft.studio.model.WheelGrade(),
                com.lumacraft.studio.model.WheelGrade(),
                com.lumacraft.studio.model.WheelGrade()
            )
        }

        val gObj = o.optJSONObject("geo") ?: JSONObject()
        val geo = GeoState(
            aspect = gObj.optString("aspect", "Original"),
            straighten = gObj.optDouble("straighten", 0.0).toFloat(),
            keystoneV = gObj.optDouble("kv", 0.0).toFloat(),
            keystoneH = gObj.optDouble("kh", 0.0).toFloat(),
            quarterTurns = gObj.optInt("turns", 0),
            flipH = gObj.optBoolean("flipH", false),
            flipV = gObj.optBoolean("flipV", false)
        )

        fun readSpots(key: String): List<HealSpot> {
            val arr = o.optJSONArray(key) ?: return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONArray(i)
                    add(HealSpot(s.getDouble(0).toFloat(), s.getDouble(1).toFloat(), s.getDouble(2).toFloat()))
                }
            }
        }
        fun readTaps(key: String): List<TapFix> {
            val arr = o.optJSONArray(key) ?: return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONArray(i)
                    add(TapFix(s.getDouble(0).toFloat(), s.getDouble(1).toFloat(), s.getDouble(2).toFloat()))
                }
            }
        }

        val strokes = buildList {
            val arr = o.optJSONArray("brushStrokes") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val s = arr.getJSONArray(i)
                val mode = BrushMode.entries.getOrElse(s.getInt(0)) { BrushMode.BLUR }
                val radius = s.getDouble(1).toFloat()
                val pts = mutableListOf<Pair<Float, Float>>()
                var k = 2
                while (k + 1 < s.length()) {
                    pts.add(s.getDouble(k).toFloat() to s.getDouble(k + 1).toFloat())
                    k += 2
                }
                add(BrushStroke(mode, radius, pts))
            }
        }

        val aiObj = o.optJSONObject("ai") ?: JSONObject()
        val bgCol = aiObj.optLong("bgColor", -1L)
        val ai = AiState(
            bgBlur = aiObj.optDouble("bgBlur", 0.0).toFloat(),
            subjectPop = aiObj.optDouble("subjectPop", 0.0).toFloat(),
            bgDim = aiObj.optDouble("bgDim", 0.0).toFloat(),
            cutout = aiObj.optBoolean("cutout", false),
            bgColor = if (bgCol == -1L) null else bgCol,
            showMask = aiObj.optBoolean("showMask", false),
            maskInvert = aiObj.optBoolean("maskInvert", false)
        )

        val texts = buildList {
            val arr = o.optJSONArray("texts") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val t = arr.getJSONArray(i)
                add(
                    TextLayer(
                        text = t.getString(0),
                        fontIdx = t.optInt(1, 0),
                        sizeRel = t.optDouble(2, 0.07).toFloat(),
                        color = t.optLong(3, 0xFFFFFFFF),
                        bold = t.optBoolean(4, true),
                        italic = t.optBoolean(5, false),
                        centered = t.optBoolean(6, true),
                        x = t.optDouble(7, 0.5).toFloat(),
                        y = t.optDouble(8, 0.85).toFloat(),
                        opacity = t.optDouble(9, 1.0).toFloat()
                    )
                )
            }
        }

        val emojis = buildList {
            val arr = o.optJSONArray("emojis") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val e = arr.getJSONArray(i)
                add(EmojiLayer(e.getString(0), e.optDouble(1, 0.14).toFloat(), e.optDouble(2, 0.5).toFloat(), e.optDouble(3, 0.3).toFloat()))
            }
        }

        val draws = buildList {
            val arr = o.optJSONArray("draws") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val d = arr.getJSONArray(i)
                val color = d.getLong(0)
                val width = d.getDouble(1).toFloat()
                val pts = mutableListOf<Pair<Float, Float>>()
                var k = 2
                while (k + 1 < d.length()) {
                    pts.add(d.getDouble(k).toFloat() to d.getDouble(k + 1).toFloat())
                    k += 2
                }
                add(DrawStroke(color, width, pts))
            }
        }

        EditState(
            adjust = adjust,
            split = split,
            curves = curves,
            hsl = hsl,
            shadowWheel = wheels[0],
            midWheel = wheels[1],
            highWheel = wheels[2],
            filterId = o.optString("filterId", "").ifEmpty { null },
            filterIntensity = o.optDouble("filterIntensity", 100.0).toFloat(),
            geo = geo,
            skinSmooth = o.optDouble("skinSmooth", 0.0).toFloat(),
            naturalEnhance = o.optDouble("naturalEnhance", 0.0).toFloat(),
            healSpots = readSpots("healSpots"),
            redEyeSpots = readTaps("redEye"),
            teethSpots = readTaps("teeth"),
            brushStrokes = strokes,
            ai = ai,
            textLayers = texts,
            emojiLayers = emojis,
            drawStrokes = draws,
            frame = FrameStyle.entries.getOrElse(o.optInt("frame", 0)) { FrameStyle.NONE },
            watermark = o.optString("watermark", "")
        )
    } catch (_: Exception) {
        null
    }

    private fun readCurve(arr: JSONArray?): List<Float> {
        if (arr == null || arr.length() != 5) return listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        return List(5) { i -> arr.getDouble(i).toFloat() }
    }
}

// ------------------------------------------------------------------ persistent stores

object LocalStores {

    private const val PREFS = "lumacraft_prefs"
    private const val KEY_SETTINGS = "settings_json"
    private const val KEY_PRESETS = "user_presets_json"
    private const val KEY_RECENTS = "recent_projects_json"
    private const val KEY_SESSIONS = "session_map_json"

    // ---- settings ----
    fun loadSettings(ctx: Context): AppSettings = try {
        val raw = prefs(ctx).getString(KEY_SETTINGS, null) ?: return AppSettings()
        val o = JSONObject(raw)
        AppSettings(
            themeMode = o.optString("themeMode", "System"),
            haptics = o.optBoolean("haptics", true),
            defaultFormat = ExportFormat.entries.getOrElse(o.optInt("fmt", 0)) { ExportFormat.JPEG },
            defaultQuality = o.optInt("quality", 92),
            stripExifDefault = o.optBoolean("stripExif", true),
            watermarkDefault = o.optString("watermark", "")
        )
    } catch (_: Exception) {
        AppSettings()
    }

    fun saveSettings(ctx: Context, s: AppSettings) {
        val o = JSONObject()
            .put("themeMode", s.themeMode)
            .put("haptics", s.haptics)
            .put("fmt", s.defaultFormat.ordinal)
            .put("quality", s.defaultQuality)
            .put("stripExif", s.stripExifDefault)
            .put("watermark", s.watermarkDefault)
        prefs(ctx).edit().putString(KEY_SETTINGS, o.toString()).apply()
    }

    // ---- user presets vault ----
    fun loadUserPresets(ctx: Context): List<PresetItem> = try {
        val raw = prefs(ctx).getString(KEY_PRESETS, null) ?: return emptyList()
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val state = StateJson.fromJson(o.optString("state")) ?: continue
                add(PresetItem(o.getString("id"), o.getString("name"), o.getLong("ts"), state))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun saveUserPreset(ctx: Context, name: String, state: EditState): List<PresetItem> {
        val list = loadUserPresets(ctx).toMutableList()
        val item = PresetItem("usr_${System.currentTimeMillis()}", name.trim().ifEmpty { "My Preset" }, System.currentTimeMillis(), state)
        list.add(0, item)
        persistPresets(ctx, list)
        return list
    }

    fun renameUserPreset(ctx: Context, id: String, newName: String): List<PresetItem> {
        val list = loadUserPresets(ctx).map { if (it.id == id) it.copy(name = newName.trim().ifEmpty { it.name }) else it }
        persistPresets(ctx, list)
        return list
    }

    fun deleteUserPreset(ctx: Context, id: String): List<PresetItem> {
        val list = loadUserPresets(ctx).filterNot { it.id == id }
        persistPresets(ctx, list)
        return list
    }

    private fun persistPresets(ctx: Context, list: List<PresetItem>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().put("id", p.id).put("name", p.name).put("ts", p.createdAt).put("state", StateJson.toJson(p.state)))
        }
        prefs(ctx).edit().putString(KEY_PRESETS, arr.toString()).apply()
    }

    // ---- recent projects ----
    fun loadRecents(ctx: Context): List<RecentItem> = try {
        val raw = prefs(ctx).getString(KEY_RECENTS, null) ?: return emptyList()
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(RecentItem(o.getString("uri"), o.getString("name"), o.getLong("ts"), o.optBoolean("session", false)))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun addRecent(ctx: Context, uri: String, name: String): List<RecentItem> {
        val existing = loadRecents(ctx).filterNot { it.uri == uri }
        val hasSession = prefs(ctx).getString(KEY_SESSIONS, "{}")?.let {
            try { JSONObject(it).has(uri) } catch (_: Exception) { false }
        } ?: false
        val list = (listOf(RecentItem(uri, name, System.currentTimeMillis(), hasSession)) + existing).take(12)
        persistRecents(ctx, list)
        return list
    }

    private fun persistRecents(ctx: Context, list: List<RecentItem>) {
        val arr = JSONArray()
        list.forEach { r -> arr.put(JSONObject().put("uri", r.uri).put("name", r.name).put("ts", r.timestamp).put("session", r.hasSession)) }
        prefs(ctx).edit().putString(KEY_RECENTS, arr.toString()).apply()
    }

    // ---- editing session persistence (restore after app restart) ----
    fun saveSession(ctx: Context, uri: String, state: EditState) {
        val map = try {
            JSONObject(prefs(ctx).getString(KEY_SESSIONS, "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        map.put(uri, StateJson.toJson(state))
        prefs(ctx).edit().putString(KEY_SESSIONS, map.toString()).apply()
        // refresh recents badge
        val recents = loadRecents(ctx).map { if (it.uri == uri) it.copy(hasSession = true) else it }
        persistRecents(ctx, recents)
    }

    fun loadSession(ctx: Context, uri: String): EditState? = try {
        val map = JSONObject(prefs(ctx).getString(KEY_SESSIONS, "{}") ?: "{}")
        map.optString(uri, "").takeIf { it.isNotEmpty() }?.let { StateJson.fromJson(it) }
    } catch (_: Exception) {
        null
    }

    fun deleteSession(ctx: Context, uri: String) {
        val map = try {
            JSONObject(prefs(ctx).getString(KEY_SESSIONS, "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        map.remove(uri)
        prefs(ctx).edit().putString(KEY_SESSIONS, map.toString()).apply()
    }

    // ---- cache management ----
    fun clearPreviewCache(ctx: Context) {
        prefs(ctx).edit().remove(KEY_SESSIONS).apply()
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
