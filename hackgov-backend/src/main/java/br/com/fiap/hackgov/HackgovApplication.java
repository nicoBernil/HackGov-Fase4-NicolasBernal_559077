package br.com.fiap.hackgov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal do HackGov.
 *
 * O metodo main() abaixo e o "botao de ligar" do sistema. Quando voce roda o
 * projeto, o Spring Boot sobe um servidor web embutido e deixa a API no ar
 * em http://localhost:8080
 */
@SpringBootApplication
public class HackgovApplication {

    public static void main(String[] args) {
        SpringApplication.run(HackgovApplication.class, args);
    }
}
