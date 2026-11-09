
package cl.duoc.usuarios_service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerarHashAdmin {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String contrasena = "EstudianteDos2026!";

        // Pega aqui el hash completo de estudiante 2
        String hashActual = "$2a$10$oSJf27bZRFCMMRajYZFh/uyCE1NQ6RrHnwpbkXDqklx1gxUyb5c2a";

        System.out.println(
                "Coincide: " + encoder.matches(contrasena, hashActual)
        );

        System.out.println(
                "Nuevo hash: " + encoder.encode(contrasena)
        );
    }
}
