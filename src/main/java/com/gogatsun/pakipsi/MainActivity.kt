package com.gogatsun.pakipsi

//import android.R
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView


@RequiresApi(api = Build.VERSION_CODES.M)
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        findViewById<View>(R.id.imageMenu).setOnClickListener {
            drawerLayout.openDrawer(
                GravityCompat.START
            )
        }
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        navigationView.itemIconTintList = null
        val navController = findNavController(this, R.id.navHostFragment)
        setupWithNavController(navigationView, navController)

        // Запрос разрешений
        requestPermissions(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                android.Manifest.permission.READ_CONTACTS
            )
        )


    }


    override fun onResume() {
        super.onResume()

        // Запуск трекинга
        findViewById<View>(R.id.startTracking).setOnClickListener {
            if (!TrackerService.isRunning)
                Intent(this, TrackerService::class.java).also { intent -> startService(intent) }
            else
                toast("Служба уже запущена")
        }

        // Остановка трекинга
        findViewById<View>(R.id.stopTracking).setOnClickListener {
            if (TrackerService.isRunning)
                Intent(this, TrackerService::class.java).also { intent -> stopService(intent) }
            else
                toast("Служба не была запущена")
        }

        // Кнопка активации
        findViewById<View>(R.id.activateButton).setOnClickListener {
            if (TrackerService.dbManager != null)
            {
                var res = ""
                // Запрос данных из базы
                val data = TrackerService.dbManager!!.readDBData()
                for (item in data) res += "(${item.date}) ${item.coord} \n"

                // Вывод данных на экран
                (findViewById<View>(R.id.edtInfo) as EditText).setText(res)

                // Формирование письма
                val mess = getMessage()

                // Получение номеров доверенных контактов
                val nums = getSelectedContacts()

                // Отправка на смс
                try {
                    for (item in nums) sendSMS(item, mess + res)
                    toast("Сообщения отправлены ${nums.size} контактам")
                }
                catch (e: Exception)
                {
                    toast("Ошибка отправки сообщений")
                }
            }
        }
    }

    private fun toast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
    }

    private fun requestPermissions(permissions : Array<String>) {
        for (item in permissions) showPhoneStatePermission(item)
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        val sentPI: PendingIntent = PendingIntent.getBroadcast(this, 0, Intent("SMS_SENT"), 0)
        SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, sentPI, null)
    }

    private fun getSelectedContacts() : Array<String> {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val selections = sharedPrefs.getStringSet("contacts_users_pref", null)
        toast(selections.toString())
        return selections?.toTypedArray() as Array<String>
    }

    private fun getMessage() : String {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val select = sharedPrefs.getString("massage_enter", null).toString()
        //toast(select)
        return select;
    }

    private fun showPhoneStatePermission(permission : String) {
        val permissionCheck = ContextCompat.checkSelfPermission(this, permission)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) showExplanation("Требуется разрешение", "$permission", permission)
            else requestPermission(permission)
        }
    }

    private fun showExplanation(title: String, message: String, permission: String, ) {
        val builder: AlertDialog.Builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, id -> requestPermission(permission) })
        builder.create().show()
    }

    private fun requestPermission(permissionName: String) {
        ActivityCompat.requestPermissions(this, arrayOf(permissionName), 0)
    }
}