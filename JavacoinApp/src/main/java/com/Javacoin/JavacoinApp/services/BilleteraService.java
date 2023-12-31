package com.Javacoin.JavacoinApp.services;

import com.Javacoin.JavacoinApp.exceptions.NegocioExcepcion;
import com.Javacoin.JavacoinApp.models.Billetera;
import com.Javacoin.JavacoinApp.models.Operacion;
import com.Javacoin.JavacoinApp.models.Orden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class BilleteraService {
    Logger LOGGER = LoggerFactory.getLogger(BilleteraService.class);

    @Autowired
    RabbitTemplate rabbitTemplate;


    @Value("${javacoin.colas.respuestaQueue}")
    private String respuestaQueue;

    @Value("${javacoin.colas.ordenQueue}")
    private String ordenQueue;

    @Value("${javacoin.colas.requestUsuarioQueue}")
    private String requestUsuarioQueue;
    @Value("${javacoin.colas.responseUsuarioQueue}")
    private String responseUsuarioQueue;

    @Value("${javacoin.exchange.respuesta}")
    private String respuestaExchange;

    @Value("${javacoin.exchange.banco}")
    private String bancoExchange;

    private List<Operacion> solicitudesOperacion = new ArrayList<>();
    HashMap<Long, Billetera> billeteras = new HashMap<>();
    HashMap<Long, Orden> ordenes = new HashMap<>();

    @RabbitListener(id = "billeteraListener", queues = {"${javacoin.colas.billeteraQueue}"})
    public void consumirSolicitudCompra(Operacion operacion) throws NegocioExcepcion {

        try {
            switch (operacion.getTipo()) {
                case COMPRA -> procesarCompra(operacion);
                case VENTA -> procesarVenta(operacion);
            }
        } catch (NegocioExcepcion e) {
            enviarErrorAUsuario(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void procesarCompra(Operacion operacion) {
        LOGGER.info(String.format("compra recibida por BilleteraService -> %s", operacion.toString()));


//        rabbitTemplate.convertAndSend(requestUsuarioQueue,operacion.getDniComprador());
        rabbitTemplate.convertAndSend(bancoExchange,"request",operacion.getDniComprador());
        Message receive = rabbitTemplate.receive(responseUsuarioQueue,3000);
        if(receive != null){
            byte[] cuerpoMensaje = receive.getBody();
            boolean existencia = Boolean.parseBoolean(new String(cuerpoMensaje));

            if(existencia){
                validarExistenciaBilletera(operacion);
                generarOrden(operacion);
                solicitudesOperacion.add(operacion);
            }
        }
    }

    private void procesarVenta(Operacion operacion) throws NegocioExcepcion {

        long nroOrden = operacion.getNroOrden();
        long dniVendedor = operacion.getDniVendedor();

        ordenes.keySet().stream()
                .filter(orden -> orden == nroOrden)
                .findFirst()
                .orElseThrow(() -> new NegocioExcepcion("No existe una orden con el numero solicitado"));

        Orden orden = ordenes.get(nroOrden);
        orden.setDniVendedor(dniVendedor);
        long dniComprador = orden.getDniComprador();
        orden.setEstadoOrden(Orden.Estado.PROCESADO);
        Billetera billeteraComprador = billeteras.get(dniComprador);
        Billetera billeteraVendedor = billeteras.get(dniVendedor);

        billeteraVendedor.restarJavacoins(orden.getJavacoins());
        billeteraComprador.sumarJavacoins(orden.getJavacoins());

        rabbitTemplate.convertAndSend(ordenQueue, orden);
    }

    private void generarOrden(Operacion operacion) {
        long nroOrden = generarNroOrdenAleatorio();
        Orden ordenCreada = new Orden(nroOrden, operacion.getDniComprador(), operacion.getJavacoin());
        ordenes.put(nroOrden, ordenCreada);
//        rabbitTemplate.convertAndSend(respuestaQueue, String.format("Orden de compra creada - Número: %s", ordenCreada.getNroOrden()));
        rabbitTemplate.convertAndSend(respuestaExchange,"respuesta",String.format("Orden de compra creada - Número: %s", ordenCreada.getNroOrden()));
    }


    private long generarNroOrdenAleatorio() {
        long nroOrden;
        do {
            nroOrden = ThreadLocalRandom.current().nextLong();
        } while (ordenes.containsKey(nroOrden));
        return nroOrden;
    }

    private void validarExistenciaBilletera(Operacion operacion) {
        billeteras.values().stream()
                .filter(b -> b.getDniUsuario() == operacion.getDniComprador())
                .findFirst()
                .orElse(billeteras.put(operacion.getDniComprador(), new Billetera(operacion.getDniComprador())));
    }

    public void ejecutarOrden(long nroOrden) {
        Orden orden = ordenes.get(nroOrden);

         if (orden != null){
            orden.ejecutar();
            rabbitTemplate.convertAndSend("ordenQueue",orden);
        }else{
            throw new NoSuchElementException(String.format("No existe orden pendiente con el nro %s", nroOrden));
        }
    }

    public void crearBilletera(long dniUsuario, double javacoin) {
        billeteras.put(dniUsuario, new Billetera(dniUsuario, javacoin));
    }

    public int cantSolicitudesCompraRecibidas() {
        return solicitudesOperacion.size();
    }

    private void enviarErrorAUsuario(String mensajeError) {
        rabbitTemplate.convertAndSend("errorQueue", mensajeError);
    }

    public List<Long> getNroOrdenes(){
        return new ArrayList<>(this.ordenes.keySet());
    }

    public Orden getOrden(long nroOrden){
        return this.ordenes.get(nroOrden);
    }

    public Billetera getBilletera(long dniUsuario){
        return this.billeteras.get(dniUsuario);
    }

    public void limpiarLista(){
        solicitudesOperacion.clear();
    }
}
