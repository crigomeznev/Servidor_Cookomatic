package org.cookomatic.jdbc;

import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cookomatic.model.sala.Cambrer;

public class DBManager {

    private Connection con;

    public DBManager(String nomFitxerPropietats) {
        Properties p = new Properties();
        try {
            p.load(new FileReader(nomFitxerPropietats));
        } catch (IOException ex) {
            System.out.println("Problemes en carregar el fitxer de configuració");
            System.out.println("Més info: " + ex.getMessage());
            System.exit(1);
        }
        // p conté les propietats necessàries per la connexió
        String url = p.getProperty("url");
        String usu = p.getProperty("usuari");
        String pwd = p.getProperty("contrasenya");
        if (url == null || usu == null || pwd == null) {
            System.out.println("Manca alguna de les propietats: url, usuari, contrasenya");
            System.exit(1);
        }
        // Ja tenim les 3 propietats
        con = null;
        try {
            con = DriverManager.getConnection(url, usu, pwd);
            System.out.println("Connexió establerta");
            con.setAutoCommit(false);   // Per defecte, tota connexió JDBC és amb AutoCommit(true)
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            System.exit(-1);
        }
    }

    public void tancarDBManager() {
        try {
            if (con != null) {
                con.rollback();		// Si no es fa commit o rollback casca... Excepció
                con.close();
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }


    // ACCÉS A DADES DE LA BD
    // Sabent que login (user) és ÚNIC
    public Cambrer getCambrerPerUser(String user) {
        PreparedStatement ps;
        Cambrer cambrer = null;
        try {
            System.out.println("Executant select * from cambrer where upper(user) like upper("+user+")");

            ps = con.prepareStatement("SELECT * FROM CAMBRER WHERE UPPER(USER) LIKE UPPER(?)");
            ps.setString(1, user);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next())
                cambrer = construirCambrer(rs);
            else
                System.out.println("Cambrer no consta a la BD");
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return cambrer;
    }

    public static void main(String[] args) {
        Properties p = new Properties();
        try {
            p.load(new FileReader(args[0]));
        } catch (IOException ex) {
            System.out.println("Problemes en carregar el fitxer de configuració");
            System.out.println("Més info: " + ex.getMessage());
            System.exit(1);
        }
        // p conté les propietats necessàries per la connexió
        String url = p.getProperty("url");
        String usu = p.getProperty("usuari");
        String pwd = p.getProperty("contrasenya");
        if (url == null || usu == null || pwd == null) {
            System.out.println("Manca alguna de les propietats: url, usuari, contrasenya");
            System.exit(1);
        }
        // Ja tenim les 3 propietats
        Connection con = null;
        try {
            con = DriverManager.getConnection(url, usu, pwd);
            System.out.println("Connexió establerta");
            con.setAutoCommit(false);   // Per defecte, tota connexió JDBC és amb AutoCommit(true)

            Statement st1, st2;
            st1 = con.createStatement();
            st2 = con.createStatement();

            int x = st1.executeUpdate("INSERT INTO Emp (emp_no, cognom, dept_no) values (7777,'Gotera',10)");
            System.out.println("Inserció de " + x + " elements.");

            System.out.println("Contingut de la taula 'Emp' via st1:");
            ResultSet rs1 = st1.executeQuery("SELECT * FROM Emp");
            displayResults(rs1);

            System.out.println("Contingut de la taula 'Emp' via st2:");
            ResultSet rs2 = st2.executeQuery("SELECT * FROM Emp");
            displayResults(rs2);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            System.exit(-1);
        } finally {
            try {
                if (con != null) {
                    con.rollback();		// Si no es fa commit o rollback casca... Excepció
                    con.close();
                }
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }
        }
    }

    static void displayResults(ResultSet rs) throws SQLException {
        while (rs.next()) {
            String cognom = rs.getString("cognom");
            int emp_no = rs.getInt("emp_no");
            System.out.println(emp_no + " - " + cognom);
        }
    }


    // Construeix un objecte cambrer a partir de la fila actual en què es troba el ResultSet
    private Cambrer construirCambrer(ResultSet rs) throws SQLException {
        System.out.println("CONSTRUIR CAMBRER");
        
        Long codi = rs.getLong("codi");
        String nom = rs.getString("nom");
        String cognom1 = rs.getString("cognom1");
        String cognom2 = rs.getString("cognom2");
        String user = rs.getString("user");
        String password = rs.getString("password");

        Cambrer cambrer = new Cambrer(codi, nom, cognom1, cognom2, user, password);
        return cambrer;
    }


}
