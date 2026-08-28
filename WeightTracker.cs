// WeightTracker.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace WeightTracker
{
    class Program
    {
        static void Main(string[] args)
        {
            var opts = ParseArgs(args);
            var tracker = new Tracker();
            if (opts.Add.HasValue)
            {
                tracker.AddRecord(opts.Add.Value, opts.Date);
            }
            else if (opts.List)
            {
                tracker.ListRecords();
            }
            else if (opts.Chart)
            {
                tracker.ShowChart();
            }
            else if (opts.Stats)
            {
                tracker.ShowStats();
            }
            else if (opts.Export != null)
            {
                tracker.ExportCsv(opts.Export);
            }
            else
            {
                Console.WriteLine("Используйте --help для справки.");
            }
        }

        static Options ParseArgs(string[] args)
        {
            var opts = new Options();
            for (int i = 0; i < args.Length; i++)
            {
                switch (args[i])
                {
                    case "--add": opts.Add = double.Parse(args[++i]); break;
                    case "--date": opts.Date = args[++i]; break;
                    case "--list": opts.List = true; break;
                    case "--chart": opts.Chart = true; break;
                    case "--stats": opts.Stats = true; break;
                    case "--export": opts.Export = args[++i]; break;
                }
            }
            return opts;
        }

        class Options
        {
            public double? Add { get; set; }
            public string Date { get; set; }
            public bool List { get; set; }
            public bool Chart { get; set; }
            public bool Stats { get; set; }
            public string Export { get; set; }
        }

        class Record
        {
            public string Date { get; set; }
            public double Weight { get; set; }
        }

        class Tracker
        {
            private const string DataFile = "weight.json";
            private List<Record> records = new List<Record>();

            public Tracker() => Load();

            private void Load()
            {
                try
                {
                    if (File.Exists(DataFile))
                    {
                        string json = File.ReadAllText(DataFile);
                        records = JsonSerializer.Deserialize<List<Record>>(json) ?? new List<Record>();
                    }
                }
                catch { records = new List<Record>(); }
                records = records.OrderBy(r => r.Date).ToList();
            }

            private void Save()
            {
                string json = JsonSerializer.Serialize(records, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(DataFile, json);
            }

            public void AddRecord(double weight, string date)
            {
                if (string.IsNullOrEmpty(date)) date = DateTime.UtcNow.ToString("yyyy-MM-dd");
                records.Add(new Record { Date = date, Weight = weight });
                records = records.OrderBy(r => r.Date).ToList();
                Save();
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Запись добавлена: {date} - {weight} кг");
                Console.ResetColor();
            }

            public void ListRecords()
            {
                if (records.Count == 0)
                {
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.WriteLine("Нет записей.");
                    Console.ResetColor();
                    return;
                }
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine("Дата       | Вес (кг)");
                Console.ResetColor();
                foreach (var r in records)
                    Console.WriteLine($"{r.Date} | {r.Weight:F1}");
            }

            public void ShowChart()
            {
                if (records.Count == 0)
                {
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.WriteLine("Нет данных для графика.");
                    Console.ResetColor();
                    return;
                }
                var weights = records.Select(r => r.Weight).ToList();
                double minW = weights.Min();
                double maxW = weights.Max();
                if (Math.Abs(maxW - minW) < 1e-9)
                {
                    Console.WriteLine("Все значения одинаковы.");
                    return;
                }
                double scale = 20.0 / (maxW - minW);
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine("График изменения веса (кг):");
                Console.ResetColor();
                foreach (var r in records)
                {
                    int barLen = (int)((r.Weight - minW) * scale) + 1;
                    string bar = new string('█', barLen);
                    Console.ForegroundColor = ConsoleColor.Green;
                    Console.WriteLine($"{r.Date} {bar} {r.Weight:F1}");
                    Console.ResetColor();
                }
            }

            public void ShowStats()
            {
                if (records.Count == 0)
                {
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.WriteLine("Нет данных.");
                    Console.ResetColor();
                    return;
                }
                var weights = records.Select(r => r.Weight).ToList();
                double minW = weights.Min();
                double maxW = weights.Max();
                double avgW = weights.Average();
                double first = weights.First();
                double last = weights.Last();
                double diff = last - first;
                string trend = diff > 0 ? "📈 растёт" : diff < 0 ? "📉 падает" : "➡️ стабилен";
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine("Статистика:");
                Console.ResetColor();
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"  Минимальный: {minW:F1} кг");
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"  Максимальный: {maxW:F1} кг");
                Console.ForegroundColor = ConsoleColor.Yellow;
                Console.WriteLine($"  Средний: {avgW:F1} кг");
                Console.ForegroundColor = ConsoleColor.Magenta;
                Console.WriteLine($"  Тренд: {trend} ({diff:+F1} кг)");
                Console.ResetColor();
            }

            public void ExportCsv(string filename)
            {
                using var sw = new StreamWriter(filename);
                sw.WriteLine("date,weight");
                foreach (var r in records)
                    sw.WriteLine($"{r.Date},{r.Weight:F1}");
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Экспортировано в {filename} (CSV)");
                Console.ResetColor();
            }
        }
    }
}
