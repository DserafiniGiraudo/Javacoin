package com.Javacoin.JavacoinApp.services;

import com.Javacoin.JavacoinApp.models.Operacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class ProductorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductorService.class);


    @Value("${javacoin.exchange.transaccion}")
    private String exchange;

    @Autowired
    RabbitTemplate rabbitTemplate;



    public void comprarJavacoin(Operacion operacion){
        operacion.setTipo(Operacion.Tipo.COMPRA);
        LOGGER.info(String.format("Operacion de compra de %s javacoins con cotizacion %s enviada por usuario %s",operacion.getJavacoin(),operacion.getCotizacion(), operacion.getDniComprador()));
        rabbitTemplate.convertAndSend(exchange,"compra", operacion);
    }

    public void venderJavacoin(Operacion operacion){
        operacion.setTipo(Operacion.Tipo.VENTA);
        LOGGER.info(String.format("aceptacion de orden %s enviada por vendedor %s", operacion.getNroOrden(),operacion.getDniVendedor()));
        LOGGER.info(String.format("Orden %s aceptada por usuario %s",operacion.getNroOrden(), operacion.getDniVendedor()));
        rabbitTemplate.convertAndSend(exchange,"venta", operacion);
    }

    @RabbitListener(queues = "${javacoin.colas.respuestaQueue}")
    public void respuestaApi(String mensaje){
        LOGGER.info(mensaje);
    }

    @RabbitListener(queues = "${javacoin.colas.errorQueue}")
    public void errorApi(String mensaje){
        LOGGER.error(mensaje);
    }

}
