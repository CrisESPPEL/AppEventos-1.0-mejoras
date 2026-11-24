package com.example.appeventos.model;

import java.util.UUID;
import java.util.Arrays;

/**
 * Representa un evento registrado en la aplicación AppEventos.
 *
 * <p>Esta clase constituye el modelo de datos principal de la aplicación.
 * Cada instancia contiene un identificador único, una categoría y un
 * conjunto de datos específicos que dependen del tipo de evento.</p>
 *
 * <p>Las categorías contempladas son Música, Tecnología, Deportes y Cultura.
 * Los datos específicos se almacenan en un array de cadenas cuyo orden
 * debe respetar la estructura definida por el formulario correspondiente.
 * Esta organización permite mantener la compatibilidad con el sistema
 * de persistencia basado en archivos CSV.</p>
 *
 * <p>La clase protege el estado interno del array mediante copias defensivas,
 * evitando que las modificaciones externas alteren accidentalmente los
 * datos almacenados en el objeto.</p>
 *
 * @author Cristian
 * @version 1.0
 * @see com.example.appeventos.persistence.RepositorioEventosCSV
 */
public class Evento {

    private final String id;
    private final String categoria;
    private String[] datos;

    /**
     * Construye un evento nuevo y genera automáticamente su identificador.
     *
     * <p>El identificador se obtiene mediante {@link UUID#randomUUID()},
     * lo que permite distinguir cada evento de los demás registros.</p>
     *
     * @param categoria categoría a la que pertenece el evento
     *                   (Música, Tecnología, Deportes o Cultura)
     * @param datos array con los valores específicos del evento, organizados
     *              según el orden establecido por su formulario
     */
    public Evento(String categoria, String[] datos) {
        this(UUID.randomUUID().toString(), categoria, datos);
    }

    /**
     * Construye un evento utilizando un identificador previamente existente.
     *
     * <p>Este constructor se utiliza principalmente cuando se recuperan
     * registros desde los archivos CSV o cuando se necesita reconstruir
     * un evento conservando su identidad original.</p>
     *
     * <p>El array recibido se copia para evitar que el código que invoca
     * al constructor pueda modificar directamente el estado interno
     * del objeto.</p>
     *
     * @param id identificador único del evento
     * @param categoria categoría del evento
     * @param datos información específica del evento
     */
    public Evento(String id, String categoria, String[] datos) {
        this.id = id;
        this.categoria = categoria;
        this.datos = datos == null ? new String[0] : Arrays.copyOf(datos, datos.length);
    }

    /**
     * Obtiene el identificador único del evento.
     *
     * @return identificador del evento
     */
    public String getId() {
        return id;
    }

    /**
     * Obtiene la categoría a la que pertenece el evento.
     *
     * @return categoría del evento
     */
    public String getCategoria() {
        return categoria;
    }

    /**
     * Obtiene los datos específicos del evento.
     *
     * <p>Devuelve una copia del array interno para preservar la
     * encapsulación del modelo y evitar modificaciones externas
     * no controladas.</p>
     *
     * @return copia del array que contiene los datos del evento
     */
    public String[] getDatos() {
        return Arrays.copyOf(datos, datos.length);
    }

    /**
     * Sustituye los datos específicos del evento.
     *
     * <p>Los valores recibidos se copian antes de almacenarse. Si el
     * argumento es {@code null}, se establece un array vacío para
     * mantener el estado interno en una condición segura.</p>
     *
     * @param datos nuevo conjunto de datos del evento; puede ser
     *              {@code null}, en cuyo caso se utilizará un array vacío
     */
    public void setDatos(String[] datos) {
        this.datos = datos == null ? new String[0] : Arrays.copyOf(datos, datos.length);
    }

    /**
     * Obtiene el nombre del evento.
     *
     * <p>El nombre se encuentra en la primera posición del array de datos,
     * independientemente de la categoría.</p>
     *
     * @return nombre del evento o una cadena vacía si el dato no está disponible
     */
    public String getNombre() {
        return dato(0);
    }

    /**
     * Obtiene el lugar donde se celebra el evento.
     *
     * <p>La posición del lugar dentro del array depende de la categoría:</p>
     * <ul>
     *   <li>Tecnología: posición 4.</li>
     *   <li>Deportes: posición 3.</li>
     *   <li>Música y Cultura: posición 2.</li>
     * </ul>
     *
     * @return lugar del evento o una cadena vacía si la categoría
     *         no está reconocida o el dato no está disponible
     */
    public String getLugar() {

        return switch (categoria) {

            case "Tecnología" -> dato(4);

            case "Deportes" -> dato(3);

            case "Música", "Cultura" -> dato(2);

            default -> "";
        };
    }

    /**
     * Obtiene la fecha de celebración del evento.
     *
     * <p>La posición de la fecha depende de la estructura de datos
     * correspondiente a cada categoría:</p>
     * <ul>
     *   <li>Tecnología: posición 5.</li>
     *   <li>Deportes: posición 4.</li>
     *   <li>Música y Cultura: posición 3.</li>
     * </ul>
     *
     * @return fecha del evento o una cadena vacía si el dato no está disponible
     */
    public String getFecha() {

        return switch (categoria) {

            case "Tecnología" -> dato(5);

            case "Deportes" -> dato(4);

            case "Música", "Cultura" -> dato(3);

            default -> "";
        };
    }

    /**
     * Obtiene la modalidad de celebración del evento.
     *
     * <p>Las modalidades utilizadas por la aplicación son Presencial,
     * Online e Híbrido. La posición del dato varía según la categoría:</p>
     * <ul>
     *   <li>Música: posición 5.</li>
     *   <li>Tecnología: posición 7.</li>
     *   <li>Deportes y Cultura: posición 6.</li>
     * </ul>
     *
     * @return modalidad del evento o una cadena vacía si no se reconoce
     *         la categoría o el dato no está disponible
     */
    public String getModalidad() {

        return switch (categoria) {

            case "Música" -> dato(5);

            case "Tecnología" -> dato(7);

            case "Deportes", "Cultura" -> dato(6);

            default -> "";
        };
    }

    /**
     * Recupera de forma segura un valor del array interno.
     *
     * <p>Este metodo auxiliar comprueba que el índice se encuentre dentro
     * de los límites del array y que el valor no sea {@code null}.
     * De esta forma, los métodos de acceso pueden evitar errores al
     * consultar posiciones inexistentes o valores no inicializados.</p>
     *
     * @param indice posición del dato que se desea recuperar
     * @return valor almacenado en la posición indicada o una cadena vacía
     *         si el índice no es válido o el valor es nulo
     */
    private String dato(int indice) {
        if (indice < 0 || indice >= datos.length || datos[indice] == null) {
            return "";
        }
        return datos[indice];
    }
}