package com.Javacoin.JavacoinApp.models;

import lombok.Data;

@Data
public class Cuenta {

    private long dniUsuario;
    private double dolares;
    private int cantOperaciones = 0;

    private double dolaresRetenidos;


    public Cuenta(long dniUsuario, double dolares) {
        this.dniUsuario = dniUsuario;
        this.dolares = dolares;
        this.cantOperaciones = 0;
    }

    public void aumentarOperaicones(){cantOperaciones++;}

    public void disminuir(double saldo){
        dolares -= saldo;
    }

    public void aumentar(double saldo){
        dolares += saldo;
    }

    public void retenerDolares(double dolaresARetener){
        disminuir(dolaresARetener);
        setDolaresRetenidos(dolaresARetener);
    }

    public void incrementarNroOperacionesCuenta(){
        cantOperaciones++;
    }

    public double liberarDolaresRetenidos() {
        double dolares = dolaresRetenidos;
        setDolaresRetenidos(0);
        return dolares;
    }
}
