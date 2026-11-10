package com.example.studenttaskmanager

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class QuizActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var endpointInput: EditText
    private lateinit var status: TextView
    private lateinit var result: TextView
    private var selectedUri: Uri? = null
    private var selectedName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("student_task_manager", MODE_PRIVATE)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 28)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        layout.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(TextView(this).apply {
            text = "AI Review Quiz"
            textSize = 26f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "Upload a PDF, DOCX, PPTX, or TXT review module. The server will create a practice quiz based on the file."
            textSize = 15f
            setPadding(0, 10, 0, 12)
        })
        endpointInput = EditText(this).apply {
            hint = "Backend URL, e.g. https://your-service.onrender.com"
            setSingleLine(true)
            setText(prefs.getString("quiz_backend_url", ""))
            textSize = 14f
        }
        content.addView(endpointInput, match())
        content.addView(Button(this).apply {
            text = "Choose review file"
            setOnClickListener { chooseFile() }
        }, match())
        status = TextView(this).apply {
            text = "No file selected."
            textSize = 14f
            setPadding(0, 12, 0, 12)
        }
        content.addView(status)
        content.addView(Button(this).apply {
            text = "Generate quiz"
            setOnClickListener { generateQuiz() }
        }, match())
        result = TextView(this).apply {
            textSize = 16f
            setPadding(0, 16, 0, 24)
        }
        content.addView(result)
        layout.addView(Button(this).apply {
            text = "Back to tasks"
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(layout)
    }

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain"
            ))
        }
        startActivityForResult(intent, 701)
    }

    @Deprecated("Legacy picker callback is used for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 701 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                selectedUri = uri
                selectedName = queryName(uri) ?: "review-module"
                status.text = "Selected: $selectedName"
                getSharedPreferences("student_task_manager", MODE_PRIVATE).edit()
                    .putString("quiz_backend_url", endpointInput.text.toString().trim()).apply()
            }
        }
    }

    private fun generateQuiz() {
        val uri = selectedUri
        val endpoint = endpointInput.text.toString().trim().trimEnd('/')
        if (uri == null) {
            Toast.makeText(this, "Choose a review file first.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!endpoint.startsWith("https://")) {
            Toast.makeText(this, "Enter your deployed HTTPS backend URL.", Toast.LENGTH_LONG).show()
            return
        }
        getSharedPreferences("student_task_manager", MODE_PRIVATE).edit()
            .putString("quiz_backend_url", endpoint).apply()
        status.text = "Uploading file and generating questions… please wait."
        result.text = ""
        executor.execute {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Could not read the selected file.")
                if (bytes.size > 10 * 1024 * 1024) throw IllegalArgumentException("Please choose a file smaller than 10 MB.")
                val mime = contentResolver.getType(uri) ?: guessMime(selectedName)
                val json = uploadFile(endpoint + "/api/quiz", selectedName, mime, bytes)
                val display = formatQuiz(json)
                runOnUiThread {
                    status.text = "Quiz ready from: $selectedName"
                    result.text = display
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Could not generate the quiz."
                    result.text = e.message ?: "Check your internet connection and backend URL."
                }
            }
        }
    }

    private fun uploadFile(urlString: String, filename: String, mime: String, bytes: ByteArray): JSONObject {
        val boundary = "----StudentQuiz${System.currentTimeMillis()}"
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 120000
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("Accept", "application/json")
        }
        try {
            DataOutputStream(connection.outputStream).use { out ->
                val safeFilename = filename.replace("\"", "").replace("\r", "_").replace("\n", "_")
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$safeFilename\"\r\n")
                out.writeBytes("Content-Type: $mime\r\n\r\n")
                out.write(bytes)
                out.writeBytes("\r\n--$boundary--\r\n")
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            val obj = JSONObject(body)
            if (code !in 200..299) throw IllegalStateException(obj.optString("error", "Server error (HTTP $code)."))
            return obj
        } finally {
            connection.disconnect()
        }
    }

    private fun formatQuiz(response: JSONObject): String {
        val questions = response.optJSONArray("questions") ?: JSONArray()
        if (questions.length() == 0) return "No questions were returned. Try a text-based review file."
        val out = StringBuilder("PRACTICE QUIZ\n\n")
        for (i in 0 until questions.length()) {
            val q = questions.getJSONObject(i)
            out.append(i + 1).append(". ").append(q.optString("question")).append("\n")
            val options = q.optJSONArray("options") ?: JSONArray()
            for (j in 0 until options.length()) {
                out.append("   ").append(('A'.code + j).toChar()).append(". ")
                    .append(options.optString(j)).append("\n")
            }
            out.append("\nAnswer: ").append(q.optString("answer", "Not provided"))
            out.append("\nExplanation: ").append(q.optString("explanation", ""))
            out.append("\n\n")
        }
        return out.toString()
    }

    private fun queryName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else null
        } finally { cursor?.close() }
    }

    private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }

    private fun match() = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        bottomMargin = 12
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
