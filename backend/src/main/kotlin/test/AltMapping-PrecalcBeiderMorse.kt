package test

import org.apache.commons.codec.language.bm.BeiderMorseEncoder
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    val encoder = BeiderMorseEncoder()
    var i = 0
    val out =
        PrintStream("C:\\Users\\mableidinger\\own\\xkcd-map-reactions\\dbMigration\\src\\main\\resources\\US-bm.txt")
    Files.lines(Paths.get("C:\\Users\\mableidinger\\own\\xkcd-map-reactions\\dbMigration\\src\\main\\resources\\US.txt"))
        .parallel()
        .map {
            val city = it.split("\t")[1]
            city to prepare(city)
        }
        .map { it.first to encoder.encode(it.second).split("|") }
        .sequential()
//            .limit(200)
        .forEach {
            i++
            if (i % 10000 == 0) {
                println("${String.format("%3.0f", i.toFloat() / 2_200_000.toFloat() * 100)}%: ${i}/${2_200_000}")
            }
            out.print(it.first)
            it.second.forEach { word ->
                out.print("|")
                out.print(word)
            }
            out.println()
        }
    out.flush()
    out.close()
}
