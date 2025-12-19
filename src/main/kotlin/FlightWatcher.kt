package org.example

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.flow.*
import org.example.BoardingState.*

val bannedPassengers = setOf("Nogartse")

fun main() {
    runBlocking {
        println("Getting the latest flight info...")

        val flights = fetchFlights()
        val flightDescriptions = flights.joinToString { "${it.passengerName} (${it.flightNumber})" }

        println("Found flights for  $flightDescriptions")

        val flightsAtGate = MutableStateFlow(flights.size)
        //flightsAtGate.value = flightsAtGate.value - 1
        launch {
            flightsAtGate
                .takeWhile { it > 0 }
                .onCompletion {
                    println("Finished tracking all flights")
                }
                .collect { flightCount ->
                    println("There are $flightCount flights being tracked")
                }
        }
        //println("Finished tracking all flights")
        launch {
            flights.forEach {
                watchFlight(it)
                flightsAtGate.value = flightsAtGate.value - 1
            }
        }
    }
}

suspend fun watchFlight(initialFlight: FlightStatus) {
    val passengerName = initialFlight.passengerName

    //flow в отложенном режиме генерирует в некотором порядке элементы, которые потребляются другим компонентом
    // Функция emit является приостанавливаемой, как и функция delay, которую мы использовали выше. Но мы не добавили
    //модификатор suspend в сигнатуру функции watchFlight, что дает нам подсказку о том, что здесь происходит: во внутренней
    //реализации flow создает область видимости сопрограммы, в которой выполняется содержимое лямбда-выражения. Это и
    //позволяет нам вызывать приостанавливаемые функции внутри лямбда-выражения. Область видимости сопрограммы,
    //используемая потоком данных, создается в начале потребления потока данных и закрывается, когда поток данных перестает
    //генерировать элементы или эти элементы перестают потребляться.
    val currentFlight: Flow<FlightStatus> = flow {
        require(passengerName !in bannedPassengers)
        {
            "Cannot track $passengerName's flight. They are banned from the airport."
        }
        var flight = initialFlight

        while (flight.departureTimeInMinutes >= 0 && !flight.isFlightCanceled) {
            //emit показывает, как задаются элементы, которые должны отправляться потребителю потока данных.
            emit(flight)
            delay(500)
            flight = flight.copy(departureTimeInMinutes = flight.departureTimeInMinutes - 1)
        }
    }
    //функция collect является приостанавливаемой. Чтобы иметь возможность вызвать collect, необходимо добавить в watchFlight модификатор
    //suspend. Сама функция collect приостанавливается до того момента, когда flow перестанет генерировать элементы.
    currentFlight
        .map { flight ->
            when (flight.boardingStatus) {
                FlightCanceled -> "Your flight was canceled"
                BoardingNotStarted -> "Boarding will start soon"
                WaitingToBoard -> "Other passengers are boarding"
                Boarding -> "You can now board the plane"
                BoardingEnded -> "The boarding doors have closed"
            } + " (Flight departs in ${flight.departureTimeInMinutes} minutes)"
        }
        .onCompletion {
            println("Finished tracking $passengerName's flight")
        }
        .collect { status ->
            println("$passengerName: $status")
        }
}

suspend fun fetchFlights(
    passengerNames: List<String> = listOf("Madrigal", "Polarcubis", "Estragon", "Taernyl"),
    numberOfWorkers: Int = 2
): List<FlightStatus> =
    coroutineScope {
        val passengerNamesChannel = Channel<String>()
        val fetchedFlightsChannel = Channel<FlightStatus>()
        launch {
            passengerNames.forEach {
                passengerNamesChannel.send(it)
            }
            passengerNamesChannel.close()
        }
        launch {
            (1..numberOfWorkers).map {
                launch {
                    fetchFlightStatuses(passengerNamesChannel, fetchedFlightsChannel)
                }
            }.joinAll()
            fetchedFlightsChannel.close()
        }
        fetchedFlightsChannel.toList()
    }

suspend fun fetchFlightStatuses(fetchChannel: ReceiveChannel<String>, resultChannel: SendChannel<FlightStatus>) {
    for (passengerName in fetchChannel) {
        val flight = fetchFlight(passengerName)
        println("Fetched flight: $flight")
        resultChannel.send(flight)
    }
}