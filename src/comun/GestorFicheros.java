package comun;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * GESTOR DE FICHEROS (capa de persistencia)
 * ---------------------------------------------------------------------
 * Es la ÚNICA clase del proyecto que lee y escribe ficheros. El resto
 * (HostMain, JugadorMain...) le pide cosas del estilo "guarda este estado"
 * o "dame las bolas que han salido" sin saber nada de File, Scanner o
 * PrintWriter.
 *
 * Está en el paquete "comun" (y no en "host") porque tanto el Host como
 * los Jugadores necesitan leer y escribir ficheros. Así el código de
 * lectura/escritura se escribe UNA sola vez.
 *
 * ESTRUCTURA DE FICHEROS:
 *   host/estado.txt            -> estado actual de la partida
 *   host/bolas.txt             -> historial de bolas (una por línea)
 *   host/cartones/<nombre>.txt -> copia del cartón de cada jugador (para validar)
 *   jugadores/<nombre>.txt     -> cartón guardado del jugador
 *   reclamaciones/             -> ficheros del flujo de línea/bingo (pendiente)
 */
public class GestorFicheros {

	// ---------------------------------------------------------------
	// RUTAS (definidas en un único sitio; si cambian, se cambian aquí)
	// ---------------------------------------------------------------
	public static final String DIR_HOST = "host/";
	public static final String DIR_CARTONES_HOST = "host/cartones/";
	public static final String DIR_JUGADORES = "jugadores/";
	public static final String DIR_RECLAMACIONES = "reclamaciones/";

	public static final String RUTA_ESTADO = DIR_HOST + "estado.txt";
	public static final String RUTA_BOLAS = DIR_HOST + "bolas.txt";

	// =================================================================
	// PREPARACIÓN
	// =================================================================

	/**
	 * Crea todas las carpetas del proyecto si no existen todavía.
	 * Hay que llamarlo al arrancar Host y Jugador: si la carpeta no existe,
	 * al intentar crear un fichero dentro Java lanza un error.
	 * mkdirs() crea también las carpetas intermedias y no falla si ya existe.
	 */
	public static void crearDirectorios() {
		new File(DIR_HOST).mkdirs();
		new File(DIR_CARTONES_HOST).mkdirs();
		new File(DIR_JUGADORES).mkdirs();
		new File(DIR_RECLAMACIONES).mkdirs();
	}

	// =================================================================
	// OPERACIONES BÁSICAS (privadas): leer y escribir líneas de texto
	// Todo lo demás se construye encima de estas dos.
	// =================================================================

	/**
	 * Lee las líneas de un fichero de texto (hasta un máximo).
	 *
	 * Como no usamos listas, no sabemos de antemano cuántas líneas habrá.
	 * Por eso se lee primero en un array "temporal" del tamaño máximo y al
	 * final se copia a otro array del tamaño EXACTO.
	 *
	 * @param ruta      ruta del fichero
	 * @param maxLineas número máximo de líneas a leer
	 * @return array con las líneas leídas, o null si el fichero no existe
	 *         o no se pudo leer
	 */
	private static String[] leerLineas(String ruta, int maxLineas) {
		File fichero = new File(ruta);
		if (!fichero.exists()) {
			return null;
		}

		String[] temporal = new String[maxLineas];
		int total = 0;

		// try-with-resources: el Scanner se cierra solo al terminar el bloque
		try (Scanner lector = new Scanner(fichero)) {
			while (lector.hasNextLine() && total < maxLineas) {
				temporal[total] = lector.nextLine();
				total++;
			}
		} catch (Exception e) {
			return null;
		}

		// Copiar al array del tamaño justo
		String[] resultado = new String[total];
		for (int i = 0; i < total; i++) {
			resultado[i] = temporal[i];
		}
		return resultado;
	}

	/**
	 * Escribe un array de líneas en un fichero.
	 *
	 * @param ruta   ruta del fichero
	 * @param lineas líneas a escribir (una por línea del fichero)
	 * @param anadir true = añadir al final de lo que ya hay;
	 *               false = borrar el contenido anterior y escribir de cero
	 * @return true si se escribió bien, false si hubo error
	 */
	private static boolean escribirLineas(String ruta, String[] lineas, boolean anadir) {
		// try-with-resources: el PrintWriter se cierra (y vuelca) solo al terminar
		try (PrintWriter escritor = new PrintWriter(new FileWriter(ruta, anadir))) {
			for (String linea : lineas) {
				escritor.println(linea);
			}
			return true;
		} catch (Exception e) {
			System.err.println("Error al escribir '" + ruta + "': " + e.getMessage());
			return false;
		}
	}

	// =================================================================
	// ESTADO DE LA PARTIDA
	// =================================================================

	/**
	 * Guarda el estado en host/estado.txt, sustituyendo el anterior.
	 *
	 * @param estado estado actual de la partida
	 */
	public static void guardarEstado(Estado estado) {
		escribirLineas(RUTA_ESTADO, estado.aLineas(), false);
	}

	/**
	 * Lee el estado actual de la partida desde host/estado.txt.
	 *
	 * @return el Estado leído, o null si el fichero no existe, está
	 *         incompleto o tiene datos incorrectos (p. ej. el Host lo
	 *         estaba escribiendo justo en ese instante). Quien llame debe
	 *         comprobar el null y volver a intentarlo más tarde.
	 */
	public static Estado leerEstado() {
		String[] lineas = leerLineas(RUTA_ESTADO, Estado.NUM_LINEAS);

		if (lineas == null || lineas.length < Estado.NUM_LINEAS) {
			return null;
		}

		try {
			return Estado.deLineas(lineas);
		} catch (NumberFormatException e) {
			return null; // fichero a medio escribir
		}
	}

	// =================================================================
	// BOLAS
	// =================================================================

	/**
	 * Vacía el historial de bolas. El Host debe llamarlo al empezar una
	 * partida nueva; si no, se mezclarían las bolas de partidas anteriores.
	 */
	public static void reiniciarBolas() {
		escribirLineas(RUTA_BOLAS, new String[0], false);
	}

	/**
	 * Añade una bola al final del historial (host/bolas.txt).
	 *
	 * @param bola número de la bola extraída
	 */
	public static void guardarBola(int bola) {
		escribirLineas(RUTA_BOLAS, new String[] { String.valueOf(bola) }, true);
	}

	/**
	 * Lee todas las bolas que han salido hasta ahora, en orden de salida.
	 *
	 * @return array con las bolas (vacío si aún no hay ninguna)
	 */
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
				bolas[i] = 0; // línea corrupta: se guarda 0, que Carton ignora
			}
		}
		return bolas;
	}

	// =================================================================
	// CARTONES
	// =================================================================

	/**
	 * @param nombre nombre del jugador
	 * @return true si ese jugador ya tiene un cartón guardado
	 */
	public static boolean existeCarton(String nombre) {
		return new File(DIR_JUGADORES + nombre + ".txt").exists();
	}

	/**
	 * Guarda el cartón del jugador en DOS sitios:
	 *   - jugadores/<nombre>.txt     : para que el jugador lo recupere si vuelve.
	 *   - host/cartones/<nombre>.txt : copia que usará el Host para comprobar
	 *                                  que una línea/bingo cantado es real.
	 *
	 * @param nombre nombre del jugador
	 * @param carton cartón a guardar
	 */
	public static void guardarCarton(String nombre, Carton carton) {
		String[] lineas = carton.aLineas();
		escribirLineas(DIR_JUGADORES + nombre + ".txt", lineas, false);
		escribirLineas(DIR_CARTONES_HOST + nombre + ".txt", lineas, false);
	}

	/**
	 * Carga el cartón guardado de un jugador.
	 *
	 * @param nombre nombre del jugador
	 * @return el cartón, o null si no existe o el fichero está corrupto
	 */
	public static Carton cargarCarton(String nombre) {
		String[] lineas = leerLineas(DIR_JUGADORES + nombre + ".txt", Carton.FILAS);
		return Carton.deLineas(lineas);
	}
}
