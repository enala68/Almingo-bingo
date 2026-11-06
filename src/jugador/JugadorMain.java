package jugador;

import java.util.Scanner;
import comun.Carton;
import comun.Estado;
import comun.GestorFicheros;
import comun.Utilidades;

/*
 * CLIENTE (JUGADOR) DEL BINGO
 * ---------------------------------------------------------------------
 * FLUJO:
 *
 *   1. PEDIR NOMBRE DE USUARIO
 *   2. ESPERAR A QUE EXISTA UNA PARTIDA
 *   3. RECUPERAR CARTON GUARDADO O GENERAR UNO NUEVO.
 *   4. MOSTRAR CARTÓN.
 *   5. VIGILAR host/estado.txt CADA POCOS SEG:
 *        - SI SALE BOLA NUEVA, TACHARLA Y MOSTRAR CARTON;
 *        - LINEA O BINGO SE AVISAN;
 *        - PARTIDA SE CANCELA O FINALIZA..
 *
 * LOGICA VIVE EN:
 * CARTÓN: TACHAR, DETECTAR, LINEA, BINGO
 * GestorFicheros: COORDINA Y COMUNICA CON EL USUARIO.
 */
public class JugadorMain {

	/* CADA CUANTO SE CONSULTA EL FICHERO DE ESTADO (2 SEG). */
	private static final int INTERVALO_CONSULTA_MS = 2000;

	public static void main(String[] args) {
		System.out.println("=== BIENVENIDO AL BINGO DE ALMINGO ===");

		// SI EL JUGADOR INICIA ANTES QUE EL HOST, CREA CARPETAS INEXISTENTES
		GestorFicheros.crearDirectorios();

		// 1. NOMBRE DE USUARIO
		String nombreUsuario = pedirNombreUsuario();

		// 2. ESPERAR PARTIDA EXISTENTE. CREAR CARTÓN DESPUÉS DE LIMPIAR EL ANTERIOR

		// 3. CARTON (RECUPERADO O NUEVO)
		Carton miCarton = obtenerOCrearCarton(nombreUsuario);

		// 4. MOSTRAR CARTON
		System.out.println("\nTu cartón:");
		miCarton.mostrarCarton();

		// 4. EINICIAR BUCLE DE LA PARTIDA
		iniciarBucleJuego(miCarton);
	}

	// ========== NOMBRE DE USUARIO ==========

	/*
	 * PIDE NOMBRE DE USUARIO. SE UTILIZA PARA CREAR EL FICHERO (jugadores/<nombre>.txt).
	 *
	 * @return NOMBRE VALIDO SIN ESPACIOS EN LOS EXTREMOS
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

	/*
	 * COMPROBAR QUE NOMBRE SEA VALIDO
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

	// ========== CARTÓN ==========

	/*
	 * RECUPERA CARTON SI EXISTE O LO CREA.
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

		// SI NO HABIA CARTÓN VALIDO
		if (carton == null) {
			System.out.println("¡Bienvenid@, " + nombreUsuario + "! Generando tu cartón...");
			carton = Carton.generarCarton();
			GestorFicheros.guardarCarton(nombreUsuario, carton);
		}

		return carton;
	}

	// ========== BUCLE PRINCIPAL DEL JUEGO ==========

	/*
	 * CONSULTA ESTADO DE PARTIDA CONSTANTEMENTE.
	 * TERMINA SI: FINALIZA, CANCELA O CANTA BINGO.
	 * @param carton cartón del jugador
	 */
	private static void iniciarBucleJuego(Carton carton) {
		System.out.println("\n[ESPERANDO] Conectado al Host. Esperando extracción de bolas...");

		// VERSION 0 (ESPERANDO) CUENTA COMO "NUEVA"
		int ultimaVersionVista = -1;

		// EVITA REPETICION DE AVISO DE LINEA CANTADA
		boolean lineaCantada = false;

		boolean juegoActivo = true;

		while (juegoActivo) {
			// ESPERA A QUE FICHERO DE HOST ESTÉ LISTO.
			Estado estado = GestorFicheros.leerEstado();

			if (estado != null) {
				String fase = estado.getFase();

				// FIN DE LA PARTIDA
				if (fase.equals(Estado.FASE_CANCELADA)) {
					System.out.println("\n⚠️ [AVISO] La partida ha sido cancelada por el Host.");
					juegoActivo = false;
				} else if (fase.equals(Estado.FASE_FINALIZADA)) {
					System.out.println("\n🏁 [AVISO] La partida ha finalizado.");
					tacharBolasSalidas(carton);
					carton.mostrarCarton();

					if (carton.tieneBingo()) {
						System.out.println("🏆 ¡¡¡ HAS COMPLETADO EL BINGO !!!");
					}
					juegoActivo = false;
				}
				// FASE JUGANDO
				else if (fase.equals(Estado.FASE_JUGANDO) || fase.equals(Estado.FASE_ESPERANDO)) {

					// COMPROBAR VERSIÓN
					if (estado.getVersion() > ultimaVersionVista) {
						ultimaVersionVista = estado.getVersion();

						// COMPROBAR ÚLTIMA BOLA
						if (estado.getUltimaBola() > 0) {
							procesarNuevaBola(estado, carton);
						}

						// COMPROBAR LINEA
						if (!lineaCantada && carton.tieneLinea()) {
							System.out.println("\n✨ ¡¡¡ HAS CANTADO LÍNEA !!! (Preparando validación de sostenibilidad...)");
							lineaCantada = true;
						}

						// COMPROBAR
						if (carton.tieneBingo()) {
							System.out.println("\n🏆 ¡¡¡ HAS COMPLETADO EL BINGO COMPLETO !!!");
							juegoActivo = false;
						}
					}
				}
				// PAUSA ANTES DE NUEVA CONSULTA
				if (juegoActivo) {
					Utilidades.pausar(INTERVALO_CONSULTA_MS);
				}
			}
		}
	}

		/*
		 * ANUNCIA BOLA NUEVA, TACHA TODAS LAS BOLAS QUE HAN SALIDO Y MUESTRA CARTÓN.
		 * SI JUGADOR ENTRA TARDE SE LE TACHAN LOS NÚMEROS ANTERIORES.
		 */
		private static void procesarNuevaBola(Estado estado, Carton carton) {
			System.out.println("\n--------------------------------------------------");
			System.out.println("📢 [NUEVA BOLA] Partida #" + estado.getIdPartida()
			+ " | ¡Ha salido el " + estado.getUltimaBola() + "!");

			int[] bolasSalidas = GestorFicheros.leerBolas();

			tacharBolasSalidas(carton);
			carton.mostrarCarton();
		}

		/*
		 * TACHA TODAS LAS BOLAS DEL HISTORIAL (host/bolas.txt)
		 * AVISA DE NÚEVO NÚMERO TACHADO.
		 */
		private static void tacharBolasSalidas(Carton carton) {
			int[] bolasSalidas = GestorFicheros.leerBolas();

			for (int i = 0; i < bolasSalidas.length; i++) {
				if (carton.marcarBola(bolasSalidas[i])) {
					System.out.println("✔ ¡Número " + bolasSalidas[i] + " tachado en tu cartón!");
				}
			}
		}

		/*
		 * ESPERAR A QUE HOST CREE PARTIDA
		 * SI FICHERO ESTADO CONTIENE PARTIDA:
		 * FINALIZADA, CANCELADA O NO EXISTE -> JUGADOR ESPERA PARTIDA NUEVA.
		 * ESPERANDO O JUGANDO -> JUGADOR CONTINUPARTIDA.
		 */
		private static void esperarPartidaActiva() {
			boolean hayPartida = false;
			boolean avisado = false;

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
