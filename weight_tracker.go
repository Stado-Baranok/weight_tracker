// weight_tracker.go
package main

import (
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"sort"
	"strconv"
	"time"
)

const dataFile = "weight.json"

type Record struct {
	Date   string  `json:"date"`
	Weight float64 `json:"weight"`
}

type Tracker struct {
	Records []Record `json:"records"`
}

func (t *Tracker) load() {
	data, err := os.ReadFile(dataFile)
	if err != nil {
		t.Records = []Record{}
		return
	}
	if err := json.Unmarshal(data, t); err != nil {
		t.Records = []Record{}
	}
	sort.Slice(t.Records, func(i, j int) bool { return t.Records[i].Date < t.Records[j].Date })
}

func (t *Tracker) save() {
	data, _ := json.MarshalIndent(t, "", "  ")
	os.WriteFile(dataFile, data, 0644)
}

func (t *Tracker) addRecord(weight float64, date string) {
	if date == "" {
		date = time.Now().Format("2006-01-02")
	}
	t.Records = append(t.Records, Record{Date: date, Weight: weight})
	sort.Slice(t.Records, func(i, j int) bool { return t.Records[i].Date < t.Records[j].Date })
	t.save()
	fmt.Printf("\033[32mЗапись добавлена: %s - %.1f кг\033[0m\n", date, weight)
}

func (t *Tracker) listRecords() {
	if len(t.Records) == 0 {
		fmt.Println("\033[33mНет записей.\033[0m")
		return
	}
	fmt.Println("\033[36mДата       | Вес (кг)\033[0m")
	for _, r := range t.Records {
		fmt.Printf("%s | %.1f\n", r.Date, r.Weight)
	}
}

func (t *Tracker) showChart() {
	if len(t.Records) == 0 {
		fmt.Println("\033[33mНет данных для графика.\033[0m")
		return
	}
	weights := make([]float64, len(t.Records))
	for i, r := range t.Records {
		weights[i] = r.Weight
	}
	minW, maxW := weights[0], weights[0]
	for _, w := range weights {
		if w < minW {
			minW = w
		}
		if w > maxW {
			maxW = w
		}
	}
	if maxW == minW {
		fmt.Println("Все значения одинаковы.")
		return
	}
	scale := 20.0 / (maxW - minW)
	fmt.Println("\033[36mГрафик изменения веса (кг):\033[0m")
	for _, r := range t.Records {
		barLen := int((r.Weight-minW)*scale) + 1
		bar := ""
		for i := 0; i < barLen; i++ {
			bar += "█"
		}
		fmt.Printf("%s \033[32m%s\033[0m %.1f\n", r.Date, bar, r.Weight)
	}
}

func (t *Tracker) showStats() {
	if len(t.Records) == 0 {
		fmt.Println("\033[33mНет данных.\033[0m")
		return
	}
	weights := make([]float64, len(t.Records))
	sum := 0.0
	for i, r := range t.Records {
		weights[i] = r.Weight
		sum += r.Weight
	}
	minW, maxW := weights[0], weights[0]
	for _, w := range weights {
		if w < minW {
			minW = w
		}
		if w > maxW {
			maxW = w
		}
	}
	avgW := sum / float64(len(weights))
	first := weights[0]
	last := weights[len(weights)-1]
	diff := last - first
	trend := "📈 растёт"
	if diff < 0 {
		trend = "📉 падает"
	} else if diff == 0 {
		trend = "➡️ стабилен"
	}
	fmt.Println("\033[36mСтатистика:\033[0m")
	fmt.Printf("  Минимальный: \033[32m%.1f\033[0m кг\n", minW)
	fmt.Printf("  Максимальный: \033[31m%.1f\033[0m кг\n", maxW)
	fmt.Printf("  Средний: \033[33m%.1f\033[0m кг\n", avgW)
	fmt.Printf("  Тренд: \033[35m%s\033[0m (%+.1f кг)\n", trend, diff)
}

func (t *Tracker) exportCSV(filename string) {
	f, err := os.Create(filename)
	if err != nil {
		fmt.Printf("Ошибка создания файла: %v\n", err)
		return
	}
	defer f.Close()
	w := csv.NewWriter(f)
	defer w.Flush()
	w.Write([]string{"date", "weight"})
	for _, r := range t.Records {
		w.Write([]string{r.Date, strconv.FormatFloat(r.Weight, 'f', 1, 64)})
	}
	fmt.Printf("\033[32mЭкспортировано в %s (CSV)\033[0m\n", filename)
}

func main() {
	var (
		add    float64
		date   string
		list   bool
		chart  bool
		stats  bool
		export string
	)
	flag.Float64Var(&add, "add", 0, "Добавить вес (кг)")
	flag.StringVar(&date, "date", "", "Дата (YYYY-MM-DD)")
	flag.BoolVar(&list, "list", false, "Показать записи")
	flag.BoolVar(&chart, "chart", false, "Показать график")
	flag.BoolVar(&stats, "stats", false, "Показать статистику")
	flag.StringVar(&export, "export", "", "Экспорт в CSV")
	flag.Parse()

	tracker := &Tracker{}
	tracker.load()

	if add != 0 {
		tracker.addRecord(add, date)
	} else if list {
		tracker.listRecords()
	} else if chart {
		tracker.showChart()
	} else if stats {
		tracker.showStats()
	} else if export != "" {
		tracker.exportCSV(export)
	} else {
		fmt.Println("Используйте --help для справки.")
	}
}
