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
    KO                      (-1),
    OK                      (0),
    
    LOGIN                   (1),
    
    GET_TAULES              (2),
    GET_CARTA               (3),
    GET_COMANDA             (4),
    CREATE_COMANDA          (5),
    BUIDAR_TAULA            (6),
    GET_TAULA_SELECCIONADA  (7),

    LOGOUT                  (10);

    //--------------------------------------------------------------------
    private int numVal;
    
    CodiOperacio(int numVal){
        this.numVal = numVal;
    }

    public int getNumVal(){
        return numVal;
    }
    
    public static CodiOperacio getCodiFromVal (int val) {
        CodiOperacio co;
        
        switch(val){
            case -1: co = KO; break;
            case 0: co = OK; break;
            case 1: co = LOGIN; break;
            case 2: co = GET_TAULES; break;
            case 3: co = GET_CARTA; break;
            case 4: co = GET_COMANDA; break;
            case 5: co = CREATE_COMANDA; break;
            case 6: co = BUIDAR_TAULA; break;
            case 7: co = GET_TAULA_SELECCIONADA; break;
            case 10: co = LOGOUT; break;
            default: co = KO;
        }
        
        return co;
    }    
}
