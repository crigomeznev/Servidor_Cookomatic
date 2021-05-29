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
public enum CodiOperacio {
    KO              (-1),
    OK              (0),
    
    LOGIN           (1),
    GET_TAULES      (2),
    GET_CARTA       (3),
    GET_COMANDA     (4),
    CREATE_COMANDA  (5),
    BUIDAR_TAULA    (6);

    //--------------------------------------------------------------------
    private int numVal;
    
    CodiOperacio(int numVal){
        this.numVal = numVal;
    }

    public int getNumVal(){
        return numVal;
    }
}
