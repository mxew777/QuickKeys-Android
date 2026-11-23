package com.quickkeys

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
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
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
    }

    override fun onCreateInputView(): View = createKeyboardView(null)

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        setInputView(createKeyboardView(info.packageName))
    }

    private fun createKeyboardView(packageName: String?): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(16, 16, 16))
        }
        root.addView(TextView(this).apply {
            text = "QuickKeys"
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(4, 4, 4, 12)
        })

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val count = prefs.getInt(COUNT, 4).coerceIn(1, 10)
        var row = newRow()
        repeat(count) { i ->
            val phrase = prefs.getString("phrase_" + i, "").orEmpty()
            val button = Button(this).apply {
                text = phrase.takeIf { it.isNotBlank() } ?: "زر " + (i + 1)
                textSize = 16f
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(Color.rgb(42, 42, 42))
                    setStroke(2, Color.rgb(150, 150, 150))
                    cornerRadius = 14f
                }
                minHeight = (if (resources.configuration.smallestScreenWidthDp >= 600) 64 else 52)
                setOnClickListener { insertPhrase(phrase, packageName == WHATSAPP_PACKAGE) }
            }
            row.addView(button, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(5, 5, 5, 5)
            })
            if ((i + 1) % 2 == 0 || i == count - 1) {
                root.addView(row)
                row = newRow()
            }
        }
        root.addView(Button(this).apply {
            text = "إعدادات QuickKeys"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.rgb(55, 55, 55))
                setStroke(2, Color.rgb(150, 150, 150))
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

    private fun newRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    private fun insertPhrase(phrase: String, shouldSend: Boolean) {
        if (phrase.isBlank()) {
            Toast.makeText(this, "اكتب العبارة أولًا من إعدادات QuickKeys.", Toast.LENGTH_SHORT).show()
            return
        }
        val connection = currentInputConnection ?: return
        connection.commitText(phrase, 1)
        if (shouldSend) {
            val sent = connection.performEditorAction(EditorInfo.IME_ACTION_SEND)
            if (!sent) {
                Toast.makeText(this, "تم إدراج النص. اضغط إرسال في واتساب.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
