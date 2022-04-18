package com.example.weatherapp.models

import java.io.Serializable


/**
 * Phản hồi Json, là 1 chuỗi Json.
 * Sau đó, chúng tôi sẽ tạo các lớp dữ liệu theo phản hồi JSON, theo định dạng của phản hồi Json.
 * Trong phản hồi JSON, chúng ta có 7 đối tượng JSON, Vì vậy, chúng ta sẽ tạo 7 model dữ liệu */
data class WeatherResponse(
    val coord: Coord,
    val weather: List<Weather>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Int,
    val sys: Sys,
    val timezone: Double,
    val id: Int,
    val name: String,
    val cod: Int
) : Serializable

/**
 * Serialization trong Java là cơ chế chuyển đổi trạng thái của một đối tượng (giá trị các thuộc tính trong object) thành một chuỗi byte sao cho chuỗi byte này có thể chuyển đổi ngược lại thành một đối tượng.
 * Quá trình chuyển đổi chuỗi byte thành đối tượng gọi là deserialization.
 * Một object có thể serializable (có thể thực hiện Serialization) nếu class của nó thực hiện implements interface java.io.Serializable
 */

/**
 * Trong Java, khi trao đổi dữ liệu giữa các thành phần khác nhau (giữa các module cùng viết bằng Java)thì dữ liệu được thể hiện dưới dạng byte chứ không phải là đối tượng.
 * Do đó ta cần có một cơ chế để hiểu các đối tượng được gửi và nhận.
 * Quá trình serilization hoàn toàn độc lập với platform (không phụ thuộc vào hệ điều hành) nên việc chuyển đổi giữa byte và object giữa các module được đảm bảo.
 */
