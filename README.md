# Student CRUD (Java Swing · CSV)

A simple desktop app to **Create / Read / Update / Delete** students, built with **Java Swing** and persisted to a local **CSV file** (`students.csv`).

## ✨ Features
- Add / edit / delete students
- Validation (email, GPA 0–10)
- Sortable table (click column headers)
- Auto-save on exit to `students.csv`
- Comma-safe CSV (quoted fields)

## 🧱 Tech
- **Java 17**
- **Swing**
- **Maven** (build & packaging)

## 📁 Project Structure
``
student-crud-swing/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── studentcrud/
│       │               ├── model/
│       │               │   └── Student.java              # POJO with id, name, email, GPA
│       │               ├── dao/
│       │               │   └── StudentCSVDao.java        # Handles reading/writing CSV
│       │               ├── ui/
│       │               │   ├── StudentForm.java          # Add/edit form UI
│       │               │   └── StudentTable.java         # JTable with sorting & actions
│       │               ├── util/
│       │               │   └── CSVUtils.java             # Comma-safe CSV helpers
│       │               └── Main.java                     # Entry point with JFrame setup
│       └── resources/
│           └── students.csv                              # Auto-saved student data
├── .gitignore
