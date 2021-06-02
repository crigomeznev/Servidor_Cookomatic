/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cookomatic.protocol;

import java.io.Serializable;
import org.cookomatic.model.sala.Cambrer;

/**
 *
 * @author Usuario
 */
public class LoginTuple implements Serializable {
    private Cambrer cambrer;
    private Long sessionId;

    public LoginTuple(Cambrer cambrer, Long sessionId) {
        this.cambrer = cambrer;
        this.sessionId = sessionId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Cambrer getCambrer() {
        return cambrer;
    }

    public void setCambrer(Cambrer cambrer) {
        this.cambrer = cambrer;
    }
    
}
