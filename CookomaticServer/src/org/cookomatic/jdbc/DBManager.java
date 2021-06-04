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
import org.cookomatic.exception.CookomaticException;
import org.cookomatic.model.cuina.Categoria;
import org.cookomatic.model.cuina.Plat;
import org.cookomatic.model.sala.Cambrer;
import org.cookomatic.model.sala.Comanda;
import org.cookomatic.model.sala.EstatLinia;
import org.cookomatic.model.sala.LiniaComanda;
import org.cookomatic.model.sala.Taula;

public class DBManager {

    private Connection con;

    private PreparedStatement getTaules, getCategories, getPlats;
    private PreparedStatement insertComanda, insertLiniaComanda, updateBuidarTaula;

    // meves
    private PreparedStatement getTaulaSeleccionada, getComandesPerTaula, getLiniesPerComanda;

    public DBManager(String nomFitxerPropietats) {
        Properties p = new Properties();
        try {
            p.load(new FileReader(nomFitxerPropietats));
        } catch (IOException ex) {
            throw new CookomaticException("Problemes en carregar el fitxer de configuració. Més info: " + ex.getMessage(), ex);
        }
        // p conté les propietats necessàries per la connexió
        String url = p.getProperty("url");
        String usu = p.getProperty("usuari");
        String pwd = p.getProperty("contrasenya");
        if (url == null || usu == null || pwd == null) {
            throw new CookomaticException("Manca alguna de les propietats: url, usuari, contrasenya");
        }
        // Ja tenim les 3 propietats
        con = null;
        try {
            con = DriverManager.getConnection(url, usu, pwd);
            con.setAutoCommit(false);   // Per defecte, tota connexió JDBC és amb AutoCommit(true)
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            prepararStatements();
        } catch (SQLException sqle) {
            throw new CookomaticException("Error en establir connexió amb la BD", sqle);
        }
    }

    private void prepararStatements() throws SQLException {
//        getTaules = con.prepareStatement(
//                "select 	t.numero as numero, coms.codi as codi_comanda,\n"
//                + "		coms.plats_totals as plats_totals, coms.plats_preparats as plats_preparats,\n"
//                + "		coms.cambrer as nom_cambrer, coms.finalitzada as finalitzada\n"
//                + "from taula t left join\n"
//                + "					(\n"
//                + "					select  co.codi as codi, ca.user as cambrer,\n"
//                + "							(select count(*) from linia_comanda where comanda = co.codi)\n"
//                + "								as plats_totals,\n"
//                + "							(select count(*) from linia_comanda where comanda = co.codi and upper(estat) like 'PREPARADA')\n"
//                + "								as plats_preparats,\n"
//                + "							co.finalitzada as finalitzada, co.taula as taula\n"
//                + "					from comanda co join cambrer ca on co.cambrer = ca.codi\n"
//                + "					where co.finalitzada = false\n"
//                + "					) coms\n"
//                + "			on coms.taula = t.numero\n"
//                + "order by t.numero"
//        );
        getTaules = con.prepareStatement("SELECT * FROM VISTA_INFOTAULA");

        getTaulaSeleccionada = con.prepareStatement("select * from taula where numero = ?");
        getComandesPerTaula = con.prepareStatement("select * from comanda where taula = ? and finalitzada = ?");

        getCategories = con.prepareStatement("select * from categoria");
        getPlats = con.prepareStatement("select * from plat");

        getLiniesPerComanda = con.prepareStatement(
                "select 	lc.comanda, lc.num, lc.plat, lc.quantitat, lc.estat,\n"
                + "		p.nom, p.descripcio_md, p.preu, p.foto, p.disponible, p.categoria\n"
                + "from linia_comanda lc join plat p on lc.plat = p.codi\n"
                + "where lc.comanda = ?");

        insertComanda = con.prepareStatement(
                "INSERT INTO COMANDA (DATA, TAULA, CAMBRER, FINALITZADA)\n"
                + "VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);

        insertLiniaComanda = con.prepareStatement(
                "INSERT INTO LINIA_COMANDA (COMANDA, NUM, PLAT, QUANTITAT, ESTAT) VALUES\n"
                + "(?, ?, ?, ?, ?)");

        updateBuidarTaula = con.prepareStatement(
                "UPDATE COMANDA SET FINALITZADA = TRUE WHERE TAULA = ? AND FINALITZADA = FALSE");
    }

    public void tancarDBManager() {
        try {
            if (con != null) {
                con.rollback();		// Si no es fa commit o rollback casca... Excepció
                con.close();
            }
        } catch (SQLException sqle) {
            throw new CookomaticException("Error en tancar connexió amb la BD", sqle);
        }
    }

    //---------------------------------------------------------------------------------------------------------------
    // ACCÉS A DADES DE LA BD
    // Sabent que login (user) és ÚNIC
    // Si hi ha qualsevol problema, cambrer retornat serà = NULL
    public Cambrer getCambrerPerUser(String user) {
        PreparedStatement ps;
        Cambrer cambrer = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement("SELECT * FROM CAMBRER WHERE UPPER(USER) LIKE UPPER(?)");
            ps.setString(1, user);
            rs = ps.executeQuery();

            if (rs.next()) {
                cambrer = construirCambrer(rs);
            } else {
                System.out.println("[DBM]: ERROR Cambrer no consta a la BD");
            }
        } catch (SQLException ex) {
            System.out.println("[DBM]: ERROR En buscar cambrer per usuari a la BD");
            System.out.println(ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ex) {
                System.out.println("[DBM]: Error en tancar resultset: " + ex.getMessage());
            }
        }
        return cambrer;
    }

    // Retorna informació de les taules actuals en relació a l'usuari que fa la consulta
    // Si hi ha qualsevol problema, taules retornades serà = NULL
    public List<InfoTaula> getTaules(String user) {
        List<InfoTaula> infoTaules = new ArrayList<>();
        ResultSet rs = null;
        try {
            rs = getTaules.executeQuery();

            while (rs.next()) {
                InfoTaula it = construirInfoTaula(rs, user);
                infoTaules.add(it);
            }
        } catch (SQLException ex) {
            System.out.println("[DBM]: ERROR En get informació actual de les taules a la BD");
            System.out.println(ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ex) {
                System.out.println("[DBM]: Error en tancar resultset: " + ex.getMessage());
            }
        }
        return infoTaules;
    }

    // Retorna taula seleccionada si trobada, null si no trobada en la BD
    public Taula getTaulaSeleccionada(int numeroTaula) {
        Taula taulaSeleccionada = null;
        ResultSet rs = null;
        try {
            getTaulaSeleccionada.setInt(1, numeroTaula);
            rs = getTaulaSeleccionada.executeQuery();

            if (rs.next()) {
                taulaSeleccionada = construirTaulaSeleccionada(rs);
            }
        } catch (SQLException ex) {
            System.out.println("[DBM]: ERROR En get informació actual de taula seleccionada");
            System.out.println(ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ex) {
                System.out.println("[DBM]: Error en tancar resultset: " + ex.getMessage());
            }
        }
        return taulaSeleccionada;
    }

    public List<Categoria> getCategories() {
        List<Categoria> categories = new ArrayList<>();
        ResultSet rs = null;
        try {
            rs = getCategories.executeQuery();

            while (rs.next()) {
                Categoria categoria = construirCategoria(rs);
                categories.add(categoria);
            }
        } catch (SQLException ex) {
            System.out.println("[DBM]: ERROR En get categories");
            System.out.println(ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ex) {
                System.out.println("[DBM]: Error en tancar resultset: " + ex.getMessage());
            }
        }
        return categories;
    }

    public List<Plat> getPlats() {
        List<Plat> plats = new ArrayList<>();
        ResultSet rs = null;
        try {
            rs = getPlats.executeQuery();

            while (rs.next()) {
                Plat plat = construirPlat(rs);
                plats.add(plat);
            }
        } catch (SQLException ex) {
            System.out.println("[DBM]: ERROR En get plats");
            System.out.println(ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ex) {
                System.out.println("[DBM]: Error en tancar resultset: " + ex.getMessage());
            }
        }
        return plats;
    }

    // DML
    // Si la insert ha tingut èxit, retorna codi d'aquest nou registre. Altrament retorna -1
    public Long insertComanda(Comanda comanda, List<LiniaComanda> linies) {
        Long codiNovaComanda = (long) -1;
        int filesInserides;
        ResultSet rs = null;
        try {
            java.sql.Date dataAux = new java.sql.Date(comanda.getData().getTime());

            insertComanda.setDate(1, dataAux);
            insertComanda.setInt(2, comanda.getTaula().getNumero());
            insertComanda.setLong(3, comanda.getCambrer().getCodi());
            insertComanda.setBoolean(4, comanda.isFinalitzada());

            filesInserides = insertComanda.executeUpdate();

            if (filesInserides != 1) {

                System.out.println("[DBM]: ERROR EN INSERIR NOVA COMANDA");
                con.rollback();
                // TODO EXIT
                return (long) -1;
            }

            // Recuperem Codi de comanda recent inserida
            rs = insertComanda.getGeneratedKeys();
            if (rs.next()) {
                codiNovaComanda = rs.getLong(1);
            } else {
                // Error en insert
                System.out.println("[DBM]: ERROR EN RECUPERAR CODI DE NOVA COMANDA");
                con.rollback();
                // TODO EXIT
                return null;
            }
            System.out.println("[DBM]: Codi de nova comanda = " + codiNovaComanda);

            // Ara inserim linies de la comanda
            int numLinia = 1;
            for (LiniaComanda lc : linies) {
                insertLiniaComanda.setLong(1, codiNovaComanda);
                insertLiniaComanda.setInt(2, numLinia);
                insertLiniaComanda.setLong(3, lc.getItem().getCodi());
                insertLiniaComanda.setInt(4, lc.getQuantitat());
                insertLiniaComanda.setString(5, EstatLinia.EN_PREPARACIO.toString());

                filesInserides = insertLiniaComanda.executeUpdate();

                if (filesInserides != 1) {
                    System.out.println("[DBM]: ERROR EN INSERT LINIES DE NOVA COMANDA");
                    // TODO EXIT
                    return (long) -1;
                }
                numLinia++;
            }

            // En acabar d'inserir tot: COMMIT
            con.commit();
        } catch (SQLException ex) {
            System.out.println(ex);
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex1) {
                    System.out.println(ex1);
                }
            }
        }
        return codiNovaComanda;
    }

    public Integer buidarTaula(int numero) {
        int filesModificades;
        try {
            updateBuidarTaula.setInt(1, numero);
            filesModificades = updateBuidarTaula.executeUpdate();

            if (filesModificades != 1) {
                System.out.println("[DBM]: ERROR EN buidarTaula");
                con.rollback();
                // TODO EXIT
                return -1;
            }

            // En acabar d'inserir tot: COMMIT
            con.commit();
            System.out.println("[DBM]: taula buidada amb exit");
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return 0;
    }

    //---------------------------------------------------------------------------------------------------------------
    // CONSTRUIR OBJECTES
    // Construeix un objecte cambrer a partir de la fila actual en què es troba el ResultSet
    private Cambrer construirCambrer(ResultSet rs) throws SQLException {
//        System.out.println("CONSTRUIR CAMBRER");

        Long codi = rs.getLong("codi");
        String nom = rs.getString("nom");
        String cognom1 = rs.getString("cognom1");
        String cognom2 = rs.getString("cognom2");
        String user = rs.getString("user");
        String password = rs.getString("password");

        Cambrer cambrer = new Cambrer(codi, nom, cognom1, cognom2, user, password);
        return cambrer;
    }

    private InfoTaula construirInfoTaula(ResultSet rs, String user) throws SQLException {
//        System.out.println("CONSTRUIR INFOTAULA");

        Integer numero = rs.getInt("numero"); // numero de taula
        Long codiComanda = rs.getLong("codi_comanda");
        Integer platsTotals = rs.getInt("plats_totals");
        Integer platsPreparats = rs.getInt("plats_preparats");
        String nomCambrer = rs.getString("nom_cambrer");
        boolean comandaFinalitzada = rs.getBoolean("finalitzada");

        boolean esMeva = false;
        // si la taula no tenia cap cambrer: rs.wasNull()
        if (!rs.wasNull()) {
            esMeva = nomCambrer.equalsIgnoreCase(user); // taula és meva si el cambrer actual sóc jo
        }
        InfoTaula infoTaula = new InfoTaula(numero, codiComanda, esMeva, platsTotals, platsPreparats, nomCambrer, comandaFinalitzada);
        return infoTaula;
    }

    // Construeix objecte taula amb comanda activa (si en té)
    private Taula construirTaulaSeleccionada(ResultSet rs) throws SQLException {
//        System.out.println("CONSTRUIR TAULA SELECCIONADA");
        Comanda comandaActiva = null;

        Integer numero = rs.getInt("numero"); // numero de taula

        // Anem a buscar codi de la comanda activa que té aquesta taula
        getComandesPerTaula.setInt(1, numero);
        getComandesPerTaula.setBoolean(2, false); // set finalitzada = false

        // només retornarà 1 registre: 1 taula només pot tenir una comanda activa a la vegada
        rs.close();
        rs = getComandesPerTaula.executeQuery();

        // si té comanda activa, la construirem
        if (rs.next()) {
            // TODO: construir comanda fetch eager, nosaltres nomes agafarem els camps que ens interessen
            comandaActiva = construirComanda(rs);
        }
        rs.close();

        Taula taula = new Taula(numero);
        taula.setComandaActiva(comandaActiva);

        return taula;
    }

    // Construeix un objecte categoria a partir de la fila actual en què es troba el ResultSet
    private Categoria construirCategoria(ResultSet rs) throws SQLException {
//        System.out.println("CONSTRUIR CATEGORIA");

        Long codi = rs.getLong("codi");
        String nom = rs.getString("nom");
        Integer color = rs.getInt("color");

        Categoria categoria = new Categoria(codi, nom, color);
        return categoria;
    }

    // Construeix un objecte categoria a partir de la fila actual en què es troba el ResultSet
    private Plat construirPlat(ResultSet rs) throws SQLException {
//        System.out.println("CONSTRUIR PLAT");

        Long codi = rs.getLong("codi");
        String nom = rs.getString("nom");
        String descripcioMD = rs.getString("descripcio_md");
        BigDecimal preu = rs.getBigDecimal("preu");
        // TODO: get foto
        Boolean disponible = rs.getBoolean("disponible");
        Long codiCategoria = rs.getLong("categoria");
        Categoria cat = new Categoria(codiCategoria, null, 0);
        java.sql.Blob foto = rs.getBlob("foto");

        // com enllacem plat amb categoria? -> diccionari de codi - categoria, codi - plat
        Plat plat = new Plat(codi, nom, descripcioMD, preu, foto, disponible, cat, null);

        return plat;
    }

    // Construeix un objecte comanda a partir de la fila actual en què es troba el ResultSet
    private Comanda construirComanda(ResultSet rs) throws SQLException {
        Long codi = rs.getLong("codi");
        Date data = rs.getDate("data");
        Integer taula = rs.getInt("taula");
        Long codiCambrer = rs.getLong("cambrer");
        Boolean finalitzada = rs.getBoolean("finalitzada");

        // TODO: recollir cambrer
        Cambrer cambrer = new Cambrer(codiCambrer, "c", "c", "c", "c", "c"); // TODO: millorar
        Comanda comanda = new Comanda(codi, data, new Taula(taula), cambrer, finalitzada);

        // TODO: recollir linies de comanda
//        List<LiniaComanda> linies = new ArrayList<>();
        getLiniesPerComanda.setLong(1, codi);
        ResultSet rsLinies = null;

        try {
            rsLinies = getLiniesPerComanda.executeQuery();
            while (rsLinies.next()) {
                LiniaComanda lc = construirLiniaComanda(rsLinies);
                comanda.addLinia(lc);
            }
        } catch (Exception e) {
            System.out.println("[DBM]: Error en poblar comanda amb línies: "+e.getMessage());
        } finally {
            if (rsLinies!=null)
                rsLinies.close();
        }
        return comanda;
    }

    // Construeix linia de comanda i plat
    private LiniaComanda construirLiniaComanda(ResultSet rs) throws SQLException {
        LiniaComanda lc = null;

        Long codiComanda = rs.getLong("comanda");
        Integer num = rs.getInt("num");
        Integer quantitat = rs.getInt("quantitat");
        String estatS = rs.getString("estat");
        EstatLinia estat = EstatLinia.valueOf(estatS);

        // Construir plat
        Plat item = null;
        Long codiPlat = rs.getLong("plat");
        String nomPlat = rs.getString("nom");
        String descripcioMD = rs.getString("descripcio_md");
        BigDecimal preu = rs.getBigDecimal("preu");
        java.sql.Blob foto = rs.getBlob("foto");
        Boolean disponible = rs.getBoolean("disponible");
        Long codiCategoria = rs.getLong("categoria");

        Categoria cat = new Categoria(codiCategoria, null, 0);

        // Construim plat i línia comanda
        item = new Plat(codiPlat, nomPlat, descripcioMD, preu, foto, disponible, cat, null);
        lc = new LiniaComanda(num, quantitat, estat, item);

        return lc;
    }

}
