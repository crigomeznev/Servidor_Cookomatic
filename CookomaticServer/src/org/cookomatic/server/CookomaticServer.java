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
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static ServerSocket server;
    //socket server port on which it will listen
    private static int port = 9876;
    
    
    // TODO: llista de sockets i clients, per ara provem amb 1 client unic
    Socket socket;
    ObjectInputStream ois;
    ObjectOutputStream oos;
    
    private static long sessionCount;
    
    
    public static void main(String args[]) throws IOException, ClassNotFoundException{
        CookomaticServer cs = new CookomaticServer();

        //create the socket server object
        server = new ServerSocket(port);
        
        //keep listens indefinitely until receives 'exit' call or program terminates
        while(true){
            System.out.println("Waiting for the client request");
            //creating socket and waiting for client connection
            cs.socket = server.accept();
            // TODO: threads per atendre clients
            sessionCount++;

            cs.iniConnection();
            
            
            // TODO switch amb codis de peticions
            cs.userLogin();

            cs.closeConnection();
        }
//        System.out.println("Shutting down Socket server!!");
        //close the ServerSocket object
//        server.close();
    }    



    /* Protocol
        
        Petició: CODI + TUPLA
    
        Resposta: CODI_RESPOSTA (1 o 0) [+ TUPLA]
    */
    

    //  Mètodes
    public void iniConnection(){
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
    
    
    
    public void userLogin(){
        LoginTuple lt = null;

        try {
            lt = (LoginTuple)ois.readObject();
            // client envia inicialment sense sessionId, som nosaltres qui l'hi donarem
            
            // prova
            boolean credencialsCorrectes = lt.getUser().equalsIgnoreCase(lt.getPassword());
            int res = credencialsCorrectes? 1 : 0;

            // enviem 1 si ok, 0 si no ok
            oos.writeInt(res);
            
            // si la resposta ha estat ok, també enviem tupla amb session_id i dades usu
            if (credencialsCorrectes) {
                lt.setSessionId(sessionCount);
                oos.writeObject(lt);
            }
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println(ex);
            ex.printStackTrace();
        }
        
    }
    
    
    public void closeConnection(){
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
    

}
