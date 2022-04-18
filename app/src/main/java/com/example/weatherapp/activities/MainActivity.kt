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
                        binding?.tvNotificationWeather?.text = "Dữ liệu thời tiết ngày: $weatherResponseOldDay"
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
                    println("Bạn đã Refresh lại Data...")
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
         * Phương thức getDefault () của lớp TimeZone trong Java được sử dụng để biết TimeZone mặc định cho hệ thống hoặc máy chủ này.
         * Điều này có thể thay đổi tùy theo việc thực hiện trong môi trường khác nhau.
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
                     * Configuration : Lớp này mô tả tất cả thông tin cấu hình thiết bị có thể ảnh hưởng đến tài nguyên mà ứng dụng truy xuất.
                     * Điều này bao gồm cả các tùy chọn cấu hình do người dùng chỉ định (danh sách ngôn ngữ (locales) và tỷ lệ)
                     * cũng như cấu hình thiết bị (chẳng hạn như chế độ nhập, kích thước màn hình và hướng màn hình)...
                     * */
                    binding?.tvTemp?.text =
                        weatherList.main.temp.toString() + getUnit(
                            application.resources.configuration.locales.get(
                                0
                            ).toString()
                        ) // Chúng ta lấy ra ngôn ngữ "do người dùng thiết lập cho thiết bị..."

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
        var value = "°C"
        if ("US" == locale || "LR" == locale || "MM" == locale) {
            value = "°F"
        }
        return value
    }

    private fun checkPermissionVsCallApi() {
        binding?.tvNotificationWeather?.visibility = View.VISIBLE
        binding?.tvNotificationWeather?.text = "Dữ liệu thời tiết hiện tại: "

        if (!isLocationEnable()) {
            // Nếu chưa bật GPS, sẽ chuyển đến setting... của thiết bị.
            showMessage("Your location provider is turned off. Please turn it...")
            val intent: Intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            // Khi đã bật GPS, kết nối internet.
            /**
             * Kiểm tra xem quyền location đã được bật chưa, nếu chưa bật... hộp thoại hiện ra.
             */
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                // Sau khi kiểm tra, nếu quyền đã được cấp.
                requestNewLocationData()
                showMessage("Permission Granted!")
            } else {
                // Nếu sau khi kiểm tra, quyền chưa đươc cấp. Hộp thoại bật lên để người dùng click cho phép, hoặc không cho phép quyền.
                val permissions = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                requestPermissions(permissions, PERMISSION_REQUEST_CODE)
                // Sau đó onRequestPermissionsResult sẽ được gọi
            }
        }
    }

    // Tạo yêu cầu vị trí, lấy tọa độ hiện tại của người dùng.
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
             * Retrofit cho phép giải quyết tốt các yêu cầu từ phía client và server một cách nhanh và hiệu quả nhất.
             * Tóm lại, Retrofit là một REST Client dành Android và cả Java.
             * Retrofit được phát triển giúp cho quá trình kết nối client – server trở nên dễ dàng, nhanh chóng.
             * Đối với Retrofit bạn có thể GET, POST, PUT, DELETE*/

            /**
             * Retrofit chính là một type-safe HTTP Client cho Java và Android.
             * Nó làm cho việc truy xuất và tải lên JSON (hoặc dữ liệu có cấu trúc khác)
             * tương đối dễ dàng thông qua một dịch vụ web dựa trên REST.
             * Trong Retrofit, bạn định cấu hình bộ chuyển đổi nào được sử dụng để tuần tự hóa dữ liệu.
             * Thông thường, đối với JSON, bạn sử dụng GSon,
             * nhưng bạn có thể thêm bộ chuyển đổi tùy chỉnh để xử lý XML hoặc các giao thức khác.
             * Retrofit sử dụng thư viện OkHttp cho các yêu cầu HTTP.
             */

            /**
             *  Vì vậy, chúng tôi sẽ sử dụng Retrofit cho các yêu cầu mạng.
             *  Retrofit là một thư viện rất phổ biến được sử dụng cho các API hoạt động và cũng rất thường được sử dụng.
             *  Chúng ta sẽ tìm hiểu nó bằng cách tạo một ứng dụng đơn giản sử dụng API để lấy một số dữ liệu bằng cách sử dụng Retrofit.
             */

            /**
             * Chúng tôi đang sử dụng GSON để chuyển đổi JSON sang đối tượng kotlin (Java).
             * Chúng tôi sẽ thêm các phụ thuộc này vào tệp build.gradle bên trong dự án của chúng tôi.
             */

            val service: WeatherService = Retrofit.Builder().baseUrl(BASE_URI)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(WeatherService::class.java)

            // launching a new coroutine
            lifecycleScope.launch(Dispatchers.IO) {
                /**
                 * Tại đây chúng ta sử dụng Coroutine để thực hiện các yêu cầu mạng dùng API.
                 * Mục đích để thực hiện trên background.
                 */
                val listCall: Call<WeatherResponse> = service
                    .getWeather(latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID)


                /**
                 * Gửi yêu cầu không đồng bộ và thông báo gọi lại về phản hồi của nó
                 * hoặc nếu xảy ra lỗi khi nói chuyện với máy chủ,
                 * tạo yêu cầu hoặc xử lý phản hồi.
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
                                        binding?.tvNotificationWeather?.text = "Dữ liệu thời tiết hiện tại:"
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

            println("Tao bắt đầu Coroutine : ${java.time.LocalTime.now()}")
            lifecycleScope.launch(Dispatchers.IO) {
                val job = launch {
                    getLocationWeatherDetails(latitude, longitude)
                }
                println("Tao ........ kiểm tra coi... chưa cancel Coroutine : ${java.time.LocalTime.now()}")
                job.join()
                job.cancel()
                println("Tao da xong rồi nhé: ${java.time.LocalTime.now()}")
            }
        }
    }

    /**
     * Kiểm tra xem GPS, kết nối internet đã bật chưa?...*/
    private fun isLocationEnable(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /**
     * Sẽ được gọi khi người dùng bấm vào hộp thoại, đòng ý hoặc từ chối cấp quyền.
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