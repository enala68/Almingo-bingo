package comun;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * GESTOR DE FICHEROS
 * ---------------------------------------------------------------------
 * ÚNICA CLASE QUE LEE Y ESCRIBE FICHEROS.
 * TANTO HOST COMO JUGADOR LO UTILIZAN.
 *
 * ESTRUCTURA DE FICHEROS:
 *   host/estado.txt            -> ESTADO ACTUAL DE LA PARTIDA
 *   host/bolas.txt             -> HISTORIAL DE BOLAS
 *   host/cartones/<nombre>.txt -> COPIA DE CADA CARTON PARA VALIDAR
 *   jugadores/<nombre>.txt     -> cCARTON DE CADA JUGADOR
 *   reclamaciones/             -> ficheros del flujo de línea/bingo (pendiente)
 */
public class GestorFicheros {

	// ---------------------------------------------------------------
	// RUTAS
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

	/*
	 * CREA TODAS LAS CARPETAS DEL PROYECTO SI NO EXISTEN TODAVÍA.
	 * AL ARRANCAR HOST Y JUGADOR.
	 * mkdirs() CREA TAMBIÉN LAS CARPETAS INTERMEDIAS Y NO FALLA SI YA EXISTEN.
	 */
	public static void crearDirectorios() {
		new File(DIR_HOST).mkdirs();
		new File(DIR_CARTONES_HOST).mkdirs();
		new File(DIR_JUGADORES).mkdirs();
		new File(DIR_RECLAMACIONES).mkdirs();
	}

	// =================================================================
	// OPERACIONES: LEER Y ESCRIBIR
	// =================================================================

	/* LEE LINEAS DE TEXTO HASTA UN MÁXIMO.
	 * COMO NO SABEMOS LINEAS TOTALES:
	 * 1. ARRAY "TEMPORAL" DE TAMAÑO MÁXIMO
	 * 2. SE COPIA A ARRAY DE TAMAÑO EXACTO
	 */
	private static String[] leerLineas(String ruta, int maxLineas) {
		File fichero = new File(ruta);
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

		// COPIAR ARRAY CON TAMAÑO JUSTO
		String[] resultado = new String[total];
		for (int i = 0; i < total; i++) {
			resultado[i] = temporal[i];
		}
		return resultado;
	}

	/* ESCRIBE ARRAY DE LINEAS EN FICHERO. */
	private static boolean escribirLineas(String ruta, String[] lineas, boolean anadir) {
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

	/** GUARDA ESTADO ACTUAL EN host/estado.txt. */
	public static void guardarEstado(Estado estado) {
		escribirLineas(RUTA_ESTADO, estado.aLineas(), false);
	}

	/* LEE ESTADO ACTUAL DESDE host/estado.txt. */
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

	/* VACIA HISTORIAL DE BOLAS. SI NO SE LLAMA AL INICIAR SE MEZCLARIAN BOLAS CON PARTIDAS ANTERIORES. */
	public static void reiniciarBolas() {
		escribirLineas(RUTA_BOLAS, new String[0], false);
	}

	/* AÑADE BOLA AL HISTORIAL (host/bolas.txt). */
	public static void guardarBola(int bola) {
		escribirLineas(RUTA_BOLAS, new String[] { String.valueOf(bola) }, true);
	}

	/* LEE TODAS LAS BOLAS QUE HAN SALIDO */
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

	/* VERIFICA SI EL JUGADOR YA TIENE CARTON PROPIO */
	public static boolean existeCarton(String nombre) {
		return new File(DIR_JUGADORES + nombre + ".txt").exists();
	}

	/*
	 * GUARDA CARTON EN 2 SITIOS:
	 * 1. jugadores/<nombre>.txt     : PARA QUE EL JUGADOR LO RECUPERE SI VUELVE.
	 * 2. host/cartones/<nombre>.txt : COPIA PARA QUE HOST REVISE QUE UNA LINEA/BINGO ES REAL.
	 */
	public static void guardarCarton(String nombre, Carton carton) {
		String[] lineas = carton.aLineas();
		escribirLineas(DIR_JUGADORES + nombre + ".txt", lineas, false);
		escribirLineas(DIR_CARTONES_HOST + nombre + ".txt", lineas, false);
	}

	/* CARGA CARTÓN GUARDADO DE JUGADOR */
	public static Carton cargarCarton(String nombre) {
		String[] lineas = leerLineas(DIR_JUGADORES + nombre + ".txt", Carton.FILAS);
		return Carton.deLineas(lineas);
	}
}
