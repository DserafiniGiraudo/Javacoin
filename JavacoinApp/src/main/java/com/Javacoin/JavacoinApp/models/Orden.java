package com.Javacoin.JavacoinApp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.ThreadLocalRandom;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Orden {

    private long nroOrden;
    private long dniComprador;

    private long dniVendedor;
    private Estado estadoOrden;

    private double javacoins;

    public Orden(long nroOrden,long dniComprador,double javacoins) {
        this.nroOrden = nroOrden;
        this.dniComprador = dniComprador;
        this.estadoOrden = Estado.PENDIENTE;
        this.javacoins = javacoins;
    }

    public void ejecutar() {
        setEstadoOrden(Estado.PROCESADO);
    }


    public enum Estado{
        PENDIENTE,PROCESADO
    }

}
