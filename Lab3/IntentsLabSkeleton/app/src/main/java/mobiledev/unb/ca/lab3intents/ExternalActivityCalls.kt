package mobiledev.unb.ca.lab3intents

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ExternalActivityCalls : AppCompatActivity() {
    // Activity listeners

    lateinit var currentPhotoPath: String
    private var cameraActivityResultLauncher: ActivityResultLauncher<Intent>? = null

    private fun createImageFile(): File {
        Log.i("Lab 3", "Create Image File")
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun galleryAddPic() {
        Log.i("Lab 3", "Add Gallery")
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val f = File(currentPhotoPath)
            mediaScanIntent.data = Uri.fromFile(f)
            sendBroadcast(mediaScanIntent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.external_activity_calls)


        val buttonCamera = findViewById<Button>(R.id.btnCamera)
        buttonCamera.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
                intent.resolveActivity(packageManager)?.also {
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        null
                    }
                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            "mobiledev.unb.ca.lab3intents.provider",
                            it
                        )
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        cameraActivityResultLauncher!!.launch(intent)
                    }
                }
            }
        }

        val buttonEmail = findViewById<Button>(R.id.btnEmail)
        buttonEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // only email apps should handle this
                putExtra(Intent.EXTRA_EMAIL, arrayOf("edimayac@unb.ca"))
                putExtra(Intent.EXTRA_SUBJECT, "CS2063 Lab 3")
                putExtra(Intent.EXTRA_TEXT, "This is a test email!")
            }

            startActivity(intent)
        }

        val buttonBack = findViewById<Button>(R.id.btnBack)
        buttonBack.setOnClickListener {
            val intent = Intent(this@ExternalActivityCalls, MainActivity::class.java)
            startActivity(intent)
        }

        // Register the activity listener
        setCameraActivityResultLauncher()
    }

    private fun setCameraActivityResultLauncher() {
        // Handle the image capture result
        cameraActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                galleryAddPic()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_external_activity_calls, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }
}