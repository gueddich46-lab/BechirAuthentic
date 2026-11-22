package com.bechir.authentic

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : Activity() {
    private val names = arrayOf("صورة القطعة", "صورة الشعار", "صورة الملصق الداخلي", "صورة رقم المنتج")
    private val brands = arrayOf("Nike", "Adidas", "Lacoste")
    private val selected = arrayOfNulls<Uri>(4)
    private lateinit var status: TextView
    private lateinit var previews: Array<ImageView>
    private lateinit var buttons: Array<Button>
    private lateinit var extracted: TextView
    private var brand = "Nike"
    private val bg = Color.rgb(17, 13, 31)
    private val accent = Color.rgb(207, 187, 255)

    private fun label(value: String, size: Float = 16f, color: Int = Color.WHITE): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.RIGHT
            setPadding(0, 10, 0, 12)
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        val scroll = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 48, 28, 48)
        }
        scroll.addView(layout)
        layout.addView(label("BECHIR AUTHENTIC", 25f))
        layout.addView(label("فحص مؤشرات أصالة المنتجات — نسخة 0.2", 16f, accent))
        layout.addView(label("اختر الماركة", 18f))
        val spinner = Spinner(this)
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, brands) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                super.getView(position, convertView, parent).apply {
                    (this as? TextView)?.apply { setTextColor(Color.WHITE); setBackgroundColor(bg); textSize = 18f; setPadding(20, 16, 20, 16) }
                }
        }
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                brand = brands[position]
            }
        }
        layout.addView(spinner)
        layout.addView(label("أضف صور القطعة والملصقات", 18f))
        previews = Array(4) { ImageView(this).apply {
            visibility = View.GONE
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(-1, 350)
            setBackgroundColor(Color.rgb(35, 28, 53))
        } }
        buttons = Array(4) { index ->
            Button(this).apply {
                text = "إضافة: " + names[index]
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "image/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    startActivityForResult(intent, index + 10)
                }
                layout.addView(this)
                layout.addView(previews[index])
            }
        }
        val scan = Button(this).apply {
            text = "قراءة الملصق ورقم المنتج"
            setOnClickListener { recognizeLabel() }
        }
        layout.addView(scan)
        extracted = label("لم تتم قراءة أي ملصق بعد.", 15f, accent)
        layout.addView(extracted)
        status = label("أضف الصور لبدء فحص البيانات.", 16f, Color.LTGRAY)
        layout.addView(status)
        layout.addView(Button(this).apply {
            text = "عرض حالة الفحص"
            setOnClickListener {
                val count = selected.count { it != null }
                status.text = "الماركة: " + brand + "\nالصور المضافة: " + count +
                    " من 4\nالنتيجة: لا يمكن إثبات الأصالة من الصور وحدها. راجع رقم الموديل مع مصادر موثوقة."
            }
        })
        layout.addView(label("قراءة النص لا تثبت أن القطعة أصلية. لا توجد شهادة تحقق رسمية.", 14f, accent))
        setContentView(scroll)
    }

    private fun recognizeLabel() {
        val uri = selected[2] ?: selected[3]
        if (uri == null) {
            extracted.text = "أضف صورة الملصق الداخلي أو رقم المنتج أولًا."
            return
        }
        extracted.text = "جارٍ استخراج النص من الصورة..."
        try {
            val image = InputImage.fromFilePath(this, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    extracted.text = if (result.text.isBlank()) "لم يُعثر على نص واضح. أعد تصوير الملصق بإضاءة جيدة."
                        else "النص المقروء (تحقق منه يدويًا):\n" + result.text
                }
                .addOnFailureListener { extracted.text = "تعذرت قراءة الصورة. جرّب صورة أوضح." }
                .addOnCompleteListener { recognizer.close() }
        } catch (e: Exception) {
            extracted.text = "تعذرت معالجة الصورة. حاول مجددًا."
        }
    }

    @Deprecated("Legacy activity result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val index = requestCode - 10
        if (resultCode == RESULT_OK && index in 0..3 && data?.data != null) {
            val uri = data.data!!
            selected[index] = uri
            buttons[index].text = "✓ " + names[index] + " — أضيفت"
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    previews[index].setImageBitmap(bitmap)
                    previews[index].visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                previews[index].visibility = View.GONE
                Toast.makeText(this, "تم اختيار الصورة لكن تعذرت معاينتها", Toast.LENGTH_SHORT).show()
            }
            status.text = "تمت إضافة الصورة. يمكنك إضافة بقية الصور."
        }
    }
}