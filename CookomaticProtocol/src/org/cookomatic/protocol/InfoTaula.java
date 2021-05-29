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
public class InfoTaula implements Serializable {
    private int numero;
    private Long codiComanda; // pot ser null!
    private boolean esMeva;
    private int platsTotals;
    private int platsPreparats;
    private String nomCambrer; // user de la BD

    public InfoTaula(int numero, Long codiComanda, boolean esMeva, int platsTotals, int platsPreparats, String nomCambrer) {
        this.numero = numero;
        this.codiComanda = codiComanda;
        this.esMeva = esMeva;
        this.platsTotals = platsTotals;
        this.platsPreparats = platsPreparats;
        this.nomCambrer = nomCambrer;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public Long getCodiComanda() {
        return codiComanda;
    }

    public void setCodiComanda(Long codiComanda) {
        this.codiComanda = codiComanda;
    }

    public boolean isEsMeva() {
        return esMeva;
    }

    public void setEsMeva(boolean esMeva) {
        this.esMeva = esMeva;
    }

    public int getPlatsTotals() {
        return platsTotals;
    }

    public void setPlatsTotals(int platsTotals) {
        this.platsTotals = platsTotals;
    }

    public int getPlatsPreparats() {
        return platsPreparats;
    }

    public void setPlatsPreparats(int platsPreparats) {
        this.platsPreparats = platsPreparats;
    }

    public String getNomCambrer() {
        return nomCambrer;
    }

    public void setNomCambrer(String nomCambrer) {
        this.nomCambrer = nomCambrer;
    }



}
