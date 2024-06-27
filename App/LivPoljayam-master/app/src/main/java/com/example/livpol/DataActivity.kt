package com.example.livpol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.livpol.databinding.ActivityDataBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast

@Suppress("DEPRECATION")
class DataActivity : AppCompatActivity() {

    private lateinit var binding : ActivityDataBinding

//    private lateinit var goBack: ImageView
//    private lateinit var people: TextView
//    private lateinit var minutes: TextView
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationChannel: NotificationChannel
//    private lateinit var circles: ImageView
    private lateinit var context: Context
    private var notified = false
    lateinit var builder: Notification.Builder
    private val channelId = "i.apps.notifications"
    private val description = "Test notification"
    var lastNotificationTime: Long = 0
    private lateinit var label: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        label=""
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        binding.toolbar.setOnClickListener{
            openMap()
        }

        createBarChart()
        createNotificationChannel()


        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Write a message to the database
        val database = Firebase.database
        val myRef = database.getReference("people")

        myRef.addValueEventListener(object: ValueEventListener {
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun onDataChange(snapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                val value = snapshot.getValue().toString()
//                val value = snapshot.getValue<HashMap<String,String>>().toString()
                Log.d("hi", "Value is: " + value)
                binding.people.text = value.replaceFirst("{Name=","").replaceFirst("}","") + " \nPeople"
                binding.minutes.text = (value.replaceFirst("{Name=","").replaceFirst("}","").toInt()*2).toString() + " \nMins"

//                val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                var v = value.replaceFirst("{Name=","").replaceFirst("}","").toInt()
                runOnUiThread(Runnable{
                    binding.peopleProgressBar.progress = v
                    binding.peopleProgressBar.max = 20
                    binding.minutesProgressBar.progress = v*2
                    binding.minutesProgressBar.max = 20
                })

                // Check condition and send notification only if 10 seconds have passed since last notification
                if (v in 1..3) {
                    // Calculate the time difference in milliseconds since the last notification
                    val currentTime = SystemClock.elapsedRealtime()
                    val timeDifference = currentTime - lastNotificationTime

                    // Check if 10 seconds (10000 milliseconds) have passed since the last notification
                    if (timeDifference >= 10000) {
                        // Trigger notification
                        sendNotification()
                    }}


                // RemoteViews are used to use the content of
                // some different layout apart from the current activity layout
//                val contentView = RemoteViews(packageName, R.layout.activity_after_notification)

            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("ji", "Failed to read value.", error.toException())
            }

        })

        val extras = intent.extras
        val latitude = ""
        val longitude = ""
        if (extras != null) {
            val epicno = extras.getString("epicno")
            val captchaData = extras.getString("captchaData")
            val captchaId = extras.getString("captchaId").toString()
            binding.lay.visibility = View.VISIBLE

            // Call the function with the retrieved values
            CoroutineScope(Dispatchers.IO).launch {
                val data = getVoterInfo(epicno, captchaData, captchaId)


                if (data != null) {
                    // Print the received data in Logcat
                    Log.d("SecondActivity", "Received data: ${data.toString()} ")
                    // Parse the JSON response
//                    val jsonResponse = JSONObject(data)
                    val content = data.getJSONObject("content")
                    try {
                        withContext(Dispatchers.Main) {
                            findViewById<TextView>(R.id.epicNumber).text =
                                content.getString("epicNumber")
                            findViewById<TextView>(R.id.firstName).text =
                                content.getString("applicantFirstName")
                            findViewById<TextView>(R.id.relationName).text =
                                content.getString("relationName")
                            findViewById<TextView>(R.id.age).text = content.getInt("age").toString()
                            findViewById<TextView>(R.id.gender).text = content.getString("gender")
                            findViewById<TextView>(R.id.birthYear).text =
                                content.optString("birthYear", "")
                            findViewById<TextView>(R.id.locality).text =
                                content.getInt("partNumber").toString()
                            findViewById<TextView>(R.id.district).text = content.getString("partName")
                            findViewById<TextView>(R.id.state).text = content.getString("stateName")
                            findViewById<TextView>(R.id.createdDttm).text =
                                content.getString("createdDttm")
                            findViewById<TextView>(R.id.pollingStation).text =
                                content.getString("psbuildingName")
                            label = content.getString("psbuildingName") + ", " + content.getString("buildingAddress")
                        }
                    } catch (e : Exception) {
                        Log.e("getVoterInfo", "An severe error occurred: ${e.message}")
                    }
                } else {
                    Log.e("SecondActivity", "Failed to get voter info")
                }

            }
        }
    }

    //notifications
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun sendNotification() {
        createNotificationChannel()
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notif)  // Set notification icon (ensure notificationIcon is not null)
            .setContentTitle("Voting Reminder")  // Set notification title
//            .setContentText("Don't forget to visit your polling station today!")  // Set notification text
            .setContentText("Less crowd detected at your polling station!!")  // Set notification text
            .setStyle(NotificationCompat.BigTextStyle().bigText("Remember, your vote is your voice. Make it heard!"))  // Optional: Expandable notification text
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // Set notification priority

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request POST_NOTIFICATIONS permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                /* Replace with a unique request code */ 101  // Example request code
            )
            return
        }
        notificationManager.notify(1, notificationBuilder.build())  // Send notification with an ID of 1
        lastNotificationTime = SystemClock.elapsedRealtime()

    }



    private val CHANNEL_ID = "voting_reminder"
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Voting Reminder Channel"
            val descriptionText = "Testing"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createBarChart() {

        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f,10f))
        entries.add(BarEntry(1f,30f))
        entries.add(BarEntry(2f,60f))
        entries.add(BarEntry(3f,100f))
        entries.add(BarEntry(5f,70f))
        entries.add(BarEntry(10f,80f))

        val barDataSet = BarDataSet(entries,"BarDataSet")

        val dataSet = BarData(barDataSet)

        dataSet.barWidth = 0.9f

        binding.barchart.data = dataSet
        binding.barchart.setFitBars(true)
        binding.barchart.invalidate()
    }

    private suspend fun getVoterInfo(
        epicno: String?,
        captchaData: String?,
        captchaId: String
    ): JSONObject? {
        val url =
            URL("https://gateway.eci.gov.in/api/v1/elastic/search-by-epic-from-national-display")

        val headers = mapOf(
            "Accept" to "application/json, text/plain, */*",
            "Accept-Language" to "en-GB,en-US;q=0.9,en;q=0.8,hi;q=0.7",
            "Connection" to "keep-alive",
            "Content-Type" to "application/json",
            "DNT" to "1",
            "Origin" to "https://electoralsearch.eci.gov.in",
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "same-site",
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
            "applicationName" to "ELECTORAL_SEARCH",
            "sec-ch-ua" to "\"Chromium\";v=\"119\", \"Not?A_Brand\";v=\"24\"",
            "sec-ch-ua-mobile" to "?0",
            "sec-ch-ua-platform" to "\"Linux\""
        )

        val data = JSONObject().apply {
            put("isPortal", true)
            put("epicNumber", epicno)
            put("captchaId", captchaId)
            put("captchaData", captchaData)
            put("securityKey", "na")
        }

        Log.d("getVoterInfo", data.toString())

        return withContext(Dispatchers.IO) {
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }

                connection.doOutput = true
                connection.outputStream.use { outputStream ->
                    outputStream.write(data.toString().toByteArray())
                }

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    val jsonResponse = response.substring(1, response.length - 1) // Remove first and last char
                    Log.d("getVoterInfo", jsonResponse)
                    JSONObject(jsonResponse)
                } else {
                    Log.e("getVoterInfo", "Failed to get data. Status code: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e("getVoterInfo", "An error occurred: ${e.message}")
                null
            }
        }

    }
    fun openMap() {
        val latitude = 12.13
        val longitude = 2.356
        if(label==""){
            label = "Virat International School Kapriwas"
        }
//        val label = "Kedarnath Hindi Primary School, Sahajramjote, Upper Bagdogra" // You can customize this label if needed
        val uri = String.format("geo:%s,%s?q=%s",latitude, longitude, label)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        Log.d("TAG",uri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Google Maps app is not installed", Toast.LENGTH_SHORT).show()
           }
        }


}