package com.Javacoin.JavacoinApp;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${javacoin.colas.bancoQueue}")
    private String bancoQueue;

    @Value("${javacoin.colas.billeteraQueue}")
    private String billeteraQueue;

    @Value("${javacoin.colas.errorQueue}")
    private String errorQueue;

    @Value("${javacoin.colas.respuestaQueue}")
    private String respuestaQueue;

    @Value("${javacoin.colas.ordenQueue}")
    private String ordenQueue;

    @Value("${javacoin.exchange.transaccion}")
    private String transaccionExchange;

    @Value("${javacoin.exchange.respuesta}")
    private String respuestaExchange;

    @Bean
    public Queue bancoQueue(){
        return new Queue(bancoQueue);
    }
    @Bean
    public Queue billeteraQueue(){
        return new Queue(billeteraQueue);
    }
    @Bean
    public Queue errorQueue(){
        return new Queue(errorQueue);
    }
    @Bean
    public Queue ordenQueue(){
        return new Queue(ordenQueue);
    }
    @Bean
    public Queue respuestaQueue(){
        return new Queue(respuestaQueue);
    }
    @Bean
    public DirectExchange transaccionExchange(){
        return new DirectExchange(transaccionExchange);
    }
    @Bean
    public DirectExchange respuestaExchange(){
        return new DirectExchange(respuestaExchange);
    }
    @Bean
    public Binding bancoQueueBindingCompra(){
        return BindingBuilder
                .bind(bancoQueue())
                .to(transaccionExchange())
                .with("compra");
    }
    @Bean
    public Binding billeteraQueueBindingCompra() {
        return BindingBuilder
                .bind(billeteraQueue())
                .to(transaccionExchange())
                .with("compra");
    }
    @Bean
    public Binding billeteraQueueBindingVenta() {
        return BindingBuilder
                .bind(billeteraQueue())
                .to(transaccionExchange())
                .with("venta");
    }
    @Bean
    public Binding errorQueueBinding(){
        return BindingBuilder
                .bind(errorQueue())
                .to(respuestaExchange())
                .with("error");
    }
    @Bean
    public Binding respuestaQueueBinding(){
        return BindingBuilder
                .bind(respuestaQueue())
                .to(respuestaExchange())
                .with("respuesta");
    }
    @Bean
    public MessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory cf){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(cf);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}
