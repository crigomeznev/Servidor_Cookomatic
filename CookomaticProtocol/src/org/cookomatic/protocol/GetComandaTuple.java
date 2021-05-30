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
public class GetComandaTuple implements Serializable{
    private long sessionId;
    private long codiComanda;

    public GetComandaTuple(long sessionId, long codiComanda) {
        setSessionId(sessionId);
        setCodiComanda(codiComanda);
    }
    
    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public long getCodiComanda() {
        return codiComanda;
    }

    public void setCodiComanda(long codiComanda) {
        this.codiComanda = codiComanda;
    }
    
    
}
