package com.quickkeys

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val fields = mutableListOf<EditText>()
    private lateinit var countInput: EditText
    private lateinit var slots: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        root.addView(TextView(this).apply { text = "QuickKeys"; textSize = 26f })
        root.addView(TextView(this).apply {
            text = "احفظ عباراتك واستخدمها من لوحة المفاتيح."
            textSize = 16f
        })
        countInput = EditText(this).apply {
            hint = "عدد الأزرار (1 إلى 10)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
        }
        root.addView(countInput)
        root.addView(Button(this).apply {
            text = "تطبيق عدد الأزرار"
            setOnClickListener { rebuildFields() }
        })
        slots = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(slots)
        root.addView(Button(this).apply {
            text = "حفظ العبارات"
            setOnClickListener { savePhrases() }
        })
        root.addView(Button(this).apply {
            text = "تفعيل لوحة QuickKeys"
            setOnClickListener {
                startActivity(Intent("android.settings.INPUT_METHOD_SETTINGS"))
                Toast.makeText(this@MainActivity, "فعّل QuickKeys من القائمة ثم اخترها كلوحة مفاتيح.", Toast.LENGTH_LONG).show()
            }
        })
        root.addView(Button(this).apply {
            text = "اختيار لوحة المفاتيح"
            setOnClickListener {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        })
        setContentView(root)
        val prefs = getSharedPreferences(QuickKeysService.PREFS, MODE_PRIVATE)
        countInput.setText(prefs.getInt(QuickKeysService.COUNT, 4).toString())
        rebuildFields()
    }

    private fun rebuildFields() {
        val count = countInput.text.toString().toIntOrNull()?.coerceIn(1, 10) ?: 4
        countInput.setText(count.toString())
        val prefs = getSharedPreferences(QuickKeysService.PREFS, MODE_PRIVATE)
        val oldValues = fields.map { it.text.toString() }
        slots.removeAllViews()
        fields.clear()
        repeat(count) { i ->
            val field = EditText(this).apply {
                hint = "العبارة " + (i + 1)
                minLines = 1
                maxLines = 3
                setText(prefs.getString("phrase_" + i, oldValues.getOrNull(i).orEmpty()))
            }
            fields.add(field)
            slots.addView(field)
        }
    }

    private fun savePhrases() {
        val editor = getSharedPreferences(QuickKeysService.PREFS, MODE_PRIVATE).edit()
        editor.putInt(QuickKeysService.COUNT, fields.size)
        fields.forEachIndexed { i, field -> editor.putString("phrase_" + i, field.text.toString()) }
        editor.apply()
        Toast.makeText(this, "تم حفظ العبارات.", Toast.LENGTH_SHORT).show()
    }
}
