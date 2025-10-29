package com.example.students;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class StudentCrudApp extends JFrame {

    // ---- Model ----
    private static class Student {
        Integer id;
        String name;
        String email;
        double gpa;

        Student(Integer id, String name, String email, double gpa) {
            this.id = id; this.name = name; this.email = email; this.gpa = gpa;
        }
    }

    // ---- UI components ----
    private final JTextField nameField = new JTextField(18);
    private final JTextField emailField = new JTextField(18);
    private final JTextField gpaField = new JTextField(6);

    private final JButton addBtn = new JButton("Add");
    private final JButton updateBtn = new JButton("Update");
    private final JButton deleteBtn = new JButton("Delete");
    private final JButton clearBtn = new JButton("Clear");

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Name", "Email", "GPA"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> Integer.class;
                case 3 -> Double.class;
                default -> String.class;
            };
        }
    };
    private final JTable table = new JTable(model);

    // ---- In-memory store ----
    private final List<Student> students = new ArrayList<>();
    private int nextId = 1;

    // ---- Persistence ----
    private final Path file = Paths.get("students.csv"); // created in working dir

    public StudentCrudApp() {
        super("Student CRUD (Swing · CSV)");

        // Basic frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 460);
        setLocationRelativeTo(null);

        // Top form
        var form = new JPanel(new GridBagLayout());
        var gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.anchor = GridBagConstraints.WEST;

        int r = 0;
        gc.gridx = 0; gc.gridy = r; form.add(new JLabel("Name:"), gc);
        gc.gridx = 1; gc.gridy = r++; form.add(nameField, gc);

        gc.gridx = 0; gc.gridy = r; form.add(new JLabel("Email:"), gc);
        gc.gridx = 1; gc.gridy = r++; form.add(emailField, gc);

        gc.gridx = 0; gc.gridy = r; form.add(new JLabel("GPA (0–10):"), gc);
        gc.gridx = 1; gc.gridy = r++; form.add(gpaField, gc);

        var btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.add(addBtn);
        btns.add(updateBtn);
        btns.add(deleteBtn);
        btns.add(clearBtn);
        gc.gridx = 0; gc.gridy = r; gc.gridwidth = 2;
        form.add(btns, gc);

        // Table
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true); // sort by clicking header
        var scroll = new JScrollPane(table);

        // Layout
        var root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        root.add(form, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        setContentPane(root);

        // Events
        addBtn.addActionListener(e -> onAdd());
        updateBtn.addActionListener(e -> onUpdate());
        deleteBtn.addActionListener(e -> onDelete());
        clearBtn.addActionListener(e -> clearForm());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) populateFormFromSelection();
        });

        // Load on start, save on close
        loadFromFile();
        refreshTable();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                saveToFile();
            }
        });
    }

    // ---- CRUD actions ----
    private void onAdd() {
        try {
            Student s = readForm(null);
            s.id = nextId++;
            students.add(s);
            refreshTable();
            clearForm();
            info("Added student with ID " + s.id);
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void onUpdate() {
        int row = table.getSelectedRow();
        if (row < 0) { warn("Select a row to update."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        Integer id = (Integer) model.getValueAt(modelRow, 0);

        try {
            Student newData = readForm(id);
            Student existing = findById(id);
            if (existing == null) { error("Selected student not found."); return; }
            existing.name = newData.name;
            existing.email = newData.email;
            existing.gpa = newData.gpa;
            refreshTable();
            info("Student updated.");
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) { warn("Select a row to delete."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        Integer id = (Integer) model.getValueAt(modelRow, 0);

        int ok = JOptionPane.showConfirmDialog(this,
                "Delete student with ID " + id + "?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        students.removeIf(s -> Objects.equals(s.id, id));
        refreshTable();
        clearForm();
        info("Student deleted.");
    }

    // ---- Helpers ----
    private Student readForm(Integer id) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String gpaText = gpaField.getText().trim();

        if (name.isEmpty()) throw new IllegalArgumentException("Name is required.");
        if (email.isEmpty()) throw new IllegalArgumentException("Email is required.");
        // Simple email sanity check
        if (!email.contains("@") || email.startsWith("@") || email.endsWith("@"))
            throw new IllegalArgumentException("Enter a valid email address.");

        double gpa;
        try { gpa = Double.parseDouble(gpaText); }
        catch (NumberFormatException nfe) { throw new IllegalArgumentException("GPA must be a number."); }
        if (gpa < 0 || gpa > 10) throw new IllegalArgumentException("GPA must be between 0 and 10.");

        return new Student(id, name, email, gpa);
    }

    private void populateFormFromSelection() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int modelRow = table.convertRowIndexToModel(row);
        nameField.setText(String.valueOf(model.getValueAt(modelRow, 1)));
        emailField.setText(String.valueOf(model.getValueAt(modelRow, 2)));
        gpaField.setText(String.valueOf(model.getValueAt(modelRow, 3)));
    }

    private void clearForm() {
        nameField.setText("");
        emailField.setText("");
        gpaField.setText("");
        table.clearSelection();
        nameField.requestFocusInWindow();
    }

    private void refreshTable() {
        model.setRowCount(0);
        for (Student s : students) {
            model.addRow(new Object[]{ s.id, s.name, s.email, s.gpa });
        }
    }

    private Student findById(Integer id) {
        for (Student s : students) if (Objects.equals(s.id, id)) return s;
        return null;
    }

    private void info(String msg) { JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE); }
    private void warn(String msg) { JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }

    // ---- Persistence: CSV with ';' separator ----
    private void loadFromFile() {
        if (!Files.exists(file)) return;
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            int maxId = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("#")) continue;       // comments
                if (line.startsWith("id;")) continue;      // header
                String[] parts = line.split(";", -1);
                if (parts.length < 4) continue;
                Integer id = tryParseInt(parts[0]);
                if (id == null) continue;
                String name = parts[1];
                String email = parts[2];
                Double gpa = tryParseDouble(parts[3]);
                if (gpa == null) gpa = 0.0;
                students.add(new Student(id, name, email, gpa));
                maxId = Math.max(maxId, id);
            }
            nextId = Math.max(nextId, maxId + 1);
        } catch (IOException ex) {
            error("Failed to load students.csv: " + ex.getMessage());
        }
    }

    private void saveToFile() {
        try (BufferedWriter bw = Files.newBufferedWriter(file)) {
            bw.write("id;name;email;gpa"); bw.newLine();
            for (Student s : students) {
                bw.write(s.id + ";" + safe(s.name) + ";" + safe(s.email) + ";" + s.gpa);
                bw.newLine();
            }
        } catch (IOException ex) {
            // Show an error but still allow closing
            JOptionPane.showMessageDialog(this, "Failed to save students.csv:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String safe(String s) {
        // Keep it simple: replace newline and semicolon (our separator)
        return s.replace("\n", " ").replace(";", ",");
    }

    private static Integer tryParseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    private static Double tryParseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return null; }
    }

    // ---- Main ----
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setSystemLookAndFeel();
            new StudentCrudApp().setVisible(true);
        });
    }

    private static void setSystemLookAndFeel() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
    }
}
