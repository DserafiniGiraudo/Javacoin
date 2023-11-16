package com.Javacoin.JavacoinApp;

import com.Javacoin.JavacoinApp.models.Billetera;
import com.Javacoin.JavacoinApp.models.Cuenta;
import com.Javacoin.JavacoinApp.models.Operacion;
import com.Javacoin.JavacoinApp.models.Orden;
import com.Javacoin.JavacoinApp.services.BancoService;
import com.Javacoin.JavacoinApp.services.BilleteraService;
import com.Javacoin.JavacoinApp.services.ProductorService;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = {
        "classpath:application-test.properties",
        "classpath:application.properties",
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JavacoinAppApplicationTests {

    @Container
    static RabbitMQContainer mqContainer = new RabbitMQContainer("rabbitmq:management");

    @Autowired
    ProductorService compradorService;

    @Autowired
    BancoService bancoService;

    @Autowired
    BilleteraService billeteraService;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
    @Autowired
    private Queue bancoQueue;

    @Autowired
    private Queue billeteraQueue;

    @Autowired
    private Queue errorQueue;

    @Autowired
    private Queue respuestaQueue;

    @Autowired
    private Queue ordenQueue;

    @Autowired
    private Queue requestUsuarioQueue;

    @Autowired
    private Queue responseUsuarioQueue;
    @Value("${javacoin.servicios.tiempoEspera}")
    private int tiempoEspera;

    private long nroOrden;

    private long dniComprador;

    private Operacion operacion;

    @BeforeAll
    void init(){
        bancoService.crearCuenta(12345678L,50);
        bancoService.crearCuenta(87654321L,0);
        billeteraService.crearBilletera(87654321L,20);

        operacion = new Operacion();
        operacion.setDniComprador(12345678L);
        operacion.setDniVendedor(87654321L);
        operacion.setJavacoin(2.0);
        operacion.setCotizacion(5.0);
        operacion.setTipo(Operacion.Tipo.COMPRA);

    }

    @BeforeEach
    void setUp() {
        limpiarTodasLasColas();
        bancoService.limpiarLista();
        billeteraService.limpiarLista();
        stopBilleteraListener();
        stopBancoListener();
    }

    private void limpiarTodasLasColas() {
        rabbitAdmin.purgeQueue(bancoQueue.getName());
        rabbitAdmin.purgeQueue(billeteraQueue.getName());
        rabbitAdmin.purgeQueue(errorQueue.getName());
        rabbitAdmin.purgeQueue(respuestaQueue.getName());
        rabbitAdmin.purgeQueue(ordenQueue.getName());
        rabbitAdmin.purgeQueue(requestUsuarioQueue.getName());
        rabbitAdmin.purgeQueue(responseUsuarioQueue.getName());
    }

    @Test
    void contextLoads() {
        assertThat(mqContainer.isCreated()).isTrue();
        assertThat(mqContainer.isRunning()).isTrue();
    }

    @Test
    void compradorEnviaOrdenCompra() {

//        stopBilleteraListener();
//        stopBancoListener();

        compradorService.comprarJavacoin(operacion);

        await()
                .atMost(Duration.ofSeconds(tiempoEspera))
                .until(() -> colaTieneMensajes(bancoQueue) && colaTieneMensajes(billeteraQueue));

        int cantMensajesBanco = cantidadMensajesCola(bancoQueue);
        int cantMensajesBilletera = cantidadMensajesCola(billeteraQueue);

        assertThat(cantMensajesBanco).isEqualTo(1);
        assertThat(cantMensajesBilletera).isEqualTo(1);

        startBancoListener();
        startBilleteraListener();

        await().atMost(tiempoEspera, SECONDS).until(() -> bancoService.cantSolicitudesCompraRecibidas() > 0 && billeteraService.cantSolicitudesCompraRecibidas() > 0);

        assertThat(bancoService.cantSolicitudesCompraRecibidas()).isEqualTo(1);
        assertThat(billeteraService.cantSolicitudesCompraRecibidas()).isEqualTo(1);
    }

    @Test
    void vendedorAceptaOrden() {

        compradorService.comprarJavacoin(operacion);

        await()
                .atMost(Duration.ofSeconds(tiempoEspera))
                .until(() -> colaTieneMensajes(bancoQueue) && colaTieneMensajes(billeteraQueue));

        startBancoListener();
        startBilleteraListener();
        startOperacionListener();

        await().atMost(tiempoEspera, SECONDS).until(() -> bancoService.cantSolicitudesCompraRecibidas() > 0 && billeteraService.cantSolicitudesCompraRecibidas() > 0);

        nroOrden = billeteraService.getNroOrdenes().stream().findFirst().get();
        Orden orden = billeteraService.getOrden(nroOrden);

        Operacion operacionVenta = new Operacion();
        operacionVenta.setNroOrden(nroOrden);
        operacionVenta.setDniVendedor(87654321L);
        operacionVenta.setTipo(Operacion.Tipo.VENTA);

        compradorService.venderJavacoin(operacionVenta);

        try{
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }



        Billetera billeteraVendedor = billeteraService.getBilletera(orden.getDniVendedor());
        Billetera billeteraComprador = billeteraService.getBilletera(orden.getDniComprador());

        assertThat(billeteraVendedor.getJavacoin()).isEqualTo(18);
        assertThat(billeteraComprador.getJavacoin()).isEqualTo(2);

        Cuenta cuentaVendedor = bancoService.getCuentaUsuario(orden.getDniVendedor());
        Cuenta cuentaComprador = bancoService.getCuentaUsuario(orden.getDniComprador());

        assertThat(cuentaVendedor.getDolares()).isEqualTo(9.5);
        assertThat(cuentaComprador.getDolares()).isEqualTo(39.5);

    }

    @Test
    void usuarioSinCuentaNoPuedeGenerarOrdenCompra() {

        operacion.setDniComprador(11122233);
        compradorService.comprarJavacoin(operacion);

        compradorService.comprarJavacoin(operacion);

        await()
            .atMost(Duration.ofSeconds(tiempoEspera))
            .until(() -> colaTieneMensajes(bancoQueue) && colaTieneMensajes(billeteraQueue));

        startBancoListener();
        startBilleteraListener();

        await().atMost(tiempoEspera, SECONDS).until(() -> true);

        assertThat(bancoService.cantSolicitudesCompraRecibidas()).isEqualTo(0);
        assertThat(billeteraService.cantSolicitudesCompraRecibidas()).isEqualTo(0);

    }

    private boolean colaTieneMensajes(Queue cola) {
        QueueInformation queueInfo = rabbitAdmin.getQueueInfo(cola.getName());
        return queueInfo != null && queueInfo.getMessageCount() > 0;
    }

    private int cantidadMensajesCola(Queue cola) {
        QueueInformation queueInfo = rabbitAdmin.getQueueInfo(cola.getName());
        assert queueInfo != null;
        return queueInfo.getMessageCount();
    }

    private void startBancoListener() {
        rabbitListenerEndpointRegistry.getListenerContainer("bancoListener").start();
    }

    private void stopBancoListener() {
        rabbitListenerEndpointRegistry.getListenerContainer("bancoListener").stop();
    }


    private void startBilleteraListener() {
        rabbitListenerEndpointRegistry.getListenerContainer("billeteraListener").start();
    }

    private void stopBilleteraListener() {
        rabbitListenerEndpointRegistry.getListenerContainer("billeteraListener").stop();
    }

    private void startOperacionListener() {
        rabbitListenerEndpointRegistry.getListenerContainer("operacionListener").start();
    }

    private void stopOperacionListener() {
        rabbitListenerEndpointRegistry.getListenerContainer("operacionListener").stop();
    }
}