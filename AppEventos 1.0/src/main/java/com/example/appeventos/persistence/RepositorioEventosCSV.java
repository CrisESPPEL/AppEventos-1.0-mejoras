package com.example.appeventos.persistence;

import com.example.appeventos.model.Evento;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona la persistencia de los eventos mediante archivos CSV.
 *
 * <p>Esta clase implementa la capa de persistencia de AppEventos.
 * Se encarga de almacenar y recuperar los eventos utilizando un archivo
 * independiente para cada categoría: Música, Tecnología, Deportes
 * y Cultura.</p>
 *
 * <p>Los registros se almacenan en formato UTF-8, utilizando el punto
 * y coma como separador de campos. Cada registro comienza con el
 * identificador del evento, seguido de los datos específicos de su
 * categoría.</p>
 *
 * <p>La clase incluye mecanismos para validar la estructura de los
 * registros, gestionar caracteres especiales mediante el escapado
 * de campos y actualizar registros existentes sin generar duplicados.</p>
 *
 * <p>Las operaciones de lectura y escritura pueden producir excepciones
 * {@link IOException}, que deberán ser gestionadas por las capas
 * superiores de la aplicación.</p>
 *
 * @author Cristian
 * @version 1.0
 * @see com.example.appeventos.model.Evento
 */
public class RepositorioEventosCSV {

    private static final String[] CATEGORIAS = {"Música", "Tecnología", "Deportes", "Cultura"};
    private final Path carpetaDatos = Path.of("datos");

    /**
     * Recupera todos los eventos almacenados en los archivos CSV.
     *
     * <p>El metodo recorre las cuatro categorías disponibles y consulta
     * el archivo asociado a cada una. Los archivos que no existen se
     * omiten, permitiendo que la aplicación funcione aunque todavía
     * no se hayan registrado eventos de alguna categoría.</p>
     *
     * <p>Antes de construir cada objeto {@link Evento}, se comprueba
     * que el registro tenga el número de columnas esperado y que
     * disponga de un identificador no vacío.</p>
     *
     * @return lista con todos los eventos recuperados de los archivos CSV;
     *         la lista estará vacía cuando no existan registros
     * @throws IOException si se produce un error de lectura o si algún
     *                     registro presenta una estructura incorrecta
     */
    public List<Evento> listarTodos() throws IOException {

        List<Evento> eventos = new ArrayList<>();

        for (String categoria : CATEGORIAS) {

            Path archivo = obtenerArchivo(categoria);

            if (!Files.exists(archivo)) {
                continue;
            }

            List<List<String>> registros = leerRegistros(archivo);

            int numeroRegistro = 0;

            for (List<String> registro : registros) {

                numeroRegistro++;

                int columnasEsperadas = columnasEsperadas(categoria);

                if (registro.size() != columnasEsperadas) {

                    throw new IOException(
                            "Formato incorrecto en "
                                    + archivo.getFileName()
                                    + ", registro "
                                    + numeroRegistro
                                    + ". Se esperaban "
                                    + columnasEsperadas
                                    + " columnas, pero se encontraron "
                                    + registro.size()
                                    + "."
                    );
                }

                String id = registro.get(0);

                if (id == null || id.isBlank()) {
                    throw new IOException(
                            "El registro "
                                    + numeroRegistro
                                    + " de "
                                    + archivo.getFileName()
                                    + " no tiene identificador."
                    );
                }

                String[] datos = registro.subList(1, registro.size())
                        .toArray(new String[0]);

                eventos.add(new Evento(id, categoria, datos));
            }
        }

        return eventos;
    }

    /**
     * Determina el número de columnas que debe contener un registro CSV.
     *
     * <p>El recuento incluye la primera columna, reservada para el
     * identificador del evento. La cantidad restante depende de la
     * estructura de datos definida para cada categoría.</p>
     *
     * @param categoria categoría del evento cuyo formato se desea comprobar
     * @return número total de columnas esperadas, incluido el identificador
     * @throws IOException si la categoría no está contemplada
     *                   por el repositorio
     */
    private int columnasEsperadas(String categoria) throws IOException {

        return switch (categoria) {

            // Incluye la columna inicial del identificador.
            case "Música" -> 13;

            case "Tecnología", "Deportes" -> 15;

            case "Cultura" -> 14;

            default -> throw new IOException(
                    "Categoría desconocida: " + categoria
            );
        };
    }

    /**
     * Almacena un evento nuevo en el archivo CSV de su categoría.
     *
     * <p>El metodo determina el archivo de destino a partir de la categoría
     * del evento, crea la carpeta de datos cuando todavía no existe y
     * convierte el objeto en una secuencia de campos CSV.</p>
     *
     * <p>El registro se añade al final del archivo, sin reemplazar
     * los eventos almacenados previamente. Si el archivo todavía no
     * existe, se crea automáticamente.</p>
     *
     * @param evento evento que se desea almacenar
     * @throws IOException si no se puede crear la carpeta, acceder
     *                     al archivo o escribir el registro
     */
    public void guardar(Evento evento) throws IOException {
        Path archivo = obtenerArchivo(evento.getCategoria());
        Files.createDirectories(carpetaDatos);
        String registro = serializar(crearCampos(evento)) + System.lineSeparator();
        Files.writeString(archivo, registro, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Actualiza los datos de un evento previamente almacenado.
     *
     * <p>La identificación del registro se realiza mediante el identificador
     * único del evento. Cuando se encuentra una coincidencia, se sustituye
     * la fila completa por la nueva información, conservando la posición
     * del registro dentro del archivo.</p>
     *
     * <p>La operación no añade una fila nueva. Si el identificador no
     * aparece en el archivo correspondiente, se informa del problema
     * mediante una excepción.</p>
     *
     * <p>Una vez modificados los registros en memoria, el contenido
     * actualizado se escribe mediante un archivo temporal. Se intenta
     * realizar un movimiento atómico para reducir el riesgo de dejar
     * el CSV parcialmente escrito si se interrumpe la sustitución.</p>
     *
     * @param eventoActualizado evento con el identificador original y
     *                          los nuevos datos que se desean guardar
     * @throws IOException si el archivo no existe, no se encuentra
     *                     el evento o falla la operación de actualización
     */
    public void actualizar(Evento eventoActualizado) throws IOException {
        Path archivo = obtenerArchivo(eventoActualizado.getCategoria());
        if (!Files.exists(archivo)) {
            throw new IOException("No existe el fichero CSV del evento que se quiere modificar.");
        }

        List<List<String>> registros = leerRegistros(archivo);
        boolean encontrado = false;
        for (int i = 0; i < registros.size(); i++) {
            List<String> registro = registros.get(i);
            if (!registro.isEmpty() && registro.get(0).equals(eventoActualizado.getId())) {
                registros.set(i, crearCampos(eventoActualizado));
                encontrado = true;
                break;
            }
        }

        if (!encontrado) {
            throw new IOException("No se ha encontrado el evento original en el fichero CSV.");
        }

        StringBuilder contenido = new StringBuilder();
        for (List<String> registro : registros) {
            contenido.append(serializar(registro)).append(System.lineSeparator());
        }

        escribirArchivoAtomico(archivo, contenido.toString());
    }

    /**
     * Sustituye el contenido de un archivo utilizando un archivo temporal.
     *
     * <p>Primero escribe el contenido completo en un archivo temporal
     * situado en el mismo directorio que el archivo original. Después
     * intenta reemplazar el original mediante {@link StandardCopyOption#ATOMIC_MOVE}.
     * Si el sistema de archivos no admite movimientos atómicos, utiliza
     * una sustitución convencional.</p>
     *
     * <p>El archivo temporal se elimina al finalizar la operación,
     * tanto si la escritura termina correctamente como si se produce
     * una excepción.</p>
     *
     * @param archivo ruta del archivo CSV que se desea sustituir
     * @param contenido nuevo contenido completo que se escribirá
     * @throws IOException si falla la creación, escritura, sustitución
     *                     o eliminación del archivo temporal
     */
    private void escribirArchivoAtomico(Path archivo, String contenido)
            throws IOException {

        Path temporal = Files.createTempFile(
                archivo.getParent(),
                archivo.getFileName().toString(),
                ".tmp"
        );

        try {

            Files.writeString(
                    temporal,
                    contenido,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            try {

                Files.move(
                        temporal,
                        archivo,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (AtomicMoveNotSupportedException e) {

                // Algunos sistemas de archivos no admiten movimientos atómicos.
                Files.move(
                        temporal,
                        archivo,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } finally {

            Files.deleteIfExists(temporal);
        }
    }

    /**
     * Convierte un evento en una lista ordenada de campos.
     *
     * <p>El identificador se añade siempre en primera posición.
     * A continuación, se incorporan los datos específicos devueltos
     * por el modelo.</p>
     *
     * @param evento evento que se desea convertir
     * @return lista de campos preparada para su serialización CSV
     */
    private List<String> crearCampos(Evento evento) {
        List<String> campos = new ArrayList<>();
        campos.add(evento.getId());
        campos.addAll(List.of(evento.getDatos()));
        return campos;
    }

    /**
     * Obtiene la ruta del archivo CSV correspondiente a una categoría.
     *
     * <p>Cada categoría dispone de un archivo independiente dentro
     * de la carpeta de datos:</p>
     * <ul>
     *   <li>Música: {@code Eventos_Musica.csv}.</li>
     *   <li>Tecnología: {@code Eventos_Tecnologia.csv}.</li>
     *   <li>Deportes: {@code Eventos_Deportes.csv}.</li>
     *   <li>Cultura: {@code Eventos_Cultura.csv}.</li>
     * </ul>
     *
     * @param categoria categoría cuyo archivo se desea localizar
     * @return ruta del archivo CSV correspondiente
     * @throws IOException si la categoría no está reconocida
     */
    private Path obtenerArchivo(String categoria) throws IOException {
        String nombre;
        switch (categoria) {
            case "Música" -> nombre = "Eventos_Musica.csv";
            case "Tecnología" -> nombre = "Eventos_Tecnologia.csv";
            case "Deportes" -> nombre = "Eventos_Deportes.csv";
            case "Cultura" -> nombre = "Eventos_Cultura.csv";
            default -> throw new IOException("Categoría no válida: " + categoria);
        }
        return carpetaDatos.resolve(nombre);
    }

    /**
     * Convierte una lista de campos en una única línea CSV.
     *
     * <p>Cada valor se procesa previamente mediante {@link #escapar(String)}
     * para proteger los caracteres que podrían interferir con la estructura
     * del archivo. Finalmente, los campos se unen utilizando el punto
     * y coma como delimitador.</p>
     *
     * @param campos valores que componen el registro
     * @return representación CSV del registro
     */
    private String serializar(List<String> campos) {
        List<String> escapados = new ArrayList<>();
        for (String campo : campos) {
            escapados.add(escapar(campo));
        }
        return String.join(";", escapados);
    }

    /**
     * Prepara un campo de texto para su almacenamiento en formato CSV.
     *
     * <p>Los valores que contienen puntos y coma, comillas dobles o
     * saltos de línea se rodean de comillas dobles. Las comillas
     * que forman parte del propio texto se duplican para cumplir
     * con las reglas de escapado utilizadas por el lector CSV.</p>
     *
     * <p>Los valores nulos se representan mediante una cadena vacía.</p>
     *
     * @param texto valor original que se desea escapar
     * @return texto preparado para incorporarse a un registro CSV
     */
    private String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        if (texto.contains(";") || texto.contains("\"")
                || texto.contains("\n") || texto.contains("\r")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    /**
     * Lee y analiza los registros contenidos en un archivo CSV.
     *
     * <p>El archivo se interpreta utilizando UTF-8 y el punto y coma
     * como separador de campos. El análisis tiene en cuenta los campos
     * entrecomillados, las comillas dobles escapadas y los saltos
     * de línea que forman parte del contenido de un campo.</p>
     *
     * <p>El metodo mantiene el seguimiento del estado de lectura para
     * distinguir entre separadores reales y caracteres que pertenecen
     * al contenido de un campo. También detecta estructuras incorrectas,
     * como comillas en posiciones no permitidas, contenido después
     * del cierre de comillas o campos entrecomillados sin cerrar.</p>
     *
     * @param archivo ruta del archivo CSV que se desea analizar
     * @return lista de registros, donde cada registro contiene
     *         sus campos en el orden original
     * @throws IOException si no se puede leer el archivo o si su
     *                     estructura CSV no es válida
     */
    private List<List<String>> leerRegistros(Path archivo)
            throws IOException {

        String contenido = Files.readString(
                archivo,
                StandardCharsets.UTF_8
        );

        List<List<String>> registros = new ArrayList<>();

        List<String> campos = new ArrayList<>();

        StringBuilder campo = new StringBuilder();

        boolean entreComillas = false;
        boolean despuesDeComillas = false;
        boolean registroConContenido = false;

        int numeroRegistro = 1;

        for (int i = 0; i < contenido.length(); i++) {

            char caracter = contenido.charAt(i);

            // Estamos leyendo un campo entre comillas.
            if (entreComillas) {

                if (caracter == '"') {

                    // Dos comillas consecutivas representan una comilla literal.
                    if (i + 1 < contenido.length()
                            && contenido.charAt(i + 1) == '"') {

                        campo.append('"');
                        i++;

                    } else {

                        entreComillas = false;
                        despuesDeComillas = true;
                    }

                } else {

                    campo.append(caracter);
                }

                continue;
            }

            // Después de cerrar comillas solo se permite separador o fin de línea.
            if (despuesDeComillas
                    && caracter != ';'
                    && caracter != '\n'
                    && caracter != '\r') {

                throw new IOException(
                        "CSV mal formado en el registro "
                                + numeroRegistro
                                + ": hay contenido después de cerrar las comillas."
                );
            }

            // Inicio de un campo entrecomillado.
            if (caracter == '"') {

                if (campo.length() != 0 || despuesDeComillas) {

                    throw new IOException(
                            "CSV mal formado en el registro "
                                    + numeroRegistro
                                    + ": comillas en una posición incorrecta."
                    );
                }

                entreComillas = true;
                registroConContenido = true;

            } else if (caracter == ';') {

                campos.add(campo.toString());

                campo.setLength(0);

                despuesDeComillas = false;
                registroConContenido = true;

            } else if (caracter == '\n' || caracter == '\r') {

                // Tratar CRLF como un único salto de línea.
                if (caracter == '\r'
                        && i + 1 < contenido.length()
                        && contenido.charAt(i + 1) == '\n') {

                    i++;
                }

                campos.add(campo.toString());

                campo.setLength(0);

                // Ignorar líneas completamente vacías.
                if (registroConContenido
                        || campos.size() > 1
                        || !campos.get(0).isEmpty()) {

                    registros.add(new ArrayList<>(campos));
                }

                campos.clear();

                despuesDeComillas = false;
                registroConContenido = false;

                numeroRegistro++;

            } else {

                campo.append(caracter);

                registroConContenido = true;
            }
        }

        // No se puede terminar el archivo dentro de unas comillas.
        if (entreComillas) {

            throw new IOException(
                    "CSV mal formado: el archivo termina con unas comillas sin cerrar."
            );
        }

        // Añadir el último registro si el archivo no termina en salto de línea.
        if (registroConContenido
                || !campos.isEmpty()
                || campo.length() > 0
                || despuesDeComillas) {

            campos.add(campo.toString());

            registros.add(new ArrayList<>(campos));
        }

        return registros;
    }
}
