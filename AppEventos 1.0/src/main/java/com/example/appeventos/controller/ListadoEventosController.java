package com.example.appeventos.controller;

import com.example.appeventos.model.Evento;
import com.example.appeventos.persistence.RepositorioEventosCSV;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Controlador de la vista de listado de eventos.
 *
 * <p>Gestiona la presentación de los eventos almacenados mediante
 * un control {@link TableView}. Se encarga de configurar las columnas,
 * asociar los datos del modelo a cada fila y aplicar estilos visuales
 * en función de la categoría de cada evento.</p>
 *
 * <p>La información se obtiene a través de
 * {@link RepositorioEventosCSV}, que proporciona los registros
 * almacenados en los archivos CSV.</p>
 *
 * <p>También permite recargar el listado y solicitar la edición
 * del evento seleccionado mediante una función de retorno
 * proporcionada por el controlador principal.</p>
 *
 * @see Evento
 * @see RepositorioEventosCSV
 */
public class ListadoEventosController {

    /**
     * Referencias a la tabla y a sus columnas, definidas en
     * listado-eventos-view.fxml.
     *
     * <p>Las columnas muestran el nombre, la categoría, la fecha,
     * el lugar y la modalidad de cada evento.</p>
     */
    @FXML private TableView<Evento> tablaEventos;
    @FXML private TableColumn<Evento, String> columnaNombre;
    @FXML private TableColumn<Evento, String> columnaCategoria;
    @FXML private TableColumn<Evento, String> columnaFecha;
    @FXML private TableColumn<Evento, String> columnaLugar;
    @FXML private TableColumn<Evento, String> columnaModalidad;

    /**
     * Colección observable que contiene los eventos mostrados en la tabla.
     *
     * <p>Al estar vinculada al TableView, permite actualizar
     * la presentación cuando se modifica su contenido.</p>
     */
    private final ObservableList<Evento> eventos = FXCollections.observableArrayList();

    /**
     * Acceso a la capa de persistencia utilizada para recuperar
     * los eventos almacenados en los archivos CSV.
     */
    private final RepositorioEventosCSV repositorio = new RepositorioEventosCSV();

    /**
     * Función de retorno que comunica al controlador principal
     * qué evento ha solicitado editar el usuario.
     *
     * <p>Inicialmente utiliza una implementación vacía para evitar
     * llamadas a una referencia no inicializada.</p>
     */
    private Consumer<Evento> accionEditar = evento -> { };

    /**
     * Inicializa la tabla y carga los eventos disponibles.
     *
     * <p>Configura las fábricas de valores de las columnas para obtener
     * la información directamente de los objetos {@link Evento}.
     * Posteriormente, vincula la colección observable con la tabla.</p>
     *
     * <p>También configura una fábrica de filas que asigna clases CSS
     * según la categoría del evento. Como las filas de TableView pueden
     * reutilizarse durante el desplazamiento, las clases visuales
     * anteriores se eliminan antes de aplicar las correspondientes
     * al nuevo elemento.</p>
     *
     * <p>Finalmente, solicita la carga inicial de los registros
     * almacenados.</p>
     */
    @FXML
    private void initialize() {
        columnaNombre.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue().getNombre()));
        columnaCategoria.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue().getCategoria()));
        columnaFecha.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue().getFecha()));
        columnaLugar.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue().getLugar()));
        columnaModalidad.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue().getModalidad()));
        tablaEventos.setItems(eventos);

        // Las filas reutilizables de TableView deben actualizar su clase CSS
        // cada vez que reciben otro Evento.
        tablaEventos.setRowFactory(tabla -> {
            TableRow<Evento> fila = new TableRow<>();
            fila.itemProperty().addListener((observable, anterior, evento) -> {
                fila.getStyleClass().removeAll(
                        "fila-evento", "evento-musica", "evento-tecnologia",
                        "evento-deportes", "evento-cultura");

                if (evento != null) {
                    fila.getStyleClass().add("fila-evento");
                    String clase = switch (evento.getCategoria()) {
                        case "Música" -> "evento-musica";
                        case "Tecnología" -> "evento-tecnologia";
                        case "Deportes" -> "evento-deportes";
                        case "Cultura" -> "evento-cultura";
                        default -> null;
                    };
                    if (clase != null) {
                        fila.getStyleClass().add(clase);
                    }
                }
            });
            return fila;
        });

        cargarEventos();
    }

    /**
     * Registra la acción que se ejecutará al solicitar la edición
     * de un evento desde el listado.
     *
     * <p>Permite desacoplar este controlador del controlador principal:
     * el listado comunica qué evento se ha seleccionado, mientras que
     * la navegación y la apertura del formulario quedan delegadas
     * en el componente que proporciona la función.</p>
     *
     * @param accionEditar función que recibe el evento seleccionado
     *                     para iniciar su edición; si es {@code null},
     *                     se establece una función vacía
     */
    public void configurar(Consumer<Evento> accionEditar) {
        this.accionEditar = accionEditar == null ? evento -> { } : accionEditar;
    }

    /**
     * Solicita la edición del evento seleccionado en la tabla.
     *
     * <p>Recupera el elemento seleccionado mediante el modelo de selección
     * de TableView. Si no existe ninguna selección, informa al usuario
     * y finaliza la operación.</p>
     *
     * <p>Cuando existe un evento seleccionado, delega la acción de edición
     * en la función de retorno configurada previamente.</p>
     */
    @FXML
    private void editarSeleccionado() {
        Evento seleccionado = tablaEventos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAviso("Selecciona primero un evento de la tabla.");
            return;
        }
        accionEditar.accept(seleccionado);
    }

    /**
     * Solicita una actualización del contenido mostrado en la tabla.
     *
     * <p>Delega la recuperación de los registros en {@code cargarEventos()},
     * permitiendo que la vista consulte de nuevo los archivos CSV.</p>
     */
    @FXML
    private void recargarListado() {
        cargarEventos();
    }

    /**
     * Recupera los eventos almacenados y actualiza la colección observable.
     *
     * <p>La información se obtiene mediante
     * {@link RepositorioEventosCSV#listarTodos()}. Cuando la lectura
     * finaliza correctamente, el contenido de la colección observable
     * se sustituye por los eventos recuperados.</p>
     *
     * <p>Si se produce un error de lectura o de formato CSV, se presenta
     * una alerta informativa sobre el problema. En ese caso, la colección
     * no se sustituye por el nuevo resultado.</p>
     */
    private void cargarEventos() {
        try {
            eventos.setAll(repositorio.listarTodos());
        } catch (IOException e) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Error al cargar eventos");
            alerta.setHeaderText(null);
            alerta.setContentText("No se pudieron leer los archivos CSV: " + e.getMessage());
            alerta.showAndWait();
        }
    }

    /**
     * Muestra una notificación informativa relacionada con el listado.
     *
     * <p>Se utiliza principalmente para comunicar que es necesario
     * seleccionar un evento antes de solicitar su edición.</p>
     *
     * @param mensaje texto que se desea presentar al usuario
     */
    private void mostrarAviso(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Listado de eventos");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
