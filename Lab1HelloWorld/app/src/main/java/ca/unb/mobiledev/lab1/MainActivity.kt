package ca.unb.mobiledev.lab1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i(TAG, "This is a log display")
    }
    companion object {
        // String for LogCat documentation
        private const val TAG = "Main Activity"
    }
}

