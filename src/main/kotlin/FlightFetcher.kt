package org.example

import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import kotlinx.coroutines.*
import java.net.URL


private const val BASE_URL = "http://localhost:8080/2e"
private const val FLIGHT_ENDPOINT =
    "$BASE_URL/flight"

fun main() {

    //Функция runBlocking — строитель сопрограммы, который блокирует свой поток до того момента, пока выполнение
    //сопрограммы не завершится. Функцию runBlocking используют для запуска сопрограмм, которые все должны завершиться до
    //возобновления работы.
    runBlocking {
        println("Started")
        launch {
            val flight = fetchFlight()
            println(flight)
        }
        println("Finished")
    }

}

suspend fun fetchFlight(): String {
    val client = HttpClient(CIO)
   //return client.get<String>(FLIGHT_ENDPOINT)
    return "JC1112,UJH,WUI,On Time,88"
    }


