package com.Javacoin.JavacoinApp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class Operacion {

    private long dniComprador;
    private long dniVendedor;
    private long nroOrden;
    private double javacoin;
    private double cotizacion;

    private Tipo tipo;

    public enum Tipo{
        COMPRA,VENTA
    }

    public double getMontoOperacion() {
        return javacoin * cotizacion;
    }
}
