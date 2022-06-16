package com.gogatsun.pakipsi

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.gogatsun.pakipsi.db.DBManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*


class TrackerService : Service() {

    //private lateinit var mHandler: Handler

    companion object { // открытые переменные статичного класса
        const val channelId = "--your channel id--"
        const val notifyId = 395 // some number
        var mInterval:Long = 2000
        val notificationName = "Имя уведомления"
        var location = "none"
        var dbManager : DBManager? = null
        var isRunning  = false
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        if (isRunning) return

        super.onCreate()
        sendStartNotification()

        dbManager = DBManager(this)
        dbManager?.openDB()

        mInterval = getInterval() * 1000
        //dbManager?.clearDB()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this!!)
        getLocationUpdates()
    }

    private fun getInterval() : Long {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val select = sharedPrefs.getString("period_saving_geo", null).toString()
        toast("Выбран интервал $select сек")
        return select.toLong();
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            toast("Служба запущена")
            startLocationUpdates()
            //timer.schedule(monitor, mInterval, mInterval)
            isRunning = true
        }
        return START_NOT_STICKY
    }

//    val timer = Timer()
//    val monitor = object : TimerTask() {
//        override fun run() {
//            getLocationUpdates()
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        //timer.cancel()
        isRunning = false
        stopLocationUpdates()
        toast("Служба остановлена")
    }

    fun toast(mes:String){
        Toast.makeText(this,mes, Toast.LENGTH_SHORT).show()
    }

    private fun sendNotify(builder: NotificationCompat.Builder){
        with(NotificationManagerCompat.from(this))
        {
            notify(notifyId, builder.build()) // посылаем уведомление
        }
    }

    private fun createNotification(title:String, text: String, notifyId : Int = 0, channelId : String = "new"):NotificationCompat.Builder {

        val notificationBuilder = NotificationCompat.Builder(this, channelId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, "Уведомление приложения", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        notificationBuilder
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)

        return  notificationBuilder
    }

    private val notificationBuilder
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationCompat.Builder(this, channelId)
        else NotificationCompat.Builder(this, channelId)

    private fun sendStartNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannel()
        val notification = buildNotification()
        this.startForeground(notifyId, notification)
    }

    private fun buildNotification(): Notification {
        val builder = notificationBuilder

        builder
            //.setSmallIcon(R.drawable.ic_menu_6)
            .setContentTitle(this.resources.getString(R.string.app_name) + " Tracking")
            .setContentText("Это уведомление")
            .setShowWhen(true)
            .setOngoing(true)
            .setProgress(100, 0, true)
        //.priority = NotificationCompat.PRIORITY_MAX
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, notificationName, NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
    }

    /// Location

    // declare a global variable FusedLocationProviderClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // globally declare LocationRequest
    private lateinit var locationRequest: com.google.android.gms.location.LocationRequest

    // globally declare LocationCallback
    private lateinit var locationCallback: LocationCallback

    /**
     * call this method in onCreate
     * onLocationResult call when location is changed
     */
    private fun getLocationUpdates()
    {
        //toast("getLocationUpdates")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this!!)
        locationRequest = com.google.android.gms.location.LocationRequest()
        locationRequest.interval = mInterval
        locationRequest.fastestInterval = mInterval
        locationRequest.smallestDisplacement = 170f // 170 m = 0.1 mile
        locationRequest.priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY //set according to your app function
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0 ?: return

                if (p0.locations.isNotEmpty()) {

                    if (isRunning) {
                        // get latest location
                        val location = p0.lastLocation
                        // use your location object
                        // get latitude , longitude and other info from this
                        TrackerService.location = "${location?.latitude} ${location?.longitude}"

                        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                        val currentDate = sdf.format(Date())
                        dbManager?.insertToDB(currentDate, TrackerService.location)
                        toast(TrackerService.location)
                    }

                    //sendNotify(createNotification("Координаты", "${location?.latitude} ${location?.longitude}"))
                }
            }
        }
    }

    //start location updates
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }

    // stop location updates
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

}