package com.adjm.blok

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PuzzleDrawable(private val tint: Int, private val bump: Float) : Drawable() {
    var active = false
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = 0x55000000
    }

    override fun draw(c: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val b = h - bump
        val r = bump
        val nx = bump * 3
        val nw = bump * 4
        val p = Path()
        p.moveTo(0f, r)
        p.quadTo(0f, 0f, r, 0f)
        p.lineTo(nx, 0f)
        p.lineTo(nx + bump, bump)
        p.lineTo(nx + nw - bump, bump)
        p.lineTo(nx + nw, 0f)
        p.lineTo(w - r, 0f)
        p.quadTo(w, 0f, w, r)
        p.lineTo(w, b - r)
        p.quadTo(w, b, w - r, b)
        p.lineTo(nx + nw, b)
        p.lineTo(nx + nw - bump, b + bump)
        p.lineTo(nx + bump, b + bump)
        p.lineTo(nx, b)
        p.lineTo(r, b)
        p.quadTo(0f, b, 0f, b - r)
        p.close()
        fill.color = if (active) tint else 0xFFBDBDBD.toInt()
        c.drawPath(p, fill)
        c.drawPath(p, edge)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

class Block(
    val id: String,
    val title: String,
    val desc: String,
    val color: Int,
    val needsFile: Boolean,
    val mime: String
) {
    var active = false
    val editable: Boolean get() = id == "yml" || id == "xml" || id == "kotlin"
    val files = mutableListOf<Pair<String, ByteArray>>()
    lateinit var bg: PuzzleDrawable
    lateinit var holder: View
    lateinit var titleView: TextView
    lateinit var statusView: TextView
    lateinit var sw: Switch
}

class MainActivity : Activity() {

    companion object {
        const val REQ_PICK = 1
        const val REQ_SAVE = 2
    }

    private val blocks = listOf(
        Block("kerangka", "Kerangka Android", "Gradle, Manifest, Activity dasar, workflow", 0xFF4CAF50.toInt(), false, "*/*"),
        Block("yml", "YML", "File workflow GitHub (.yml)", 0xFFFF9800.toInt(), true, "*/*"),
        Block("xml", "XML", "Layout, strings, colors, AndroidManifest", 0xFF2196F3.toInt(), true, "*/*"),
        Block("kotlin", "Kotlin / Java", "Kode aplikasi (.kt, .java)", 0xFF9C27B0.toInt(), true, "*/*"),
        Block("mockup", "Gambar Mockup", "Gambar acuan tampilan", 0xFFE91E63.toInt(), true, "image/*"),
        Block("ikon", "Ikon Aplikasi", "Gambar menjadi ic_launcher", 0xFF009688.toInt(), true, "image/*"),
        Block("aset", "Aset Lain", "File apa saja ke folder assets", 0xFF795548.toInt(), true, "*/*")
    )

    private lateinit var nameEdit: EditText
    private lateinit var pkgEdit: EditText
    private var pendingBlock: Block? = null
    private var pendingZip: ByteArray? = null
    private lateinit var mainView: View
    private var editing = false

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(28), dp(16), dp(28))
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "ADJM Blok"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Aktifkan blok, pilih file, lalu simpan paket proyek (ZIP)."
            setPadding(0, 0, 0, dp(12))
        })

        nameEdit = EditText(this).apply {
            hint = "Nama aplikasi"
            setText("AplikasiBaru")
            setSingleLine()
        }
        pkgEdit = EditText(this).apply {
            hint = "Package, contoh com.adjm.aplikasibaru"
            setText("com.adjm.aplikasibaru")
            setSingleLine()
        }
        root.addView(nameEdit)
        root.addView(pkgEdit)

        val bump = dp(10)
        root.addView(View(this), ViewGroup.LayoutParams(1, dp(12)))

        for ((index, b) in blocks.withIndex()) {
            b.bg = PuzzleDrawable(b.color, bump.toFloat())
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), bump + dp(12), dp(16), bump + dp(12))
                background = b.bg
            }
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            b.titleView = TextView(this).apply {
                text = b.title
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
            }
            b.statusView = TextView(this).apply { textSize = 13f }
            col.addView(b.titleView)
            col.addView(b.statusView)
            row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            b.sw = Switch(this)
            row.addView(b.sw)
            b.holder = row

            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (index > 0) lp.topMargin = -bump
            root.addView(row, lp)

            b.sw.setOnCheckedChangeListener { _, on ->
                b.active = on
                if (!on) b.files.clear()
                refresh(b)
                if (on && b.needsFile) showMenu(b)
            }
            row.setOnClickListener {
                if (!b.active) b.sw.isChecked = true
                else if (b.needsFile) showMenu(b)
            }
            refresh(b)
        }

        val btn = Button(this).apply {
            text = "SIMPAN PROYEK (ZIP)"
            setOnClickListener { startExport() }
        }
        val blp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        blp.topMargin = dp(24)
        root.addView(btn, blp)

        root.addView(TextView(this).apply {
            text = "Setelah ZIP tersimpan: ekstrak, unggah semua file (tanpa folder) lewat Add file > Upload files. Workflow dibuat lewat Add file > Create new file, nama .github/workflows/build.yml, isinya salin dari build.yml. Lalu jalankan Build APK dan unduh dari Artifacts."
            textSize = 12f
            setPadding(0, dp(12), 0, 0)
        })

        mainView = scroll
        setContentView(scroll)
    }

    private fun refresh(b: Block) {
        b.bg.active = b.active
        b.holder.invalidate()
        val c = if (b.active) Color.WHITE else 0xFF424242.toInt()
        b.titleView.setTextColor(c)
        b.statusView.setTextColor(c)
        b.statusView.text = when {
            !b.active -> b.desc
            !b.needsFile -> "Aktif: " + b.desc
            b.files.isEmpty() -> "Ketuk blok untuk memilih file"
            else -> b.files.joinToString(", ") { it.first } + "  (ketuk untuk tambah)"
        }
    }

    private fun pick(b: Block) {
        pendingBlock = b
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = b.mime
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(i, REQ_PICK)
    }

    private fun showMenu(b: Block) {
        if (!b.editable) {
            pick(b)
            return
        }
        val labels = mutableListOf("Pilih file dari HP", "Tulis file baru")
        for (f in b.files) labels.add("Edit: " + f.first)
        AlertDialog.Builder(this)
            .setTitle(b.title)
            .setItems(labels.toTypedArray()) { _, which ->
                when {
                    which == 0 -> pick(b)
                    which == 1 -> openEditor(b, null, defaultName(b), template(b))
                    else -> {
                        val f = b.files[which - 2]
                        openEditor(b, f.first, f.first, String(f.second, Charsets.UTF_8))
                    }
                }
            }
            .show()
    }

    private fun defaultName(b: Block): String = when (b.id) {
        "xml" -> "activity_main.xml"
        "kotlin" -> "MainActivity.kt"
        else -> "build.yml"
    }

    private fun template(b: Block): String = when (b.id) {
        "xml" -> "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n    android:layout_width=\"match_parent\"\n    android:layout_height=\"match_parent\"\n    android:orientation=\"vertical\">\n\n</LinearLayout>\n"
        "kotlin" -> "package " + pkgEdit.text.toString().trim().lowercase() + "\n\n"
        else -> ""
    }

    private fun openEditor(b: Block, oldName: String?, fileName: String, isi: String) {
        editing = true
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(28), dp(12), dp(12))
        }
        col.addView(TextView(this).apply {
            text = "Editor blok " + b.title
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        })
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val batal = Button(this).apply { text = "BATAL" }
        val simpan = Button(this).apply { text = "SIMPAN" }
        bar.addView(batal, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(simpan, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        col.addView(bar)
        val nameBox = EditText(this).apply {
            hint = "Nama file"
            setText(fileName)
            setSingleLine()
        }
        col.addView(nameBox)
        val body = EditText(this).apply {
            setText(isi)
            gravity = Gravity.TOP or Gravity.START
            typeface = Typeface.MONOSPACE
            textSize = 13f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        col.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        batal.setOnClickListener { closeEditor(b) }
        simpan.setOnClickListener {
            var n = nameBox.text.toString().trim()
            if (n.isEmpty()) {
                Toast.makeText(this, "Isi nama file dulu", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val ext = when (b.id) {
                "xml" -> ".xml"
                "kotlin" -> ".kt"
                else -> ".yml"
            }
            if (!n.contains('.')) n += ext
            val data = body.text.toString().toByteArray(Charsets.UTF_8)
            b.files.removeAll { it.first == n || it.first == oldName }
            b.files.add(n to data)
            closeEditor(b)
            Toast.makeText(this, "Tersimpan di blok " + b.title, Toast.LENGTH_SHORT).show()
        }
        setContentView(col)
    }

    private fun closeEditor(b: Block) {
        editing = false
        setContentView(mainView)
        refresh(b)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (editing) {
            editing = false
            setContentView(mainView)
        } else {
            super.onBackPressed()
        }
    }

    private fun nameOf(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) return c.getString(i)
        }
        return uri.lastPathSegment ?: "file"
    }

    private fun blk(id: String): Block = blocks.first { it.id == id }

    private fun startExport() {
        val bad = blocks.filter { it.active && it.needsFile && it.files.isEmpty() }
        if (bad.isNotEmpty()) {
            Toast.makeText(this, "Blok " + bad[0].title + " belum ada file", Toast.LENGTH_LONG).show()
            return
        }
        if (blocks.none { it.active }) {
            Toast.makeText(this, "Aktifkan minimal satu blok", Toast.LENGTH_LONG).show()
            return
        }
        try {
            pendingZip = buildZip()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal merakit: " + e.message, Toast.LENGTH_LONG).show()
            return
        }
        val safe = nameEdit.text.toString().trim().ifEmpty { "AplikasiBaru" }
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, "$safe.zip")
        }
        startActivityForResult(i, REQ_SAVE)
    }

    private fun buildZip(): ByteArray {
        val rawName = nameEdit.text.toString().trim().ifEmpty { "AplikasiBaru" }
        val name = rawName.replace("\"", "").replace('$', ' ').replace("\\", "")
        val xmlName = name.replace("&", "&amp;").replace("<", "&lt;")
        val pkg = pkgEdit.text.toString().trim().lowercase().ifEmpty { "com.adjm.aplikasibaru" }
        val out = ByteArrayOutputStream()
        val used = LinkedHashSet<String>()
        var hasIcon = false

        ZipOutputStream(out).use { zip ->
            fun put(path: String, data: ByteArray) {
                if (used.add(path)) {
                    zip.putNextEntry(ZipEntry(path))
                    zip.write(data)
                    zip.closeEntry()
                }
            }
            fun putText(path: String, text: String) = put(path, text.toByteArray())

            // Semua file diletakkan datar (tanpa folder); susun.sh yang menyusunnya di GitHub
            for (b in blocks.filter { it.active }) {
                for ((f, data) in b.files) {
                    when (b.id) {
                        "yml" -> put(f, data)
                        "xml" -> put(if (f == "AndroidManifest.xml") f else f.lowercase(), data)
                        "kotlin" -> put(f, data)
                        "mockup" -> put("mockup_$f", data)
                        "aset" -> put("aset_$f", data)
                    }
                }
            }

            val ik = blk("ikon")
            if (ik.active && ik.files.isNotEmpty()) {
                val src = ik.files[0].second
                val bmp = BitmapFactory.decodeByteArray(src, 0, src.size)
                if (bmp != null) {
                    val sc = Bitmap.createScaledBitmap(bmp, 192, 192, true)
                    val bo = ByteArrayOutputStream()
                    sc.compress(Bitmap.CompressFormat.PNG, 100, bo)
                    put("ic_launcher.png", bo.toByteArray())
                    hasIcon = true
                }
            }

            val needSusun = blocks.any { it.active && it.id != "yml" && it.id != "mockup" }
            if (needSusun) put("susun.sh", assets.open("susun.sh").use { it.readBytes() })

            if (blk("kerangka").active) {
                putText("settings.gradle.kts", """
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "app-proyek"
include(":app")
""".trimStart())

                putText("build.gradle.kts", """
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
""".trimStart())

                putText("gradle.properties", "org.gradle.jvmargs=-Xmx2048m\nandroid.useAndroidX=true\nandroid.nonTransitiveRClass=true\n")

                putText("app-build.gradle.kts", """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "$pkg"
    compileSdk = 34

    defaultConfig {
        applicationId = "$pkg"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
""".trimStart())

                val iconAttr = if (hasIcon) "\n        android:icon=\"@mipmap/ic_launcher\"" else ""
                putText("AndroidManifest.xml", """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="$xmlName"$iconAttr
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""".trimStart())

                if (used.none { it.startsWith("MainActivity.") }) {
                    putText("MainActivity.kt", """
package $pkg

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = TextView(this)
        t.text = "$name"
        t.textSize = 24f
        t.setPadding(48, 96, 48, 48)
        setContentView(t)
    }
}
""".trimStart())
                }

                if (!blk("yml").active) {
                    putText("build.yml", """
name: Build APK
on:
  push:
    branches: [ main, master ]
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: bash susun.sh
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.7'
      - run: gradle assembleDebug --no-daemon
      - uses: actions/upload-artifact@v4
        with:
          name: apk
          path: app/build/outputs/apk/debug/*.apk
""".trimStart())
                }
            }
        }
        return out.toByteArray()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        try {
            if (requestCode == REQ_PICK) {
                val b = pendingBlock ?: return
                val uris = mutableListOf<Uri>()
                val cd = data.clipData
                if (cd != null) {
                    for (i in 0 until cd.itemCount) uris.add(cd.getItemAt(i).uri)
                } else {
                    data.data?.let { uris.add(it) }
                }
                for (u in uris) {
                    val bytes = contentResolver.openInputStream(u)?.use { it.readBytes() } ?: continue
                    val n = nameOf(u)
                    b.files.removeAll { it.first == n }
                    b.files.add(n to bytes)
                }
                refresh(b)
            } else if (requestCode == REQ_SAVE) {
                val uri = data.data ?: return
                contentResolver.openOutputStream(uri)?.use { it.write(pendingZip ?: ByteArray(0)) }
                Toast.makeText(this, "ZIP tersimpan", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal: " + e.message, Toast.LENGTH_LONG).show()
        }
    }
}
