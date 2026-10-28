package com.example.service

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.net.ssl.SSLSocketFactory

object EmailSender {
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = 465
    private const val SENDER_EMAIL = "medinplayhub@gmail.com"
    private const val APP_PASSWORD = "wzmp xghk rftg vbhm"

    suspend fun sendEmail(recipientEmail: String, subject: String, bodyText: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val pass = APP_PASSWORD.replace(" ", "")
            val sslSocketFactory = SSLSocketFactory.getDefault()
            val socket = sslSocketFactory.createSocket(SMTP_HOST, SMTP_PORT)
            socket.soTimeout = 10000

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = socket.getOutputStream()

            fun readResponse(): String {
                return reader.readLine() ?: ""
            }

            fun sendCommand(cmd: String) {
                writer.write((cmd + "\r\n").toByteArray(Charsets.UTF_8))
                writer.flush()
            }

            readResponse() // Greeting 220

            sendCommand("EHLO gmail.com")
            while (true) {
                val line = readResponse()
                if (line.length >= 4 && line[3] == ' ') break
            }

            sendCommand("AUTH LOGIN")
            readResponse()

            sendCommand(Base64.encodeToString(SENDER_EMAIL.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            readResponse()

            sendCommand(Base64.encodeToString(pass.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            val authRes = readResponse()
            if (!authRes.startsWith("235")) {
                socket.close()
                return@withContext false
            }

            sendCommand("MAIL FROM:<$SENDER_EMAIL>")
            readResponse()

            sendCommand("RCPT TO:<$recipientEmail>")
            readResponse()

            sendCommand("DATA")
            readResponse()

            val msg = StringBuilder()
            msg.append("From: MedinPlay Admin <$SENDER_EMAIL>\r\n")
            msg.append("To: <$recipientEmail>\r\n")
            msg.append("Subject: $subject\r\n")
            msg.append("Content-Type: text/plain; charset=UTF-8\r\n")
            msg.append("\r\n")
            msg.append(bodyText)
            msg.append("\r\n.\r\n")

            sendCommand(msg.toString())
            readResponse()

            sendCommand("QUIT")
            socket.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
