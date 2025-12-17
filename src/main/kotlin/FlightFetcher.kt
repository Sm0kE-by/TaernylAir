package org.example

import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import kotlinx.coroutines.*
import java.net.URL


private const val BASE_URL = "http://localhost:8080/2e"
private const val FLIGHT_ENDPOINT = "$BASE_URL/flight"
private const val LOYALTY_ENDPOINT = "$BASE_URL/loyalty"

fun main() {

    //Функция runBlocking — строитель сопрограммы, который блокирует свой поток до того момента, пока выполнение
    //сопрограммы не завершится. Функцию runBlocking используют для запуска сопрограмм, которые все должны завершиться до
    //возобновления работы.
    runBlocking {
        println("Started")
        //launch - Строитель сопрограммы — функция, создающая новую сопрограмму
        launch {
            val flight = fetchFlight("Madrigal")
            println(flight)
        }
        println("Finished")
    }

}

suspend fun fetchFlight(passengerName: String): FlightStatus = coroutineScope {
    //Также можно удалить вызов withContext, потому что Ktor автоматически перемещает сетевой запрос в фоновый поток и
    //приостанавливается до его завершения.
    val client = HttpClient(CIO)


    // val flightResponse = async {
    //println("Started fetching flight info")
    // client.get<String>(FLIGHT_ENDPOINT).also {
    //println("Finished fetching flight info")
    //}}

    val flightResponse = async {
        println("Started fetching flight info")
        delay(5000)
        println("Finished fetching flight info")
        "VA4520,RXF,PBY,On Time,95"
    }


    // val loyaltyResponse = async {
    //println("Started fetching loyalty info")
    // client.get<String>(LOYALTY_ENDPOINT).also {
    //println("Finished fetching loyalty info")
    //}
    println("Combining flight data")
    val loyaltyResponse = async {
        println("Started fetching loyalty info")
        delay(2000)
        println("Finished fetching loyalty info")
        "Platinum,90781,9218"
    }
    delay(500)
    println("Combining flight data")
    FlightStatus.parse(
        passengerName = passengerName,
        flightResponse = flightResponse.await(),
        loyaltyResponse = loyaltyResponse.await()
    )
}



