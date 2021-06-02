/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cookomatic.protocol;

import java.io.Serializable;
import java.util.List;
import org.cookomatic.model.sala.LiniaComanda;
import org.cookomatic.model.sala.Taula;

/**
 *
 * @author Usuario
 */
public class CreateComandaTuple implements Serializable{
    private long sessionId;
    private Taula taula;
    private int numLiniesComanda;
    private List<LiniaComanda> linies;

    public CreateComandaTuple(long sessionId, Taula taula, int numLiniesComanda, List<LiniaComanda> linies) {
        setSessionId(sessionId);
        setTaula(taula);
        setNumLiniesComanda(numLiniesComanda);
        this.linies = linies;
    }
    
    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public Taula getTaula() {
        return taula;
    }

    public void setTaula(Taula taula) {
        this.taula = taula;
    }

    public int getNumLiniesComanda() {
        return numLiniesComanda;
    }

    public void setNumLiniesComanda(int numLiniesComanda) {
        this.numLiniesComanda = numLiniesComanda;
    }

    public List<LiniaComanda> getLinies() {
        return linies;
    }

    public void setLinies(List<LiniaComanda> linies) {
        this.linies = linies;
    }

    
}
