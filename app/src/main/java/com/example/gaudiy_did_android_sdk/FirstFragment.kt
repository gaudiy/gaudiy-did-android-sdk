package com.example.gaudiy_did_android_sdk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebStorage
import android.widget.Button
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.*
import kotlinx.serialization.json.*


class FirstFragment : Fragment() {
    private lateinit var didButton:Button

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_first, container, false)
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
    private fun asyncExecute(apiKey: String, serviceUserId: String, redirectSchema: String, siteUrl: String) {
        lifecycleScope.launch {
            val result = backgroundTaskRunner(apiKey, serviceUserId, redirectSchema)
            val openURL = Intent(Intent.ACTION_VIEW)
            openURL.data = Uri.parse("${siteUrl}?exId=${result}")
            startActivity(openURL)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        didButton = view.findViewById<Button>(R.id.button_first);
        didButton.setOnClickListener {
            Toast.makeText(context, "DID認証画面を開く", Toast.LENGTH_SHORT).show()

            // SDK呼び出し
            asyncExecute("ak_3ch89dsedpa55532", "serviceUserId_android_${Math.random()}", "sample-gaudiy-app://verification", "http://192.168.0.115:3001/top")
        }
    }
}
