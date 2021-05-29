package org.cookomatic.jdbc;

import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cookomatic.model.sala.Cambrer;
import org.cookomatic.model.sala.Taula;
import org.cookomatic.protocol.InfoTaula;

public class DBManager {

    private Connection con;
    
    private PreparedStatement getTaules;
    

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
            
            prepararStatements();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            System.exit(-1);
        }
    }
    
    private void prepararStatements() throws SQLException {
        getTaules = con.prepareStatement(
        "select 	t.numero as taula, co.codi as codi_comanda,\n" +
        "        (select count(*) from linia_comanda where comanda = co.codi)\n" +
        "			as plats_totals, -- platsTotals\n" +
        "        (select count(*) from linia_comanda where comanda = co.codi and upper(estat) like 'PREPARADA')\n" +
        "			as plats_preparants, -- platsPreparats\n" +
        "		ca.user as nom_cambrer\n" +
        "from taula t 	left join comanda co on co.taula = t.numero\n" +
        "				left join cambrer ca on co.cambrer = ca.codi\n" +
        "where not(co.finalitzada) or co.finalitzada is null\n" +
        "order by t.numero");
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
    
    // Retorna informació de les taules actuals en relació a l'usuari que fa la consulta
    public List<InfoTaula> getTaules(String user) {
        List<InfoTaula> infoTaules = new ArrayList<>();
        try {
            System.out.println("Executant prepared statement getTaules");

            ResultSet rs = getTaules.executeQuery();
            
            while(rs.next())
            {
                InfoTaula it = construirInfoTaula(rs, user);
                infoTaules.add(it);
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return infoTaules;
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

    /**
     * Retorna objecte InfoTaula a partir de dades BD
     * @param rs: resultSet
     * @param user: login de l'usuari que vol consultar aquesta informació
     * @return InfoTaula
     */
    private InfoTaula construirInfoTaula(ResultSet rs, String user) throws SQLException {
        System.out.println("CONSTRUIR INFOTAULA");
        
        Integer numero = rs.getInt("numero"); // numero de taula
        Long codiComanda = rs.getLong("codi_comanda");
        Integer platsTotals = rs.getInt("plats_totals");
        Integer platsPreparats = rs.getInt("plats_preparants");
        String nomCambrer = rs.getString("nom_cambrer");
        
        boolean esMeva = nomCambrer.equalsIgnoreCase(user); // taula és meva si el cambrer actual sóc jo

        InfoTaula infoTaula = new InfoTaula(numero, codiComanda, esMeva, platsTotals, platsPreparats, nomCambrer);
        return infoTaula;
    }



}
