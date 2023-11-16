package com.Javacoin.JavacoinApp;

import com.Javacoin.JavacoinApp.models.Operacion;
import com.Javacoin.JavacoinApp.services.BancoService;
import com.Javacoin.JavacoinApp.services.BilleteraService;
import com.Javacoin.JavacoinApp.services.ProductorService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class JavacoinAppApplication {

	public static void main(String[] args) throws InterruptedException {

		ConfigurableApplicationContext context = SpringApplication.run(JavacoinAppApplication.class, args);
		BancoService bancoService = context.getBean(BancoService.class);
		BilleteraService billeteraService = context.getBean(BilleteraService.class);
		ProductorService productorService = context.getBean(ProductorService.class);

		long dniComprador = 12345678L;
		double dineroEnCuentaComprador = 50.0;
		long dniVendedor = 87654321L;
		double dineroEnCuentaVendedor = 0;

		bancoService.crearCuenta(dniComprador,dineroEnCuentaComprador);
		bancoService.crearCuenta(dniVendedor,dineroEnCuentaVendedor);
		billeteraService.crearBilletera(dniComprador,0);
		billeteraService.crearBilletera(dniVendedor,20);


		System.out.println("--COMPRA--");
		System.out.println(String.format("Cuenta usuarioComprador: %s", bancoService.getCuentaUsuario(dniComprador).toString()));
		System.out.println(String.format("Cuenta usuarioVendedor: %s", bancoService.getCuentaUsuario(dniVendedor).toString()));
		System.out.println(String.format("Billetera usuarioComprador: %s", billeteraService.getBilletera(dniComprador).toString()));
		System.out.println(String.format("Billetera usuarioVendedor: %s", billeteraService.getBilletera(dniVendedor).toString()));

		System.out.println("Generando orden de compra de 10 javacoins a 20 dolares para usuario 12345678L");
		Operacion operacionCompra = new Operacion();

		operacionCompra.setDniComprador(dniComprador);
		operacionCompra.setJavacoin(20);
		operacionCompra.setCotizacion(2);

		System.out.println("Enviando orden de compra: " + operacionCompra.toString());
		productorService.comprarJavacoin(operacionCompra);

		//Damos unos segundos a que el sistema haga la transaccion.
		Thread.sleep(6000);
		Long nroOrden = billeteraService.getNroOrdenes().stream().findFirst().get();


		System.out.println(String.format("Cuenta usuarioComprador: %s", bancoService.getCuentaUsuario(dniComprador).toString()));
		System.out.println(String.format("Cuenta usuarioVendedor: %s", bancoService.getCuentaUsuario(dniVendedor).toString()));


		System.out.println("--VENTA--");
		System.out.println("Generando orden de venta de al nro de orden");

		Operacion operacionVenta = new Operacion();
		operacionVenta.setDniVendedor(dniVendedor);
		operacionVenta.setNroOrden(nroOrden);

		System.out.println("Enviando orden de venta: " + operacionVenta.toString());
		productorService.venderJavacoin(operacionVenta);
		Thread.sleep(6000);

		System.out.println(String.format("Cuenta usuarioComprador: %s", bancoService.getCuentaUsuario(dniComprador).toString()));
		System.out.println(String.format("Cuenta usuarioVendedor: %s", bancoService.getCuentaUsuario(dniVendedor).toString()));
		System.out.println(String.format("Billetera usuarioComprador: %s", billeteraService.getBilletera(dniComprador).toString()));
		System.out.println(String.format("Billetera usuarioVendedor (no tiene billetera aun): %s", billeteraService.getBilletera(dniVendedor).toString()));
	}

}
