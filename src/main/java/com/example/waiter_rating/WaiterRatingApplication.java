package com.example.waiter_rating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WaiterRatingApplication {

	public static void main(String[] args) {
		// 🔍 DEBUG: Verificar variables de entorno
		//System.out.println("GOOGLE_CLIENT_ID: " + System.getenv("GOOGLE_CLIENT_ID"));
		//System.out.println("GOOGLE_CLIENT_SECRET: " + (System.getenv("GOOGLE_CLIENT_SECRET") != null ? "***configurado***" : "NO CONFIGURADO"));

		SpringApplication.run(WaiterRatingApplication.class, args);
	}
}