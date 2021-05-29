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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cookomatic.jdbc.DBManager;
import org.cookomatic.model.sala.Cambrer;
import org.cookomatic.protocol.LoginTuple;

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
    private long sessionId;
    private CookomaticServer server;
    private DBManager dbManager;

    private boolean fiConnexio;

    // Constructor
    public ClientHandler(Socket socket, ObjectInputStream ois, ObjectOutputStream oos, long sessionId, CookomaticServer server, DBManager dbManager) {
        this.socket = socket;
        this.ois = ois;
        this.oos = oos;
        this.sessionId = sessionId;
        this.fiConnexio = false;
        this.server = server; // passem referència per esborrar clientHandler de la list de clientHandlers que té el servidor
        this.dbManager = dbManager;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void run() {
        int codiOperacio = 0;

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
                        // TODO userLogin();
                        break;
                    case 2:
                        System.out.println("User asked for GetTaules");
                        break;
                    case 3:
                        System.out.println("User asked for GetCarta");
                        break;
                    case 4:
                        System.out.println("User asked for GetComanda");
                        break;
                    case 5:
                        System.out.println("User asked for CreateComanda");
                        break;
                    case 6:
                        System.out.println("User asked for BuidarTaula");
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

    public void userLogin_COPY() {
        LoginTuple lt = null;

        try {
            // llegim login
            String login = ois.readUTF();
            // enviem ok
            oos.writeInt(1);
            oos.flush();

            // llegim passwd
            String password = ois.readUTF();
            // enviem ok
            oos.writeInt(1);
            oos.flush();

            lt = new LoginTuple(login, password, null);
            // client envia inicialment sense sessionId, som nosaltres qui l'hi donarem

            // prova
            boolean credencialsCorrectes = lt.getUser().equalsIgnoreCase(lt.getPassword());
            int res = credencialsCorrectes ? 1 : 0;

            // enviem 1 si ok, 0 si no ok
            System.out.println("Enviant resposta");
            oos.flush();
            oos.writeInt(res);
//            oos.writeObject(res);
            System.out.println("Resposta enviada");

            // si la resposta ha estat ok, també enviem tupla amb session_id i dades usu
            if (credencialsCorrectes) {
                lt.setSessionId(sessionId);
//                oos.writeObject(lt);

                // enviem login
                oos.writeUTF(login);
                oos.flush();
                // llegim ok
                ois.readInt();

                // enviem login
                oos.writeUTF(password);
                oos.flush();
                // llegim ok
                ois.readInt();

                // enviem login
                oos.writeLong(lt.getSessionId());
                oos.flush();
                // llegim ok
                ois.readInt();

            }
        } catch (IOException ex) {
            System.out.println(ex);
            ex.printStackTrace();
        }

    }

    public void userLogin() {
        LoginTuple lt = null;

        try {
            // llegim loginTuple
            System.out.println("llegint logintuple");
            lt = (LoginTuple) ois.readObject();
            System.out.println("logintuple rebuda: " + lt.getUser() + "/" + lt.getPassword());
            // enviem ok
            oos.writeInt(1);
            oos.flush();

            // client envia inicialment sense sessionId, som nosaltres qui l'hi donarem
            // Comprovació que la contrasenya introduida per l'usuari és la correcta
            Cambrer c = dbManager.getCambrerPerUser(lt.getUser());

            // prova
//            boolean credencialsCorrectes = lt.getUser().equalsIgnoreCase(lt.getPassword());
            if (c != null) {
                boolean credencialsCorrectes = lt.getPassword().equalsIgnoreCase(c.getPassword());
                int res = credencialsCorrectes ? 1 : 0;

                // enviem 1 si ok, 0 si no ok
                System.out.println("Enviant resposta");
                oos.flush();
                oos.writeInt(res);
//            oos.writeObject(res);
                System.out.println("Resposta enviada");

                // si la resposta ha estat ok, també enviem tupla amb session_id i dades usu
                if (credencialsCorrectes) {
                    lt.setSessionId(sessionId);
//                oos.writeObject(lt);

                    System.out.println("enviant lt amb sessionId");
                    oos.writeObject(lt);
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

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
