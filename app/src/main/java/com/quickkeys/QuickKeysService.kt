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
import android.widget.Button
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

    override fun onCreateInputView(): View = createKeyboardView()

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        setInputView(createKeyboardView())
    }

    private fun createKeyboardView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 10)
            setBackgroundColor(Color.BLACK)
        }
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val imageUri = prefs.getString(BACKGROUND_URI, null)
        if (!imageUri.isNullOrBlank()) {
            try {
                contentResolver.openInputStream(android.net.Uri.parse(imageUri))?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) root.background = BitmapDrawable(resources, bitmap).apply {
                        gravity = Gravity.FILL
                    }
                }
            } catch (_: Exception) { }
        }

        root.addView(TextView(this).apply {
            text = APP_NAME
            textSize = 19f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 1f, 1f, Color.BLACK)
            setPadding(5, 3, 5, 9)
        })

        val count = prefs.getInt(COUNT, 4).coerceIn(1, 10)
        var row = newRow()
        repeat(count) { i ->
            val phrase = prefs.getString("phrase_" + i, "").orEmpty()
            val phraseView = TextView(this).apply {
                text = phrase.takeIf { it.isNotBlank() } ?: "زر " + (i + 1)
                textSize = 16f
                setTextColor(Color.WHITE)
                setShadowLayer(2f, 1f, 1f, Color.BLACK)
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 4
                ellipsize = android.text.TextUtils.TruncateAt.END
                setLineSpacing(0f, 1.0f)
                isClickable = true
                isFocusable = true
                background = GradientDrawable().apply {
                    setColor(Color.argb(220, 38, 38, 38))
                    setStroke(2, Color.rgb(165, 165, 165))
                    cornerRadius = 14f
                }
                setOnClickListener { insertPhrase(phrase) }
            }
            row.addView(phraseView, LinearLayout.LayoutParams(0, dp(82), 1f).apply {
                setMargins(5, 5, 5, 5)
            })
            if ((i + 1) % 2 == 0 || i == count - 1) {
                root.addView(row, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(92)
                ))
                row = newRow()
            }
        }
        root.addView(Button(this).apply {
            text = "❖ 𝐒𝐘𝐒𝐓𝐄𝐌 ❖"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.argb(230, 45, 45, 45))
                setStroke(2, Color.LTGRAY)
                cornerRadius = 12f
            }
            setOnClickListener {
                val intent = packageManager.getLaunchIntentForPackage(this@QuickKeysService.packageName)
                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent != null) startActivity(intent)
            }
        })
        return root
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun newRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun insertPhrase(phrase: String) {
        if (phrase.isBlank()) {
            Toast.makeText(this, "اكتب العبارة أولًا من الإعدادات.", Toast.LENGTH_SHORT).show()
            return
        }
        val connection = currentInputConnection ?: return
        connection.commitText(phrase, 1)
        val sent = connection.performEditorAction(EditorInfo.IME_ACTION_SEND)
        if (!sent) {
            Toast.makeText(this, "تم إدراج النص. قد تحتاج للضغط على إرسال داخل التطبيق.", Toast.LENGTH_LONG).show()
        }
    }
}
