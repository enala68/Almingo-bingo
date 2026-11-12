package comun;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Scanner;

/*
 * GESTOR DE FICHEROS.
 * ÚNICA CLASE QUE LEE Y ESCRIBE FICHEROS (LA USAN HOST Y JUGADOR).
 *
 * ESTRUCTURA (DENTRO DE LA CARPETA BASE):
 *   host/estado.txt              -> ESTADO DE LA PARTIDA
 *   host/bolas.txt               -> HISTORIAL DE BOLAS
 *   host/bote.txt                -> BOTE EN CÉNTIMOS (LO ESCRIBE EL HOST)
 *   host/cartones/<nombre>.txt   -> COPIA DE CADA CARTÓN PARA VALIDAR
 *   host/respuestas/<nombre>.txt -> RESPUESTAS S/N (UNA LÍNEA POR BOLA: "42;S")
 *   jugadores/<nombre>.txt       -> CARTÓN DE CADA JUGADOR
 *   reclamaciones/               -> FICHEROS DE LÍNEA/BINGO
 *
 * RUTA BASE: POR DEFECTO LA ACTUAL (""). SE CAMBIA CON setRutaBase(...) PARA USAR
 * UNA CARPETA EN RED (EJEMPLO: "\\\\192.168.0.112\\BingoCompartido").
 */
public class GestorFicheros {

	// ---------------------------------------------------------------
	// RUTA BASE (POR DEFECTO, CARPETA ACTUAL)
	// ---------------------------------------------------------------
	private static String rutaBase = "";

	/** ESTABLECE LA CARPETA RAÍZ. LLAMAR ANTES DE CUALQUIER OTRA OPERACIÓN. */
	public static void setRutaBase(String ruta) {
		if (ruta == null || ruta.trim().isEmpty()) {
			rutaBase = "";
		} else {
			ruta = ruta.trim().replace('/', '\\');
			while (ruta.endsWith("\\") && ruta.length() > 1) {
				ruta = ruta.substring(0, ruta.length() - 1);
			}
			rutaBase = ruta;
		}
	}

	/** DEVUELVE LA RUTA BASE (VACÍA = CARPETA DE TRABAJO). */
	public static String getRutaBase() {
		return rutaBase;
	}

	/** CONSTRUYE LA RUTA COMPLETA: BASE + RELATIVA. */
	private static String ruta(String relativa) {
		if (rutaBase.isEmpty()) {
			return relativa;
		}
		return rutaBase + "\\" + relativa.replace('/', '\\');
	}

	// ---------------------------------------------------------------
	// RUTAS RELATIVAS
	// ---------------------------------------------------------------
	public static final String DIR_HOST = "host/";
	public static final String DIR_CARTONES_HOST = "host/cartones/";
	public static final String DIR_RESPUESTAS = "host/respuestas/";
	public static final String DIR_JUGADORES = "jugadores/";
	public static final String DIR_RECLAMACIONES = "reclamaciones/";

	public static final String RUTA_ESTADO = DIR_HOST + "estado.txt";
	public static final String RUTA_BOLAS = DIR_HOST + "bolas.txt";
	public static final String RUTA_BOTE = DIR_HOST + "bote.txt";

	// =================================================================
	// PREPARACIÓN
	// =================================================================

	public static void crearDirectorios() {
		new File(ruta(DIR_HOST)).mkdirs();
		new File(ruta(DIR_CARTONES_HOST)).mkdirs();
		new File(ruta(DIR_RESPUESTAS)).mkdirs();
		new File(ruta(DIR_JUGADORES)).mkdirs();
		new File(ruta(DIR_RECLAMACIONES)).mkdirs();
	}

	public static void limpiarPartidaAnterior() {
		new File(ruta(RUTA_ESTADO)).delete();
		new File(ruta(RUTA_BOTE)).delete();
		reiniciarBolas();
		borrarFicherosDeCarpeta(DIR_JUGADORES);
		borrarFicherosDeCarpeta(DIR_CARTONES_HOST);
		borrarFicherosDeCarpeta(DIR_RESPUESTAS);
		borrarFicherosDeCarpeta(DIR_RECLAMACIONES);
	}

	private static void borrarFicherosDeCarpeta(String rutaRelativa) {
		File[] ficheros = new File(ruta(rutaRelativa)).listFiles();
		if (ficheros == null) {
			return;
		}
		for (File fichero : ficheros) {
			if (fichero.isFile()) {
				fichero.delete();
			}
		}
	}

	// =================================================================
	// OPERACIONES BÁSICAS
	// =================================================================

	private static String[] leerLineas(String rutaRelativa, int maxLineas) {
		File fichero = new File(ruta(rutaRelativa));
		if (!fichero.exists()) {
			return null;
		}

		String[] temporal = new String[maxLineas];
		int total = 0;

		try (Scanner lector = new Scanner(fichero)) {
			while (lector.hasNextLine() && total < maxLineas) {
				temporal[total] = lector.nextLine();
				total++;
			}
		} catch (Exception e) {
			return null;
		}

		String[] resultado = new String[total];
		for (int i = 0; i < total; i++) {
			resultado[i] = temporal[i];
		}
		return resultado;
	}

	private static boolean escribirLineas(String rutaRelativa, String[] lineas, boolean anadir) {
		try (PrintWriter escritor = new PrintWriter(new FileWriter(ruta(rutaRelativa), anadir))) {
			for (String linea : lineas) {
				escritor.println(linea);
			}
			return true;
		} catch (Exception e) {
			System.err.println("Error al escribir '" + ruta(rutaRelativa) + "': " + e.getMessage());
			return false;
		}
	}

	// =================================================================
	// ESTADO
	// =================================================================

	public static void guardarEstado(Estado estado) {
		escribirLineas(RUTA_ESTADO, estado.aLineas(), false);
	}

	public static Estado leerEstado() {
		String[] lineas = leerLineas(RUTA_ESTADO, Estado.NUM_LINEAS);

		if (lineas == null || lineas.length < Estado.NUM_LINEAS) {
			return null;
		}

		try {
			return Estado.deLineas(lineas);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	// =================================================================
	// BOLAS
	// =================================================================

	public static void reiniciarBolas() {
		escribirLineas(RUTA_BOLAS, new String[0], false);
	}

	public static void guardarBola(int bola) {
		escribirLineas(RUTA_BOLAS, new String[] { String.valueOf(bola) }, true);
	}

	public static int[] leerBolas() {
		String[] lineas = leerLineas(RUTA_BOLAS, Bombo.TOTAL_BOLAS);
		if (lineas == null) {
			return new int[0];
		}

		int[] bolas = new int[lineas.length];
		for (int i = 0; i < lineas.length; i++) {
			try {
				bolas[i] = Integer.parseInt(lineas[i].trim());
			} catch (NumberFormatException e) {
				bolas[i] = 0;
			}
		}
		return bolas;
	}

	// =================================================================
	// BOTE
	// =================================================================

	/* GUARDA EL BOTE (EN CÉNTIMOS) EN host/bote.txt. SOLO LO ESCRIBE EL HOST. */
	public static void guardarBote(int centimos) {
		escribirLineas(RUTA_BOTE, new String[] { String.valueOf(centimos) }, false);
	}

	/* LEE EL BOTE EN CÉNTIMOS. DEVUELVE -1 SI NO EXISTE AÚN O SE LEE A MEDIAS. */
	public static int leerBote() {
		String[] lineas = leerLineas(RUTA_BOTE, 1);
		if (lineas == null || lineas.length == 0) {
			return -1;
		}
		try {
			return Integer.parseInt(lineas[0].trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	// =================================================================
	// CARTONES
	// =================================================================

	public static boolean existeCarton(String nombre) {
		return new File(ruta(DIR_JUGADORES + nombre + ".txt")).exists();
	}

	public static void guardarCarton(String nombre, Carton carton) {
		String[] lineas = carton.aLineas();
		escribirLineas(DIR_JUGADORES + nombre + ".txt", lineas, false);
		escribirLineas(DIR_CARTONES_HOST + nombre + ".txt", lineas, false);
	}

	public static Carton cargarCarton(String nombre) {
		String[] lineas = leerLineas(DIR_JUGADORES + nombre + ".txt", Carton.FILAS);
		return Carton.deLineas(lineas);
	}

	public static Carton cargarCartonHost(String nombre) {
		String[] lineas = leerLineas(DIR_CARTONES_HOST + nombre + ".txt", Carton.FILAS);
		return Carton.deLineas(lineas);
	}

	/* DEVUELVE LOS NOMBRES DE LOS JUGADORES (UN CARTÓN EN host/cartones/ POR JUGADOR). */
	public static String[] listarJugadores() {
		File[] ficheros = new File(ruta(DIR_CARTONES_HOST)).listFiles();
		if (ficheros == null) {
			return new String[0];
		}

		int total = 0;
		String[] temporal = new String[ficheros.length];
		for (File f : ficheros) {
			String nombreFichero = f.getName();
			if (f.isFile() && nombreFichero.endsWith(".txt")) {
				temporal[total] = nombreFichero.substring(0, nombreFichero.length() - ".txt".length());
				total++;
			}
		}

		String[] resultado = new String[total];
		for (int i = 0; i < total; i++) {
			resultado[i] = temporal[i];
		}
		return resultado;
	}

	// =================================================================
	// RESPUESTAS S/N
	// =================================================================

	public static void guardarRespuesta(String nombre, int bola, boolean dijoSi) {
		String letra = dijoSi ? "S" : "N";
		escribirLineas(DIR_RESPUESTAS + nombre + ".txt", new String[] { bola + ";" + letra }, true);
	}

	public static String[][] leerRespuestas(String nombre) {
		String[] lineas = leerLineas(DIR_RESPUESTAS + nombre + ".txt", Bombo.TOTAL_BOLAS);
		if (lineas == null || lineas.length == 0) {
			return new String[0][0];
		}
		String[][] resultado = new String[lineas.length][2];
		for (int i = 0; i < lineas.length; i++) {
			String[] partes = lineas[i].split(";");
			if (partes.length == 2) {
				resultado[i][0] = partes[0].trim();
				resultado[i][1] = partes[1].trim().toUpperCase();
			} else {
				resultado[i][0] = "0";
				resultado[i][1] = "N";
			}
		}
		return resultado;
	}

	// =================================================================
	// RECLAMACIONES (LÍNEA / BINGO)
	// =================================================================

	private static String rutaReclamo(String nombre) {
		return DIR_RECLAMACIONES + nombre + "_reclamo.txt";
	}

	private static String rutaPregunta(String nombre) {
		return DIR_RECLAMACIONES + nombre + "_pregunta.txt";
	}

	private static String rutaRespuestaReclamo(String nombre) {
		return DIR_RECLAMACIONES + nombre + "_respuesta.txt";
	}

	private static String rutaResultado(String nombre) {
		return DIR_RECLAMACIONES + nombre + "_resultado.txt";
	}

	public static void guardarReclamo(String nombre, String tipo) {
		escribirLineas(rutaReclamo(nombre), new String[] { tipo }, false);
	}

	public static String leerReclamo(String nombre) {
		String[] lineas = leerLineas(rutaReclamo(nombre), 1);
		if (lineas == null || lineas.length == 0) {
			return null;
		}
		return lineas[0].trim().toUpperCase();
	}

	public static boolean existeReclamo(String nombre) {
		return new File(ruta(rutaReclamo(nombre))).exists();
	}

	public static String[] listarReclamosPendientes() {
		File carpeta = new File(ruta(DIR_RECLAMACIONES));
		File[] ficheros = carpeta.listFiles();
		if (ficheros == null || ficheros.length == 0) {
			return new String[0];
		}

		int total = 0;
		String[] temporal = new String[ficheros.length];
		for (File f : ficheros) {
			String nombreFichero = f.getName();
			if (nombreFichero.endsWith("_reclamo.txt")) {
				String nombre = nombreFichero.substring(0, nombreFichero.length() - "_reclamo.txt".length());
				if (!new File(ruta(rutaResultado(nombre))).exists()) {
					temporal[total] = nombre;
					total++;
				}
			}
		}

		String[] resultado = new String[total];
		for (int i = 0; i < total; i++) {
			resultado[i] = temporal[i];
		}
		return resultado;
	}

	public static void guardarPreguntaReclamo(String nombre, String[] lineasPregunta) {
		escribirLineas(rutaPregunta(nombre), lineasPregunta, false);
	}

	public static String[] leerPreguntaReclamo(String nombre) {
		return leerLineas(rutaPregunta(nombre), 10);
	}

	public static void guardarRespuestaReclamo(String nombre, String letra) {
		escribirLineas(rutaRespuestaReclamo(nombre), new String[] { letra.trim().toUpperCase() }, false);
	}

	public static String leerRespuestaReclamo(String nombre) {
		String[] lineas = leerLineas(rutaRespuestaReclamo(nombre), 1);
		if (lineas == null || lineas.length == 0) {
			return null;
		}
		return lineas[0].trim().toUpperCase();
	}

	public static void guardarResultadoReclamo(String nombre, String resultado) {
		escribirLineas(rutaResultado(nombre), new String[] { resultado }, false);
	}

	public static String leerResultadoReclamo(String nombre) {
		String[] lineas = leerLineas(rutaResultado(nombre), 1);
		if (lineas == null || lineas.length == 0) {
			return null;
		}
		return lineas[0].trim().toUpperCase();
	}

	public static void limpiarFicherosReclamo(String nombre) {
		new File(ruta(rutaReclamo(nombre))).delete();
		new File(ruta(rutaPregunta(nombre))).delete();
		new File(ruta(rutaRespuestaReclamo(nombre))).delete();
		new File(ruta(rutaResultado(nombre))).delete();
	}
}