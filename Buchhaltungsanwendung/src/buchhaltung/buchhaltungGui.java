package buchhaltung;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.time.LocalDateTime;

public class buchhaltungGui {
    private JPanel buchhaltungGuiPanel;
    private JTable history;
    private JTextField betrag;
    private JLabel betragLabel;
    private JTextField zusatzinfo;
    private JLabel zusatzInfoLabel;
    private JComboBox kategorie;
    private JLabel kategorieLabel;
    private JLabel ein_ausLabel;
    private JRadioButton einCheck;
    private JRadioButton ausCheck;
    private JButton saveBtn;
    private JButton deleteBtn;
    private JLabel einnahmeLabel;
    private JLabel ausgabeLabel;

    private int selectedId = -1;

    // Datenbankverbindung
    private static Connection connection;

    // Initialisierung der Datenbankverbindung in einem statischen Block
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost/buchhaltungdb", "root", "");
        } catch (Exception e) {
            System.out.println("Die Verbindung zur Datenbank konnte nicht hergestellt werden.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    public buchhaltungGui() {
        loadData();
        saveBtn.addActionListener(e -> saveData());
        deleteBtn.addActionListener(e -> {
            if (selectedId != -1) {
                deleteData(selectedId);
                selectedId = -1; // Auswahl zurücksetzen
            } else {
                JOptionPane.showMessageDialog(buchhaltungGuiPanel, "Kein Datensatz ausgewählt.", "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });
        selectData();
    }

    private void selectData() {
        history.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = history.getSelectedRow();
                if (selectedRow != -1) {
                    selectedId = (int) history.getValueAt(selectedRow, 0); // ID speichern
                    kategorie.setSelectedItem(history.getValueAt(selectedRow, 1).toString());
                    String datum = history.getValueAt(selectedRow, 2).toString();
                    zusatzinfo.setText(history.getValueAt(selectedRow, 3).toString());
                    betrag.setText(history.getValueAt(selectedRow, 4).toString());
                }
            }
        });
    }

    private void loadData() {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Kategorie", "Datum", "Zusatzinfo", "Betrag", "Ein/Aus"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        history.setModel(model);

        try (Statement statement = connection.createStatement()) {
            // Führe eine JOIN-Abfrage durch, um die 'ein_aus'-Spalte abzurufen
            String query = "SELECT b.id, b.kategorie_id, b.datum, b.zusatzinfo, b.betrag, k.ein_aus " +
                    "FROM buchung b " +
                    "JOIN kostenkategorie k ON b.kategorie_id = k.id";
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                int kategorieId = resultSet.getInt("kategorie_id");
                String datum = resultSet.getString("datum");
                String zusatzinfo = resultSet.getString("zusatzinfo");
                double betrag = resultSet.getDouble("betrag");
                String einAus = resultSet.getString("ein_aus"); // Ein/Aus von der kostenkategorie-Tabelle

                model.addRow(new Object[]{id, kategorieId, datum, zusatzinfo, betrag, einAus});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(buchhaltungGuiPanel, "Fehler beim Laden der Daten: " + e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveData() {
        if (connection == null) {
            JOptionPane.showMessageDialog(buchhaltungGuiPanel, "Keine Verbindung zur Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Kategorie-ID basierend auf der Auswahl im JComboBox berechnen
        int kategorieId = kategorie.getSelectedIndex() + 1;  // IDs beginnen bei 1

        LocalDateTime currentDateTime = LocalDateTime.now();
        String zusatzinfoText = zusatzinfo.getText().trim();

        double betragValue;
        try {
            if (betrag.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(buchhaltungGuiPanel, "Bitte geben Sie einen Betrag ein.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
            betragValue = Double.parseDouble(betrag.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(buchhaltungGuiPanel, "Bitte geben Sie einen gültigen Betrag ein.", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String query = "INSERT INTO buchung (kategorie_id, datum, zusatzinfo, betrag) VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, kategorieId);
            preparedStatement.setString(2, currentDateTime.toString());
            preparedStatement.setString(3, zusatzinfoText);
            preparedStatement.setDouble(4, betragValue);

            preparedStatement.executeUpdate();
            JOptionPane.showMessageDialog(buchhaltungGuiPanel, "Daten erfolgreich gespeichert!", "Erfolg", JOptionPane.INFORMATION_MESSAGE);

            loadData();
            zusatzinfo.setText("");
            betrag.setText("");
            kategorie.setSelectedIndex(0);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(buchhaltungGuiPanel, "Fehler beim Speichern der Daten: " + e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteData(int id) {
        if (connection == null) {
            JOptionPane.showMessageDialog(buchhaltungGuiPanel, "Keine Verbindung zur Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Datum des Datensatzes abrufen
        String query = "SELECT datum FROM buchung WHERE id = ?";
        LocalDateTime datumDesDatensatzes = null;

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                // Datum des Datensatzes aus der Datenbank lesen
                datumDesDatensatzes = resultSet.getTimestamp("datum").toLocalDateTime();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(buchhaltungGuiPanel, "Fehler beim Abrufen des Datensatzes: " + e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Überprüfen, ob das Datum des Datensatzes vor heute liegt
        if (datumDesDatensatzes != null && datumDesDatensatzes.toLocalDate().isBefore(LocalDateTime.now().toLocalDate())) {
            JOptionPane.showMessageDialog(buchhaltungGuiPanel, "Sie können nur Datensätze löschen die Heute erstellt wurden", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(buchhaltungGuiPanel, "Sind Sie sicher, dass Sie den Datensatz löschen möchten?", "Bestätigung", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String deleteQuery = "DELETE FROM buchung WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
            preparedStatement.setInt(1, id);
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(buchhaltungGuiPanel, "Datensatz erfolgreich gelöscht!", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                loadData();
                zusatzinfo.setText("");
                betrag.setText("");
                kategorie.setSelectedIndex(0);
            } else {
                JOptionPane.showMessageDialog(buchhaltungGuiPanel, "Kein Datensatz mit dieser ID gefunden.", "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(buchhaltungGuiPanel, "Fehler beim Löschen des Datensatzes: " + e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Buchhaltung GUI");
        frame.setContentPane(new buchhaltungGui().buchhaltungGuiPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setVisible(true);
    }
}
