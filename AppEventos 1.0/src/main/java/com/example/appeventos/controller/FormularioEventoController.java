package com.example.appeventos.controller;

import com.example.appeventos.model.Evento;
import com.example.appeventos.persistence.RepositorioEventosCSV;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controlador de la interfaz de creación y edición de eventos.
 *
 * <p>Esta clase gestiona el comportamiento del formulario de AppEventos
 * y actúa como intermediaria entre los controles definidos en
 * {@code formulario-evento-view.fxml}, el modelo {@link Evento} y la
 * capa de persistencia {@link RepositorioEventosCSV}.</p>
 *
 * <p>El formulario adapta dinámicamente sus campos a las cuatro categorías
 * disponibles: Música, Tecnología, Deportes y Cultura. Cada categoría
 * dispone de controles, opciones y datos específicos, mientras que
 * determinados campos, como el nombre, la fecha, el aforo y la descripción,
 * forman parte de la información común.</p>
 *
 * <p>También centraliza las validaciones de entrada, incluyendo la
 * obligatoriedad de los campos, el formato de la fecha, la restricción
 * de fechas anteriores al día actual y los límites del aforo.</p>
 *
 * <p>El controlador permite trabajar en dos modos:
 * creación de nuevos eventos y modificación de eventos previamente
 * almacenados. Una vez completada correctamente la operación de
 * persistencia, notifica el resultado mediante una función de retorno.</p>
 *
 * @see Evento
 * @see RepositorioEventosCSV
 */
public class FormularioEventoController {

    /**
     * Referencias a los controles gráficos definidos en
     * formulario-evento-view.fxml.
     *
     * <p>Incluyen campos de texto, listas desplegables, selectores de fecha,
     * controles numéricos, botones de opción, casillas de verificación,
     * deslizadores y contenedores utilizados para adaptar visualmente
     * el formulario a la categoría seleccionada.</p>
     */
    @FXML private Label lblTitulo;
    @FXML private VBox contenedorFormulario;
    @FXML private ComboBox<String> cbCategoria;
    @FXML private TextField txtNombre;
    @FXML private HBox filaDeporte;
    @FXML private TextField txtDeporte;
    @FXML private ComboBox<String> cbTipo;
    @FXML private HBox filaTematica;
    @FXML private TextField txtTematica;
    @FXML private HBox filaNivel;
    @FXML private ComboBox<String> cbNivel;
    @FXML private TextField txtLugar;
    @FXML private DatePicker dpFecha;
    @FXML private Spinner<Integer> spAforo;
    @FXML private ComboBox<String> cbPublico;
    @FXML private RadioButton rbPresencial;
    @FXML private RadioButton rbOnline;
    @FXML private RadioButton rbHibrido;
    @FXML private HBox filaInscripcion;
    @FXML private CheckBox chkInscripcion;
    @FXML private HBox filaMaterial;
    @FXML private CheckBox chkMaterial;
    @FXML private HBox filaDestacado;
    @FXML private CheckBox chkDestacado;
    @FXML private HBox filaGratuito;
    @FXML private CheckBox chkGratuito;
    @FXML private Slider slInteres;
    @FXML private Slider slDifusion;
    @FXML private Slider slOrganizacion;
    @FXML private Label lblInteres;
    @FXML private Label lblDifusion;
    @FXML private Label lblOrganizacion;
    @FXML private TextArea txtDescripcion;

    /**
     * Agrupa los botones de selección de modalidad para permitir
     * una única opción activa entre Presencial, Online e Híbrido.
     */
    private final ToggleGroup grupoModalidad = new ToggleGroup();

    /**
     * Componente encargado de realizar las operaciones de lectura,
     * creación y actualización de eventos en los archivos CSV.
     */
    private final RepositorioEventosCSV repositorio = new RepositorioEventosCSV();

    /**
     * Evento que se está modificando, o {@code null} cuando el formulario
     * se utiliza para registrar un evento nuevo.
     *
     * <p>Su identificador permite conservar la identidad del registro
     * durante una operación de edición.</p>
     */
    private Evento eventoOriginal;

    /**
     * Función de retorno ejecutada después de completar correctamente
     * la operación de almacenamiento.
     *
     * <p>Recibe un valor booleano que indica el resultado de la operación.
     * En la implementación actual, se invoca con {@code true} cuando
     * el guardado o la actualización han finalizado correctamente.</p>
     */
    private Consumer<Boolean> alGuardar = correcto -> { };

    /**
     * Indicador utilizado para evitar que el cambio programático de
     * categoría durante la configuración inicial active innecesariamente
     * el manejador de selección del ComboBox.
     */
    private boolean actualizandoCategoria;

    /**
     * Formato de presentación y validación de fechas del formulario.
     *
     * <p>Utiliza el patrón día/mes/año y resolución estricta para
     * rechazar fechas que no existan en el calendario.</p>
     */
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /**
     * Inicializa y configura los controles del formulario después
     * de cargar su vista FXML.
     *
     * <p>Durante la inicialización se realizan las siguientes tareas:</p>
     * <ul>
     *   <li>Se incorporan las categorías disponibles.</li>
     *   <li>Se configuran las opciones del público objetivo.</li>
     *   <li>Se establece el conversor y el filtro de entrada de fechas.</li>
     *   <li>Se configura el control de aforo con sus restricciones.</li>
     *   <li>Se agrupan los botones correspondientes a la modalidad.</li>
     *   <li>Se inicializan los deslizadores y sus etiquetas de valor.</li>
     *   <li>Se registra el manejador que responde a los cambios de categoría.</li>
     * </ul>
     *
     * <p>Este metodo es invocado automáticamente por JavaFX mediante
     * {@link FXML} cuando se carga la vista asociada al controlador.</p>
     */
    @FXML
    private void initialize() {

        cbCategoria.getItems().setAll("Música", "Tecnología", "Deportes", "Cultura");
        cbPublico.setEditable(true);
        cbPublico.getItems().setAll(
                "Público general",
                "Estudiantes",
                "Profesionales",
                "Familias",
                "Jóvenes",
                "Adultos"
        );

        /** Configurar formato y validación estricta de fecha. */
        dpFecha.setConverter(new StringConverter<>() {

            @Override
            public String toString(LocalDate fecha) {
                return fecha == null ? "" : FORMATO_FECHA.format(fecha);
            }

            @Override
            public LocalDate fromString(String texto) {

                if (texto == null || texto.isBlank()) {
                    return null;
                }

                try {
                    return LocalDate.parse(texto.trim(), FORMATO_FECHA);

                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });

        /** Limitar los caracteres y la estructura de la fecha escrita. */
        dpFecha.getEditor().setTextFormatter(
                new TextFormatter<String>(cambio -> {

                    String nuevoTexto = cambio.getControlNewText();

                    if (nuevoTexto.matches(
                            "\\d{0,2}(/\\d{0,2}(/\\d{0,4})?)?"
                    )) {
                        return cambio;
                    }

                    return null;
                })
        );

        cbPublico.setPromptText("Escribe el público objetivo");
        configurarAforo();
        rbPresencial.setToggleGroup(grupoModalidad);
        rbOnline.setToggleGroup(grupoModalidad);
        rbHibrido.setToggleGroup(grupoModalidad);
        rbPresencial.setSelected(true);

        prepararSlider(slInteres, lblInteres);
        prepararSlider(slDifusion, lblDifusion);
        prepararSlider(slOrganizacion, lblOrganizacion);

        cbCategoria.setOnAction(evento -> {
            if (!actualizandoCategoria && cbCategoria.getValue() != null) {
                actualizarCamposCategoria(cbCategoria.getValue());
            }
        });
    }

    /**
     * Configura el formulario según el modo de operación solicitado.
     *
     * <p>Cuando no se proporciona un evento existente, el controlador
     * prepara el formulario para crear uno nuevo. Si se recibe un evento,
     * sus datos se cargan en los controles y se bloquea la modificación
     * de su categoría.</p>
     *
     * <p>También registra la función de retorno que se ejecutará tras
     * completar correctamente la operación de persistencia.</p>
     *
     * @param categoria categoría inicial del formulario; si es
     *                  {@code null}, se establece Música
     * @param evento evento que se desea modificar, o {@code null}
     *               para iniciar una creación
     * @param alGuardar función que se ejecutará cuando la operación
     *                  de almacenamiento finalice correctamente;
     *                  si es {@code null}, se utiliza una función vacía
     */
    public void configurar(String categoria, Evento evento, Consumer<Boolean> alGuardar) {
        this.eventoOriginal = evento;
        this.alGuardar = alGuardar == null ? correcto -> { } : alGuardar;

        actualizandoCategoria = true;
        cbCategoria.setValue(categoria == null ? "Música" : categoria);
        actualizandoCategoria = false;
        actualizarCamposCategoria(cbCategoria.getValue());

        if (evento == null) {
            lblTitulo.setText("Nuevo evento");
            cbCategoria.setDisable(false);
            limpiarFormulario();
        } else {
            lblTitulo.setText("Modificar evento");
            cbCategoria.setDisable(true);
            cargarEvento(evento);
        }
    }

    /**
     * Obtiene y valida la fecha escrita en el editor del DatePicker.
     *
     * <p>El texto introducido se analiza utilizando el formato estricto
     * definido por {@code FORMATO_FECHA}. Cuando la fecha es válida,
     * se actualiza también el valor interno del DatePicker.</p>
     *
     * <p>Los campos vacíos y las fechas que no cumplen el formato
     * establecido se consideran entradas no válidas.</p>
     *
     * @return fecha convertida a {@link LocalDate}, o {@code null}
     *         cuando el campo está vacío o contiene una fecha inválida
     */
    private LocalDate obtenerFechaIntroducida() {

        String texto = dpFecha.getEditor().getText();

        if (texto == null || texto.isBlank()) {
            return null;
        }

        try {

            LocalDate fecha =
                    LocalDate.parse(texto.trim(), FORMATO_FECHA);

            dpFecha.setValue(fecha);

            return fecha;

        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Configura el Spinner destinado a introducir el aforo del evento.
     *
     * <p>Establece un valor mínimo de 1 y un máximo de 1.000.000,
     * con un valor inicial de 1. También incorpora un filtro de entrada
     * para limitar el contenido del editor a caracteres numéricos
     * y evitar entradas superiores a siete dígitos.</p>
     *
     * <p>La validación definitiva del valor introducido se realiza
     * posteriormente durante el proceso de guardado.</p>
     */
    private void configurarAforo() {

        SpinnerValueFactory.IntegerSpinnerValueFactory fabrica =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, 1_000_000, 1
                );

        spAforo.setValueFactory(fabrica);

        TextFormatter<Integer> formatter = new TextFormatter<>(
                new IntegerStringConverter(), 1,
                cambio -> {
                    String nuevoTexto = cambio.getControlNewText();

                    if (nuevoTexto.isEmpty()
                            || nuevoTexto.matches("\\d{1,7}")) {
                        return cambio;
                    }

                    return null;
                }
        );

        spAforo.getEditor().setTextFormatter(formatter);
    }

    /**
     * Inicializa un deslizador y sincroniza su valor con una etiqueta.
     *
     * <p>Configura el intervalo permitido entre 1 y 10, establece
     * el valor inicial en 5 y activa las marcas y etiquetas de escala.
     * Además, registra un listener para actualizar automáticamente
     * el texto de la etiqueta cuando cambia el valor del deslizador.</p>
     *
     * @param slider control deslizante que se desea configurar
     * @param etiquetaValor etiqueta que mostrará el valor seleccionado
     */
    private void prepararSlider(Slider slider, Label etiquetaValor) {
        slider.setMin(1);
        slider.setMax(10);
        slider.setValue(5);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setSnapToTicks(true);
        slider.setBlockIncrement(1);
        slider.valueProperty().addListener((observable, anterior, nuevo) ->
                etiquetaValor.setText(String.valueOf(nuevo.intValue())));
        etiquetaValor.setText("5");
    }

    /**
     * Adapta el contenido del formulario a la categoría seleccionada.
     *
     * <p>El metodo determina qué controles deben mostrarse y qué
     * opciones estarán disponibles en las listas desplegables.</p>
     *
     * <p>La configuración específica es la siguiente:</p>
     * <ul>
     *   <li>Música: opciones de conciertos, festivales y recitales.</li>
     *   <li>Tecnología: temática, nivel e inscripción.</li>
     *   <li>Deportes: disciplina y disponibilidad de material.</li>
     *   <li>Cultura: posibilidad de indicar si el evento es gratuito.</li>
     * </ul>
     *
     * <p>También actualiza la paleta visual correspondiente y reinicia
     * las opciones de tipo y nivel para evitar conservar selecciones
     * incompatibles con la nueva categoría.</p>
     *
     * @param categoria categoría cuyos controles y opciones se
     *                   desean configurar
     */
    private void actualizarCamposCategoria(String categoria) {
        aplicarPaletaCategoria(categoria);

        boolean musica = "Música".equals(categoria);
        boolean tecnologia = "Tecnología".equals(categoria);
        boolean deportes = "Deportes".equals(categoria);
        boolean cultura = "Cultura".equals(categoria);

        mostrarFila(filaDeporte, deportes);
        mostrarFila(filaTematica, tecnologia);
        mostrarFila(filaNivel, tecnologia);
        mostrarFila(filaInscripcion, tecnologia);
        mostrarFila(filaMaterial, deportes);
        mostrarFila(filaDestacado, !tecnologia);
        mostrarFila(filaGratuito, cultura);

        List<String> tipos = switch (categoria) {
            case "Música" -> List.of("Concierto", "Festival", "Recital");
            case "Tecnología" -> List.of("Charla", "Taller", "Feria", "Jornada");
            case "Deportes" -> List.of("Competición", "Torneo", "Exhibición", "Actividad deportiva");
            case "Cultura" -> List.of("Exposición", "Obra de teatro", "Cine", "Feria", "Encuentro cultural");
            default -> List.of();
        };
        cbTipo.getItems().setAll(tipos);
        cbTipo.setValue(null);

        cbNivel.getItems().setAll("Iniciación", "Intermedio", "Avanzado");
        cbNivel.setValue(null);

        if (musica || tecnologia || deportes || cultura) {
            // Todas las categorías mantienen los mismos controles comunes.
            cbPublico.setPromptText("Escribe el público objetivo");
        }
    }

    /**
     * Aplica al contenedor principal la clase CSS asociada a una categoría.
     *
     * <p>Primero elimina las clases visuales de las categorías anteriores
     * y después incorpora la correspondiente a la selección actual.
     * De esta forma, los estilos definidos en la hoja CSS pueden adaptar
     * los colores del formulario sin acumular clases incompatibles.</p>
     *
     * @param categoria categoría cuya paleta visual se desea aplicar
     */
    private void aplicarPaletaCategoria(String categoria) {
        contenedorFormulario.getStyleClass().removeAll(
                "evento-musica", "evento-tecnologia", "evento-deportes", "evento-cultura");

        String clase = switch (categoria) {
            case "Música" -> "evento-musica";
            case "Tecnología" -> "evento-tecnologia";
            case "Deportes" -> "evento-deportes";
            case "Cultura" -> "evento-cultura";
            default -> null;
        };

        if (clase != null) {
            contenedorFormulario.getStyleClass().add(clase);
        }
    }

    /**
     * Controla la visibilidad y la participación de una fila en el diseño.
     *
     * <p>Además de modificar la propiedad de visibilidad, actualiza
     * la propiedad {@code managed}. Esto permite que JavaFX reserve
     * espacio para la fila únicamente cuando está visible, evitando
     * huecos en el formulario.</p>
     *
     * @param fila contenedor horizontal que se desea controlar
     * @param mostrar {@code true} para mostrar y gestionar la fila;
     *                {@code false} para ocultarla y retirarla del diseño
     */
    private void mostrarFila(HBox fila, boolean mostrar) {
        fila.setVisible(mostrar);
        fila.setManaged(mostrar);
    }

    /**
     * Valida y almacena los datos introducidos en el formulario.
     *
     * <p>Antes de realizar cualquier operación de persistencia,
     * comprueba que se hayan seleccionado los campos obligatorios
     * y que sus valores cumplan las restricciones establecidas.</p>
     *
     * <p>Entre las comprobaciones realizadas se encuentran:</p>
     * <ul>
     *   <li>Existencia de una categoría y un nombre de evento.</li>
     *   <li>Selección del tipo de actividad.</li>
     *   <li>Validación de los campos específicos de cada categoría.</li>
     *   <li>Existencia y validez de la fecha introducida.</li>
     *   <li>Rechazo de fechas anteriores al día actual.</li>
     *   <li>Validación numérica del aforo y de sus límites.</li>
     *   <li>Existencia del público objetivo y de una modalidad válida.</li>
     * </ul>
     *
     * <p>Una vez superadas las validaciones, recopila los valores
     * del formulario y delega la operación en {@link RepositorioEventosCSV}.</p>
     *
     * <p>Si se trata de una creación, genera un nuevo objeto {@link Evento}.
     * Si se trata de una edición, conserva el identificador original y
     * sustituye los datos del registro existente.</p>
     *
     * <p>Cuando la operación termina correctamente, muestra una notificación
     * y ejecuta la función de retorno configurada. Los errores de
     * persistencia se comunican mediante una alerta.</p>
     */
    @FXML
    private void guardarEvento() {

        String categoria = cbCategoria.getValue();

        // Si no hay categoría que guardar
        if (categoria == null || categoria.isBlank()) {
            mostrarError("Selecciona una categoría.");
            return;
        }
        // Si no encuentra un nombre que guardar
        if (txtNombre.getText().trim().isEmpty()) {
            mostrarError("Introduce el nombre del evento.");
            return;
        }

        // Si no encuentra un tipo de evento que guardar
        if (cbTipo.getValue() == null) {
            mostrarError("Selecciona el tipo de evento o actividad.");
            return;
        }
        // Si la categoría es Tecnología
        if ("Tecnología".equals(categoria)) {
            // Si no encuentra una tecnología o temática principal que guardar
            if (txtTematica.getText().trim().isEmpty()) {
                mostrarError("Introduce la tecnología o temática principal.");
                return;
            }
            // Si no encuentra un nivel que guardar
            if (cbNivel.getValue() == null) {
                mostrarError("Selecciona el nivel.");
                return;
            }
        }
        // Si no encuentra un deporte que guardar
        if ("Deportes".equals(categoria) && txtDeporte.getText().trim().isEmpty()) {
            mostrarError("Introduce el deporte o disciplina.");
            return;
        }
        // Si no encuentra un lugar que guardar
        if (txtLugar.getText().trim().isEmpty()) {
            mostrarError("Introduce el lugar del evento.");
            return;
        }

        // Comprobar que se ha seleccionado una fecha
        LocalDate fechaSeleccionada = obtenerFechaIntroducida();
        // Si no encuentra una fecha del evento que guardar
        if (fechaSeleccionada == null) {
            mostrarError("Selecciona una fecha válida.");
            return;
        }

        // Comprobar que la fecha no sea anterior a hoy
        if (fechaSeleccionada.isBefore(LocalDate.now())) {
            mostrarError("La fecha no puede ser anterior a hoy.");
            return;
        }

        // Confirmar el valor escrito en el Spinner del aforo
        try {
            spAforo.commitValue();

            int aforo = spAforo.getValue();

            if (aforo < 1 || aforo > 1_000_000) {
                mostrarError("El aforo debe estar entre 1 y 1.000.000.");
                return;
            }

        } catch (RuntimeException e) {
            mostrarError("Introduce un aforo numérico válido.");
            return;
        }

        String publico = cbPublico.getEditor().getText().trim();
        // Si no encuentra un público objetivo que guardar
        if (publico.isEmpty()) {
            mostrarError("Introduce el público objetivo.");
            return;
        }

        String modalidad = obtenerModalidad();
        // Si no encuentra una modalidad que guardar
        if (modalidad.isEmpty()) {
            mostrarError("Selecciona una modalidad.");
            return;
        }

        String[] datos = crearDatos(categoria, publico, modalidad);
        try {
            if (eventoOriginal == null) {
                repositorio.guardar(new Evento(categoria, datos));
            } else {
                Evento actualizado = new Evento(eventoOriginal.getId(), categoria, datos);
                repositorio.actualizar(actualizado);
            }
            mostrarConfirmacion(eventoOriginal == null
                    ? "Evento guardado correctamente."
                    : "Evento modificado correctamente.");
            alGuardar.accept(true);
        } catch (IOException e) {
            mostrarError("No se pudo guardar el evento: " + e.getMessage());
        }
    }

    /**
     * Construye el array de datos que representa la información del evento.
     *
     * <p>La estructura del array depende de la categoría seleccionada
     * y debe mantener exactamente el orden utilizado por el modelo
     * {@link Evento} y por el repositorio CSV.</p>
     *
     * <p>Los datos comunes, como las valoraciones de interés, difusión
     * y organización, así como la descripción, se incorporan al final
     * de todas las estructuras.</p>
     *
     * <p>La fecha se almacena en formato ISO mediante
     * {@link LocalDate#toString()}, independientemente de su formato
     * de presentación en el formulario.</p>
     *
     * @param categoria categoría que determina la estructura del array
     * @param publico texto correspondiente al público objetivo
     * @param modalidad modalidad seleccionada para el evento
     * @return array de cadenas con los datos organizados en el orden
     *         requerido por el modelo y el almacenamiento CSV
     * @throws IllegalArgumentException si se recibe una categoría desconocida
     */
    private String[] crearDatos(String categoria, String publico, String modalidad) {
        List<String> datos = new ArrayList<>();
        datos.add(txtNombre.getText().trim());

        // Dependiendo de la categoría se guardan los datos recopilados
        switch (categoria) {
            case "Música" -> {
                datos.add(cbTipo.getValue());
                datos.add(txtLugar.getText().trim());
                datos.add(dpFecha.getValue().toString());
                datos.add(String.valueOf(spAforo.getValue()));
                datos.add(modalidad);
                datos.add(publico);
                datos.add(chkDestacado.isSelected() ? "Sí" : "No");
            }
            case "Tecnología" -> {
                datos.add(cbTipo.getValue());
                datos.add(txtTematica.getText().trim());
                datos.add(cbNivel.getValue());
                datos.add(txtLugar.getText().trim());
                datos.add(dpFecha.getValue().toString());
                datos.add(String.valueOf(spAforo.getValue()));
                datos.add(modalidad);
                datos.add(chkInscripcion.isSelected() ? "Sí" : "No");
                datos.add(publico);
            }
            case "Deportes" -> {
                datos.add(txtDeporte.getText().trim());
                datos.add(cbTipo.getValue());
                datos.add(txtLugar.getText().trim());
                datos.add(dpFecha.getValue().toString());
                datos.add(String.valueOf(spAforo.getValue()));
                datos.add(modalidad);
                datos.add(chkMaterial.isSelected() ? "Sí" : "No");
                datos.add(chkDestacado.isSelected() ? "Sí" : "No");
                datos.add(publico);
            }
            case "Cultura" -> {
                datos.add(cbTipo.getValue());
                datos.add(txtLugar.getText().trim());
                datos.add(dpFecha.getValue().toString());
                datos.add(String.valueOf(spAforo.getValue()));
                datos.add(publico);
                datos.add(modalidad);
                datos.add(chkGratuito.isSelected() ? "Sí" : "No");
                datos.add(chkDestacado.isSelected() ? "Sí" : "No");
            }
            default -> throw new IllegalArgumentException("Categoría desconocida: " + categoria);
        }

        // Datos que son universales entre las 4 categorías
        datos.add(String.valueOf((int) slInteres.getValue()));
        datos.add(String.valueOf((int) slDifusion.getValue()));
        datos.add(String.valueOf((int) slOrganizacion.getValue()));
        datos.add(txtDescripcion.getText().trim());

        return datos.toArray(new String[0]);
    }

    /**
     * Obtiene la modalidad seleccionada mediante los botones de opción.
     *
     * <p>Reconoce las modalidades Presencial, Online e Híbrido,
     * devolviendo el valor textual correspondiente a la selección activa.</p>
     *
     * @return modalidad seleccionada o una cadena vacía si no hay
     *         ningún botón activo
     */
    private String obtenerModalidad() {
        if (rbPresencial.isSelected()) return "Presencial";
        if (rbOnline.isSelected()) return "Online";
        if (rbHibrido.isSelected()) return "Híbrido";
        return "";
    }

    /**
     * Restaura en los controles del formulario la información de un evento.
     *
     * <p>El metodo consulta la categoría del evento y asigna cada valor
     * a su control correspondiente. Como la estructura de datos varía
     * entre categorías, cada caso utiliza las posiciones específicas
     * definidas al construir el array mediante {@code crearDatos()}.</p>
     *
     * <p>También restaura las opciones booleanas, la modalidad, los
     * valores de los deslizadores y la descripción.</p>
     *
     * @param evento evento cuyos datos se desean cargar en el formulario
     */
    private void cargarEvento(Evento evento) {
        String[] datos = evento.getDatos();
        String categoria = evento.getCategoria();
        txtNombre.setText(dato(datos, 0));

        // Según la categoría del evento cargamos los datos de cada campo en la posición deseada
        switch (categoria) {
            case "Música" -> {
                cbTipo.setValue(dato(datos, 1));
                txtLugar.setText(dato(datos, 2));
                dpFecha.setValue(fecha(dato(datos, 3)));
                establecerAforo(dato(datos, 4));
                seleccionarModalidad(dato(datos, 5));
                cbPublico.getEditor().setText(dato(datos, 6));
                chkDestacado.setSelected("Sí".equalsIgnoreCase(dato(datos, 7)));
                establecerSliders(datos, 8, 9, 10);
                txtDescripcion.setText(dato(datos, 11));
            }
            case "Tecnología" -> {
                cbTipo.setValue(dato(datos, 1));
                txtTematica.setText(dato(datos, 2));
                cbNivel.setValue(dato(datos, 3));
                txtLugar.setText(dato(datos, 4));
                dpFecha.setValue(fecha(dato(datos, 5)));
                establecerAforo(dato(datos, 6));
                seleccionarModalidad(dato(datos, 7));
                chkInscripcion.setSelected("Sí".equalsIgnoreCase(dato(datos, 8)));
                cbPublico.getEditor().setText(dato(datos, 9));
                establecerSliders(datos, 10, 11, 12);
                txtDescripcion.setText(dato(datos, 13));
            }
            case "Deportes" -> {
                txtDeporte.setText(dato(datos, 1));
                cbTipo.setValue(dato(datos, 2));
                txtLugar.setText(dato(datos, 3));
                dpFecha.setValue(fecha(dato(datos, 4)));
                establecerAforo(dato(datos, 5));
                seleccionarModalidad(dato(datos, 6));
                chkMaterial.setSelected("Sí".equalsIgnoreCase(dato(datos, 7)));
                chkDestacado.setSelected("Sí".equalsIgnoreCase(dato(datos, 8)));
                cbPublico.getEditor().setText(dato(datos, 9));
                establecerSliders(datos, 10, 11, 12);
                txtDescripcion.setText(dato(datos, 13));
            }
            case "Cultura" -> {
                cbTipo.setValue(dato(datos, 1));
                txtLugar.setText(dato(datos, 2));
                dpFecha.setValue(fecha(dato(datos, 3)));
                establecerAforo(dato(datos, 4));
                cbPublico.getEditor().setText(dato(datos, 5));
                seleccionarModalidad(dato(datos, 6));
                chkGratuito.setSelected("Sí".equalsIgnoreCase(dato(datos, 7)));
                chkDestacado.setSelected("Sí".equalsIgnoreCase(dato(datos, 8)));
                establecerSliders(datos, 9, 10, 11);
                txtDescripcion.setText(dato(datos, 12));
            }
            default -> mostrarError("La categoría del evento no es válida.");
        }
    }

    /**
     * Recupera un elemento de un array de datos con comprobación de límites.
     *
     * <p>Comprueba que el índice se encuentre dentro del rango disponible
     * y que el valor almacenado no sea {@code null}. Esta protección
     * permite manejar estructuras incompletas sin provocar errores
     * de acceso a posiciones inexistentes.</p>
     *
     * @param datos array que contiene la información del evento
     * @param indice posición del elemento que se desea recuperar
     * @return valor correspondiente al índice indicado o una cadena vacía
     *         cuando el índice no es válido o el contenido es nulo
     */
    private String dato(String[] datos, int indice) {
        return indice >= 0 && indice < datos.length && datos[indice] != null ? datos[indice] : "";
    }

    /**
     * Convierte una representación textual ISO de fecha a LocalDate.
     *
     * <p>Este metodo se utiliza al recuperar fechas almacenadas
     * en los registros CSV. Los valores vacíos o que no puedan
     * interpretarse como fechas válidas producen un resultado nulo.</p>
     *
     * @param texto representación textual de la fecha, normalmente
     *              en formato ISO {@code yyyy-MM-dd}
     * @return objeto {@link LocalDate} cuando la conversión es válida;
     *         {@code null} en caso de texto vacío o inválido
     */
    private LocalDate fecha(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return LocalDate.parse(texto);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Establece en el Spinner el aforo recuperado de un evento.
     *
     * <p>Convierte el texto recibido a un número entero y limita
     * su valor al intervalo comprendido entre 1 y 1.000.000.</p>
     *
     * <p>Cuando el contenido no es numérico, se establece el valor
     * predeterminado de 1.</p>
     *
     * @param texto valor textual del aforo almacenado en el evento
     */
    private void establecerAforo(String texto) {
        try {
            int valor = Integer.parseInt(texto);
            spAforo.getValueFactory().setValue(Math.max(1, Math.min(1_000_000, valor)));
        } catch (NumberFormatException e) {
            spAforo.getValueFactory().setValue(1);
        }
    }

    /**
     * Activa el botón de opción correspondiente a una modalidad.
     *
     * <p>Identifica las opciones Online e Híbrido. Para cualquier
     * otro valor, selecciona Presencial como modalidad predeterminada.</p>
     *
     * @param modalidad modalidad que se desea restaurar en el formulario
     */
    private void seleccionarModalidad(String modalidad) {
        switch (modalidad) {
            case "Online" -> rbOnline.setSelected(true);
            case "Híbrido" -> rbHibrido.setSelected(true);
            default -> rbPresencial.setSelected(true);
        }
    }

    /**
     * Restaura los valores de los tres deslizadores a partir del array.
     *
     * <p>Cada deslizador utiliza el índice indicado para localizar
     * su valor en la estructura de datos. La conversión y el control
     * de los límites se delegan en {@code enteroEntre()}.</p>
     *
     * @param datos array que contiene los datos del evento
     * @param interes posición correspondiente a la valoración de interés
     * @param difusion posición correspondiente a la valoración de difusión
     * @param organizacion posición correspondiente a la valoración de organización
     */
    private void establecerSliders(String[] datos, int interes, int difusion, int organizacion) {
        slInteres.setValue(enteroEntre(dato(datos, interes)));
        slDifusion.setValue(enteroEntre(dato(datos, difusion)));
        slOrganizacion.setValue(enteroEntre(dato(datos, organizacion)));
    }

    /**
     * Convierte un texto a un entero válido para los deslizadores.
     *
     * <p>El resultado se limita al intervalo comprendido entre 1 y 10.
     * Si el texto no representa un número entero válido, se devuelve
     * el valor predeterminado de 5.</p>
     *
     * @param texto valor textual que se desea convertir
     * @return número entero comprendido entre 1 y 10, o 5 si la
     *         conversión no es posible
     */
    private int enteroEntre(String texto) {
        try {
            return Math.max(1, Math.min(10, Integer.parseInt(texto)));
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    /**
     * Restablece los controles del formulario a sus valores iniciales.
     *
     * <p>Elimina los textos introducidos, reinicia las selecciones,
     * desactiva las casillas de verificación y restablece los valores
     * predeterminados de los controles numéricos.</p>
     *
     * <p>El aforo vuelve a 1, la modalidad a Presencial y los tres
     * deslizadores a 5. También se elimina la descripción introducida.</p>
     *
     * <p>Este metodo está asociado a la interfaz mediante FXML.</p>
     */
    @FXML
    private void limpiarFormulario() {
        txtNombre.clear();
        txtDeporte.clear();
        txtTematica.clear();
        cbTipo.setValue(null);
        cbNivel.setValue(null);
        txtLugar.clear();
        dpFecha.setValue(null);
        spAforo.getValueFactory().setValue(1);
        cbPublico.setValue(null);
        cbPublico.getEditor().clear();
        rbPresencial.setSelected(true);
        chkInscripcion.setSelected(false);
        chkMaterial.setSelected(false);
        chkDestacado.setSelected(false);
        chkGratuito.setSelected(false);
        slInteres.setValue(5);
        slDifusion.setValue(5);
        slOrganizacion.setValue(5);
        txtDescripcion.clear();
    }

    /**
     * Presenta al usuario una ventana de alerta de tipo error.
     *
     * <p>La alerta utiliza el título general de la aplicación, no
     * incorpora un encabezado adicional y muestra el mensaje recibido.
     * La ventana permanece activa hasta que el usuario la cierra.</p>
     *
     * @param mensaje explicación del error que se desea comunicar
     */
    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("AppEventos");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    /**
     * Presenta una notificación informativa al finalizar una operación.
     *
     * <p>Se utiliza para comunicar al usuario que una acción,
     * como guardar o modificar un evento, se ha completado correctamente.</p>
     *
     * @param mensaje texto de confirmación que se mostrará
     */
    private void mostrarConfirmacion(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("AppEventos");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
