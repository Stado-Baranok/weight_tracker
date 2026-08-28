// WeightTracker.java
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

public class WeightTracker {
    private static final String DATA_FILE = "weight.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Record>>(){}.getType();

    @Parameter(names = "--add")
    private Double addWeight;
    @Parameter(names = "--date")
    private String date;
    @Parameter(names = "--list")
    private boolean list;
    @Parameter(names = "--chart")
    private boolean chart;
    @Parameter(names = "--stats")
    private boolean stats;
    @Parameter(names = "--export")
    private String exportFile;

    static class Record {
        String date;
        double weight;
        Record(String date, double weight) { this.date = date; this.weight = weight; }
    }

    private List<Record> records = new ArrayList<>();

    private void load() {
        try {
            String json = new String(Files.readAllBytes(Paths.get(DATA_FILE)));
            records = GSON.fromJson(json, LIST_TYPE);
        } catch (Exception e) {
            records = new ArrayList<>();
        }
        records.sort(Comparator.comparing(r -> r.date));
    }

    private void save() {
        try {
            Files.write(Paths.get(DATA_FILE), GSON.toJson(records).getBytes());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения: " + e.getMessage());
        }
    }

    private void addRecord(double weight, String dateStr) {
        if (dateStr == null) dateStr = LocalDate.now().toString();
        records.add(new Record(dateStr, weight));
        records.sort(Comparator.comparing(r -> r.date));
        save();
        System.out.println("\u001B[32mЗапись добавлена: " + dateStr + " - " + weight + " кг\u001B[0m");
    }

    private void listRecords() {
        if (records.isEmpty()) {
            System.out.println("\u001B[33mНет записей.\u001B[0m");
            return;
        }
        System.out.println("\u001B[36mДата       | Вес (кг)\u001B[0m");
        for (Record r : records) {
            System.out.printf("%s | %.1f%n", r.date, r.weight);
        }
    }

    private void showChart() {
        if (records.isEmpty()) {
            System.out.println("\u001B[33mНет данных для графика.\u001B[0m");
            return;
        }
        double minW = records.stream().mapToDouble(r -> r.weight).min().orElse(0);
        double maxW = records.stream().mapToDouble(r -> r.weight).max().orElse(0);
        if (Math.abs(maxW - minW) < 1e-9) {
            System.out.println("Все значения одинаковы.");
            return;
        }
        double scale = 20.0 / (maxW - minW);
        System.out.println("\u001B[36mГрафик изменения веса (кг):\u001B[0m");
        for (Record r : records) {
            int barLen = (int)((r.weight - minW) * scale) + 1;
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < barLen; i++) bar.append("█");
            System.out.printf("%s \u001B[32m%s\u001B[0m %.1f%n", r.date, bar, r.weight);
        }
    }

    private void showStats() {
        if (records.isEmpty()) {
            System.out.println("\u001B[33mНет данных.\u001B[0m");
            return;
        }
        double minW = records.stream().mapToDouble(r -> r.weight).min().orElse(0);
        double maxW = records.stream().mapToDouble(r -> r.weight).max().orElse(0);
        double avgW = records.stream().mapToDouble(r -> r.weight).average().orElse(0);
        double first = records.get(0).weight;
        double last = records.get(records.size()-1).weight;
        double diff = last - first;
        String trend = diff > 0 ? "📈 растёт" : diff < 0 ? "📉 падает" : "➡️ стабилен";
        System.out.println("\u001B[36mСтатистика:\u001B[0m");
        System.out.printf("  Минимальный: \u001B[32m%.1f\u001B[0m кг%n", minW);
        System.out.printf("  Максимальный: \u001B[31m%.1f\u001B[0m кг%n", maxW);
        System.out.printf("  Средний: \u001B[33m%.1f\u001B[0m кг%n", avgW);
        System.out.printf("  Тренд: \u001B[35m%s\u001B[0m (%+.1f кг)%n", trend, diff);
    }

    private void exportCsv(String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("date,weight");
            for (Record r : records) {
                pw.printf("%s,%.1f%n", r.date, r.weight);
            }
        }
        System.out.println("\u001B[32mЭкспортировано в " + filename + " (CSV)\u001B[0m");
    }

    public void run() throws Exception {
        load();
        if (addWeight != null) {
            addRecord(addWeight, date);
        } else if (list) {
            listRecords();
        } else if (chart) {
            showChart();
        } else if (stats) {
            showStats();
        } else if (exportFile != null) {
            exportCsv(exportFile);
        } else {
            System.out.println("Используйте --help для справки.");
        }
    }

    public static void main(String[] args) throws Exception {
        WeightTracker tracker = new WeightTracker();
        JCommander.newBuilder().addObject(tracker).build().parse(args);
        tracker.run();
    }
}
