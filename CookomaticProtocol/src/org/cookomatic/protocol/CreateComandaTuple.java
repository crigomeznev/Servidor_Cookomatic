/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cookomatic.protocol;

/**
 *
 * @author Usuario
 */
public class CreateComandaTuple {
    private long sessionId;
    private int taula;
    private int numLiniesComanda;
    // TODO Array de linia comanda

    public CreateComandaTuple(long sessionId, int taula, int numLiniesComanda) {
        setSessionId(sessionId);
        setTaula(taula);
        setNumLiniesComanda(numLiniesComanda);
    }
    
    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public int getTaula() {
        return taula;
    }

    public void setTaula(int taula) {
        this.taula = taula;
    }

    public int getNumLiniesComanda() {
        return numLiniesComanda;
    }

    public void setNumLiniesComanda(int numLiniesComanda) {
        this.numLiniesComanda = numLiniesComanda;
    }

    
}
