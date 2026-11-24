package com.example.appeventos.controller;

import com.example.appeventos.model.Evento;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Controlador principal encargado de la navegación de AppEventos.
 *
 * <p>Actúa como coordinador de las distintas vistas de la aplicación.
 * Gestiona la carga de las pantallas FXML correspondientes al inicio,
 * al formulario de eventos y al listado de registros.</p>
 *
 * <p>Para mantener separadas las responsabilidades de cada pantalla,
 * utiliza {@link FXMLLoader} para cargar las vistas y obtener sus
 * respectivos controladores. Posteriormente, establece las funciones
 * de retorno necesarias para conectar las acciones entre pantallas.</p>
 *
 * <p>El contenido visual se incorpora al contenedor central de la
 * ventana principal, sustituyendo la vista anterior sin necesidad
 * de crear un escenario independiente para cada pantalla.</p>
 *
 * @see InicioController
 * @see FormularioEventoController
 * @see ListadoEventosController
 * @see Evento
 */
public class MainController {

    /**
     * Contenedor central de la ventana principal donde se incorporan
     * las diferentes vistas FXML de la aplicación.
     *
     * <p>Su contenido se sustituye cuando el usuario navega entre
     * las pantallas disponibles.</p>
     */
    @FXML private VBox contenedorCentral;

    /**
     * Inicializa el controlador principal y carga la pantalla de inicio.
     *
     * <p>JavaFX invoca este metodo después de completar la carga
     * de la vista principal. Su ejecución establece la pantalla
     * inicial de la aplicación mediante {@code mostrarInicio()}.</p>
     */
    @FXML
    private void initialize() {
        mostrarInicio();
    }

    /**
     * Carga y presenta la pantalla inicial de la aplicación.
     *
     * <p>Delega la carga de inicio-view.fxml en el metodo
     * {@code cargarVista()}, que también configura la comunicación
     * entre las tarjetas de categorías y el formulario.</p>
     */
    @FXML
    private void mostrarInicio() {
        cargarVista("/com/example/appeventos/view/inicio-view.fxml");
    }

    /**
     * Inicia la creación de un nuevo evento desde la navegación principal.
     *
     * <p>Abre el formulario en modo de creación, estableciendo Música
     * como categoría inicial y sin proporcionar un evento existente.</p>
     */
    @FXML
    private void nuevoEvento() {
        mostrarFormulario("Música", null);
    }

    /**
     * Carga y presenta la vista que contiene el listado de eventos.
     *
     * <p>Utiliza {@link FXMLLoader} para cargar la vista FXML,
     * obtiene su controlador y configura la acción que permitirá
     * solicitar la edición de los eventos seleccionados.</p>
     *
     * <p>Una vez configurado el controlador del listado, incorpora
     * la vista al contenedor central. Los errores producidos durante
     * la carga del recurso se comunican mediante una alerta.</p>
     */
    @FXML
    private void mostrarListado() {
        try {
            FXMLLoader loader = crearLoader("/com/example/appeventos/view/listado-eventos-view.fxml");
            Parent vista = loader.load();
            ListadoEventosController controller = loader.getController();
            controller.configurar(this::editarEvento);
            mostrarContenido(vista);
        } catch (IOException e) {
            mostrarError("No se pudo abrir el listado de eventos: " + e.getMessage());
        }
    }

    /**
     * Abre el formulario para crear o modificar un evento.
     *
     * <p>El metodo carga formulario-evento-view.fxml y obtiene
     * su controlador mediante {@link FXMLLoader}. A continuación,
     * le proporciona la categoría inicial, el evento que debe cargar
     * y una función de retorno que permite volver al listado cuando
     * se completa correctamente la operación de almacenamiento.</p>
     *
     * <p>Cuando {@code evento} es nulo, el formulario funciona en modo
     * de creación. Cuando contiene un objeto, se utiliza para recuperar
     * los datos del registro que se desea modificar.</p>
     *
     * @param categoria categoría inicial que se desea utilizar
     * @param evento evento que se desea editar, o {@code null}
     *               para crear uno nuevo
     */
    public void mostrarFormulario(String categoria, Evento evento) {
        // Cargamos formulario-evento-view.fxml, que muestra la vista de los formularios de los eventos.
        try {
            FXMLLoader loader = crearLoader("/com/example/appeventos/view/formulario-evento-view.fxml");
            Parent vista = loader.load();
            FormularioEventoController controller = loader.getController();
            controller.configurar(categoria, evento, guardado -> mostrarListado());
            mostrarContenido(vista);
        } catch (IOException e) {
            mostrarError("No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    /**
     * Inicia la edición de un evento seleccionado desde el listado.
     *
     * <p>Recupera la categoría del evento y delega la apertura
     * del formulario en {@code mostrarFormulario()}, conservando
     * el objeto original y, por tanto, su identificador.</p>
     *
     * @param evento evento seleccionado que se desea modificar
     */
    private void editarEvento(Evento evento) {
        mostrarFormulario(evento.getCategoria(), evento);
    }

    /**
     * Carga una vista FXML y establece su configuración específica.
     *
     * <p>El recurso indicado se carga mediante un {@link FXMLLoader}.
     * Una vez obtenida la vista, se comprueba si corresponde a la pantalla
     * inicial para configurar la comunicación entre su controlador
     * y el formulario de eventos.</p>
     *
     * <p>La vista cargada se incorpora al contenedor central.
     * Los errores de lectura o carga del recurso se comunican
     * mediante una alerta.</p>
     *
     * @param recurso ruta del recurso FXML que se desea cargar
     */
    private void cargarVista(String recurso) {
        // Cargamos inicio-view.fxml, que muestra la vista del contenido del panel central del menú principal.
        try {
            FXMLLoader loader = crearLoader(recurso);
            Parent vista = loader.load();
            if (recurso.endsWith("inicio-view.fxml")) {
                InicioController controller = loader.getController();
                controller.configurarCategoria(categoria -> mostrarFormulario(categoria, null));
            }
            mostrarContenido(vista);
        } catch (IOException e) {
            mostrarError("No se pudo cargar la vista: " + e.getMessage());
        }
    }

    /**
     * Sustituye el contenido actual del contenedor central.
     *
     * <p>Elimina los nodos hijos existentes e incorpora la nueva vista.
     * También establece su prioridad de crecimiento vertical para
     * permitir que ocupe el espacio disponible dentro del VBox.</p>
     *
     * @param vista nodo raíz de la vista que se desea mostrar
     */
    private void mostrarContenido(Parent vista) {
        contenedorCentral.getChildren().setAll(vista);
        VBox.setVgrow(vista, javafx.scene.layout.Priority.ALWAYS);
    }

    /**
     * Construye un FXMLLoader asociado a un recurso de la aplicación.
     *
     * <p>La ruta se resuelve desde el classpath utilizando el contexto
     * de la clase del controlador. El objeto devuelto permite cargar
     * posteriormente la vista FXML y acceder a su controlador.</p>
     *
     * @param recurso ruta del recurso FXML dentro del classpath
     * @return instancia de FXMLLoader preparada para cargar el recurso
     */
    private FXMLLoader crearLoader(String recurso) {
        return new FXMLLoader(getClass().getResource(recurso));
    }

    /**
     * Presenta una alerta de error al usuario.
     *
     * <p>Se utiliza para comunicar problemas que impiden cargar
     * correctamente una pantalla o completar una operación de navegación.</p>
     *
     * @param mensaje descripción del problema que se desea mostrar
     */
    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("AppEventos");
        alerta.setHeaderText("Ha ocurrido un error");
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
