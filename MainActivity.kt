@file:Suppress("DEPRECATION")

package com.example.myweather

import WeatherDatabaseHelper
import WeatherContract
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myweather.R
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import java.util.Random


class MainActivity : AppCompatActivity() {
    private val API_KEY = "70929f38c07180a770ce1eabc4635d65"
    private lateinit var dateEditText: EditText
    private lateinit var yearEditText: EditText
    private lateinit var cityEditText: EditText
    private lateinit var resultTextView: TextView

    // Database helper
    private lateinit var dbHelper: WeatherDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize database helper
        dbHelper = WeatherDatabaseHelper(this)

        dateEditText = findViewById(R.id.dateEditText)
        yearEditText = findViewById(R.id.yearEditText)
        cityEditText = findViewById(R.id.cityEditText)
        resultTextView = findViewById(R.id.resultTextView)

        insertRandomDataForMumbai()
    }

    private fun insertRandomDataForMumbai() {
        val random = Random()
        val db = dbHelper.writableDatabase

        // Loop through the years from 2010 to 2024
        for (year in 2010..2024) {
            // Generate random temperature values for max and min temperatures
            val maxTemp = random.nextDouble() * 40 // Random max temperature between 0°C and 40°C
            val minTemp = random.nextDouble() * 20 // Random min temperature between 0°C and 20°C

            // Prepare the ContentValues to insert into the database
            val values = ContentValues().apply {
                put(WeatherContract.WeatherEntry.COLUMN_CITY, "Mumbai")
                put(WeatherContract.WeatherEntry.COLUMN_DATE, year.toString())
                put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, maxTemp)
                put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, minTemp)
            }

            // Insert the data into the database
            val newRowId = db?.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values)

            // Check if the data was inserted successfully
            if (newRowId != null && newRowId != -1L) {
                Log.d("DATABASE", "Data inserted successfully for year $year")
            } else {
                Log.e("DATABASE", "Failed to insert data for year $year")
            }
        }
    }


    fun getWeather(view: View) {
        val date = dateEditText.text.toString()
        val year = yearEditText.text.toString()
        val city = cityEditText.text.toString()

        if (date.isEmpty() || year.isEmpty() || city.isEmpty()) {
            resultTextView.text = "Please enter all fields."
            return
        }

        FetchWeatherTask().execute(city, date, year)
    }

    private inner class FetchWeatherTask : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String): String? {
            val city = params[0]
            val date = params[1]
            val year = params[2]

            // Check if the date is in the future
            if (isFuture(year.toInt())) {
                val avgTemps = calculateAverage(city, date)
                val avgMaxTemp = avgTemps.first
                val avgMinTemp = avgTemps.second
                return String.format(
                    Locale.getDefault(),
                    "Average Max Temp: %.2f°C\nAverage Min Temp: %.2f°C",
                    avgMaxTemp,
                    avgMinTemp
                )
            } else {
                return fetchWeatherFromApi(city)
            }
        }

        override fun onPostExecute(result: String?) {
            if (result == null) {
                resultTextView.text = "Error fetching data"
            } else {
                resultTextView.text = result
            }
        }

        private fun fetchWeatherFromApi(city: String,date: String): String? {
            val apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=$city&dt=$date&appid=$API_KEY"
            val jsonResponse = getUrlResponse(apiUrl)

            val maxTemp: Double
            val minTemp: Double

            try {
                val main = JSONObject(jsonResponse).getJSONObject("main")
                maxTemp = main.getDouble("temp_max")
                minTemp = main.getDouble("temp_min")

                // Insert data into the database
                insertDataToDatabase(city, "", maxTemp, minTemp)
            } catch (e: JSONException) {
                Log.e("ERROR", "Error parsing JSON: " + e.message)
                return null
            }

            return String.format(
                Locale.getDefault(),
                "Max Temp: %.2f°C\nMin Temp: %.2f°C",
                maxTemp,
                minTemp
            )
        }

        private fun getUrlResponse(apiUrl: String): String {
            try {
                val url = URL(apiUrl)
                val urlConnection = url.openConnection() as HttpURLConnection
                try {
                    val `in`: InputStream = urlConnection.inputStream
                    val reader = BufferedReader(InputStreamReader(`in`))
                    val stringBuilder = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line).append("\n")
                    }
                    reader.close()
                    return stringBuilder.toString()
                } finally {
                    urlConnection.disconnect()
                }
            } catch (e: IOException) {
                Log.e("ERROR", "Error downloading data: " + e.message)
                throw e
            }
        }

        private fun isFuture(year: Int): Boolean {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            return year > currentYear
        }

        private fun calculateAverage(city: String, date: String): Pair<Double, Double> {
            // Implement logic to calculate average temperatures from database
            // This logic is already provided in your code
            val db = dbHelper.readableDatabase
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val tenYearsAgo = currentYear - 10

            // Prepare the selection and selectionArgs for querying the database
            val selection = "${WeatherContract.WeatherEntry.COLUMN_CITY}=? AND ${WeatherContract.WeatherEntry.COLUMN_DATE}<=?"
            val selectionArgs = arrayOf(city, tenYearsAgo.toString())
            val cursor = db.query(
                WeatherContract.WeatherEntry.TABLE_NAME,
                arrayOf(
                    "AVG(${WeatherContract.WeatherEntry.COLUMN_MAX_TEMP})",
                    "AVG(${WeatherContract.WeatherEntry.COLUMN_MIN_TEMP})"
                ),
                selection,
                selectionArgs,
                null,
                null,
                null
            )

            var avgMaxTemp = 0.0
            var avgMinTemp = 0.0

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    avgMaxTemp = cursor.getDouble(0)
                    avgMinTemp = cursor.getDouble(1)
                }
                cursor.close()
            }

            return Pair(avgMaxTemp, avgMinTemp)
        }
    }


    private fun insertDataToDatabase(city: String, date: String, maxTemp: Double, minTemp: Double) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(WeatherContract.WeatherEntry.COLUMN_CITY, city)
            put(WeatherContract.WeatherEntry.COLUMN_DATE, date)
            put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, maxTemp)
            put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, minTemp)
        }

        val newRowId = db?.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values)

        if (newRowId != null && newRowId != -1L) {
            Log.d("DATABASE", "Data inserted successfully")
        } else {
            Log.e("DATABASE", "Failed to insert data")
        }
    }
    private fun queryTemperaturesForYear(city: String, date: String): Pair<Double, Double>? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            WeatherContract.WeatherEntry.TABLE_NAME,
            arrayOf(
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
            ),
            "${WeatherContract.WeatherEntry.COLUMN_CITY}=? AND ${WeatherContract.WeatherEntry.COLUMN_DATE}=?",
            arrayOf(city, date),
            null,
            null,
            null
        )

        var maxTemp: Double? = null
        var minTemp: Double? = null

        if (cursor != null && cursor.moveToFirst()) {
            val maxTempIndex = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)
            val minTempIndex = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)

            if (maxTempIndex != -1 && minTempIndex != -1) {
                maxTemp = cursor.getDouble(maxTempIndex)
                minTemp = cursor.getDouble(minTempIndex)
            }
            cursor.close()
        }

        return if (maxTemp != null && minTemp != null) Pair(maxTemp, minTemp) else null
    }


}
