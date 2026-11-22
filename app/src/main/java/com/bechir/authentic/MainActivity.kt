package com.bechir.authentic

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private val names = arrayOf("صورة القطعة", "صورة الشعار", "صورة الملصق الداخلي", "صورة رقم المنتج")
    private val selected = BooleanArray(4)
    private lateinit var status: TextView
    private lateinit var pickerButtons: Array<Button>
    private var brand = "Nike"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(17, 13, 31)
        window.navigationBarColor = Color.rgb(17, 13, 31)
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.rgb(17, 13, 31))
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 30, 28, 36) }
        scroll.addView(layout)
        fun label(s: String, size: Float, color: Int): TextView = TextView(this).apply {
            text = s; textSize = size; setTextColor(color); gravity = Gravity.RIGHT; setPadding(0, 10, 0, 12)
        }
        layout.addView(label("BECHIR AUTHENTIC", 25f, Color.WHITE))
        layout.addView(label("فحص مؤشرات أصالة الملابس — نسخة تجريبية", 16f, Color.LTGRAY))
        layout.addView(label("اختر الماركة", 18f, Color.WHITE))
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("Nike", "Adidas", "Lacoste"))
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { brand = arrayOf("Nike", "Adidas", "Lacoste")[position] }
        }
        layout.addView(spinner)
        layout.addView(label("أضف صور القطعة لفحصها لاحقًا", 18f, Color.WHITE))
        pickerButtons = Array(4) { index ->
            Button(this).apply { text = "إضافة: ${names[index]}"; setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
                startActivityForResult(intent, index + 10)
            }; layout.addView(this) }
        }
        status = label("لم تُضف صور بعد.", 16f, Color.LTGRAY)
        layout.addView(status)
        val inspect = Button(this).apply { text = "عرض حالة الفحص"; setOnClickListener {
            val count = selected.count { it }
            status.text = "الماركة: $brand\nالصور المضافة: $count من 4\nالنتيجة: أدلة غير كافية.\nهذه النسخة تجمع الصور فقط، ولا تتضمن نموذج تحقق من الأصالة. لا يمكن اعتبار القطعة أصلية أو مقلدة من هذه الخطوة."
        } }
        layout.addView(inspect)
        layout.addView(label("تنبيه: التطبيق لا يقدم حاليًا تحققًا رسميًا أو تحليلًا آليًا للصور.", 14f, Color.rgb(211, 190, 255)))
        setContentView(scroll)
    }
    @Deprecated("Legacy activity result used to avoid external dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val i = requestCode - 10
        if (resultCode == RESULT_OK && i in 0..3 && data?.data != null) {
            selected[i] = true
            pickerButtons[i].text = "✓ ${names[i]} — أضيفت"
            status.text = "تم اختيار الصورة. يمكنك إضافة بقية الصور."
        }
    }
}