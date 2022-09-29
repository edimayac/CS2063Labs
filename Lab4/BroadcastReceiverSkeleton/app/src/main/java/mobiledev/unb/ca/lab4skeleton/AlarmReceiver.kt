package mobiledev.unb.ca.lab4skeleton

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings.Global.getString
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService

class AlarmReceiver() : BroadcastReceiver(){

    override fun onReceive(p0: Context, p1: Intent?) {
        Log.e("onReceive", "Sending a notification")
        var builder = NotificationCompat.Builder(p0, "ALARM_RECEIVER")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentTitle(p0.getString(R.string.notification_title))
            .setContentText(p0.getString(R.string.notification_text))
            .setPriority(NotificationManager.IMPORTANCE_HIGH)


        with(NotificationManagerCompat.from(p0)) {
            // notificationId is a unique int for each notification that you must define
            notify(187968, builder.build())
        }
    }


}