package com.Javacoin.JavacoinApp.services;

import com.Javacoin.JavacoinApp.exceptions.NegocioExcepcion;
import com.Javacoin.JavacoinApp.models.Cuenta;
import com.Javacoin.JavacoinApp.models.Operacion;
import com.Javacoin.JavacoinApp.models.Orden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class BancoService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BancoService.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    private List<Operacion> solicitudesOperacion = new ArrayList<Operacion>();
    private HashMap<Long, Cuenta> cuentas = new HashMap<Long, Cuenta>();

    @Value("${javacoin.exchange.respuesta}")
    private String respuestaExchange;

    @RabbitListener(id = "bancoListener", queues = {"${javacoin.colas.bancoQueue}"})
    @Transactional
    public void consumirSolicitud(Operacion operacion) throws NegocioExcepcion {

        try {
            if (operacion.getTipo() == Operacion.Tipo.COMPRA) {
                procesarCompra(operacion);
            }
        } catch (NegocioExcepcion e) {
            enviarErrorAUsuario(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(id = "operacionListener", queues = {"${javacoin.colas.ordenQueue}"})
    @Transactional
    private void procesarOperacion(Orden orden) {

        //obtener dni comprador y vendedor
        long dniComprador = orden.getDniComprador();
        long dniVendedor = orden.getDniVendedor();
        //obtener cuenta comprador y vendedor
        Cuenta cuentaComprador = cuentas.get(dniComprador);
        Cuenta cuentaVendedor = cuentas.get(dniVendedor);

        //transferir dolares retenidos de comprador a vendedor
        double dolaresATransferir = cuentaComprador.liberarDolaresRetenidos();
        double comisionComprador = calcularComision(cuentaComprador);
        double comisionVendedor = calcularComision(cuentaVendedor)-1;

        double montoComision = dolaresATransferir*comisionComprador-dolaresATransferir;
        cuentaComprador.disminuir(montoComision);

        double dolaresRealesAAcreditarVendedor = dolaresATransferir - (dolaresATransferir * comisionVendedor);
        cuentaVendedor.aumentar(dolaresRealesAAcreditarVendedor);

        cuentaComprador.incrementarNroOperacionesCuenta();
        cuentaVendedor.incrementarNroOperacionesCuenta();

        rabbitTemplate.convertAndSend(respuestaExchange,"respuesta",String.format("Venta realizada del vendedor %s al comprador %s",orden.getDniVendedor(),orden.getDniComprador()));
    }


    private void procesarCompra(Operacion operacion) throws NegocioExcepcion {
        LOGGER.info(String.format("solicitud compra recibida por BancoService -> %s", operacion.toString()));

        //valida existencia usuario
        validarUsuario(operacion);
        Cuenta cuentaComprador = cuentas.get(operacion.getDniComprador());
        //valida que el saldo en la cuenta sea correcto para realizar la operacion
        validarSaldoCuenta(cuentaComprador, operacion);
        //retiene el saldo en la cuenta
        retenerSaldoCuenta(cuentaComprador, operacion);
        solicitudesOperacion.add(operacion);
    }



    private void retenerSaldoCuenta(Cuenta cuentaUsuario, Operacion operacion) {
        double montoOperacion = operacion.getMontoOperacion();
        cuentaUsuario.retenerDolares(montoOperacion);
    }

    private void enviarErrorAUsuario(String mensajeError) {
        rabbitTemplate.convertAndSend(respuestaExchange,"error", mensajeError);
    }

    private void validarSaldoCuenta(Cuenta cuentaUsuario, Operacion operacion) throws NegocioExcepcion {

        double dolaresCuenta = cuentaUsuario.getDolares();
        double montoOperacion = operacion.getMontoOperacion();
        double comision = calcularComision(cuentaUsuario);

        if (!(dolaresCuenta >= (montoOperacion * comision))) {
            throw new NegocioExcepcion("La cuenta no posee el saldo suficiente para realizar la operacion");
        }
    }

    private double calcularComision(Cuenta cuenta) {
        int cantOperaciones = cuenta.getCantOperaciones();
        return cantOperaciones < 3 ? 1.05 : (cantOperaciones <= 6 ? 1.03 : 1.0);
    }


    private void validarUsuario(Operacion solicitud) throws NegocioExcepcion {

        if (!cuentas.containsKey(solicitud.getDniComprador())) {
            throw new NegocioExcepcion("El usuario no tiene cuenta en el banco");
        }
    }

    public void crearCuenta(long dniUsuario, double dolares) {
        cuentas.put(dniUsuario, new Cuenta(dniUsuario, dolares));
    }

    public Cuenta getCuentaUsuario(long dniUsuario){
        return this.cuentas.get(dniUsuario);
    }

    public int cantSolicitudesCompraRecibidas() {
        return solicitudesOperacion.size();
    }

    public void limpiarLista(){
        solicitudesOperacion.clear();
    }

}
