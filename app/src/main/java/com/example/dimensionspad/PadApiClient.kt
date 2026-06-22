package com.example.dimensionspad

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// Data models
// ---------------------------------------------------------------------------

data class PadSlot(
    val index: Int,
    val pad: Int,
    val occupied: Boolean,
    val figureId: Int,
    val figureName: String
)

data class ApiResult(val ok: Boolean, val error: String? = null)

// ---------------------------------------------------------------------------
// PadApiClient
//
// All functions are suspend funs that run on Dispatchers.IO.
// Throws IOException on network failure; callers should catch.
// ---------------------------------------------------------------------------

class PadApiClient(private var baseUrl: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun setHost(host: String, port: Int = 8031) {
        baseUrl = "http://$host:$port"
    }

    // GET /list → list of PadSlot
    suspend fun listSlots(): List<PadSlot> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/list").get().build()
        val body = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            resp.body?.string() ?: throw IOException("Empty body")
        }
        val root = JSONObject(body)
        val arr = root.getJSONArray("slots")
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            PadSlot(
                index      = obj.getInt("index"),
                pad        = obj.getInt("pad"),
                occupied   = obj.getBoolean("occupied"),
                figureId   = obj.getInt("figureId"),
                figureName = obj.getString("figureName")
            )
        }
    }

    // POST /place
    suspend fun place(slot: Int, figureId: Int): ApiResult = post(
        "/place",
        JSONObject().apply { put("slot", slot); put("figureId", figureId) }
    )

    // POST /remove
    suspend fun remove(slot: Int): ApiResult = post(
        "/remove",
        JSONObject().apply { put("slot", slot) }
    )

    // POST /move
    suspend fun move(fromSlot: Int, toSlot: Int): ApiResult = post(
        "/move",
        JSONObject().apply { put("fromSlot", fromSlot); put("toSlot", toSlot) }
    )

    private suspend fun post(path: String, body: JSONObject): ApiResult =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("$baseUrl$path")
                .post(body.toString().toRequestBody(JSON))
                .build()
            val respBody = client.newCall(req).execute().use { resp ->
                resp.body?.string() ?: "{}"
            }
            val obj = JSONObject(respBody)
            if (obj.optBoolean("ok", false)) {
                ApiResult(ok = true)
            } else {
                ApiResult(ok = false, error = obj.optString("error", "unknown error"))
            }
        }
}
