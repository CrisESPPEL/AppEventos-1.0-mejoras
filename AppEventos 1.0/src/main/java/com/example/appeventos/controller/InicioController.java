package com.example.appeventos.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class InicioController {

    @FXML private VBox tarjetaMusica;
    @FXML private VBox tarjetaTecnologia;
    @FXML private VBox tarjetaDeportes;
    @FXML private VBox tarjetaCultura;

    private Consumer<String> alSeleccionarCategoria = categoria -> { };

    @FXML
    private void initialize() {
        // Cada tarjeta recibe su paleta. Los colores looked-up se heredan
        // desde la tarjeta a los iconos y botones que contiene.
        tarjetaMusica.getStyleClass().add("evento-musica");
        tarjetaTecnologia.getStyleClass().add("evento-tecnologia");
        tarjetaDeportes.getStyleClass().add("evento-deportes");
        tarjetaCultura.getStyleClass().add("evento-cultura");
    }

    // Metodo que establece una acción al seleccionar una categoría
    public void configurarCategoria(Consumer<String> accion) {
        alSeleccionarCategoria = accion == null ? categoria -> { } : accion;
    }

    // Métodos para abrir formularios según cuál se ha seleccionado
    @FXML private void abrirMusica() {
        alSeleccionarCategoria.accept("Música");
    }

    @FXML private void abrirTecnologia() {
        alSeleccionarCategoria.accept("Tecnología");
    }

    @FXML private void abrirDeportes() {
        alSeleccionarCategoria.accept("Deportes");
    }

    @FXML private void abrirCultura() {
        alSeleccionarCategoria.accept("Cultura");
    }
}
