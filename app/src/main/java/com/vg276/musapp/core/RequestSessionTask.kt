package com.vg276.musapp.core

import android.os.StrictMode
import com.vg276.musapp.USER_AGENT
import com.vg276.musapp.base.BaseTask
import java.io.BufferedInputStream
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class RequestResult
{
    ErrorInternet,
    ErrorAvailable,
    ErrorRequest,
    ErrorAuth,
    Success
}

class RequestSessionTask(private val urlString: String,
                         private val parameters: ArrayList<RequestParam>? = null,
                         val completionHandler: (responseCode: Int, data: ByteArray?) -> Unit): BaseTask()
{
    override fun run()
    {
        // усыпим поток на 250 мс
        sleep(250)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.doOutput = true
        connection.useCaches = false
        connection.requestMethod = "POST"
        connection.connectTimeout = 1000
        connection.setRequestProperty("User-Agent", USER_AGENT)

        parameters?.let {
            val builder = StringBuilder()

            it.forEach { param ->
                builder.append(URLEncoder.encode(param.key, "UTF-8"))
                builder.append("=")
                builder.append(URLEncoder.encode(param.value.toString(), "UTF-8"))
                builder.append("&")
            }

            builder.removeRange(builder.lastIndex - 1, builder.lastIndex)

            val outputStream = connection.outputStream
            val writer = BufferedWriter(OutputStreamWriter(outputStream, "UTF-8"))
            writer.write(builder.toString())
            writer.flush()
            writer.close()
            outputStream.close()
        }

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            var inputStream = connection.inputStream
            inputStream = BufferedInputStream(inputStream)

            val byteArrayOutputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var readBytes: Int

            while (inputStream.read(buffer).also { readBytes = it } > 0) {
                byteArrayOutputStream.write(buffer, 0, readBytes)
            }

            byteArrayOutputStream.flush()
            byteArrayOutputStream.close()
            inputStream.close()

            completionHandler(connection.responseCode, byteArrayOutputStream.toByteArray())
        } else {
            completionHandler(connection.responseCode, null)
        }
    }
}