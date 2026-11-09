package comun;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Scanner;

/*
 * GESTOR DE FICHEROS
 * ---------------------------------------------------------------------
 * ÚNICA CLASE QUE LEE Y ESCRIBE FICHEROS.
 * TANTO EL HOST COMO EL JUGADOR LA UTILIZAN.
 *
 * ESTRUCTURA DE FICHEROS (dentro de la carpeta base):
 *   host/estado.txt              -> ESTADO ACTUAL DE LA PARTIDA
 *   host/bolas.txt               -> HISTORIAL DE BOLAS
 *   host/cartones/<nombre>.txt   -> COPIA DE CADA CARTÓN PARA VALIDAR
 *   host/respuestas/<nombre>.txt -> RESPUESTAS S/N DE CADA JUGADOR (UNA LÍNEA POR BOLA: "42;S")
 *   jugadores/<nombre>.txt       -> CARTÓN DE CADA JUGADOR
 *   reclamaciones/               -> FICHEROS DEL FLUJO DE LÍNEA/BINGO (PENDIENTE)
 *
 * RUTA BASE:
 *   Por defecto trabaja en la carpeta actual ("").
 *   Se puede cambiar con setRutaBase(...) para usar una carpeta compartida en red
 *   (ejemplo: "\\\\192.168.0.112\\BingoCompartido").
 */
public class GestorFicheros {

	// ---------------------------------------------------------------
	// RUTA BASE (CARPETA COMPARTIDA). POR DEFECTO LA CARPETA ACTUAL.
	// ---------------------------------------------------------------
	private static String rutaBase = "";

	/*
	 * ESTABLECE LA CARPETA RAÍZ DONDE SE CREARÁN host/, jugadores/, etc.
	 * DEBE LLAMARSE ANTES DE LLAMAR A crearDirectorios() O CUALQUIERO OTRA.
	 * ACEPTA RUTAS LOCALES o UNC (\\\\IP\\NombreCompartido).
	 */
	public static void setRutaBase(String ruta) {
		if (ruta == null || ruta.trim().isEmpty()) {
			rutaBase = "";
		} else {
			// QUITA BARRA FINAL (SI LA TRAE) PARA CONSTRUIR LAS RUTAS DE FORMA LIMPIA.
			ruta = ruta.trim().replace('/', '\\');
			while (ruta.endsWith("\\") && ruta.length() > 1) {
				ruta = ruta.substring(0, ruta.length() - 1);
			}
			rutaBase = ruta;
		}
	}

	/* DEVUELVE LA RUTA BASE ACTUAL (VACIO = CARPETA DE TRABAJO) */
	public static String getRutaBase() {
		return rutaBase;
	}

	/* CONSTRUYE RUTA COMPLETA (BASE + RUTA RELATIVA). */
	private static String ruta(String relativa) {
		if (rutaBase.isEmpty()) {
			return relativa;
		}
		// EN WINDOWS/UNC la barra correcta es \
		return rutaBase + "\\" + relativa.replace('/', '\\');
	}

	// ---------------------------------------------------------------
	// RUTAS RELATIVAS (SE COMBINAN CON rutaBase)
	// ---------------------------------------------------------------
	public static final String DIR_HOST = "host/";
	public static final String DIR_CARTONES_HOST = "host/cartones/";
	public static final String DIR_RESPUESTAS = "host/respuestas/";
	public static final String DIR_JUGADORES = "jugadores/";
	public static final String DIR_RECLAMACIONES = "reclamaciones/";

	public static final String RUTA_ESTADO = DIR_HOST + "estado.txt";
	public static final String RUTA_BOLAS = DIR_HOST + "bolas.txt";

	// =================================================================
	// PREPARACIÓN
	// =================================================================

	/*
	 * CREA TODAS LAS CARPETAS DEL PROYECTO SI NO EXISTEN TODAVÍA.
	 * SE LLAMA AL ARRANCAR EL HOST Y EL JUGADOR.
	 * mkdirs() CREA TAMBIÉN LAS CARPETAS INTERMEDIAS Y NO FALLA SI YA EXISTEN.
	 */
	public static void crearDirectorios() {
		new File(ruta(DIR_HOST)).mkdirs();
		new File(ruta(DIR_CARTONES_HOST)).mkdirs();
		new File(ruta(DIR_RESPUESTAS)).mkdirs();
		new File(ruta(DIR_JUGADORES)).mkdirs();
		new File(ruta(DIR_RECLAMACIONES)).mkdirs();
	}

	/*
	 * BORRA LOS RESTOS DE LA PARTIDA ANTERIOR PARA EMPEZAR DE CERO:
	 * 1. ELIMINA EL ESTADO VIEJO (host/estado.txt). ASÍ UN JUGADOR QUE ARRANQUE
	 *    ANTES QUE EL HOST NO LEE UNA PARTIDA TERMINADA O A MEDIAS.
	 * 2. VACÍA EL HISTORIAL DE BOLAS.
	 * 3. BORRA CARTONES (jugadores/ Y host/cartones/) Y RESPUESTAS.
	 * LO LLAMA EL HOST AL ARRANCAR.
	 */
	public static void limpiarPartidaAnterior() {
		new File(ruta(RUTA_ESTADO)).delete();
		reiniciarBolas();
		borrarFicherosDeCarpeta(DIR_JUGADORES);
		borrarFicherosDeCarpeta(DIR_CARTONES_HOST);
		borrarFicherosDeCarpeta(DIR_RESPUESTAS);
	}

	/*
	 * BORRA TODOS LOS FICHEROS DE UNA CARPETA SIN BORRAR LA CARPETA.
	 * listFiles() DEVUELVE UN ARRAY CON LO QUE HAY DENTRO (O null SI LA CARPETA NO EXISTE).
	 */
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
	// OPERACIONES BÁSICAS: LEER Y ESCRIBIR LÍNEAS
	// TODO LO DEMÁS SE CONSTRUYE ENCIMA DE ESTAS DOS.
	// =================================================================

	/*
	 * LEE LÍNEAS DE TEXTO DE UN FICHERO HASTA UN MÁXIMO.
	 * COMO NO USAMOS LISTAS Y NO SABEMOS CUÁNTAS LÍNEAS HABRÁ:
	 * 1. SE LEE EN UN ARRAY "TEMPORAL" DE TAMAÑO MÁXIMO.
	 * 2. SE COPIA A UN ARRAY DE TAMAÑO EXACTO.
	 * DEVUELVE null SI EL FICHERO NO EXISTE O NO SE PUDO LEER.
	 */
	private static String[] leerLineas(String rutaRelativa, int maxLineas) {
		File fichero = new File(ruta(rutaRelativa));
		if (!fichero.exists()) {
			return null;
		}

		String[] temporal = new String[maxLineas];
		int total = 0;

		// EL Scanner SE CIERRA SOLO AL TERMINAR EL BLOQUE
		try (Scanner lector = new Scanner(fichero)) {
			while (lector.hasNextLine() && total < maxLineas) {
				temporal[total] = lector.nextLine();
				total++;
			}
		} catch (Exception e) {
			return null;
		}

		// COPIAR A UN ARRAY CON EL TAMAÑO JUSTO
		String[] resultado = new String[total];
		for (int i = 0; i < total; i++) {
			resultado[i] = temporal[i];
		}
		return resultado;
	}

	/*
	 * ESCRIBE UN ARRAY DE LÍNEAS EN UN FICHERO.
	 * anadir = true  -> AÑADE AL FINAL DE LO QUE YA HAY.
	 * anadir = false -> BORRA LO ANTERIOR Y ESCRIBE DE CERO.
	 * DEVUELVE TRUE SI SE ESCRIBIÓ BIEN.
	 */
	private static boolean escribirLineas(String rutaRelativa, String[] lineas, boolean anadir) {
		// try-with-resources: EL PrintWriter SE CIERRA (Y GUARDA) SOLO AL TERMINAR
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
	// ESTADO DE LA PARTIDA
	// =================================================================

	/* GUARDA EL ESTADO ACTUAL EN host/estado.txt. */
	public static void guardarEstado(Estado estado) {
		escribirLineas(RUTA_ESTADO, estado.aLineas(), false);
	}

	/*
	 * LEE EL ESTADO ACTUAL DESDE host/estado.txt.
	 * DEVUELVE null SI EL FICHERO NO EXISTE, ESTÁ INCOMPLETO O TIENE DATOS
	 * ROTOS (POR EJEMPLO, SI EL HOST LO ESTABA ESCRIBIENDO JUSTO AHORA).
	 * QUIEN LLAME DEBE COMPROBAR EL null Y REINTENTAR MÁS TARDE.
	 */
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

	/* VACÍA EL HISTORIAL DE BOLAS. SI NO SE HACE AL EMPEZAR, SE MEZCLARÍAN CON LAS DE PARTIDAS ANTERIORES. */
	public static void reiniciarBolas() {
		escribirLineas(RUTA_BOLAS, new String[0], false);
	}

	/* AÑADE UNA BOLA AL HISTORIAL (host/bolas.txt). */
	public static void guardarBola(int bola) {
		escribirLineas(RUTA_BOLAS, new String[] { String.valueOf(bola) }, true);
	}

	/* LEE TODAS LAS BOLAS QUE HAN SALIDO, EN ORDEN DE SALIDA. */
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
				bolas[i] = 0; // LÍNEA ROTA: SE GUARDA 0, QUE EL CARTÓN IGNORA
			}
		}
		return bolas;
	}

	// =================================================================
	// CARTONES
	// =================================================================

	/* COMPRUEBA SI EL JUGADOR YA TIENE UN CARTÓN GUARDADO. */
	public static boolean existeCarton(String nombre) {
		return new File(ruta(DIR_JUGADORES + nombre + ".txt")).exists();
	}

	/*
	 * GUARDA EL CARTÓN EN 2 SITIOS:
	 * 1. jugadores/<nombre>.txt     : PARA QUE EL JUGADOR LO RECUPERE SI VUELVE.
	 * 2. host/cartones/<nombre>.txt : COPIA PARA QUE EL HOST COMPRUEBE QUE UNA LÍNEA/BINGO ES REAL.
	 */
	public static void guardarCarton(String nombre, Carton carton) {
		String[] lineas = carton.aLineas();
		escribirLineas(DIR_JUGADORES + nombre + ".txt", lineas, false);
		escribirLineas(DIR_CARTONES_HOST + nombre + ".txt", lineas, false);
	}

	/* CARGA EL CARTÓN GUARDADO DE UN JUGADOR (null SI NO EXISTE O ESTÁ ROTO). */
	public static Carton cargarCarton(String nombre) {
		String[] lineas = leerLineas(DIR_JUGADORES + nombre + ".txt", Carton.FILAS);
		return Carton.deLineas(lineas);
	}

	// =================================================================
	// RESPUESTAS S/N
	// =================================================================

	/*
	 * ANOTA LA RESPUESTA DE UN JUGADOR A UNA BOLA EN host/respuestas/<nombre>.txt.
	 * SE AÑADE UNA LÍNEA POR BOLA CON FORMATO "bola;S" O "bola;N". EJEMPLO: "42;S".
	 * CON ESTE FICHERO EL HOST PODRÁ REPETIR TODAS LAS RESPUESTAS SOBRE LA COPIA
	 * DEL CARTÓN Y COMPROBAR QUÉ HA PASADO DE VERDAD (SIN CREER SOLO AL JUGADOR).
	 */
	public static void guardarRespuesta(String nombre, int bola, boolean dijoSi) {
		String letra = dijoSi ? "S" : "N";
		escribirLineas(DIR_RESPUESTAS + nombre + ".txt", new String[] { bola + ";" + letra }, true);
	}
}