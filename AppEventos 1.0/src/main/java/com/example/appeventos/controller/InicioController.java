package com.example.appeventos.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Controlador de la pantalla inicial de AppEventos.
 *
 * <p>Gestiona las tarjetas de selección de categorías disponibles
 * en la pantalla de inicio: Música, Tecnología, Deportes y Cultura.</p>
 *
 * <p>Se encarga de aplicar las clases CSS correspondientes a cada
 * tarjeta y de comunicar la categoría seleccionada mediante una
 * función de retorno proporcionada por el controlador principal.</p>
 *
 * <p>De esta manera, el controlador mantiene separada la lógica
 * visual de las tarjetas de la responsabilidad de navegación
 * y apertura de formularios.</p>
 *
 * @see MainController
 * @see FormularioEventoController
 */
public class InicioController {

    /**
     * Referencias a las cuatro tarjetas de categorías definidas
     * en la vista de inicio.
     *
     * <p>Cada tarjeta representa una opción de acceso al formulario
     * correspondiente a una categoría de evento.</p>
     */
    @FXML private VBox tarjetaMusica;
    @FXML private VBox tarjetaTecnologia;
    @FXML private VBox tarjetaDeportes;
    @FXML private VBox tarjetaCultura;

    /**
     * Función de retorno utilizada para comunicar al componente principal
     * la categoría seleccionada por el usuario.
     *
     * <p>Recibe el nombre de la categoría y permite delegar la apertura
     * del formulario sin establecer una dependencia directa con
     * la lógica de navegación del controlador principal.</p>
     *
     * <p>Inicialmente utiliza una implementación vacía para evitar
     * invocaciones sobre una referencia no configurada.</p>
     */
    private Consumer<String> alSeleccionarCategoria = categoria -> { };

    /**
     * Inicializa las tarjetas de categorías de la pantalla de inicio.
     *
     * <p>Asigna a cada tarjeta la clase CSS correspondiente a su categoría.
     * Las clases permiten aplicar las paletas visuales definidas en la
     * hoja de estilos y facilitan que los controles interiores hereden
     * los colores asociados.</p>
     *
     * <p>JavaFX ejecuta automáticamente este metodo después de cargar
     * la vista FXML.</p>
     */
    @FXML
    private void initialize() {
        // Cada tarjeta recibe su paleta. Los colores looked-up se heredan
        // desde la tarjeta a los iconos y botones que contiene.
        tarjetaMusica.getStyleClass().add("evento-musica");
        tarjetaTecnologia.getStyleClass().add("evento-tecnologia");
        tarjetaDeportes.getStyleClass().add("evento-deportes");
        tarjetaCultura.getStyleClass().add("evento-cultura");
    }

    /**
     * Establece la función que se ejecutará al seleccionar una categoría.
     *
     * <p>El metodo permite conectar las tarjetas de la pantalla inicial
     * con la lógica de navegación del controlador principal sin acoplar
     * directamente ambas clases.</p>
     *
     * @param accion función que recibe el nombre de la categoría
     *               seleccionada; si es {@code null}, se establece
     *               una función vacía
     */
    public void configurarCategoria(Consumer<String> accion) {
        alSeleccionarCategoria = accion == null ? categoria -> { } : accion;
    }

    /**
     * Comunica la selección de la categoría Música.
     *
     * <p>Invoca la función de retorno configurada y envía el nombre
     * de la categoría para que el controlador principal pueda abrir
     * el formulario correspondiente.</p>
     */
    @FXML private void abrirMusica() {
        alSeleccionarCategoria.accept("Música");
    }

    /**
     * Comunica la selección de la categoría Tecnología.
     *
     * <p>Invoca la función de retorno configurada con el identificador
     * textual de esta categoría.</p>
     */
    @FXML private void abrirTecnologia() {
        alSeleccionarCategoria.accept("Tecnología");
    }

    /**
     * Comunica la selección de la categoría Deportes.
     *
     * <p>Invoca la función de retorno configurada para solicitar
     * la apertura del formulario de eventos deportivos.</p>
     */
    @FXML private void abrirDeportes() {
        alSeleccionarCategoria.accept("Deportes");
    }

    /**
     * Comunica la selección de la categoría Cultura.
     *
     * <p>Invoca la función de retorno configurada para solicitar
     * la apertura del formulario de eventos culturales.</p>
     */
    @FXML private void abrirCultura() {
        alSeleccionarCategoria.accept("Cultura");
    }
}
