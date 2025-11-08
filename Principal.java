package sql;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class Principal extends JFrame {

    private Connection con;
    private JTable tablaDatos;
    private JComboBox<String> comboTablas;
    private DefaultTableModel modeloTabla;

    public Principal() {
        setTitle("Gestor de Inventario - MySQL (modo visual)");
        setSize(950, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        conectarBaseDeDatos();

        // --- Panel superior ---
        JPanel panelSuperior = new JPanel(new FlowLayout());
        comboTablas = new JComboBox<>();
        JButton btnMostrarTablas = new JButton("Mostrar");
        JButton btnTriggers = new JButton("Ver Triggers");
        JButton btnProcedimientos = new JButton("Ver Procedimientos");
        JButton btnSalir = new JButton("Salir");

        panelSuperior.add(new JLabel("Tabla:"));
        panelSuperior.add(comboTablas);
        panelSuperior.add(btnMostrarTablas);
        panelSuperior.add(btnTriggers);
        panelSuperior.add(btnProcedimientos);
        panelSuperior.add(btnSalir);
        add(panelSuperior, BorderLayout.NORTH);

        // --- Tabla central ---
        modeloTabla = new DefaultTableModel();
        tablaDatos = new JTable(modeloTabla);
        tablaDatos.setFillsViewportHeight(true);
        tablaDatos.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tablaDatos.setCellSelectionEnabled(true);
        add(new JScrollPane(tablaDatos), BorderLayout.CENTER);

        // --- Panel inferior ---
        JPanel panelInferior = new JPanel(new FlowLayout());
        JButton btnAgregarFila = new JButton("+ Agregar fila");
        JButton btnInsertar = new JButton("Insertar");
        JButton btnActualizar = new JButton("Actualizar");
        JButton btnEliminar = new JButton("Eliminar");
        panelInferior.add(btnAgregarFila);
        panelInferior.add(btnInsertar);
        panelInferior.add(btnActualizar);
        panelInferior.add(btnEliminar);
        add(panelInferior, BorderLayout.SOUTH);

        // --- Acciones ---
        cargarNombresDeTablas();
        btnMostrarTablas.addActionListener(e -> mostrarTablaSeleccionada());
        btnTriggers.addActionListener(e -> mostrarTriggers());
        btnProcedimientos.addActionListener(e -> mostrarProcedimientos());
        btnSalir.addActionListener(e -> System.exit(0));

        btnAgregarFila.addActionListener(e -> agregarFilaVacia());
        btnInsertar.addActionListener(e -> insertarFilaSeleccionada());
        btnActualizar.addActionListener(e -> actualizarFilaSeleccionada());
        btnEliminar.addActionListener(e -> eliminarFilaSeleccionada());
    }

    private void conectarBaseDeDatos() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/inventario_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    "root",
                    "gate"
            );
            System.out.println("✅ Conectado a MySQL inventario_db.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error de conexión:\n" + e);
        }
    }

    private void cargarNombresDeTablas() {
        try {
            comboTablas.removeAllItems();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLES;");
            while (rs.next()) comboTablas.addItem(rs.getString(1));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar tablas:\n" + e);
        }
    }

    private void mostrarTablaSeleccionada() {
        String tabla = (String) comboTablas.getSelectedItem();
        if (tabla == null) return;
        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + tabla);
            ResultSetMetaData meta = rs.getMetaData();

            modeloTabla = new DefaultTableModel();
            int columnas = meta.getColumnCount();
            for (int i = 1; i <= columnas; i++) modeloTabla.addColumn(meta.getColumnName(i));

            while (rs.next()) {
                Object[] fila = new Object[columnas];
                for (int i = 1; i <= columnas; i++) fila[i - 1] = rs.getObject(i);
                modeloTabla.addRow(fila);
            }

            tablaDatos.setModel(modeloTabla);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al mostrar:\n" + e);
        }
    }

    // --- Agregar una fila vacía al final ---
    private void agregarFilaVacia() {
        if (modeloTabla == null || modeloTabla.getColumnCount() == 0) {
            JOptionPane.showMessageDialog(this, "Primero selecciona una tabla para editar.");
            return;
        }
        Object[] vacio = new Object[modeloTabla.getColumnCount()];
        modeloTabla.addRow(vacio);
        tablaDatos.scrollRectToVisible(tablaDatos.getCellRect(modeloTabla.getRowCount() - 1, 0, true));
    }

    // --- Insertar una nueva fila en la base de datos ---
    private void insertarFilaSeleccionada() {
        int fila = tablaDatos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona la fila que deseas insertar.");
            return;
        }

        String tabla = (String) comboTablas.getSelectedItem();
        try {
            ResultSetMetaData meta = con.createStatement()
                    .executeQuery("SELECT * FROM " + tabla + " LIMIT 1").getMetaData();
            int columnas = meta.getColumnCount();

            StringBuilder campos = new StringBuilder();
            StringBuilder valores = new StringBuilder();

            for (int i = 1; i <= columnas; i++) {
                Object valor = modeloTabla.getValueAt(fila, i - 1);
                if (valor != null && !valor.toString().trim().isEmpty()) {
                    campos.append(meta.getColumnName(i)).append(",");
                    valores.append("'").append(valor).append("',");
                }
            }

            if (campos.length() == 0) {
                JOptionPane.showMessageDialog(this, "No hay datos para insertar.");
                return;
            }

            campos.deleteCharAt(campos.length() - 1);
            valores.deleteCharAt(valores.length() - 1);

            String sql = "INSERT INTO " + tabla + " (" + campos + ") VALUES (" + valores + ")";
            Statement stmt = con.createStatement();
            stmt.executeUpdate(sql);

            JOptionPane.showMessageDialog(this, "✅ Fila insertada correctamente.");
            mostrarTablaSeleccionada();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Error al insertar:\n" + e);
        }
    }

    // --- Actualizar la fila seleccionada ---
    private void actualizarFilaSeleccionada() {
        int fila = tablaDatos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una fila para actualizar.");
            return;
        }

        String tabla = (String) comboTablas.getSelectedItem();
        try {
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM " + tabla + " LIMIT 1");
            ResultSetMetaData meta = rs.getMetaData();
            String columnaClave = meta.getColumnName(1);
            Object valorClave = modeloTabla.getValueAt(fila, 0); // Se asume primera columna como clave

            StringBuilder setClause = new StringBuilder();
            for (int i = 2; i <= meta.getColumnCount(); i++) {
                Object valor = modeloTabla.getValueAt(fila, i - 1);
                setClause.append(meta.getColumnName(i)).append("='").append(valor).append("',");
            }
            setClause.deleteCharAt(setClause.length() - 1);

            String sql = "UPDATE " + tabla + " SET " + setClause + " WHERE " + columnaClave + "='" + valorClave + "'";
            Statement stmt = con.createStatement();
            int filas = stmt.executeUpdate(sql);

            if (filas > 0)
                JOptionPane.showMessageDialog(this, "✅ Fila actualizada correctamente.");
            else
                JOptionPane.showMessageDialog(this, "⚠️ No se encontró el registro para actualizar.");

            mostrarTablaSeleccionada();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Error al actualizar:\n" + e);
        }
    }

    // --- Eliminar la fila seleccionada ---
    private void eliminarFilaSeleccionada() {
        int fila = tablaDatos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una fila para eliminar.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "¿Seguro que deseas eliminar esta fila?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String tabla = (String) comboTablas.getSelectedItem();
        try {
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM " + tabla + " LIMIT 1");
            ResultSetMetaData meta = rs.getMetaData();
            String columnaClave = meta.getColumnName(1);
            Object valorClave = modeloTabla.getValueAt(fila, 0);

            String sql = "DELETE FROM " + tabla + " WHERE " + columnaClave + "='" + valorClave + "'";
            Statement stmt = con.createStatement();
            int filas = stmt.executeUpdate(sql);

            if (filas > 0) {
                JOptionPane.showMessageDialog(this, "✅ Fila eliminada correctamente.");
                mostrarTablaSeleccionada();
            } else {
                JOptionPane.showMessageDialog(this, "⚠️ No se encontró el registro a eliminar.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Error al eliminar:\n" + e);
        }
    }

    // --- Mostrar triggers y procedimientos ---
    private void mostrarTriggers() {
        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TRIGGERS FROM inventario_db;");
            DefaultTableModel modelo = new DefaultTableModel(new String[]{"Trigger", "Evento", "Tabla"}, 0);
            while (rs.next()) {
                modelo.addRow(new Object[]{rs.getString("Trigger"), rs.getString("Event"), rs.getString("Table")});
            }
            tablaDatos.setModel(modelo);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al mostrar triggers:\n" + e);
        }
    }

    private void mostrarProcedimientos() {
        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW PROCEDURE STATUS WHERE Db = 'inventario_db';");
            DefaultTableModel modelo = new DefaultTableModel(new String[]{"Nombre", "Tipo", "Creado"}, 0);
            while (rs.next()) {
                modelo.addRow(new Object[]{rs.getString("Name"), rs.getString("Type"), rs.getString("Created")});
            }
            tablaDatos.setModel(modelo);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al mostrar procedimientos:\n" + e);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Principal().setVisible(true));
    }
}
