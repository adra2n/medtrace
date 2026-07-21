package com.yy.medtrace.data.backup

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection

class GistSync(private val token: String) {
    private val client = HttpClientProvider.client
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val fileName = "chiyaole_backup.json"

    suspend fun upload(content: String, existingGistId: String?): String = withContext(Dispatchers.IO) {
        val files = JSONObject().apply {
            put(fileName, JSONObject().put("content", content))
        }
        val body = JSONObject().apply {
            put("public", false)
            put("files", files)
            if (existingGistId == null) put("description", "医迹 数据备份")
        }

        val request = if (existingGistId == null) {
            Request.Builder()
                .url("https://api.github.com/gists")
                .post(body.toString().toRequestBody(mediaType))
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()
        } else {
            Request.Builder()
                .url("https://api.github.com/gists/$existingGistId")
                .patch(body.toString().toRequestBody(mediaType))
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()
        }

        val response = client.newCall(request).execute()
        val respBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw IllegalStateException("Gist 同步失败：${response.code} ${respBody.take(200)}")
        }
        val json = JSONObject(respBody)
        json.getString("id")
    }

    suspend fun download(gistId: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/gists/$gistId")
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .build()

        val response = client.newCall(request).execute()
        val respBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw IllegalStateException("Gist 获取失败：${response.code} ${respBody.take(200)}")
        }
        val json = JSONObject(respBody)
        val files = json.optJSONObject("files") ?: throw IllegalStateException("Gist 无文件")
        val file = files.optJSONObject(fileName)
            ?: files.keys().asSequence().mapNotNull { files.optJSONObject(it) }.firstOrNull()
            ?: throw IllegalStateException("Gist 备份文件不存在")
        file.getString("content")
    }
}
