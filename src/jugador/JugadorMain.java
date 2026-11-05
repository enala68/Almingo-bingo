package jugador;

import java.util.Scanner;
import comun.Carton;
import comun.Estado;
import comun.GestorFicheros;
import comun.Utilidades;

/**
 * CLIENTE (JUGADOR) DEL BINGO
 * ---------------------------------------------------------------------
 * Es el programa que ejecuta cada jugador. Su flujo es:
 *
 *   1. Pedir el nombre de usuario.
 *   2. Recuperar su cartón guardado o generar uno nuevo.
 *   3. Mostrar el cartón.
 *   4. Vigilar el fichero host/estado.txt cada pocos segundos:
 *        - si sale una bola nueva, tacharla y mostrar el cartón;
 *        - si se completa una línea o el bingo, avisar;
 *        - si la partida se finaliza o cancela, terminar.
 *
 * Toda la lógica del cartón (tachar, detectar línea/bingo) vive en la clase
 * Carton, y todo el acceso a ficheros vive en GestorFicheros. Esta clase
 * solo coordina y se comunica con el usuario.
 */
public class JugadorMain {

	/** Cada cuántos milisegundos se consulta el fichero de estado (2 s). */
	private static final int INTERVALO_CONSULTA_MS = 2000;

	public static void main(String[] args) {
		System.out.println("=== BIENVENIDO AL BINGO DE ALMINGO ===");

		// Si el jugador arranca antes que el Host, las carpetas aún no existen
		GestorFicheros.crearDirectorios();

		// 1. Nombre del jugador
		String nombreUsuario = pedirNombreUsuario();

		// 2. Esperar a que exista una partida (así el cartón se crea DESPUÉS
		//    de que el Host haya limpiado los restos de la anterior)
		esperarPartidaActiva();

		// 3. Cartón (recuperado o nuevo)
		Carton miCarton = obtenerOCrearCarton(nombreUsuario);

		// 4. Mostrar el cartón inicial
		System.out.println("\nTu cartón:");
		miCarton.mostrarCarton();

		// 4. Escuchar al Host hasta que termine la partida
		iniciarBucleJuego(miCarton);
	}

	// =================================================================
	// NOMBRE DE USUARIO
	// =================================================================

	/**
	 * Pide el nombre por teclado hasta que sea válido.
	 * El nombre se usa para crear un fichero (jugadores/<nombre>.txt), por
	 * lo que no puede estar vacío ni contener símbolos como "/" o "..".
	 *
	 * @return nombre válido, sin espacios en los extremos
	 */
	private static String pedirNombreUsuario() {
		Scanner teclado = new Scanner(System.in);
		String nombre;

		do {
			System.out.print("Introduce tu nombre de usuario (letras, números o _): ");
			nombre = teclado.nextLine().trim();

			if (!esNombreValido(nombre)) {
				System.out.println("Nombre no válido. Inténtalo de nuevo.");
			}
		} while (!esNombreValido(nombre));

		return nombre;
	}

	/**
	 * Comprueba que un nombre no esté vacío y solo tenga letras, dígitos o "_".
	 *
	 * @param nombre texto a comprobar
	 * @return true si es válido
	 */
	private static boolean esNombreValido(String nombre) {
		if (nombre.length() == 0) {
			return false;
		}
		for (int i = 0; i < nombre.length(); i++) {
			char c = nombre.charAt(i);
			if (!Character.isLetterOrDigit(c) && c != '_') {
				return false;
			}
		}
		return true;
	}

	// =================================================================
	// CARTÓN
	// =================================================================

	/**
	 * Devuelve el cartón del jugador: el guardado si existe y es legible;
	 * en caso contrario, uno nuevo que se guarda para futuras sesiones.
	 *
	 * @param nombreUsuario nombre del jugador
	 * @return cartón listo para jugar
	 */
	private static Carton obtenerOCrearCarton(String nombreUsuario) {
		Carton carton = null;

		if (GestorFicheros.existeCarton(nombreUsuario)) {
			carton = GestorFicheros.cargarCarton(nombreUsuario);

			if (carton != null) {
				System.out.println("Bienvenid@ de nuevo, " + nombreUsuario
						+ "! Hemos recuperado tu cartón guardado.");
			} else {
				System.out.println("[AVISO] Tu cartón guardado está dañado. Se generará uno nuevo.");
			}
		}

		// Llegamos aquí si no había cartón o si estaba dañado
		if (carton == null) {
			System.out.println("¡Bienvenid@, " + nombreUsuario + "! Generando tu cartón...");
			carton = Carton.generarCarton();
			GestorFicheros.guardarCarton(nombreUsuario, carton);
		}

		return carton;
	}

	// =================================================================
	// BUCLE PRINCIPAL DEL JUEGO
	// =================================================================

	/**
	 * Consulta el estado de la partida cada pocos segundos y reacciona.
	 * Termina cuando la partida se finaliza, se cancela o el jugador
	 * completa el bingo.
	 *
	 * @param carton cartón del jugador
	 */
	private static void iniciarBucleJuego(Carton carton) {
		System.out.println("\n[ESPERANDO] Conectado al Host. Esperando extracción de bolas...");

		// -1 para que incluso la versión 0 (ESPERANDO) cuente como "nueva"
		int ultimaVersionVista = -1;

		// Evita repetir el aviso de línea en cada bola posterior
		boolean lineaCantada = false;

		boolean juegoActivo = true;

		while (juegoActivo) {
			// leerEstado() devuelve null si el fichero no está listo (el Host
			// puede estar escribiéndolo justo ahora): se ignora y se reintenta
			Estado estado = GestorFicheros.leerEstado();

			if (estado != null) {

				String fase = estado.getFase();

				// ---- Fin de partida ----
				if (fase.equals(Estado.FASE_CANCELADA)) {
					System.out.println("\n⚠️ [AVISO] La partida ha sido cancelada por el Host.");
					juegoActivo = false;
				} else if (fase.equals(Estado.FASE_FINALIZADA)) {
					System.out.println("\n🏁 [AVISO] La partida ha finalizado.");

					// Última sincronización: la partida pudo acabar entre dos
					// consultas y quedar bolas sin tachar
					tacharBolasSalidas(carton);
					carton.mostrarCarton();

					if (carton.tieneBingo()) {
						System.out.println("🏆 ¡¡¡ HAS COMPLETADO EL BINGO !!!");
					}
					juegoActivo = false;
				}
			}
		}

		// Esperar antes de volver a consultar (solo si seguimos jugando)
		if (juegoActivo) {
			Utilidades.pausar(INTERVALO_CONSULTA_MS);
		}
	}

	/**
	 * Anuncia la bola nueva, tacha en el cartón todas las bolas que han
	 * salido y muestra el cartón actualizado.
	 *
	 * Se tachan TODAS las bolas del historial (host/bolas.txt) y no solo la
	 * última. Así, si el jugador entra tarde, se reconecta o se le escapa
	 * una bola, su cartón queda siempre al día. Como marcarBola() solo
	 * devuelve true la primera vez, el aviso "tachado" no se repite.
	 *
	 * @param estado estado actual leído del fichero
	 * @param carton cartón del jugador
	 */
	private static void procesarNuevaBola(Estado estado, Carton carton) {
		System.out.println("\n--------------------------------------------------");
		System.out.println("📢 [NUEVA BOLA] Partida #" + estado.getIdPartida()
		+ " | ¡Ha salido el " + estado.getUltimaBola() + "!");

		int[] bolasSalidas = GestorFicheros.leerBolas();

		tacharBolasSalidas(carton);
		carton.mostrarCarton();
	}

	/**
	 * Tacha en el cartón TODAS las bolas del historial (host/bolas.txt) y
	 * avisa de cada número que se tacha por primera vez. Como marcarBola()
	 * solo devuelve true la primera vez, no se repiten avisos.
	 *
	 * @param carton cartón del jugador
	 */
	private static void tacharBolasSalidas(Carton carton) {
		int[] bolasSalidas = GestorFicheros.leerBolas();

		for (int i = 0; i < bolasSalidas.length; i++) {
			if (carton.marcarBola(bolasSalidas[i])) {
				System.out.println("✔ ¡Número " + bolasSalidas[i] + " tachado en tu cartón!");
			}
		}
	}

	/**
	 * Espera hasta que el Host haya creado una partida nueva o en marcha.
	 *
	 * Si el fichero de estado contiene una partida terminada (FINALIZADA o
	 * CANCELADA) o no existe, son restos de una partida anterior o el Host
	 * aún no ha arrancado, así que el jugador espera. Solo continúa cuando
	 * la fase es ESPERANDO (partida recién creada) o JUGANDO (partida ya
	 * empezada, el jugador entra tarde).
	 */
	private static void esperarPartidaActiva() {
		boolean hayPartida = false;
		boolean avisado = false; // para escribir el aviso solo una vez

		while (!hayPartida) {
			Estado estado = GestorFicheros.leerEstado();

			if (estado != null && (estado.getFase().equals(Estado.FASE_ESPERANDO)
					|| estado.getFase().equals(Estado.FASE_JUGANDO))) {
				hayPartida = true;
			} else {
				if (!avisado) {
					System.out.println("\n[ESPERANDO] No hay ninguna partida activa. "
							+ "Esperando a que el Host inicie una nueva...");
					avisado = true;
				}
				Utilidades.pausar(INTERVALO_CONSULTA_MS);
			}
		}
	}
}
