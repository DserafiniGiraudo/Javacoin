package com.Javacoin.JavacoinApp.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Billetera {

    private long dniUsuario;
    private double javacoin;

    public Billetera(long dniUsuario) {
        this.dniUsuario = dniUsuario;
        this.javacoin = 0;
    }

    public void restarJavacoins(double javacoin){
        this.javacoin -= javacoin;
    }

    public void sumarJavacoins(double javacoin){
        this.javacoin += javacoin;
    }
}
