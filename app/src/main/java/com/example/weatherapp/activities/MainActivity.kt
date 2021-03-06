package com.example.weatherapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.weatherapp.R
import com.example.weatherapp.app.AppActivity
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.example.weatherapp.utils.Constants
import com.google.android.gms.location.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

// OpenWeather Link : https://openweathermap.org/api
/**
 * The useful link or some more explanation for this app you can checkout this link :
 * https://medium.com/@sasude9/basic-android-weather-app-6a7c0855caf4
 */

class MainActivity : AppActivity() {
    val BASE_URI = "http://api.openweathermap.org/data/"


    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog: Dialog? = null
    private lateinit var mSharedPreferences: SharedPreferences
    private lateinit var mSharedPreferencesOldDay: SharedPreferences

    companion object {
        const val PERMISSION_REQUEST_CODE = 1
    }

    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        mProgressDialog = Dialog(this)
        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if(!Constants.isNetworkAvailable(this@MainActivity) || !isLocationEnable()){
            showCustomProgressDialog()



            lifecycleScope.launch(Dispatchers.IO) {
                var weatherResponseOldDay : String? = null
                val job = lifecycleScope.launch {
                    weatherResponseOldDay =
                        mSharedPreferences.getString(Constants.WEATHER_RESPONSE_OLD_DAY, "").toString()
                }
                job.join()
                job.cancel()
                withContext(Dispatchers.Main){
                    if (weatherResponseOldDay != null && weatherResponseOldDay!!.isNotEmpty()){
                        binding?.tvNotificationWeather?.visibility = View.VISIBLE
                        binding?.tvNotificationWeather?.text = "D??? li???u th???i ti???t ng??y: $weatherResponseOldDay"
                    }else{
                        binding?.tvNotificationWeather?.visibility =View.INVISIBLE
                    }
                }
                setupUI()
            }

        }
        checkPermissionVsCallApi()
    }

    override fun onDestroy() {
        mProgressDialog = null
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ) {
                    requestNewLocationData()
                    println("B???n ???? Refresh l???i Data...")
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        val sdf = SimpleDateFormat("HH:mm", Locale.UK)
        /**
         * Ph????ng th???c getDefault () c???a l???p TimeZone trong Java ???????c s??? d???ng ????? bi???t TimeZone m???c ?????nh cho h??? th???ng ho???c m??y ch??? n??y.
         * ??i???u n??y c?? th??? thay ?????i t??y theo vi???c th???c hi???n trong m??i tr?????ng kh??c nhau.
         */

        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    @SuppressLint("SetTextI18n")
    private suspend fun setupUI() {
        val weatherResponseJsonString =
            mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, "")
        withContext(Dispatchers.Main) {
            if(!weatherResponseJsonString.isNullOrEmpty()){
                val weatherList = Gson().fromJson(weatherResponseJsonString, WeatherResponse::class.java)
                for (i in weatherList.weather.indices) {
                    Log.i("Weather Name", weatherList.weather.toString())
                    binding?.tvMain?.text = weatherList.weather[i].main
                    binding?.tvMainDescription?.text = weatherList.weather[i].description
                    /**
                     * Configuration : L???p n??y m?? t??? t???t c??? th??ng tin c???u h??nh thi???t b??? c?? th??? ???nh h?????ng ?????n t??i nguy??n m?? ???ng d???ng truy xu???t.
                     * ??i???u n??y bao g???m c??? c??c t??y ch???n c???u h??nh do ng?????i d??ng ch??? ?????nh (danh s??ch ng??n ng??? (locales) v?? t??? l???)
                     * c??ng nh?? c???u h??nh thi???t b??? (ch???ng h???n nh?? ch??? ????? nh???p, k??ch th?????c m??n h??nh v?? h?????ng m??n h??nh)...
                     * */
                    binding?.tvTemp?.text =
                        weatherList.main.temp.toString() + getUnit(
                            application.resources.configuration.locales.get(
                                0
                            ).toString()
                        ) // Ch??ng ta l???y ra ng??n ng??? "do ng?????i d??ng thi???t l???p cho thi???t b???..."

                    binding?.tvSunriseTime?.text = unixTime(weatherList.sys.sunrise)
                    binding?.tvSunsetTime?.text = unixTime(weatherList.sys.sunset)

                    binding?.tvHumidity?.text = weatherList.main.humidity.toString() + " per cent"

                    binding?.tvMin?.text = weatherList.main.temp_min.toString() + " min"
                    binding?.tvMax?.text = weatherList.main.temp_max.toString() + " max"

                    binding?.tvSpeed?.text = weatherList.wind.speed.toString()
                    binding?.tvName?.text = weatherList.name

                    binding?.tvCountry?.text = weatherList.sys.country

                    when (weatherList.weather[i].icon) {
                        "01d" -> {
                            binding?.ivMain?.setImageResource(R.drawable.sunny)
                        }
                        "02d" -> {
                            binding?.ivMain?.setImageResource(R.drawable.cloud)
                        }
                        "03d" -> {
                            binding?.ivMain?.setImageResource(R.drawable.cloud)
                        }
                        "04d" -> {
                            binding?.ivMain?.setImageResource(R.drawable.cloud)
                        }
                        "09d" -> {
                            binding?.ivMain?.setImageResource(R.drawable.rain)
                        }
                        "10d" -> {
                            binding?.ivMain?.setImageResource(R.drawable.rain)
                        }
                        "11d" -> {
                            binding?.ivMain?.setImageResource(R.drawable.storm)
                        }
                        "13d" -> {
                            binding?.ivMain?.setImageResource(R.drawable.snowflake)
                        }
                        "50d" -> {
                            binding?.ivMain?.setImageResource(R.drawable.mist)
                        }
                        "01n" -> {
                            binding?.ivMain?.setImageResource(R.drawable.sunny)
                        }
                        "02n" -> {
                            binding?.ivMain?.setImageResource(R.drawable.cloud)
                        }
                        "03n" -> {
                            binding?.ivMain?.setImageResource(R.drawable.cloud)
                        }
                        "04n" -> {
                            binding?.ivMain?.setImageResource(R.drawable.cloud)
                        }
                        "09n" -> {
                            binding?.ivMain?.setImageResource(R.drawable.rain)
                        }
                        "10n" -> {
                            binding?.ivMain?.setImageResource(R.drawable.rain)
                        }
                        "11n" -> {
                            binding?.ivMain?.setImageResource(R.drawable.storm)
                        }
                        "13n" -> {
                            binding?.ivMain?.setImageResource(R.drawable.snowflake)
                        }
                        "50n" -> {
                            binding?.ivMain?.setImageResource(R.drawable.mist)
                        }
                    }
                }
            }
            hideCustomProgressDialog()
        }
    }

    private fun getUnit(locale: String): String {
        var value = "??C"
        if ("US" == locale || "LR" == locale || "MM" == locale) {
            value = "??F"
        }
        return value
    }

    private fun checkPermissionVsCallApi() {
        binding?.tvNotificationWeather?.visibility = View.VISIBLE
        binding?.tvNotificationWeather?.text = "D??? li???u th???i ti???t hi???n t???i: "

        if (!isLocationEnable()) {
            // N???u ch??a b???t GPS, s??? chuy???n ?????n setting... c???a thi???t b???.
            showMessage("Your location provider is turned off. Please turn it...")
            val intent: Intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            // Khi ???? b???t GPS, k???t n???i internet.
            /**
             * Ki???m tra xem quy???n location ???? ???????c b???t ch??a, n???u ch??a b???t... h???p tho???i hi???n ra.
             */
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                // Sau khi ki???m tra, n???u quy???n ???? ???????c c???p.
                requestNewLocationData()
                showMessage("Permission Granted!")
            } else {
                // N???u sau khi ki???m tra, quy???n ch??a ??????c c???p. H???p tho???i b???t l??n ????? ng?????i d??ng click cho ph??p, ho???c kh??ng cho ph??p quy???n.
                val permissions = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                requestPermissions(permissions, PERMISSION_REQUEST_CODE)
                // Sau ???? onRequestPermissionsResult s??? ???????c g???i
            }
        }
    }

    // T???o y??u c???u v??? tr??, l???y t???a ????? hi???n t???i c???a ng?????i d??ng.
    @SuppressLint("MissingPermission")
    fun requestNewLocationData() {
        val mLocationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            numUpdates = 1
        }
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()!!
        )
    }

    private fun showCustomProgressDialog() {
        if(mProgressDialog != null && !mProgressDialog?.isShowing!!){
            mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
            mProgressDialog!!.show()
        }
    }

    private fun hideCustomProgressDialog() {
        if (mProgressDialog != null && mProgressDialog?.isShowing!!) {
            mProgressDialog?.dismiss()
        }
    }

    private suspend fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this@MainActivity)) {
            /**
             * Retrofit cho ph??p gi???i quy???t t???t c??c y??u c???u t??? ph??a client v?? server m???t c??ch nhanh v?? hi???u qu??? nh???t.
             * T??m l???i, Retrofit l?? m???t REST Client d??nh Android v?? c??? Java.
             * Retrofit ???????c ph??t tri???n gi??p cho qu?? tr??nh k???t n???i client ??? server tr??? n??n d??? d??ng, nhanh ch??ng.
             * ?????i v???i Retrofit b???n c?? th??? GET, POST, PUT, DELETE*/

            /**
             * Retrofit ch??nh l?? m???t type-safe HTTP Client cho Java v?? Android.
             * N?? l??m cho vi???c truy xu???t v?? t???i l??n JSON (ho???c d??? li???u c?? c???u tr??c kh??c)
             * t????ng ?????i d??? d??ng th??ng qua m???t d???ch v??? web d???a tr??n REST.
             * Trong Retrofit, b???n ?????nh c???u h??nh b??? chuy???n ?????i n??o ???????c s??? d???ng ????? tu???n t??? h??a d??? li???u.
             * Th??ng th?????ng, ?????i v???i JSON, b???n s??? d???ng GSon,
             * nh??ng b???n c?? th??? th??m b??? chuy???n ?????i t??y ch???nh ????? x??? l?? XML ho???c c??c giao th???c kh??c.
             * Retrofit s??? d???ng th?? vi???n OkHttp cho c??c y??u c???u HTTP.
             */

            /**
             *  V?? v???y, ch??ng t??i s??? s??? d???ng Retrofit cho c??c y??u c???u m???ng.
             *  Retrofit l?? m???t th?? vi???n r???t ph??? bi???n ???????c s??? d???ng cho c??c API ho???t ?????ng v?? c??ng r???t th?????ng ???????c s??? d???ng.
             *  Ch??ng ta s??? t??m hi???u n?? b???ng c??ch t???o m???t ???ng d???ng ????n gi???n s??? d???ng API ????? l???y m???t s??? d??? li???u b???ng c??ch s??? d???ng Retrofit.
             */

            /**
             * Ch??ng t??i ??ang s??? d???ng GSON ????? chuy???n ?????i JSON sang ?????i t?????ng kotlin (Java).
             * Ch??ng t??i s??? th??m c??c ph??? thu???c n??y v??o t???p build.gradle b??n trong d??? ??n c???a ch??ng t??i.
             */

            val service: WeatherService = Retrofit.Builder().baseUrl(BASE_URI)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(WeatherService::class.java)

            // launching a new coroutine
            lifecycleScope.launch(Dispatchers.IO) {
                /**
                 * T???i ????y ch??ng ta s??? d???ng Coroutine ????? th???c hi???n c??c y??u c???u m???ng d??ng API.
                 * M???c ????ch ????? th???c hi???n tr??n background.
                 */
                val listCall: Call<WeatherResponse> = service
                    .getWeather(latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID)


                /**
                 * G???i y??u c???u kh??ng ?????ng b??? v?? th??ng b??o g???i l???i v??? ph???n h???i c???a n??
                 * ho???c n???u x???y ra l???i khi n??i chuy???n v???i m??y ch???,
                 * t???o y??u c???u ho???c x??? l?? ph???n h???i.
                 */
                listCall.enqueue(object : retrofit2.Callback<WeatherResponse> {
                    @SuppressLint("CommitPrefEdits")
                    override fun onResponse(
                        call: Call<WeatherResponse>,
                        response: Response<WeatherResponse>
                    ) {
                        if (response.isSuccessful) {
                            val weatherList: WeatherResponse? = response.body()
                            Log.i("Response Result", "$weatherList")
                            Log.e("Response Result", "$weatherList")

                            if (weatherList != null) {
                                val weatherResponseJsonString = Gson().toJson(weatherList)
                                val editor = mSharedPreferences.edit()

                                editor.putString(
                                    Constants.WEATHER_RESPONSE_DATA,
                                    weatherResponseJsonString
                                )
                                editor.putString(
                                    Constants.WEATHER_RESPONSE_OLD_DAY,
                                    LocalDate.now().toString()
                                )
                                editor.apply()
                                lifecycleScope.launch {
                                    setupUI()
                                    withContext(Dispatchers.Main){
                                        binding?.tvNotificationWeather?.visibility =View.VISIBLE
                                        binding?.tvNotificationWeather?.text = "D??? li???u th???i ti???t hi???n t???i:"
                                    }
                                }
                            }
                        } else {
                            when (response.code()) {
                                400 -> {
                                    Log.e("Error 400", "Bad Connection")
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        showMessage("Error 400")
                                        hideCustomProgressDialog()
                                    }
                                }
                                404 -> {
                                    Log.e("Error 404", "Not Found")
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        showMessage("Error 404")
                                        hideCustomProgressDialog()
                                    }
                                }
                                else -> {
                                    Log.e("Error", "Generic Error ${response.code()}")
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        showMessage("Generic Error ${response.code()}")
                                        hideCustomProgressDialog()
                                    }
                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            showMessage("Error: ${t.message.toString()}")
                            Log.e("Errorrrr", t.message.toString())
                            hideCustomProgressDialog()
                        }
                    }
                })
            }

        } else {
            withContext(Dispatchers.Main){
                showMessage("No internet connection available.")
                hideCustomProgressDialog()
            }
        }
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            showCustomProgressDialog()

            val mLastLocation: Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation.longitude
            Log.i("Current Longitude", "$longitude")

            println("Tao b???t ?????u Coroutine : ${java.time.LocalTime.now()}")
            lifecycleScope.launch(Dispatchers.IO) {
                val job = launch {
                    getLocationWeatherDetails(latitude, longitude)
                }
                println("Tao ........ ki???m tra coi... ch??a cancel Coroutine : ${java.time.LocalTime.now()}")
                job.join()
                job.cancel()
                println("Tao da xong r???i nh??: ${java.time.LocalTime.now()}")
            }
        }
    }

    /**
     * Ki???m tra xem GPS, k???t n???i internet ???? b???t ch??a?...*/
    private fun isLocationEnable(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /**
     * S??? ???????c g???i khi ng?????i d??ng b???m v??o h???p tho???i, ????ng ?? ho???c t??? ch???i c???p quy???n.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                && (grantResults.isNotEmpty() && grantResults[1] == PackageManager.PERMISSION_GRANTED)
            ) {
                showMessage("Permission Granted!")
            } else {
                showMessage("Permission Denied!")
            }
        }
    }
}