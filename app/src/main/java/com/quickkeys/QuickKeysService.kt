package com.quickkeys

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class QuickKeysService : InputMethodService() {
    companion object {
        const val PREFS = "quickkeys_prefs"
        const val COUNT = "button_count"
        const val BACKGROUND_URI = "background_uri"
        const val APP_NAME = "𝐋𝐞𝐨𝐫𝐝𝐨𝐮 ⃟⛧"
    }

    private var typingMode = false
    private var english = false
    private var shifted = false
    private var symbols = false

    override fun onCreateInputView(): View = createKeyboardView()

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        setInputView(createKeyboardView())
    }

    private fun createKeyboardView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(7), dp(8), dp(7))
            setBackgroundColor(Color.BLACK)
        }
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val imageUri = prefs.getString(BACKGROUND_URI, null)
        if (!imageUri.isNullOrBlank()) {
            try {
                contentResolver.openInputStream(android.net.Uri.parse(imageUri))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { bitmap ->
                        root.background = BitmapDrawable(resources, bitmap).apply { gravity = Gravity.FILL }
                    }
                }
            } catch (_: Exception) { }
        }

        if (typingMode) {
            buildTypingKeyboard(root)
        } else {
            root.addView(TextView(this).apply {
                text = APP_NAME
                textSize = 19f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(4f, 1f, 1f, Color.BLACK)
                setPadding(dp(5), dp(3), dp(5), dp(9))
            })
            val count = prefs.getInt(COUNT, 4).coerceIn(1, 10)
            var row = newRow()
            repeat(count) { i ->
                val phrase = prefs.getString("phrase_" + i, "").orEmpty()
                val phraseView = keyView(phrase.takeIf { it.isNotBlank() } ?: "زر " + (i + 1), 16f).apply {
                    maxLines = 4
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setOnClickListener { insertPhrase(phrase) }
                }
                row.addView(phraseView, LinearLayout.LayoutParams(0, dp(82), 1f).apply {
                    setMargins(dp(5), dp(5), dp(5), dp(5))
                })
                if ((i + 1) % 2 == 0 || i == count - 1) {
                    root.addView(row, LinearLayout.LayoutParams(-1, dp(92)))
                    row = newRow()
                }
            }
            root.addView(actionKey("⏆ 𝐒𝐖𝐈𝐓𝐂𝐇 ⏆", 16f) {
                typingMode = true
                setInputView(createKeyboardView())
            }, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(dp(3), dp(4), dp(3), dp(4)) })
            root.addView(actionKey("❖ 𝐒𝐘𝐒𝐓𝐄𝐌 ❖", 16f) {
                val intent = packageManager.getLaunchIntentForPackage(this@QuickKeysService.packageName)
                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent != null) startActivity(intent)
            }, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(dp(3), dp(4), dp(3), dp(4)) })
        }
        return root
    }

    private fun buildTypingKeyboard(root: LinearLayout) {
        val numberRow = if (symbols) listOf("1","2","3","4","5","6","7","8","9","0","⌫")
            else listOf("١","٢","٣","٤","٥","٦","٧","٨","٩","٠","⌫")
        addKeyRow(root, numberRow, dp(49))
        val rows = if (symbols) {
            listOf(listOf("@","#","$","%","&","-","+","(",")","/"),
                listOf("*","\"","'",":",";","!","?","⌫"))
        } else if (english) {
            listOf("qwertyuiop".map { it.toString() },
                "asdfghjkl".map { it.toString() },
                listOf("⇧") + "zxcvbnm".map { it.toString() } + listOf("⌫"))
        } else {
            listOf("ضصثقفغعهخحج".map { it.toString() },
                "شسيبلاتنمكط".map { it.toString() },
                listOf("⇧") + "ذءؤرىةوزظد".map { it.toString() } + listOf("،", ".", "⌫"))
        }
        rows.forEach { addKeyRow(root, it, dp(53)) }

        val bottom = newRow()
        fun addBottom(label: String, weight: Float, action: () -> Unit) {
            bottom.addView(actionKey(label, 15f, action),
                LinearLayout.LayoutParams(0, dp(53), weight).apply {
                    setMargins(dp(2), dp(3), dp(2), dp(3))
                })
        }
        addBottom(if (symbols) "ABC" else "?123", 1.05f) { symbols = !symbols; setInputView(createKeyboardView()) }
        addBottom("␠", 1.2f) { currentInputConnection?.commitText(" ", 1) }
        addBottom("⏆ 𝐒𝐖𝐈𝐓𝐂𝐇 ⏆", 3.4f) {
            typingMode = false
            setInputView(createKeyboardView())
        }
        addBottom(if (english) "🌐 EN" else "🌐 ع", 1.15f) {
            english = !english
            shifted = false
            setInputView(createKeyboardView())
        }
        addBottom("↵", 1.0f) { sendEnter() }
        root.addView(bottom, LinearLayout.LayoutParams(-1, dp(59)))
    }

    private fun addKeyRow(root: LinearLayout, keys: List<String>, height: Int) {
        val row = newRow()
        keys.forEach { key ->
            row.addView(actionKey(key, 20f) { handleKey(key) },
                LinearLayout.LayoutParams(0, height, 1f).apply {
                    setMargins(dp(2), dp(3), dp(2), dp(3))
                })
        }
        root.addView(row, LinearLayout.LayoutParams(-1, height + dp(6)))
    }

    private fun handleKey(key: String) {
        when (key) {
            "⌫" -> currentInputConnection?.deleteSurroundingText(1, 0)
            "⇧" -> { shifted = !shifted; setInputView(createKeyboardView()) }
            else -> {
                val value = if (english && shifted) key.uppercase() else key
                currentInputConnection?.commitText(value, 1)
                if (shifted) { shifted = false; setInputView(createKeyboardView()) }
            }
        }
    }

    private fun sendEnter() {
        val connection = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
        if (action == EditorInfo.IME_ACTION_SEND || action == EditorInfo.IME_ACTION_GO ||
            action == EditorInfo.IME_ACTION_DONE || action == EditorInfo.IME_ACTION_SEARCH) {
            connection.performEditorAction(action!!)
        } else {
            connection.commitText("\n", 1)
        }
    }

    private fun insertPhrase(phrase: String) {
        if (phrase.isBlank()) {
            Toast.makeText(this, "اكتب العبارة أولًا من الإعدادات.", Toast.LENGTH_SHORT).show()
            return
        }
        val connection = currentInputConnection ?: return
        connection.commitText(phrase, 1)
        val sent = connection.performEditorAction(EditorInfo.IME_ACTION_SEND)
        if (!sent) Toast.makeText(this, "تم إدراج النص. قد تحتاج للضغط على إرسال داخل التطبيق.", Toast.LENGTH_LONG).show()
    }

    private fun keyView(label: String, size: Float) = TextView(this).apply {
        text = label
        textSize = size
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        includeFontPadding = false
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
        background = GradientDrawable().apply {
            setColor(Color.rgb(43, 43, 43))
            setStroke(dp(1), Color.rgb(70, 70, 70))
            cornerRadius = dp(7).toFloat()
        }
        isClickable = true
        isFocusable = true
    }

    private fun actionKey(label: String, size: Float, action: () -> Unit) =
        keyView(label, size).apply { setOnClickListener { action() } }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun newRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
}
