package com.example.weatherapp.network

import com.example.weatherapp.models.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


/** Đây là lệnh gọi API
 * https://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&appid={API key}
 *
 * Phần cơ bản, cố định: https://api.openweathermap.org/data/
 *
 * Phần thay đổi: 2.5/weather?lat={lat}&lon={lon}&appid={API key}
 *
 * Vì vậy:
 * Chúng tôi sẽ tạo Retrofit interface để thêm các điểm cuối của URL
 * (dấu ngoặc kép trong trường hợp của chúng tôi là điểm cuối)
 * */

interface WeatherService {
    @GET("2.5/weather")
    fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String,
        @Query("appid") appid: String,
    ): Call<WeatherResponse>
}