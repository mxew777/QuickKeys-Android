package com.quickkeys

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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

    private val white = Color.WHITE
    private val muted = Color.LTGRAY
    private val backgroundColor = Color.rgb(16, 16, 16)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(backgroundColor)
        }
        root.addView(TextView(this).apply {
            text = "QuickKeys"
            textSize = 28f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "احفظ عباراتك واستخدمها من لوحة المفاتيح."
            textSize = 16f
            setTextColor(muted)
            setPadding(0, 8, 0, 16)
        })
        countInput = EditText(this).apply {
            hint = "عدد الأزرار (1 إلى 10)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setTextColor(white)
            setHintTextColor(Color.GRAY)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.LTGRAY)
        }
        root.addView(countInput)
        root.addView(makeButton("تطبيق عدد الأزرار") { rebuildFields() })
        slots = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(slots)
        root.addView(makeButton("حفظ العبارات") { savePhrases() })
        root.addView(makeButton("تفعيل لوحة QuickKeys") {
            startActivity(Intent("android.settings.INPUT_METHOD_SETTINGS"))
            Toast.makeText(this@MainActivity, "فعّل QuickKeys من القائمة ثم اخترها كلوحة مفاتيح.", Toast.LENGTH_LONG).show()
        })
        root.addView(makeButton("اختيار لوحة المفاتيح") {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        })
        setContentView(root)
        val prefs = getSharedPreferences(QuickKeysService.PREFS, MODE_PRIVATE)
        countInput.setText(prefs.getInt(QuickKeysService.COUNT, 4).toString())
        rebuildFields()
    }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(white)
        background = GradientDrawable().apply {
            setColor(Color.rgb(42, 42, 42))
            setStroke(2, Color.rgb(130, 130, 130))
            cornerRadius = 14f
        }
        setOnClickListener { action() }
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
                setTextColor(white)
                setHintTextColor(Color.GRAY)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.LTGRAY)
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
