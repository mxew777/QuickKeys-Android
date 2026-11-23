package com.quickkeys

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

    override fun onCreateInputView(): View {
        // The editor info may not be available yet here. Build the correct
        // view again in onStartInputView(), when Android supplies it.
        return createKeyboardView(null)
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        setInputView(createKeyboardView(info.packageName))
    }

    private fun createKeyboardView(packageName: String?): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }
        root.addView(TextView(this).apply {
            text = "QuickKeys"
            textSize = 16f
        })

        if (packageName != WHATSAPP_PACKAGE) {
            root.addView(TextView(this).apply {
                text = "افتح واتساب لاستخدام أزرار QuickKeys."
                textSize = 14f
            })
            return root
        }

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val count = prefs.getInt(COUNT, 4).coerceIn(1, 10)
        var row = newRow()
        repeat(count) { i ->
            val phrase = prefs.getString("phrase_" + i, "").orEmpty()
            row.addView(Button(this).apply {
                text = phrase.takeIf { it.isNotBlank() } ?: "زر " + (i + 1)
                textSize = 12f
                isAllCaps = false
                setOnClickListener { insertAndSend(phrase) }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            if ((i + 1) % 2 == 0 || i == count - 1) {
                root.addView(row)
                row = newRow()
            }
        }
        root.addView(Button(this).apply {
            text = "إعدادات QuickKeys"
            setOnClickListener {
                val intent = packageManager.getLaunchIntentForPackage(packageName = this@QuickKeysService.packageName)
                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent != null) startActivity(intent)
            }
        })
        return root
    }

    private fun newRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    private fun insertAndSend(phrase: String) {
        if (phrase.isBlank()) {
            Toast.makeText(this, "اكتب العبارة أولًا من إعدادات QuickKeys.", Toast.LENGTH_SHORT).show()
            return
        }
        val connection = currentInputConnection ?: return
        connection.commitText(phrase, 1)
        val sent = connection.performEditorAction(EditorInfo.IME_ACTION_SEND)
        if (!sent) {
            Toast.makeText(this, "تم إدراج النص. اضغط إرسال في واتساب.", Toast.LENGTH_LONG).show()
        }
    }
}
