package com.Javacoin.JavacoinApp;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class Coordinador {

    private static final AtomicBoolean bancoServiceListo = new AtomicBoolean(false);
    private static final AtomicBoolean flagRollback = new AtomicBoolean(false);

    public static boolean getBancoServiceListo() {
        return bancoServiceListo.get();
    }

    public static void setBancoServiceListo(boolean listo) {
        bancoServiceListo.set(listo);
    }

    public static boolean debeHacerRollback() {
        return flagRollback.get();
    }

    public static void setRollbackFlag(boolean rollback) {
        flagRollback.set(rollback);
    }
}

