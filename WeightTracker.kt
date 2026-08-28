// WeightTracker.kt
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDate

data class Record(val date: String, val weight: Double)

class WeightTracker {
    @Parameter(names = ["--add"])
    private var addWeight: Double? = null

    @Parameter(names = ["--date"])
    private var date: String? = null

    @Parameter(names = ["--list"])
    private var list: Boolean = false

    @Parameter(names = ["--chart"])
    private var chart: Boolean = false

    @Parameter(names = ["--stats"])
    private var stats: Boolean = false

    @Parameter(names = ["--export"])
    private var exportFile: String? = null

    private val dataFile = "weight.json"
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val type = object : TypeToken<MutableList<Record>>() {}.type
    private val records = mutableListOf<Record>()

    private fun load() {
        val f = File(dataFile)
        if (!f.exists()) return
        try {
            val json = f.readText()
            val list = gson.fromJson<MutableList<Record>>(json, type)
            records.addAll(list)
            records.sortBy { it.date }
        } catch (e: Exception) { /* ignore */ }
    }

    private fun save() {
        val json = gson.toJson(records)
        File(dataFile).writeText(json)
    }

    private fun addRecord(weight: Double, date: String?) {
        val d = date ?: LocalDate.now().toString()
        records.add(Record(d, weight))
        records.sortBy { it.date }
        save()
        println("\u001B[32mЗапись добавлена: $d - $weight кг\u001B[0m")
    }

    private fun listRecords() {
        if (records.isEmpty()) {
            println("\u001B[33mНет записей.\u001B[0m")
            return
        }
        println("\u001B[36mДата       | Вес (кг)\u001B[0m")
        for (r in records) {
            println("${r.date} | ${"%.1f".format(r.weight)}")
        }
    }

    private fun showChart() {
        if (records.isEmpty()) {
            println("\u001B[33mНет данных для графика.\u001B[0m")
            return
        }
        val weights = records.map { it.weight }
        val minW = weights.minOrNull() ?: 0.0
        val maxW = weights.maxOrNull() ?: 0.0
        if (maxW == minW) {
            println("Все значения одинаковы.")
            return
        }
        val scale = 20.0 / (maxW - minW)
        println("\u001B[36mГрафик изменения веса (кг):\u001B[0m")
        for (r in records) {
            val barLen = ((r.weight - minW) * scale).toInt() + 1
            val bar = "█".repeat(barLen)
            println("${r.date} \u001B[32m$bar\u001B[0m ${"%.1f".format(r.weight)}")
        }
    }

    private fun showStats() {
        if (records.isEmpty()) {
            println("\u001B[33mНет данных.\u001B[0m")
            return
        }
        val weights = records.map { it.weight }
        val minW = weights.minOrNull() ?: 0.0
        val maxW = weights.maxOrNull() ?: 0.0
        val avgW = weights.average()
        val first = weights.first()
        val last = weights.last()
        val diff = last - first
        val trend = when {
            diff > 0 -> "📈 растёт"
            diff < 0 -> "📉 падает"
            else -> "➡️ стабилен"
        }
        println("\u001B[36mСтатистика:\u001B[0m")
        println("  Минимальный: \u001B[32m${"%.1f".format(minW)}\u001B[0m кг")
        println("  Максимальный: \u001B[31m${"%.1f".format(maxW)}\u001B[0m кг")
        println("  Средний: \u001B[33m${"%.1f".format(avgW)}\u001B[0m кг")
        println("  Тренд: \u001B[35m$trend\u001B[0m (${if (diff >= 0) "+" else ""}${"%.1f".format(diff)} кг)")
    }

    private fun exportCsv(filename: String) {
        File(filename).printWriter().use { pw ->
            pw.println("date,weight")
            for (r in records) {
                pw.println("${r.date},${"%.1f".format(r.weight)}")
            }
        }
        println("\u001B[32mЭкспортировано в $filename (CSV)\u001B[0m")
    }

    fun run() {
        load()
        when {
            addWeight != null -> addRecord(addWeight!!, date)
            list -> listRecords()
            chart -> showChart()
            stats -> showStats()
            exportFile != null -> exportCsv(exportFile!!)
            else -> println("Используйте --help для справки.")
        }
    }
}

fun main(args: Array<String>) {
    val tracker = WeightTracker()
    JCommander.newBuilder().addObject(tracker).build().parse(*args)
    tracker.run()
}
