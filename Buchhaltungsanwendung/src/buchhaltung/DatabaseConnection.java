package buchhaltung;

import java.sql.*;

public class DatabaseConnection {

    private static Connection connect = null;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connect = DriverManager.getConnection("jdbc:mysql://localhost/buchhaltungdb", "root", "");
        } catch (Exception e) {
            System.out.println("Die Verbindung zur Datenbank konnte nicht hergestellt werden.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    // Verbindung abrufen
    public static Connection getConnection() {
        return connect;
    }

    // Schließen der Verbindung und Ressourcen
//    public static void close() {
//        try {
//            if (connect != null) {
//                connect.close();
//            }
//        } catch (Exception e) {
//            System.out.println("Die Verbindung zur Datenbank konnte nicht geschlossen werden.");
//            System.out.println("Details: " + e.getMessage());
//        }
//    }
}
