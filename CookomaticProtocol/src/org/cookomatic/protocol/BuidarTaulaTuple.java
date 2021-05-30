/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cookomatic.protocol;

import java.io.Serializable;

/**
 *
 * @author Usuario
 */
public class BuidarTaulaTuple implements Serializable{
    private long sessionId;
    private int taula;

    public BuidarTaulaTuple(long sessionId, int taula) {
        setSessionId(sessionId);
        setTaula(taula);
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

    
}
