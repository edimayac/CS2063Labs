package mobiledev.unb.ca.lab4skeleton

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.Intent.ACTION_BATTERY_LOW
import android.content.Intent.ACTION_BATTERY_OKAY
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.AlarmManagerCompat.setAlarmClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    // Attributes for storing the file photo path
    private lateinit var currentPhotoPath: String
    private lateinit var imageFileName: String
    private var alarmMgr: AlarmManager? = null
    private lateinit var alarmIntent: PendingIntent

    // Activity listeners
    private var cameraActivityResultLauncher: ActivityResultLauncher<Intent>? = null

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryInfoReceiver)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val cameraButton = findViewById<Button>(R.id.button)
        cameraButton.setOnClickListener { dispatchTakePhotoIntent() }

        // Register the activity listener
        setCameraActivityResultLauncher()

        Log.e("onCreate()", "Set Alarm")

        createNotificationChannel()

        val filter = IntentFilter()
        filter.addAction(ACTION_BATTERY_LOW)
        filter.addAction(ACTION_BATTERY_OKAY)

        registerReceiver(batteryInfoReceiver, filter)

        alarmMgr = this@MainActivity.getSystemService(ALARM_SERVICE) as AlarmManager

        alarmIntent = Intent(this@MainActivity, AlarmReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(this@MainActivity, 0, intent, 0)
        }
        alarmMgr?.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 1000,
            30000,
            alarmIntent
        )
   }

    private val batteryInfoReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.e("batteryInfo", intent.action.toString())

            if (intent.action.toString() == ACTION_BATTERY_LOW) {
                alarmMgr?.cancel(alarmIntent)
                Toast.makeText(context, "Alarm Manager has been cancelled", Toast.LENGTH_LONG).show();
            } else {
                alarmMgr?.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 1000,
                    30000,
                    alarmIntent
                )
                Toast.makeText(context, "Alarm Manager has reset", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Private Helper Methods
    private fun setCameraActivityResultLauncher() {
        cameraActivityResultLauncher = registerForActivityResult(
            StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                galleryAddPic()
            }
        }
    }

    // Camera methods
    private fun dispatchTakePhotoIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there is a camera activity to handle the intent
            try {
                // Set the File object used to save the photo
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e(TAG, "Exception found when creating the photo save file")
                }

                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI = FileProvider.getUriForFile(
                        this,
                        "mobiledev.unb.ca.lab4skeleton.provider",
                        photoFile
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

                    // Calling this method allows us to capture the return code
                    cameraActivityResultLauncher!!.launch(takePictureIntent)
                }
            } catch (ex: ActivityNotFoundException) {
                Log.e(TAG, "Unable to load activity", ex)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat(TIME_STAMP_FORMAT, Locale.getDefault()).format(Date())
        imageFileName = "IMG_" + timeStamp + "_"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            imageFileName,  // prefix
            ".jpg",  // suffix
            storageDir // directory
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun galleryAddPic() {
        Log.d(TAG, "Saving image to the gallery")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 and above
            mediaStoreAddPicToGallery()
        } else {
            // Pre Android 10
            mediaScannerAddPicToGallery()
        }
        Log.i(TAG, "Image saved!")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mediaStoreAddPicToGallery() {
        val name = imageFileName
        val bitmap = BitmapFactory.decodeFile(currentPhotoPath)

        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)

        val resolver = contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        try {
            resolver.openOutputStream(imageUri!!).use { fos ->
                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    100,
                    fos
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error saving the file ", e)
        }
    }

    private fun mediaScannerAddPicToGallery() {
        val file = File(currentPhotoPath)
        MediaScannerConnection.scanFile(this@MainActivity,
            arrayOf(file.toString()),
            arrayOf(file.name),
            null)
    }


    companion object {
        private const val TAG = "MainActivity"
        private const val CHANNEL_ID = "ALARM_RECEIVER"
        private const val TIME_STAMP_FORMAT = "yyyyMMdd_HHmmss"
    }
}