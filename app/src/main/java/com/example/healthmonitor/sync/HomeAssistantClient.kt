package com.example.healthmonitor.sync

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object HomeAssistantClient {

    fun sendState(
        baseUrl: String,
        token: String,
        entityId: String,
        state: String,
        attributes: Map<String, Any?>
    ) {
        val payload = JSONObject().apply {
            put("state", state)
            put("attributes", JSONObject(attributes))
        }

        var connection: HttpURLConnection? = null
        try {
            connection = URL("$baseUrl/api/states/$entityId").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { out ->
                out.write(payload.toString().toByteArray(Charsets.UTF_8))
                out.flush()
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("Home Assistant responded with HTTP $code")
            }
        } finally {
            connection?.disconnect()
        }
    }
}
