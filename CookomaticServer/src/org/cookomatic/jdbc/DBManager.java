package org.cookomatic.jdbc;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cookomatic.protocol.InfoTaula;
import org.milaifontanals.cookomatic.model.cuina.Categoria;
import org.milaifontanals.cookomatic.model.cuina.Plat;
import org.milaifontanals.cookomatic.model.sala.Cambrer;
import org.milaifontanals.cookomatic.model.sala.Comanda;
import org.milaifontanals.cookomatic.model.sala.EstatLinia;
import org.milaifontanals.cookomatic.model.sala.LiniaComanda;
import org.milaifontanals.cookomatic.model.sala.Taula;

public class DBManager {

    private Connection con;
    
    private PreparedStatement getTaules, getCategories, getPlats;
    private PreparedStatement insertComanda, insertLiniaComanda;
    private String insertComandaSql;
    
    // meves
    private PreparedStatement getTaulaSeleccionada, getComandesPerTaula;

    

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
        "select 	t.numero as numero, co.codi as codi_comanda,\n" +
        "        (select count(*) from linia_comanda where comanda = co.codi)\n" +
        "			as plats_totals, -- platsTotals\n" +
        "        (select count(*) from linia_comanda where comanda = co.codi and upper(estat) like 'PREPARADA')\n" +
        "			as plats_preparants, -- platsPreparats\n" +
        "		ca.user as nom_cambrer\n" +
        "from taula t 	left join comanda co on co.taula = t.numero\n" +
        "				left join cambrer ca on co.cambrer = ca.codi\n" +
        "where not(co.finalitzada) or co.finalitzada is null\n" +
        "order by t.numero");
        
        getTaulaSeleccionada = con.prepareStatement("select * from taula where numero = ?");
        getComandesPerTaula = con.prepareStatement("select * from comanda where taula = ? and finalitzada = ?");
        
        getCategories = con.prepareStatement("select * from categoria");
        getPlats = con.prepareStatement("select * from plat");
        

        insertComanda = con.prepareStatement(
                "INSERT INTO COMANDA (DATA, TAULA, CAMBRER, FINALITZADA)\n" +
                            "VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);

        insertLiniaComanda = con.prepareStatement(
            "INSERT INTO LINIA_COMANDA (COMANDA, NUM, PLAT, QUANTITAT, ESTAT) VALUES\n" +
            "(?, ?, ?, ?, ?)");
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
    
    // Retorna taula seleccionada si trobada, null si no trobada en la BD
    public Taula getTaulaSeleccionada(int numeroTaula) {
        Taula taulaSeleccionada = null;
        try {
            System.out.println("Executant prepared statement getTaulaSeleccionada");
            
            getTaulaSeleccionada.setInt(1, numeroTaula);
            ResultSet rs = getTaulaSeleccionada.executeQuery();
            
            if (rs.next())
            {
                taulaSeleccionada = construirTaulaSeleccionada(rs);
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return taulaSeleccionada;
    }


    public List<Categoria> getCategories() {
        List<Categoria> categories = new ArrayList<>();
        try {
            System.out.println("Executant prepared statement getCategories");

            ResultSet rs = getCategories.executeQuery();
            
            while(rs.next())
            {
                Categoria categoria = construirCategoria(rs);
                categories.add(categoria);
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return categories;
    }
    
    
    public List<Plat> getPlats() {
        List<Plat> plats = new ArrayList<>();
        try {
            System.out.println("Executant prepared statement getCategories");

            ResultSet rs = getPlats.executeQuery();
            
            while(rs.next())
            {
                Plat plat = construirPlat(rs);
                plats.add(plat);
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return plats;
    }
    
    
    
    // DML
    // Si la insert ha tingut èxit, retorna codi d'aquest nou registre. Altrament retorna -1
    public Long insertComanda(Comanda comanda, List<LiniaComanda> linies) {
        Long codiNovaComanda = (long)-1;
        int filesInserides;
        ResultSet rs = null;
        try {
            System.out.println("Executant prepared statement insertComanda");

            java.sql.Date dataAux = new java.sql.Date(comanda.getData().getTime());
        
            insertComanda.setDate(1, dataAux);
            insertComanda.setInt(2, comanda.getTaula().getNumero());
            insertComanda.setLong(3, comanda.getCambrer().getCodi());
            insertComanda.setBoolean(4, comanda.isFinalitzada());

            filesInserides = insertComanda.executeUpdate();

            if (filesInserides != 1) {
                System.out.println("ERROR EN INSERT COMANDA");
                con.rollback();
                // TODO EXIT
                return (long)-1;
            }
            
            // Recuperem Codi de comanda recent inserida
            rs = insertComanda.getGeneratedKeys();
            if (rs.next()) {
                codiNovaComanda = rs.getLong(1);
            } else {
                // Error en insert
                System.out.println("ERROR EN INSERT COMANDA");
                con.rollback();
                // TODO EXIT
                return null;
            }
            System.out.println("Codi de nova comanda = "+codiNovaComanda);


            // Ara inserim linies de la comanda
            int numLinia = 1;
            for (LiniaComanda lc : linies)
            {
                insertLiniaComanda.setLong(1, codiNovaComanda);
                insertLiniaComanda.setInt(2, numLinia);
                insertLiniaComanda.setLong(3, lc.getItem().getCodi());
                insertLiniaComanda.setInt(4, lc.getQuantitat());
                insertLiniaComanda.setString(5, EstatLinia.EN_PREPARACIO.toString());
                
                filesInserides = insertLiniaComanda.executeUpdate();

                if (filesInserides != 1) {
                    System.out.println("ERROR EN INSERT LINIA COMANDA");
                    // TODO EXIT
                    return (long)-1;
                }                
                numLinia++;
            }
            
            // En acabar d'inserir tot: COMMIT
            con.commit();
        } catch (SQLException ex) {
            System.out.println(ex);
            if (rs != null){
                try {
                    rs.close();
                } catch (SQLException ex1) {
                    System.out.println(ex1);
                }
            }
        }
        return codiNovaComanda;
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
        
        boolean esMeva = false;
        // si la taula no tenia cap cambrer: rs.wasNull()
        if (!rs.wasNull())
            esMeva = nomCambrer.equalsIgnoreCase(user); // taula és meva si el cambrer actual sóc jo

        InfoTaula infoTaula = new InfoTaula(numero, codiComanda, esMeva, platsTotals, platsPreparats, nomCambrer);
        System.out.println("Nova infotaula = "+numero+"/"+codiComanda+" cambrer="+nomCambrer+" esmeva="+esMeva);
        System.out.println("\tuser="+user);
        return infoTaula;
    }


    private Taula construirTaulaSeleccionada(ResultSet rs) throws SQLException {
        System.out.println("CONSTRUIR TAULA SELECCIONADA");
        Comanda comandaActiva = null;
        
        Integer numero = rs.getInt("numero"); // numero de taula

        // Anem a buscar codi de la comanda activa que té aquesta taula
        getComandesPerTaula.setInt(1, numero);
        getComandesPerTaula.setBoolean(2, false); // set finalitzada = false
        
        // només retornarà 1 registre: 1 taula només pot tenir una comanda activa a la vegada
        rs.close();
        rs = getComandesPerTaula.executeQuery();
        
        if (rs.next())
        {
            // TODO: construir comanda fetch eager, nosaltres nomes agafarem els camps que ens interessen
            comandaActiva = construirComanda(rs);
        }
        
        Taula taula = new Taula(numero);
        taula.setComandaActiva(comandaActiva);

        return taula;
    }





    // Construeix un objecte categoria a partir de la fila actual en què es troba el ResultSet
    private Categoria construirCategoria(ResultSet rs) throws SQLException {
        System.out.println("CONSTRUIR CATEGORIA");
        
        Long codi = rs.getLong("codi");
        String nom = rs.getString("nom");
        Integer color = rs.getInt("color");

        Categoria categoria = new Categoria(codi, nom, color);
        return categoria;        
    }
    


    // Construeix un objecte categoria a partir de la fila actual en què es troba el ResultSet
    private Plat construirPlat(ResultSet rs) throws SQLException {
        System.out.println("CONSTRUIR PLAT");
        
        Long codi = rs.getLong("codi");
        String nom = rs.getString("nom");
        String descripcioMD = rs.getString("descripcio_md");
        BigDecimal preu = rs.getBigDecimal("preu");
        // TODO: get foto
        Boolean disponible = rs.getBoolean("disponible");
        Long codiCategoria = rs.getLong("categoria");
        Categoria cat = new Categoria(codiCategoria, null, 0);

        // com enllacem plat amb categoria? -> diccionari de codi - categoria, codi - plat
        Plat plat = new Plat(codi, nom, descripcioMD, preu, null, disponible, cat, null);

        return plat;
    }


    // Construeix un objecte comanda a partir de la fila actual en què es troba el ResultSet
    private Comanda construirComanda(ResultSet rs) throws SQLException {
        System.out.println("CONSTRUIR COMANDA");
        
        Long codi = rs.getLong("codi");
        Date data = rs.getDate("data");
        Integer taula = rs.getInt("taula");
        Long codiCambrer = rs.getLong("cambrer");
        Boolean finalitzada = rs.getBoolean("finalitzada");
        
        // TODO: recollir linies de comanda

        // TODO: recollir cambrer
        Cambrer cambrer = new Cambrer(codiCambrer, "c", "c", "c", "c", "c");
        Comanda comanda = new Comanda(codi, data, new Taula(taula), cambrer, finalitzada);
        
        return comanda;
    }

}
