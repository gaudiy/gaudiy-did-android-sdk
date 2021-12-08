package com.gaudiy.gaudiy_did

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
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

val VERIFICATION_API_KEY = "ak_3ch89dsedpa55532"

class GaudiySignup {
    @Serializable
    @Keep
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
    private suspend fun backgroundTaskRunner(apiKey: String, serviceUserId: String, redirectSchema: String, failedCallback: () -> Void): Any {
        val returnVal = withContext(Dispatchers.IO) {
            var result = ""
            val payload = DIDSignUpRequest(serviceUserId, apiKey, redirectSchema, ORIGIN)
            val string = Json.encodeToString(payload)
            val bodyData = string.toByteArray()
            val url = URL(getMiddlemanEndpoint(apiKey) + "/usecases/sign_up/execution_id")
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

                failedCallback()
            } finally {
                connection.disconnect()
            }
        }

        return returnVal
    }

    private fun getMiddlemanEndpoint(apiKey: String): String {
        val isProduction = apiKey.startsWith("ak_") && apiKey != VERIFICATION_API_KEY;
        if(isProduction){
            return "https://middleman-txib6zhhnq-an.a.run.app"
        };

        return "https://middleman-r2cu5dszea-an.a.run.app"
    }

    @Serializable
    @Keep
    data class SignUpResponseData (
        val executionId: String
    )
    @Serializable
    @Keep
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
     * @params context Context
     * @param apiKey
     * @param serviceUserId
     * @param redirectSchema
     * @param siteUrl authgatewaySiteUrl
     */
    @UiThread
    fun asyncExecute(context: Context, apiKey: String, serviceUserId: String, redirectSchema: String, siteUrl: String, failedCallback: ()-> Void) {
        GlobalScope.launch {
            val result = backgroundTaskRunner(apiKey, serviceUserId, redirectSchema, failedCallback)
            val openURL = Intent(Intent.ACTION_VIEW)
            openURL.data = Uri.parse("${siteUrl}?exId=${result}")
            context.startActivity(openURL)
        }
    }
}