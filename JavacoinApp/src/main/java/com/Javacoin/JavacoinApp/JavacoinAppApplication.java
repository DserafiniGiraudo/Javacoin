package com.Javacoin.JavacoinApp;

import com.Javacoin.JavacoinApp.services.BancoService;
import com.Javacoin.JavacoinApp.services.BilleteraService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class JavacoinAppApplication {

	public static void main(String[] args) {

		ConfigurableApplicationContext context = SpringApplication.run(JavacoinAppApplication.class, args);
		BancoService bancoService = context.getBean(BancoService.class);
		BilleteraService billeteraService = context.getBean(BilleteraService.class);

		bancoService.crearCuenta(12345678L,50);
		bancoService.crearCuenta(87654321L,0);
		billeteraService.crearBilletera(87654321L,20);


	}

}
