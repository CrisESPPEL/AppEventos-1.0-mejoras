package com.example.appeventos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Punto de entrada principal de la aplicación AppEventos.
 *
 * <p>Esta clase inicia la aplicación JavaFX y configura la ventana
 * principal. Se encarga de cargar la vista FXML inicial, construir
 * la escena, aplicar la hoja de estilos CSS y mostrar la ventana.</p>
 *
 * <p>La interfaz principal se obtiene del recurso
 * {@code main-view.fxml}, mientras que los estilos visuales se
 * cargan desde {@code estilos.css}.</p>
 *
 * @author Cristian
 * @version 1.0
 * @see javafx.application.Application
 * @see com.example.appeventos.controller.MainController
 */
public class MainApp extends Application {

    /**
     * Inicializa y muestra la ventana principal de AppEventos.
     *
     * <p>Durante el inicio se realizan las siguientes operaciones:</p>
     * <ol>
     *   <li>Se carga la vista principal mediante {@link FXMLLoader}.</li>
     *   <li>Se crea una escena con dimensiones iniciales de 1100 × 720 píxeles.</li>
     *   <li>Se incorpora la hoja de estilos CSS de la aplicación.</li>
     *   <li>Se establece el título y el tamaño mínimo de la ventana.</li>
     *   <li>Se asocia la escena al escenario y se muestra la ventana.</li>
     * </ol>
     *
     * @param stage escenario principal proporcionado por JavaFX
     * @throws IOException si se produce un error al cargar la vista FXML
     *                     o al inicializar sus recursos
     */
    @Override
    public void start(Stage stage) throws IOException {

        // Cargamos el FXML
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/example/appeventos/view/main-view.fxml"));
        Parent root = loader.load();

        // Creamos la escena incial
        Scene scene = new Scene(root, 1100, 720);

        // Aplicamos el CSS
        scene.getStylesheets().add(
                Objects.requireNonNull(MainApp.class.getResource("/com/example/appeventos/css/estilos.css")).toExternalForm());

        // Características de la escena
        stage.setTitle("AppEventos v1.0");
        stage.setMinWidth(700);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();


    }

    /**
     * Metodo de entrada de la aplicación.
     *
     * <p>Delega el arranque en el ciclo de vida de JavaFX mediante
     * {@link #launch(String...)}.</p>
     *
     * @param args argumentos recibidos desde la línea de comandos
     */
    public static void main(String[] args) {
        launch(args);
    }
}
