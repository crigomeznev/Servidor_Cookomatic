/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cookomatic.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cookomatic.jdbc.DBManager;
import org.cookomatic.protocol.LoginTuple;

/**
 *
 * @author Usuari
 */
public class CookomaticServer {

    /**
     * @param args the command line arguments
     */
//static ServerSocket variable
    private static ServerSocket socketConnections;
    //socket server port on which it will listen
    private static int port = 9876;

    // TODO: llista de sockets i clients, per ara provem amb 1 client unic
    Socket socket;
    ObjectInputStream ois;
    ObjectOutputStream oos;

    private static long sessionCount;
    
    private List<Thread> clientHandlers;
    
    // Accés a la BD
    private DBManager dbManager;

    
    
    // Constructor
    public CookomaticServer(String nomFitxerPropietats) {
        this.dbManager = new DBManager(nomFitxerPropietats);
        this.clientHandlers = new ArrayList<>();

        try {
            //create the socket server object
            this.socketConnections = new ServerSocket(port);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    
    
    
    
    // Main: el traurem d'aquí posteriorment
    public static void main(String args[]) throws IOException, ClassNotFoundException {
        CookomaticServer cs = new CookomaticServer("connexioMySQL.properties");

        while (true) { // TODO: while ! tancar servidor
            Socket s = null;

            try {
                // socket object to receive incoming client requests
                System.out.println("Esperant clients...");
                s = socketConnections.accept();

                System.out.println("A new client is connected : " + s);

                // obtaining input and out streams
//                DataInputStream dis = new DataInputStream(s.getInputStream());
//                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                
                System.out.println("Assigning new thread for this client");

                // create a new thread object
//                sessionCount++;
                ClientHandler ch = new ClientHandler(s, ois, oos, sessionCount++, cs, cs.dbManager);
                cs.clientHandlers.add(ch);
                // ara donem session id al thread
//                ch.setSessionId(client);
                
                // Invoking the start() method
                ch.start();

            } catch (Exception e) {
                s.close();
                e.printStackTrace();
            }
        }
        // TODO: tancar el server i el dbmanager
//        cs.dbManager.tancarDBManager();

//        System.out.println("Shutting down Socket server!!");
        //close the ServerSocket object
//        server.close();
    }

    /* Protocol
        
        Petició: CODI + TUPLA
    
        Resposta: CODI_RESPOSTA (1 o 0) [+ TUPLA]
     */
    //  Mètodes
    public void iniConnection() {
        try {
            //read from socket to ObjectInputStream object
            ois = new ObjectInputStream(socket.getInputStream());
            //create ObjectOutputStream object
            oos = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            System.out.println(ex);
            ex.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            //close resources
            ois.close();
            oos.close();
            socket.close();
        } catch (IOException ex) {
            System.out.println(ex);
            ex.printStackTrace();
        }

    }
    
    
    
    public void removeClientHandler(ClientHandler ch){
        
        // TODO: afegir mutex
        if (ch.socket.isClosed())
            this.clientHandlers.remove(ch);
        else
            System.out.println("No es pot eliminar clientHandler perque socket no està tancat");
    }
    
    
    
    
    

}





