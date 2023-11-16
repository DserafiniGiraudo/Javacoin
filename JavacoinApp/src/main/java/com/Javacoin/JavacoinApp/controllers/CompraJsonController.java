package com.Javacoin.JavacoinApp.controllers;

import com.Javacoin.JavacoinApp.models.Operacion;
import com.Javacoin.JavacoinApp.services.ProductorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/javacoin")
public class CompraJsonController {

    private ProductorService compradorService;

    public CompraJsonController(ProductorService compradorService){
        this.compradorService = compradorService;
    }

    @PostMapping("/comprar")
    public ResponseEntity<String> realizarCompra(@RequestBody Operacion operacion){
        compradorService.comprarJavacoin(operacion);
        return ResponseEntity.ok("Solicitud de Compra realizada");
    }

    @PostMapping("/aceptar")
    public ResponseEntity<String> aceptarCompra(@RequestBody Operacion operacion){
        compradorService.venderJavacoin(operacion);
        return ResponseEntity.ok("Venta realizada");
    }

}
