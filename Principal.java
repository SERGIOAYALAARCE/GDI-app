package sql;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

public class Principal extends JFrame {

    private Connection con;
    private JTable tablaDatos;
    private JComboBox<String> comboTablas;

    public Principal() {
        setTitle("Gestor de Inventario - MySQL");
        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        conectarBaseDeDatos();

        JPanel panelBotones = new JPanel();
        JButton btnCargarTablas = new JButton("Mostrar Tablas");
        JButton btnSalir = new JButton("Salir");

        comboTablas = new JComboBox<>();
        tablaDatos = new JTable();

        JScrollPane scroll = new JScrollPane(tablaDatos);

        panelBotones.add(new JLabel("Selecciona una tabla:"));
        panelBotones.add(comboTablas);
        panelBotones.add(btnCargarTablas);
        panelBotones.add(btnSalir);

        add(panelBotones, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        // Cargar nombres de tablas al combo
        cargarNombresDeTablas();

        // Acción botón Mostrar
        btnCargarTablas.addActionListener(e -> mostrarTablaSeleccionada());

        // Acción botón Salir
        btnSalir.addActionListener(e -> System.exit(0));
    }

    private void conectarBaseDeDatos() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC",
                    "root",
                    "gate"
            );

            Statement stmt = con.createStatement();
            stmt.execute("CREATE DATABASE IF NOT EXISTS inventario_db;");
            stmt.execute("USE inventario_db;");
            System.out.println("✅ Base de datos inventario_db lista.");

            // Leer archivo SQL y crear tablas si no existen
            String scriptCreacion = new String(Files.readAllBytes(
                    Paths.get("D:/JAVA/DIRECTORIO/SQL/src/sql/Creacion e Insersion inventario_db.txt")));
            String[] comandos = scriptCreacion.split(";");
            for (String sql : comandos) {
                sql = sql.trim();
                if (sql.length() > 5) {
                    try {
                        stmt.execute(sql + ";");
                    } catch (Exception ignored) {}
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al conectar a MySQL:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarNombresDeTablas() {
        try {
            comboTablas.removeAllItems();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLES;");
            while (rs.next()) {
                comboTablas.addItem(rs.getString(1));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al obtener tablas:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarTablaSeleccionada() {
        String nombreTabla = (String) comboTablas.getSelectedItem();
        if (nombreTabla == null) {
            JOptionPane.showMessageDialog(this, "No hay tablas disponibles.");
            return;
        }

        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + nombreTabla);
            ResultSetMetaData meta = rs.getMetaData();

            DefaultTableModel modelo = new DefaultTableModel();
            int columnas = meta.getColumnCount();

            // Nombres de columnas
            for (int i = 1; i <= columnas; i++) {
                modelo.addColumn(meta.getColumnName(i));
            }

            // Datos de filas
            while (rs.next()) {
                Object[] fila = new Object[columnas];
                for (int i = 1; i <= columnas; i++) {
                    fila[i - 1] = rs.getObject(i);
                }
                modelo.addRow(fila);
            }

            tablaDatos.setModel(modelo);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al mostrar tabla:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Principal ventana = new Principal();
            ventana.setVisible(true);
        });
    }
}
