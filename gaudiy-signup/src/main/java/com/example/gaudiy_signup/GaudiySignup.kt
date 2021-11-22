package com.gaudiy.gaudiy_did

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.fragment.app.FragmentActivity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*

class GaudiySignup {
    private lateinit var ctx:FragmentActivity
    fun setController(ctx: FragmentActivity) {
        this.ctx = ctx
        println(ctx)
    }

    @Serializable
    data class DIDSignUpRequest (
        val serviceUserId: String,
        val apiKey: String,
        val redirectSchema: String,
        val origin: String
    )

    @JvmField
    val CONNECTION_TIMEOUT_MILLISECONDS = 60000
    @JvmField
    val READ_TIMEOUT_MILLISECONDS = 60000
    @JvmField
    val ORIGIN = "androidBrowser"

    @WorkerThread
    private suspend fun backgroundTaskRunner(apiKey: String, serviceUserId: String, redirectSchema: String): Any {
        val returnVal = withContext(Dispatchers.IO) {
            var result = ""
            val payload = DIDSignUpRequest(serviceUserId, apiKey, redirectSchema, ORIGIN)
            val string = Json.encodeToString(payload)
            val bodyData = string.toByteArray()

            // TODO: middleman の エンドポイントを環境変数ごとに変える
            val url = URL("https://middleman-r2cu5dszea-an.a.run.app/usecases/sign_up/execution_id")
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.connectTimeout = CONNECTION_TIMEOUT_MILLISECONDS
                connection.readTimeout = READ_TIMEOUT_MILLISECONDS

                // Bodyへ書き込むを行う(doOutput だけでも POST になる)
                connection.requestMethod = "POST"
                connection.doOutput = true

                connection.setChunkedStreamingMode(0)

                connection.setRequestProperty("Content-type", "application/json; charset=utf-8")

                val outputStream = connection.outputStream
                outputStream.write(bodyData)
                outputStream.flush()
                outputStream.close()

                val statusCode = connection.responseCode
                if (statusCode == HttpURLConnection.HTTP_OK) {
                    result = readStream(connection.inputStream)
                }

                Json.decodeFromString<SignUpResponse>(result).data.executionId
            } catch (exception: Exception) {
                Log.e("Error", exception.toString())
            } finally {
                connection.disconnect()
            }
        }

        return returnVal
    }

    @Serializable
    data class SignUpResponseData (
        val executionId: String
    )
    @Serializable
    data class SignUpResponse (
        var data: SignUpResponseData,
        val code: Int
    )
    @WorkerThread
    private fun readStream(inputStream: InputStream): String {
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val responseBody = bufferedReader.use { it.readText() }
        bufferedReader.close()

        return responseBody
    }

    /**
     * DID連携リクエスト処理を行うメソッド。
     *
     * @param apiKey
     * @param serviceUserId
     * @param redirectSchema
     * @param siteUrl authgatewaySiteUrl
     */
    @UiThread
    fun asyncExecute(apiKey: String, serviceUserId: String, redirectSchema: String, siteUrl: String) {
        GlobalScope.launch {
            val result = backgroundTaskRunner(apiKey, serviceUserId, redirectSchema)
            val openURL = Intent(Intent.ACTION_VIEW)
            openURL.data = Uri.parse("${siteUrl}?exId=${result}")
            ctx.startActivity(openURL)
        }
    }
}