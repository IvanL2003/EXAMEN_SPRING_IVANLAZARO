package com.salesianos.bibliobox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Clase principal de la aplicacion BiblioBox.
 *
 * Extiende SpringBootServletInitializer para permitir el despliegue
 * como WAR en un servidor externo (Tomcat, WildFly, etc.).
 * El metodo configure() es el punto de entrada que usa el servidor
 * al arrancar el WAR (en lugar del main habitual).
 * El main() sigue funcionando para arrancar en local con el JAR embebido.
 */
@SpringBootApplication
public class BiblioBoxApplication extends SpringBootServletInitializer {

    /**
     * Punto de entrada para despliegue en servidor externo.
     * El contenedor de servlets llama a este metodo en lugar de main().
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(BiblioBoxApplication.class);
    }

    /**
     * Punto de entrada para ejecucion local (mvn spring-boot:run o java -jar).
     */
    public static void main(String[] args) {
        SpringApplication.run(BiblioBoxApplication.class, args);
    }
}
