package com.bechir.authentic

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.WindowManager
import android.view.WindowInsets
import android.os.Build
import android.os.Bundle
import java.net.URL
import java.net.HttpURLConnection
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.concurrent.thread
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : Activity() {
    private val names = arrayOf("صورة القطعة", "صورة الشعار", "صورة الملصق الداخلي", "صورة رقم المنتج")
    private val brands = arrayOf("اختر الماركة أولًا", "Nike", "Adidas", "Lacoste")
    private val selected = arrayOfNulls<Uri>(4)
    private lateinit var status: TextView
    private lateinit var previews: Array<ImageView>
    private lateinit var buttons: Array<Button>
    private lateinit var extracted: TextView
    private lateinit var modelInput: EditText
    private lateinit var brandSpinner: Spinner
    private lateinit var searchReport: TextView
    private var brand = ""
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
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val scroll = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 60)
        }
        scroll.addView(layout)
        if (Build.VERSION.SDK_INT >= 30) {
            scroll.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(0, bars.top, 0, bars.bottom)
                insets
            }
        } else {
            scroll.fitsSystemWindows = true
        }
        layout.addView(label("BECHIR AUTHENTIC", 25f))
        layout.addView(label("فحص مؤشرات أصالة المنتجات — نسخة 0.5", 16f, accent))
        layout.addView(label("اختر الماركة", 18f))
        val spinner = Spinner(this)
        brandSpinner = spinner
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
                brand = if (position == 0) "" else brands[position]
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
        layout.addView(label("رقم الموديل — يمكنك تصحيح قراءة الصورة", 17f))
        modelInput = EditText(this).apply {
            hint = "مثال: 7-40CFA000302H"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textDirection = View.TEXT_DIRECTION_LTR
            setSingleLine(true)
            setPadding(16, 18, 16, 18)
            setBackgroundColor(Color.rgb(43, 35, 62))
        }
        layout.addView(modelInput)
        layout.addView(Button(this).apply {
            text = "بحث تلقائي عن الموديل وتحليل النتائج"
            setOnClickListener { searchModel() }
        })
        searchReport = label("لم يبدأ البحث بعد. نتائج البحث ليست إثباتًا للأصالة.", 15f, accent)
        layout.addView(searchReport)
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


    private fun searchModel() {
        val code = modelInput.text.toString().trim().uppercase().replace(" ", "")
        if (brand.isBlank() || code.isBlank()) {
            searchReport.text = "اختر الماركة وأدخل رقم المنتج أولًا."
            return
        }
        searchReport.text = "جارٍ البحث تلقائيًا... قد يستغرق بضع ثوانٍ."
        thread {
            try {
                val query = Uri.encode("$brand $code")
                val connection = URL("https://www.bing.com/search?format=rss&q=$query").openConnection() as HttpURLConnection
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; BechirAuthentic/0.5)")
                val entries = connection.inputStream.use { input ->
                    val factory = DocumentBuilderFactory.newInstance()
                    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                    factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
                    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                    val doc = factory.newDocumentBuilder().parse(input)
                    val nodes = doc.getElementsByTagName("item")
                    (0 until minOf(nodes.length, 5)).map { i ->
                        val node = nodes.item(i)
                        val children = node.childNodes
                        fun field(name: String): String {
                            for (j in 0 until children.length) {
                                if (children.item(j).nodeName == name) return children.item(j).textContent.orEmpty()
                            }
                            return ""
                        }
                        Triple(field("title"), field("link"), field("description"))
                    }
                }
                connection.disconnect()
                val exact = entries.count { (title, _, description) ->
                    (title + " " + description).uppercase().replace(Regex("[^A-Z0-9]"), "")
                        .contains(code.replace(Regex("[^A-Z0-9]"), ""))
                }
                val message = if (entries.isEmpty()) "لم تُرجع خدمة البحث نتائج. لا يمكن تحديد الأصالة."
                    else buildString {
                        append("نتائج البحث: ${entries.size}\n")
                        append("نتائج تذكر الرمز كاملًا: $exact\n")
                        append(if (exact > 0) "عُثر على إشارات للرمز في نتائج الويب؛ لم تُراجع صحة المصادر أو الصور."
                            else "لم يظهر تطابق نصي كامل في النتائج المسترجعة.")
                        append("\n\n")
                        entries.forEachIndexed { i, (title, link, _) ->
                            append("${i + 1}. $title\n$link\n\n")
                        }
                        append("الحكم على الأصالة: أدلة غير كافية. تطابق رمز الموديل لا يثبت أصالة القطعة.")
                    }
                runOnUiThread { searchReport.text = message }
            } catch (e: Exception) {
                runOnUiThread { searchReport.text = "تعذر البحث التلقائي أو قراءة النتائج. تحقق من الإنترنت أو أعد المحاولة. لم يتم تحديد الأصالة." }
            }
        }
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
                    if (result.text.isNotBlank()) {
                        val candidates = Regex("""(?:[0-9]-)?[0-9A-Z]{3,}(?:-[0-9A-Z]+)+""").findAll(result.text.uppercase()).map { it.value }.toList()
                        if (candidates.isNotEmpty()) modelInput.setText(candidates.maxByOrNull { it.length } ?: "")
                        if (brand.isBlank() && result.text.contains("LEROND", ignoreCase = true)) brandSpinner.setSelection(3)
                    }
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