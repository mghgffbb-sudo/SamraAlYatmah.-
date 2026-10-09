package com.samraalyatmah.app

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private const val NAVY = 0xFF073B78.toInt()
private const val GOLD = 0xFFF5B82E.toInt()

class MainActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun setup(title: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFFF4F7FB.toInt())
        }
        val header = TextView(this).apply {
            text = "🎓  سمراء الكبودي\n$title"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(-1)
            setBackgroundColor(NAVY)
            setPadding(12, 24, 12, 24)
        }
        root.addView(header, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(root)
    }

    private fun addButton(label: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = label
            setTextColor(NAVY)
            setBackgroundColor(GOLD)
            textSize = 16f
            setOnClickListener { action() }
        }
        val p = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT)
        p.setMargins(0, 10, 0, 4)
        root.addView(b, p)
    }

    private fun addText(label: String, size: Float = 17f) {
        root.addView(TextView(this).apply {
            text = label
            textSize = size
            setTextColor(NAVY)
            setPadding(4, 12, 4, 12)
        })
    }

    private fun showHome() {
        setup("مدرسة اليتمة")
        addText("تعليم • قيم • مستقبل أفضل", 18f)
        addText("منصة المدرسة للتسجيل والنتائج والإعلانات")
        addButton("تسجيل طالب / طالبة") { showRegistration() }
        addButton("أسماء الطلاب والطالبات") { showStudents() }
        addButton("نتائج الاختبارات") { showResults() }
        addButton("دخول المديرة") { showAdminLogin() }
        addButton("التواصل مع إدارة المدرسة: 771775551") {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:771775551")))
        }
        addText("ملاحظة: تظهر البيانات الحقيقية بعد إعداد Firebase وتسجيل الدخول والصلاحيات.")
    }

    private fun field(hintText: String): EditText {
        val e = EditText(this)
        e.hint = hintText
        e.textSize = 16f
        e.setSingleLine(true)
        root.addView(e, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        return e
    }

    private fun showRegistration() {
        setup("تسجيل طالب جديد")
        val name = field("اسم الطالب / الطالبة")
        val grade = field("الصف الدراسي")
        val guardian = field("اسم ولي الأمر")
        val phone = field("رقم هاتف ولي الأمر")
        addButton("إرسال طلب التسجيل") {
            if (name.text.isBlank() || grade.text.isBlank() || phone.text.isBlank()) {
                Toast.makeText(this, "يرجى إدخال الاسم والصف ورقم الهاتف", Toast.LENGTH_LONG).show()
                return@addButton
            }
            val data = hashMapOf(
                "name" to name.text.toString().trim(),
                "grade" to grade.text.toString().trim(),
                "guardian" to guardian.text.toString().trim(),
                "guardianPhone" to phone.text.toString().trim(),
                "status" to "pending",
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("registrations").add(data)
                .addOnSuccessListener { Toast.makeText(this, "تم إرسال طلب التسجيل", Toast.LENGTH_LONG).show(); showHome() }
                .addOnFailureListener { Toast.makeText(this, "تعذّر الإرسال. تأكد من إعداد الاتصال وقواعد الأمان.", Toast.LENGTH_LONG).show() }
        }
        addButton("رجوع") { showHome() }
    }

    private fun showStudents() {
        setup("أسماء الطلاب والطالبات")
        addText("هذه البيانات خاصة بالمدرسة ولا تظهر إلا بعد تسجيل الدخول المصرح به.")
        if (auth.currentUser == null) {
            addButton("تسجيل دخول") { showAdminLogin() }
        } else {
            db.collection("students").get()
                .addOnSuccessListener { docs ->
                    if (docs.isEmpty) addText("لا توجد أسماء مضافة بعد.")
                    docs.forEach { doc -> addText("${doc.getString("name") ?: "—"}  •  ${doc.getString("grade") ?: ""}") }
                }
                .addOnFailureListener { addText("لا يمكن عرض البيانات. تحقق من الصلاحيات.") }
        }
        addButton("رجوع") { showHome() }
    }

    private fun showResults() {
        setup("نتائج الاختبارات")
        addText("تُعرض نتائج الطالب بعد التحقق من الهوية والصلاحية.")
        if (auth.currentUser == null) addButton("تسجيل دخول") { showAdminLogin() }
        else {
            val studentId = field("معرّف الطالب")
            addButton("عرض النتائج") {
                db.collection("results").whereEqualTo("studentId", studentId.text.toString().trim()).get()
                    .addOnSuccessListener { docs ->
                        if (docs.isEmpty) addText("لا توجد نتائج لهذا المعرّف.")
                        docs.forEach { addText("${it.getString("subject") ?: "المادة"}: ${it.getLong("score") ?: 0}") }
                    }.addOnFailureListener { addText("تعذّر جلب النتائج؛ تحقق من الصلاحيات.") }
            }
        }
        addButton("رجوع") { showHome() }
    }

    private fun showAdminLogin() {
        setup("دخول المديرة")
        val email = field("البريد الإلكتروني الإداري")
        val password = field("كلمة المرور")
        password.inputType = 129
        addButton("تسجيل الدخول") {
            auth.signInWithEmailAndPassword(email.text.toString().trim(), password.text.toString())
                .addOnSuccessListener {
                    db.collection("users").document(auth.currentUser!!.uid).get()
                        .addOnSuccessListener { profile ->
                            if (profile.getString("role") == "admin") {
                                Toast.makeText(this, "تم تسجيل الدخول", Toast.LENGTH_SHORT).show()
                                showAdminPanel()
                            } else {
                                auth.signOut()
                                Toast.makeText(this, "هذا الحساب ليس حساب مديرة", Toast.LENGTH_LONG).show()
                            }
                        }
                }
                .addOnFailureListener { Toast.makeText(this, "فشل تسجيل الدخول. تحقق من البيانات.", Toast.LENGTH_LONG).show() }
        }
        addButton("رجوع") { showHome() }
    }

    private fun showAdminPanel() {
        setup("لوحة تحكم المديرة")
        addText("تم تسجيل الدخول. استخدمي أدوات موثوقة لإدارة صلاحيات الحسابات.")
        addText("نسخة البداية: أضيفي بيانات الطلاب والنتائج بعد إعداد قواعد الأمان.")
        addButton("تسجيل الخروج") { auth.signOut(); showHome() }
    }
}
