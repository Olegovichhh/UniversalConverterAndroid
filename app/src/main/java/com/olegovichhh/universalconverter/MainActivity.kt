package com.olegovichhh.universalconverter

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
 private var inputUri: Uri? = null
 private lateinit var fileText: TextView
 private lateinit var statusText: TextView
 private lateinit var spinner: Spinner
 private lateinit var progress: ProgressBar
 private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  uri ?: return@registerForActivityResult
  inputUri=uri
  try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_:Exception) {}
  fileText.text=displayName(uri); statusText.text="Файл выбран"
 }
 private val saver=registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { out -> out?.let { convertImage(it) } }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState); setContentView(R.layout.activity_main)
  fileText=findViewById(R.id.fileText); statusText=findViewById(R.id.statusText); spinner=findViewById(R.id.formatSpinner); progress=findViewById(R.id.progress)
  spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("JPG","PNG","WEBP","MP3","WAV","FLAC","M4A","MP4","WEBM"))
  findViewById<Button>(R.id.pickButton).setOnClickListener { picker.launch(arrayOf("*/*")) }
  findViewById<Button>(R.id.convertButton).setOnClickListener { startConversion() }
 }
 private fun startConversion() {
  if(inputUri==null){ Toast.makeText(this,"Сначала выберите файл",Toast.LENGTH_SHORT).show(); return }
  val fmt=spinner.selectedItem.toString().lowercase()
  if(fmt in listOf("jpg","png","webp")) saver.launch("converted.$fmt")
  else statusText.text="Аудио/видео формат будет подключён в v0.2"
 }
 private fun convertImage(out:Uri) {
  val source=inputUri?:return; progress.visibility=View.VISIBLE; statusText.text="Конвертация…"
  Thread {
   try {
    val bitmap=contentResolver.openInputStream(source).use { BitmapFactory.decodeStream(it) } ?: error("Неподдерживаемое изображение")
    val format=when(spinner.selectedItem.toString()){"PNG"->Bitmap.CompressFormat.PNG;"WEBP"->Bitmap.CompressFormat.WEBP_LOSSY;else->Bitmap.CompressFormat.JPEG}
    contentResolver.openOutputStream(out,"w").use { stream -> requireNotNull(stream); check(bitmap.compress(format,92,stream)) }
    bitmap.recycle(); runOnUiThread { progress.visibility=View.GONE; statusText.text="Готово. Файл сохранён." }
   } catch(ex:Exception) { runOnUiThread { progress.visibility=View.GONE; statusText.text="Ошибка: "+ex.message } }
  }.start()
 }
 private fun displayName(uri:Uri):String {
  contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { if(it.moveToFirst()) return it.getString(0) }
  return uri.lastPathSegment?:"Файл"
 }
}
