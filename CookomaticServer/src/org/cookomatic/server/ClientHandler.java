/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cookomatic.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cookomatic.jdbc.DBManager;
import org.cookomatic.protocol.CodiOperacio;
import org.cookomatic.protocol.CreateComandaTuple;
import org.cookomatic.protocol.InfoTaula;
import org.cookomatic.protocol.LoginTuple;
import org.milaifontanals.cookomatic.model.cuina.Categoria;
import org.milaifontanals.cookomatic.model.cuina.Plat;
import org.milaifontanals.cookomatic.model.sala.Cambrer;
import org.milaifontanals.cookomatic.model.sala.Comanda;
import org.milaifontanals.cookomatic.model.sala.LiniaComanda;
import org.milaifontanals.cookomatic.model.sala.Taula;
import org.milaifontanals.cookomatic.exception.CookomaticException;

/**
 *
 * @author Usuario
 */
public class ClientHandler extends Thread {

//    DateFormat fordate = new SimpleDateFormat("yyyy/MM/dd");
//    DateFormat fortime = new SimpleDateFormat("hh:mm:ss");
    final ObjectInputStream ois;
    final ObjectOutputStream oos;
    final Socket socket;
    
    private LoginTuple loginTuple;
    
//    private long sessionId;
    private CookomaticServer server;
    private DBManager dbManager;

    private boolean fiConnexio;

//    private String nomFitxerPropietats;
    
    // Constructor
    public ClientHandler(Socket socket, ObjectInputStream ois, ObjectOutputStream oos, 
            CookomaticServer server, String nomFitxerPropietats) {
        this.socket = socket;
        this.ois = ois;
        this.oos = oos;
        this.loginTuple = new LoginTuple(null, null); // construim provisionalment tupla login amb id de sessió
        this.fiConnexio = false;
        this.server = server; // passem referència per esborrar clientHandler de la list de clientHandlers que té el servidor
        
        
        this.dbManager = new DBManager(nomFitxerPropietats);
        
        
       
    }

//    public long getSessionId() {
//        return sessionId;
//    }
//
//    public void setSessionId(long sessionId) {
//        this.sessionId = sessionId;
//    }

    @Override
    public void run() {
        int codiOperacio = 0;
        
        // el primer que fem es el login
//        System.out.println("Esperant login del client");
//        userLogin();
//        System.out.println("Login realitzat");

        while (!fiConnexio) {
            try {
//                do {

                // Llegim codi operació que vol el client
                System.out.println("Esperant petició del client");
                codiOperacio = ois.readInt();
                System.out.println("Petició rebuda = " + codiOperacio);

                // enviem ok
//                    oos.writeInt(1);
                switch (codiOperacio) {
                    case 1:
                        System.out.println("User asked for LOGIN");
                        userLogin();
                        break;
                    case 2:
                        System.out.println("User asked for GetTaules");
                        getTaules();
                        break;
                    case 3:
                        System.out.println("User asked for GetCarta");
                        getCarta();
                        break;
                    case 4:
                        System.out.println("User asked for GetComanda");
                        break;
                    case 5:
                        System.out.println("User asked for CreateComanda");
                        createComanda();
                        break;
                    case 6:
                        System.out.println("User asked for BuidarTaula");
                        buidarTaula();
                        break;
                    case 7:
                        System.out.println("User asked for GetTaulaSeleccionada");
                        getTaulaSeleccionada();
                        break;
                    case -1:
                        System.out.println("User asked for TancarConnexio");
                        break;
                    default:
                        System.out.println("Invalid operation");
                    // TODO: llençar exception
                    }
//                } while (codiOperacio != -1); // TODO: while !tancarConnexio

                System.out.println("Client " + this.socket + " sends exit...");

            } catch (IOException ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            } finally {
                tancarClientHandler();
            }

        }
    }

    
    
    
    // ACCÉS A LA BD
    
    // Login
    public void userLogin() {
        try {
            // llegim loginTuple
            System.out.println("llegint logintuple");
            loginTuple = (LoginTuple) ois.readObject();
            System.out.println("logintuple rebuda: " + loginTuple.getCambrer().getUser() + "/" + loginTuple.getCambrer().getPassword());
            // enviem ok
            oos.writeInt(1);
            oos.flush();

            // client envia inicialment sense sessionId, som nosaltres qui l'hi donarem
            // Comprovació que la contrasenya introduida per l'usuari és la correcta
            Cambrer c = dbManager.getCambrerPerUser(loginTuple.getCambrer().getUser());

            // prova
//            boolean credencialsCorrectes = lt.getUser().equalsIgnoreCase(lt.getPassword());
            if (c != null) {
                boolean credencialsCorrectes = loginTuple.getCambrer().getPassword().equalsIgnoreCase(c.getPassword());
                int res = credencialsCorrectes ? 1 : 0;

                // enviem 1 si ok, 0 si no ok
                System.out.println("Enviant resposta");
                oos.flush();
                oos.writeInt(res);
//            oos.writeObject(res);
                System.out.println("Resposta enviada");

                // si la resposta ha estat ok, també enviem tupla amb session_id i dades usu
                if (credencialsCorrectes) {
                    // Assignem dades del cambrer a la loginTuple
                    loginTuple.setCambrer(c);
                    
                    // demanem al server un nou session id (ell s'encarregarà d'afegir-lo en una llista)
                    loginTuple.setSessionId(server.getNewSessionId());

                    System.out.println("enviant lt amb sessionId");
                    oos.writeObject(loginTuple);
                    oos.flush();
                    System.out.println("lt amb sessionId enviat");
                    // llegim ok
                    ois.readInt();
                }
            } else 
            {
                // Cambrer no trobat en la BD
                // enviem 1 si ok, 0 si no ok
                System.out.println("El cambrer no consta a la BD. Enviant resposta");
                oos.flush();
                oos.writeInt(0);
//            oos.writeObject(res);
                System.out.println("El cambrer no consta a la BD. Resposta enviada");
            }

        } catch (IOException | ClassNotFoundException ex) {
            System.out.println(ex);
            ex.printStackTrace();
        } finally {
            // EL LOGIN NO HA DE TANCAR LA CONNEXIÓ!!!
//            try {
//                oos.close();
//                ois.close();
//                socket.close();
//            } catch (IOException ex) {
//                System.out.println(ex);
//                ex.printStackTrace();
//            }
        }

    }

    // GetTaules
    // Usem classe InfoTaula, que utilitza dades de les taules: Taula, Comanda, LiniaComanda i Cambrer
    public void getTaules()
    {
        List<InfoTaula> infoTaules = new ArrayList<>();
        LoginTuple loginTuple;
        long sessionId;
        int res;
        
        try {
            // llegim sessionId (logintuple)
            System.out.println("llegint logintuple");
            loginTuple = (LoginTuple)ois.readObject();
            System.out.println("sessionId llegit = "+loginTuple.getSessionId());

            // enviem resposta: procedirem si session id existeix en el server actualment, altrament avortarem
            if (server.sessionIdExists(loginTuple.getSessionId()))
                res = CodiOperacio.OK.getNumVal();
            else
                res = CodiOperacio.KO.getNumVal();
            
            oos.writeInt(res);
            oos.flush();
            
            // Si el session id del client no coincideix amb el del client actual, avortem operació
            // Cada ClientHandler que atén a un client té un session id
            if (res == CodiOperacio.KO.getNumVal()){
                System.out.println("Session ID erroni, avortem operació");
                return;
            }
            
            // sessionId vàlid, llegim info taules de la BD
            infoTaules = dbManager.getTaules(loginTuple.getCambrer().getUser()); // TODO: es podria fer en un singleton
            System.out.println("gettaules = ");
            for(InfoTaula it : infoTaules){
                System.out.println("Nova infotaula = "+it.getNumero()+"/"+it.getCodiComanda()+" esmeva="+it.isEsMeva()+", finalitzada="+it.isComandaFinalitzada());
                System.out.println("\tuser="+it.getNomCambrer());
            }
            if (infoTaules.isEmpty()) System.out.println("BUIDA");
            
            // Enviem tamany de l'arraylist infoTaules
            oos.writeInt(infoTaules.size());
            oos.flush();
            
            // Enviem arraylist element a element
            for(InfoTaula it : infoTaules)
            {
                oos.writeObject(it);
                oos.flush();
                
                // llegim ok
                ois.read();
            }
            System.out.println("Infotaules enviades amb exit");

        } catch (IOException | ClassNotFoundException ex) {
            System.out.println(ex);
            ex.printStackTrace();
        } finally {
            try {
                oos.close();
                ois.close();
                socket.close();
            } catch (IOException ex) {
                System.out.println(ex);
                ex.printStackTrace();
            }
        }

        
    }
    
    
    
    public void getTaulaSeleccionada()
    {
        Taula taulaSeleccionada = null;
        Comanda comandaActiva = null;
        int numTaula;
        LoginTuple loginTuple;
        int res;
        
        try {
            // llegim sessionId (logintuple)
            System.out.println("llegint logintuple");
            loginTuple = (LoginTuple)ois.readObject();
            System.out.println("sessionId llegit = "+loginTuple.getSessionId());

            // enviem resposta: procedirem si session id existeix en el server actualment, altrament avortarem
            if (server.sessionIdExists(loginTuple.getSessionId()))
                res = CodiOperacio.OK.getNumVal();
            else
                res = CodiOperacio.KO.getNumVal();
            
            oos.writeInt(res);
            oos.flush();
            
            // Si el session id del client no coincideix amb el del client actual, avortem operació
            // Cada ClientHandler que atén a un client té un session id
            if (res == CodiOperacio.KO.getNumVal()){
                // TODO: THROW EXCEPTION I TANCAR SOCKETS!!!!
                throw new Exception("Session ID erroni, avortem operació");
            }
            
            // sessionId vàlid, busquem taula seleccionada en la BD
            numTaula = ois.readInt();
            
            taulaSeleccionada = dbManager.getTaulaSeleccionada(numTaula);

            if (taulaSeleccionada == null)
            {
                res = CodiOperacio.KO.getNumVal();
            }
            
            // enviem ok si taula trobada, ko si no trobada
            oos.writeInt(res);
            oos.flush();
            if (res == CodiOperacio.KO.getNumVal())
            {
                throw new Exception("Taula no trobada en la BD, avortem operació");                
            }
            
            // Taula trobada, l'enviem
            oos.writeObject(taulaSeleccionada);
            oos.flush();

            // llegim ok
            ois.read();

            System.out.println("Taula seleccionada enviada amb exit");
        } catch (Exception ex) {
            System.out.println(ex);
            ex.printStackTrace();
        } finally {
            try {
                oos.close();
                ois.close();
                socket.close();
            } catch (IOException ex) {
                System.out.println(ex);
                ex.printStackTrace();
            }
        }

        
    }
    
    public void getCarta()
    {
        List<Categoria> categories = new ArrayList<>();
        List<Plat> plats = new ArrayList<>();
        LoginTuple loginTuple;
        int res;
        
        try {
            // llegim sessionId (logintuple)
            System.out.println("llegint logintuple");
            loginTuple = (LoginTuple)ois.readObject();
            System.out.println("sessionId llegit = "+loginTuple.getSessionId());

            // enviem resposta: procedirem si session id existeix en el server actualment, altrament avortarem
            if (server.sessionIdExists(loginTuple.getSessionId()))
                res = CodiOperacio.OK.getNumVal();
            else
                res = CodiOperacio.KO.getNumVal();
            
            oos.writeInt(res);
            oos.flush();
            
            // Si el session id del client no coincideix amb el del client actual, avortem operació
            // Cada ClientHandler que atén a un client té un session id
            if (res == CodiOperacio.KO.getNumVal()){
                System.out.println("Session ID erroni, avortem operació");
                return;
            }
            
            // 1 - get categories------------------------------------------------------------------
            
            // sessionId vàlid, llegim info taules de la BD
            categories = dbManager.getCategories(); // TODO: es podria fer amb un singleton

            // Enviem tamany de l'arraylist infoTaules
            oos.writeInt(categories.size());
            oos.flush();
            
            // Enviem arraylist element a element
            for(Categoria cat : categories)
            {
                System.out.println(cat);
                oos.writeObject(cat);
                oos.flush();
                
                // llegim ok
                ois.read();
            }
            System.out.println("Categories enviades amb exit");


            // 2 - get plats------------------------------------------------------------------
            
            // sessionId vàlid, llegim info taules de la BD
            plats = dbManager.getPlats(); // TODO: es podria fer amb un singleton

            // Enviem tamany de l'arraylist infoTaules
            oos.writeInt(plats.size());
            oos.flush();
            
            // Enviem arraylist element a element
            for(Plat plat : plats)
            {
                System.out.println(plat);
                oos.writeObject(plat);
                oos.flush();
                
                // llegim ok
                ois.read();
            }
            System.out.println("Plats enviades amb exit");

        } catch (IOException | ClassNotFoundException ex) {
            System.out.println(ex);
            ex.printStackTrace();
        } finally {
            try {
                oos.close();
                ois.close();
                socket.close();
            } catch (IOException ex) {
                System.out.println(ex);
                ex.printStackTrace();
            }
        }

        
    }

    
    
    // TODO: get Comanda

    
    
    
    // TODO!!!: MUTEX!!!!!!
    public void createComanda()
    {
        CreateComandaTuple createComandaTuple;
        LoginTuple loginTuple;
        Comanda comanda = null;
        Long nouCodi = null;
        int res;
        
        try {
            // llegim sessionId (logintuple)
            System.out.println("llegint logintuple");
            loginTuple = (LoginTuple)ois.readObject();
            System.out.println("sessionId llegit = "+loginTuple.getSessionId());

            // enviem resposta: procedirem si session id existeix en el server actualment, altrament avortarem
            if (server.sessionIdExists(loginTuple.getSessionId()))
                res = CodiOperacio.OK.getNumVal();
            else
                res = CodiOperacio.KO.getNumVal();
            
            oos.writeInt(res);
            oos.flush();
            
            // Si el session id del client no coincideix amb el del client actual, avortem operació
            // Cada ClientHandler que atén a un client té un session id
            if (res == CodiOperacio.KO.getNumVal()){
//                System.out.println("Session ID erroni, avortem operació");
//                return;
                throw new Exception("Session ID erroni, avortem operació");
            }
            
            // 1 - createComanda------------------------------------------------------------------
            
            // sessionId vàlid, llegim CreateComandaTuple
            createComandaTuple = (CreateComandaTuple)ois.readObject();

            // inicialment no donem codi a la comanda, ens el donarà la BD
            try{
                comanda = new Comanda(0, new Date(), createComandaTuple.getTaula(), loginTuple.getCambrer(), false);

                // dbManager retornarà -1 si no ha pogut fer l'insert
                nouCodi = dbManager.insertComanda(comanda, createComandaTuple.getLinies());
            
            } catch(CookomaticException ex){
                // taula ja te comanda activa, retornem codi null
                System.out.println(ex.getMessage());
                System.out.println("TAULA JA TENIA COMANDA. NOU CODI = -1");
                nouCodi = (long)-1;
            }
            
//            if (nouCodi == -1)
//            {
//                throw new Exception("CH: ERROR EN INSERT NOVA COMANDA");
//            }
            
            // Enviem codi de nova comanda: pot ser -1: comanda no inserida amb exit
            oos.writeLong(nouCodi);
            oos.flush();

            // llegim ok
            ois.read();
            
            if (nouCodi != -1)
                System.out.println("Comanda inserida amb èxit");
            else
                System.out.println("ERror en inserir comanda");
        } catch (Exception ex) {
            System.out.println(ex);
            ex.printStackTrace();
        } finally {
            try {
                oos.close();
                ois.close();
                socket.close();
            } catch (IOException ex) {
                System.out.println(ex);
                ex.printStackTrace();
            }
        }

        
    }


    public void buidarTaula()
    {
        InfoTaula infoTaula;
        LoginTuple loginTuple;
        int res;
        
        try {
            // llegim sessionId (logintuple)
            System.out.println("llegint logintuple");
            loginTuple = (LoginTuple)ois.readObject();
            System.out.println("sessionId llegit = "+loginTuple.getSessionId());

            // enviem resposta: procedirem si session id existeix en el server actualment, altrament avortarem
            if (server.sessionIdExists(loginTuple.getSessionId()))
                res = CodiOperacio.OK.getNumVal();
            else
                res = CodiOperacio.KO.getNumVal();
            
            oos.writeInt(res);
            oos.flush();
            
            // Si el session id del client no coincideix amb el del client actual, avortem operació
            // Cada ClientHandler que atén a un client té un session id
            if (res == CodiOperacio.KO.getNumVal()){
//                System.out.println("Session ID erroni, avortem operació");
//                return;
                throw new Exception("Session ID erroni, avortem operació");
            }
            
            // buidarTaula------------------------------------------------------------------
            
            // sessionId vàlid, llegim infoTaula
            infoTaula = (InfoTaula)ois.readObject();

            res = dbManager.buidarTaula(infoTaula.getNumero());
            
            // Enviem resposta
            oos.writeInt(res);
            oos.flush();

        } catch (Exception ex) {
            System.out.println(ex);
            ex.printStackTrace();
        } finally {
            try {
                oos.close();
                ois.close();
                socket.close();
            } catch (IOException ex) {
                System.out.println(ex);
                ex.printStackTrace();
            }
        }

        
    }
    
    
    
    
    
    
    
    
    private void tancarClientHandler() {
        try {
            // closing resources
            System.out.println("Closing this connection.");
            this.socket.close();
            this.ois.close();
            this.oos.close();
            System.out.println("Connection closed");
            fiConnexio = true;

            // ara eliminem aquest registre de la llista de clienthandlers del servidor
            server.removeClientHandler(this);

            // tanquem connexió a la BD
            this.dbManager.tancarDBManager();
            
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
