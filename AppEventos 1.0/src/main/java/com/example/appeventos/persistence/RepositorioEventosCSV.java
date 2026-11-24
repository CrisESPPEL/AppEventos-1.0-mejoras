package com.example.appeventos.persistence;

import com.example.appeventos.model.Evento;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** Centraliza la lectura, el alta y la actualización de los ficheros CSV. */
public class RepositorioEventosCSV {

    private static final String[] CATEGORIAS = {"Música", "Tecnología", "Deportes", "Cultura"};
    private final Path carpetaDatos = Path.of("datos");

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

    /** Controlamos el número esperado de columnas por categoría */
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

    public void guardar(Evento evento) throws IOException {
        Path archivo = obtenerArchivo(evento.getCategoria());
        Files.createDirectories(carpetaDatos);
        String registro = serializar(crearCampos(evento)) + System.lineSeparator();
        Files.writeString(archivo, registro, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Sustituye la fila cuyo identificador coincide. No añade una fila nueva,
     * por lo que la edición no crea un registro duplicado.
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

    private List<String> crearCampos(Evento evento) {
        List<String> campos = new ArrayList<>();
        campos.add(evento.getId());
        campos.addAll(List.of(evento.getDatos()));
        return campos;
    }

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

    private String serializar(List<String> campos) {
        List<String> escapados = new ArrayList<>();
        for (String campo : campos) {
            escapados.add(escapar(campo));
        }
        return String.join(";", escapados);
    }

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

    /** Lee registros completos, incluidos los campos que contengan saltos de línea entre comillas. */
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
