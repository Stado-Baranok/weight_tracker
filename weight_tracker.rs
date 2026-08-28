// weight_tracker.rs
use chrono::Local;
use clap::{App, Arg};
use serde::{Deserialize, Serialize};
use serde_json;
use std::fs;
use std::io::Write;
use colored::*;

const DATA_FILE: &str = "weight.json";

#[derive(Serialize, Deserialize, Clone)]
struct Record {
    date: String,
    weight: f64,
}

struct Tracker {
    records: Vec<Record>,
}

impl Tracker {
    fn new() -> Self {
        let mut t = Tracker { records: Vec::new() };
        t.load();
        t
    }

    fn load(&mut self) {
        if let Ok(data) = fs::read_to_string(DATA_FILE) {
            if let Ok(records) = serde_json::from_str(&data) {
                self.records = records;
                self.records.sort_by(|a, b| a.date.cmp(&b.date));
                return;
            }
        }
        self.records = Vec::new();
    }

    fn save(&self) {
        let json = serde_json::to_string_pretty(&self.records).unwrap();
        fs::write(DATA_FILE, json).unwrap();
    }

    fn add_record(&mut self, weight: f64, date: Option<&str>) {
        let date_str = date.unwrap_or(&Local::now().format("%Y-%m-%d").to_string()).to_string();
        self.records.push(Record { date: date_str.clone(), weight });
        self.records.sort_by(|a, b| a.date.cmp(&b.date));
        self.save();
        println!("{}", format!("Запись добавлена: {} - {:.1} кг", date_str, weight).green());
    }

    fn list_records(&self) {
        if self.records.is_empty() {
            println!("{}", "Нет записей.".yellow());
            return;
        }
        println!("{}", "Дата       | Вес (кг)".cyan());
        for r in &self.records {
            println!("{} | {:.1}", r.date, r.weight);
        }
    }

    fn show_chart(&self) {
        if self.records.is_empty() {
            println!("{}", "Нет данных для графика.".yellow());
            return;
        }
        let weights: Vec<f64> = self.records.iter().map(|r| r.weight).collect();
        let min_w = weights.iter().fold(f64::INFINITY, |a, &b| a.min(b));
        let max_w = weights.iter().fold(f64::NEG_INFINITY, |a, &b| a.max(b));
        if (max_w - min_w).abs() < 1e-9 {
            println!("Все значения одинаковы.");
            return;
        }
        let scale = 20.0 / (max_w - min_w);
        println!("{}", "График изменения веса (кг):".cyan());
        for r in &self.records {
            let bar_len = ((r.weight - min_w) * scale) as usize + 1;
            let bar = "█".repeat(bar_len);
            println!("{} {:.1}", format!("{} {}", r.date, bar.green()), r.weight);
        }
    }

    fn show_stats(&self) {
        if self.records.is_empty() {
            println!("{}", "Нет данных.".yellow());
            return;
        }
        let weights: Vec<f64> = self.records.iter().map(|r| r.weight).collect();
        let min_w = weights.iter().fold(f64::INFINITY, |a, &b| a.min(b));
        let max_w = weights.iter().fold(f64::NEG_INFINITY, |a, &b| a.max(b));
        let sum: f64 = weights.iter().sum();
        let avg_w = sum / weights.len() as f64;
        let first = weights[0];
        let last = weights[weights.len()-1];
        let diff = last - first;
        let trend = if diff > 0.0 { "📈 растёт" } else if diff < 0.0 { "📉 падает" } else { "➡️ стабилен" };
        println!("{}", "Статистика:".cyan());
        println!("  Минимальный: {:.1} кг", min_w.to_string().green());
        println!("  Максимальный: {:.1} кг", max_w.to_string().red());
        println!("  Средний: {:.1} кг", avg_w.to_string().yellow());
        println!("  Тренд: {} ({:+.1} кг)", trend.magenta(), diff);
    }

    fn export_csv(&self, filename: &str) {
        let mut wtr = csv::Writer::from_path(filename).unwrap();
        wtr.write_record(&["date", "weight"]).unwrap();
        for r in &self.records {
            wtr.write_record(&[&r.date, &r.weight.to_string()]).unwrap();
        }
        wtr.flush().unwrap();
        println!("{}", format!("Экспортировано в {} (CSV)", filename).green());
    }
}

fn main() {
    let matches = App::new("Weight Tracker")
        .arg(Arg::with_name("add").long("add").takes_value(true).help("Добавить вес (кг)"))
        .arg(Arg::with_name("date").long("date").takes_value(true).help("Дата (YYYY-MM-DD)"))
        .arg(Arg::with_name("list").long("list").help("Показать записи"))
        .arg(Arg::with_name("chart").long("chart").help("Показать график"))
        .arg(Arg::with_name("stats").long("stats").help("Показать статистику"))
        .arg(Arg::with_name("export").long("export").takes_value(true).help("Экспорт в CSV"))
        .get_matches();

    let mut tracker = Tracker::new();

    if let Some(weight_str) = matches.value_of("add") {
        let weight: f64 = weight_str.parse().expect("Неверный вес");
        tracker.add_record(weight, matches.value_of("date"));
    } else if matches.is_present("list") {
        tracker.list_records();
    } else if matches.is_present("chart") {
        tracker.show_chart();
    } else if matches.is_present("stats") {
        tracker.show_stats();
    } else if let Some(file) = matches.value_of("export") {
        tracker.export_csv(file);
    } else {
        println!("Используйте --help для справки.");
    }
}
