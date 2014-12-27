package de.kasoki.swtrealtime

import scalaj.http.Http
import scalaj.http.HttpOptions
import org.json4s._
import org.json4s.native.JsonMethods._
import scala.collection.mutable._
import scala.math.pow

import java.util.Date
import java.text.SimpleDateFormat;

class ServerResponseException(val code:Int) extends Exception("[RESPONSE CODE: " + code + "] Server returned an error.")

object BusTime {
    var timeout:Int = 500

    def fromBusStop(busStop:BusStop.BusStopType):List[BusTime] = BusTime.fromStopCode(busStop.code)

    def fromStopCode(stopCode:String):List[BusTime] = {
        val url = "http://212.18.193.124/onlineinfo/onlineinfo/stopData"
        val charset = "UTF-8"

        val body = "5|0|6|http://212.18.193.124/onlineinfo/onlineinfo/|7E201FB9D23B0EA0BDBDC82C554E92FE|com.initka.onlineinfo.client.services.StopDataService|getDepartureInformationForStop|java.lang.String/2004016611|" + stopCode + "|1|2|3|4|1|5|6|"

        val response = Http.postData(url, body)
            .option(HttpOptions.connTimeout(timeout))
            .option(HttpOptions.readTimeout(timeout))
            .header("Accept-Charset", charset)
            .header("X-GWT-Module-Base", "http://212.18.193.124/onlineinfo/onlineinfo/")
            .header("X-GWT-Permutation", "D8AB656D349DD625FC1E4BA18B0A253C")
            .header("Content-Type", "text/x-gwt-rpc; charset=" + charset)

        val responseCode = response.responseCode
        val responseText = response.asString

        return parseResponse(responseCode, responseText)
    }

    private def parseResponse(code:Int, text:String):List[BusTime] = {
        var busTimes = Buffer[BusTime]()

        if(code != 200) {
            throw new ServerResponseException(code)
        }

        if(text.startsWith("//OK")) {
            val jsonString = text.substring(4, text.length).replace('\'', '"')

            val list = parse(jsonString).values.asInstanceOf[List[Any]]

            val innerInput = list(list.length - 3).asInstanceOf[List[Any]]

            def getFromInnerInput(index:Int):Any = innerInput(list(index).asInstanceOf[BigInt].intValue - 1)
            def getTimestamp(index:Int):String = list(index).asInstanceOf[String]

            for(i <- 0 until Math.floor(list.length / 9).asInstanceOf[Int]) {
                try {
                    val destination = getFromInnerInput(i * 9 + 5).asInstanceOf[String]
                    val number = getFromInnerInput(i * 9 + 4).asInstanceOf[String].toInt

                    val arrival = new Date(decodeTime(getTimestamp(i * 9 + 2)) * 1000)
                    val expectedArrival = new Date(decodeTime(getTimestamp(i * 9 + 6)) * 1000)

                    busTimes += new BusTime(number, destination, arrival, expectedArrival)
                } catch {
                    // if this happens we've found a bus without destination or number... don't know what they
                    // are but they actually exist! Oo
                    case ex: Exception => {}
                }
            }
        } else {
            throw new RuntimeException("Server response isn't okay: \"" + text + "\"")
        }

        return busTimes.toList
    }

    def decodeTime(time:String):Long = {
        val base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_$"

        var sum = 0L

        for(i <- 0 until time.length) {
            sum += base.indexOf(time.charAt(i)) * pow(base.length, time.length - i - 1).asInstanceOf[Long]
        }

        return sum / 1000
    }

    def main(args:Array[String]) {
        val stops = BusTime.fromBusStop(BusStop.HAUPTBAHNHOF)

        println(stops.length)
        stops.foreach { println }
    }
}

class BusTime private(val number:Int, val destination:String, val arrival:Date, val expectedArrival:Date) extends Ordered[BusTime] {
    var format:SimpleDateFormat = new SimpleDateFormat("HH:mm")

    def compare(other:BusTime) = arrival.compareTo(other.arrival)

    def delay:Int = {
        val difference = expectedArrival.getTime() - arrival.getTime()

        val differenceMinutes = difference / (60 * 1000)

        return differenceMinutes.asInstanceOf[Int]
    }

    override def toString:String = asString

    def asString:String = {
        return "[" + number + "] " + destination + ", arrival: " + arrivalTimeAsString + " + " + delay + "m"
    }

    def arrivalTimeAsString:String = {
        return format.format(arrival)
    }

    def expectedArrivalTimeAsString:String = {
        return format.format(expectedArrival)
    }
}
