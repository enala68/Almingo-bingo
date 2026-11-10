package jugador;

import comun.Bombo;
import comun.Carton;
import comun.Estado;
import comun.GestorFicheros;
import comun.Reglas;
import comun.Utilidades;

/*
 * CLIENTE (JUGADOR) DEL BINGO
 * ---------------------------------------------------------------------
 * FLUJO:
 *
 *   1. PEDIR NOMBRE DE USUARIO.
 *   2. ESPERAR A QUE EL HOST CREE UNA PARTIDA NUEVA (SOLO SE PUEDE ENTRAR
 *      ANTES DE QUE SALGA LA PRIMERA BOLA).
 *   3. RECUPERAR EL CARTÓN GUARDADO O GENERAR UNO NUEVO.
 *   4. MOSTRAR EL CARTÓN Y EXPLICAR LAS REGLAS.
 *   5. VIGILAR host/bolas.txt Y host/estado.txt CADA POCO TIEMPO:
 *        - POR CADA BOLA NUEVA, PREGUNTAR AL JUGADOR SI ESTÁ EN SU CARTÓN (S/N);
 *        - SEGÚN SU RESPUESTA SE TACHA, SE PERDONA, SE PENALIZA O SE SIGUE;
 *        - SI SE COMPLETA UNA LÍNEA O EL BINGO, SE RECLAMA AL HOST (Reclamador):
 *          HAY QUE ACERTAR UNA PREGUNTA DE SOSTENIBILIDAD PARA GANAR EL PREMIO;
 *        - SE TERMINA SI LA PARTIDA SE CANCELA O FINALIZA.
 *
 * PANTALLA: POR CADA BOLA SE MUESTRAN SOLO DOS PANTALLAS LIMPIAS:
 *   - PANTALLA DE PREGUNTA:  CABECERA + BOLAS ANTERIORES + BOLA NUEVA + CARTÓN + PREGUNTA.
 *   - PANTALLA DE RESULTADO: CABECERA + QUÉ HA PASADO + CARTÓN ACTUALIZADO.
 *   ASÍ SIEMPRE HAY UN ÚNICO CARTÓN EN PANTALLA Y NO SE VA APILANDO TEXTO.
 *   LA CABECERA INCLUYE EL BOTE, QUE PUBLICA EL HOST EN host/bote.txt.
 *
 * REPARTO DEL TRABAJO:
 *   - Carton:          LA LÓGICA DEL CARTÓN (responder S/N, DETECTAR LÍNEA Y BINGO).
 *   - GestorFicheros:  LEER Y ESCRIBIR FICHEROS.
 *   - Reglas:          TIEMPOS Y CONSTANTES DEL JUEGO.
 *   - LectorTeclado:   LEE EL TECLADO CON TIEMPO LÍMITE (HILO EN SEGUNDO PLANO).
 *   - Reclamador:      PROCESO DE RECLAMAR LÍNEA/BINGO Y CONTESTAR LA PREGUNTA.
 *   - JugadorMain:     COORDINA TODO Y HABLA CON EL USUARIO USANDO LAS ANTERIORES.
 */
public class JugadorMain {

	/* LONGITUD DE LAS LÍNEAS DECORATIVAS DE LA CABECERA. */
	private static final String LINEA = "==================================================";

	/*
	 * TIEMPO MÁXIMO QUE SE ESPERA A QUE EL HOST ACTUALICE EL BOTE TRAS UNA MULTA (2,5 SEG).
	 * EL HOST RECALCULA EL BOTE CADA SEGUNDO, ASÍ QUE NORMALMENTE BASTA CON UNO O DOS.
	 */
	private static final int ESPERA_BOTE_MS = 2500;

	/*
	 * ÚNICO LECTOR DE TECLADO DEL PROGRAMA (NOMBRE, RESPUESTAS S/N Y PREGUNTAS A/B/C).
	 * ES UN HILO: SE ARRANCA UNA VEZ EN main() Y SE QUEDA ESPERANDO AL TECLADO.
	 */
	private static final LectorTeclado teclado = new LectorTeclado();

	public static void main(String[] args) {
		System.out.println("=== BIENVENIDO AL BINGO DE ALMINGO ===");

		// 0. RUTA BASE (carpeta compartida). Se pasa como primer argumento.
		//    Ejemplo: java jugador.JugadorMain "\\192.168.0.112\BingoCompartido"
		if (args.length >= 1 && args[0] != null && !args[0].trim().isEmpty()) {
			GestorFicheros.setRutaBase(args[0]);
			System.out.println("Conectado a: " + GestorFicheros.getRutaBase());
		} else {
			System.out.println("No se indicó carpeta compartida. Usando carpeta local.");
		}

		// SI EL JUGADOR ARRANCA ANTES QUE EL HOST, SE CREAN LAS CARPETAS QUE FALTEN
		GestorFicheros.crearDirectorios();

		// ARRANCAR EL HILO QUE ESCUCHA EL TECLADO (SOLO SE HACE UNA VEZ)
		teclado.start();

		// 1. NOMBRE DE USUARIO
		String nombreUsuario = pedirNombreUsuario();

		// 2. ESPERAR A QUE EL HOST CREE LA PARTIDA. ASÍ EL CARTÓN SE CREA
		// DESPUÉS DE QUE EL HOST HAYA LIMPIADO LOS RESTOS DE LA PARTIDA ANTERIOR.
		esperarPartidaActiva();

		// 3. CARTÓN (RECUPERADO O NUEVO)
		Carton miCarton = obtenerOCrearCarton(nombreUsuario);

		// 4. MOSTRAR CARTÓN Y EXPLICAR CÓMO SE JUEGA
		System.out.println("\nTu cartón:");
		miCarton.mostrarCarton();
		explicarReglas();

		// 5. BUCLE DE LA PARTIDA
		iniciarBucleJuego(nombreUsuario, miCarton);
	}

	// ========== NOMBRE DE USUARIO ==========

	/*
	 * PIDE EL NOMBRE POR TECLADO HASTA QUE SEA VÁLIDO.
	 * SE UTILIZA PARA CREAR FICHEROS (jugadores/<nombre>.txt), POR ESO NO
	 * PUEDE ESTAR VACÍO NI TENER SÍMBOLOS COMO "/" O "..".
	 */
	private static String pedirNombreUsuario() {
		String nombre;

		do {
			System.out.print("Introduce tu nombre de usuario (letras, números o _): ");
			nombre = teclado.leerLinea();

			if (!esNombreValido(nombre)) {
				System.out.println("Nombre no válido. Inténtalo de nuevo.");
			}
		} while (!esNombreValido(nombre));

		return nombre;
	}

	/* COMPRUEBA QUE EL NOMBRE NO ESTÉ VACÍO Y SOLO TENGA LETRAS, NÚMEROS O "_". */
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

	// ========== ESPERAR A LA PARTIDA ==========

	/*
	 * ESPERA A QUE EL HOST HAYA CREADO UNA PARTIDA NUEVA Y TODAVÍA NO HAYA EMPEZADO.
	 *
	 *   - ESPERANDO                 -> SE PUEDE ENTRAR: SIGUE EL PROGRAMA.
	 *   - JUGANDO                   -> LA PARTIDA YA EMPEZÓ: NO SE PUEDE ENTRAR (HABRÍA
	 *                                  BOLAS QUE NO PUDO RESPONDER). ESPERA A LA SIGUIENTE.
	 *   - FINALIZADA, CANCELADA O
	 *     NO HAY FICHERO            -> NO HAY PARTIDA: ESPERA A QUE EL HOST CREE UNA.
	 */
	private static void esperarPartidaActiva() {
		boolean puedeEntrar = false;
		String ultimoAviso = ""; // PARA NO REPETIR EL MISMO AVISO CADA MEDIO SEGUNDO

		while (!puedeEntrar) {
			Estado estado = GestorFicheros.leerEstado();

			if (estado != null && estado.getFase().equals(Estado.FASE_ESPERANDO)) {
				puedeEntrar = true;
			} else {
				String aviso;
				if (estado != null && estado.getFase().equals(Estado.FASE_JUGANDO)) {
					aviso = "\n[ESPERANDO] La partida ya ha empezado. Espera a la siguiente...";
				} else {
					aviso = "\n[ESPERANDO] No hay ninguna partida activa. Esperando a que el Host inicie una nueva...";
				}

				if (!aviso.equals(ultimoAviso)) {
					System.out.println(aviso);
					ultimoAviso = aviso;
				}
				Utilidades.pausar(Reglas.INTERVALO_CONSULTA_MS);
			}
		}
	}

	// ========== CARTÓN ==========

	/*
	- DEVUELVE EL CARTÓN DEL JUGADOR: EL GUARDADO SI EXISTE Y SE PUEDE LEER
	- (Y RECONSTRUYE SUS TACHADOS/PERDONES CON LAS RESPUESTAS ANTERIORES);
	- SI NO, GENERA UNO NUEVO Y LO GUARDA.
	 */
	private static Carton obtenerOCrearCarton(String nombreUsuario) {
		Carton carton = null;

		if (GestorFicheros.existeCarton(nombreUsuario)) {
			carton = GestorFicheros.cargarCarton(nombreUsuario);

			if (carton != null) {
				System.out.println("Bienvenid@ de nuevo, " + nombreUsuario
						+ "! Hemos recuperado tu cartón guardado.");

				// RECONSTRUIR TACHADOS, PERDONES Y FALSAS ALARMAS REPRODUCIENDO RESPUESTAS ANTERIORES
				reproducirRespuestasGuardadas(carton, nombreUsuario);
			} else {
				System.out.println("[AVISO] Tu cartón guardado está dañado. Se generará uno nuevo.");
			}
		}

		// SI NO HABÍA CARTÓN VÁLIDO, SE CREA UNO NUEVO
		if (carton == null) {
			System.out.println("¡Bienvenid@, " + nombreUsuario + "! Generando tu cartón...");
			carton = Carton.generarCarton();
			GestorFicheros.guardarCarton(nombreUsuario, carton);
		}

		return carton;
	}

	/* REPRODUCE LAS RESPUESTAS GUARDADAS EN EL FICHERO PARA QUE EL CARTÓN RECALCULE SU ESTADO */
	private static void reproducirRespuestasGuardadas(Carton carton, String nombreUsuario) {
		String[][] respuestas = GestorFicheros.leerRespuestas(nombreUsuario);
		int[] salidas = GestorFicheros.leerBolas();

		for (int i = 0; i < respuestas.length; i++) {
			try {
				int bola = Integer.parseInt(respuestas[i][0]);
				boolean dijoSi = respuestas[i][1].equals("S");

				// Solo aplicamos la respuesta si esa bola realmente ha salido en la partida
				if (haSalido(salidas, bola)) {
					carton.responder(bola, dijoSi);
				}
			} catch (NumberFormatException e) {
				// Ignorar líneas corruptas
			}
		}
	}

	/* COMPRUEBA SI UNA BOLA HA SALIDO YA DEL BOMBO */
	private static boolean haSalido(int[] salidas, int bola) {
		for (int i = 0; i < salidas.length; i++) {
			if (salidas[i] == bola) {
				return true;
			}
		}
		return false;
	}

	/* MUESTRA POR PANTALLA CÓMO FUNCIONA LA MECÁNICA DEL JUEGO. */
	private static void explicarReglas() {
		// Códigos de color ANSI
				String MORADO = "\033[95m";
				String ROSA = "\033[1;95m";
				String RESET = "\033[0m";

				System.out.println(ROSA + "________  ___       _____ ______   ___  ________   ________  ________      " + RESET);
				System.out.println(MORADO + "|\\   __  \\|\\  \\       |\\   _ \\  _   \\|\\  \\|\\   __  \\|\\   ____\\|\\   __  \\    " + RESET);
				System.out.println(MORADO + "\\ \\  \\|\\  \\ \\  \\       \\ \\  \\__\\ \\  \\ \\  \\ \\  \\\\ \\  \\ \\  \\___|\\ \\  \\|\\  \\    " + RESET);
				System.out.println(MORADO + " \\ \\   __  \\ \\  \\       \\ \\  \\|__| \\  \\ \\  \\ \\  \\\\ \\  \\ \\  \\  __\\ \\  \\\\\\  \\   " + RESET);
				System.out.println(MORADO + "  \\ \\  \\ \\  \\ \\  \\____\\ \\  \\    \\ \\  \\ \\  \\ \\  \\\\ \\  \\ \\  \\|\\  \\ \\  \\\\\\  \\  " + RESET);
				System.out.println(ROSA + "   \\ \\__\\ \\__\\ \\_______\\\\ \\__\\    \\ \\__\\ \\__\\ \\__\\\\ \\__\\ \\_______\\ \\_______\\ " + RESET);
				System.out.println(ROSA + "    \\|__|\\|__|\\_______|\\__|     \\|__|\\|__|\\__| \\__|\\_______|\\_______| " + RESET);

		System.out.println("\n=== CÓMO SE JUEGA ===");
		System.out.println("Cada vez que salga una bola, escribe S si está en tu cartón o N si no, y pulsa ENTER.");
		System.out.println("Tienes " + (Reglas.TIEMPO_RESPUESTA_MS / 1000)
				+ " segundos. Si no contestas a tiempo, tu respuesta cuenta como N.");
		System.out.println(" - Dices S y la tienes: se tacha (XX).");
		System.out.println(" - Dices N y no la tienes: perfecto, sigues.");
		System.out.println(" - Dices N y SÍ la tenías (olvido): no se tacha (!!), esa fila ya no puede");
		System.out.println("   dar línea y pierdes el Bingo. Se te perdonan " + Reglas.OLVIDOS_PERDONADOS
				+ " olvido(s) por partida: en ese caso se tacha solo.");
		System.out.println(" - Dices S y NO la tienes (falsa alarma): se te perdonan "
				+ Reglas.FALSAS_ALARMAS_PERDONADAS + "; a partir de ahí, multa de "
				+ Reglas.formatearEuros(Reglas.MULTA_FALSA_ALARMA_CTS) + " cada una, que se suma al bote.");
		System.out.println("\nPREMIOS (el bote son las entradas de los jugadores más las multas):");
		System.out.println(" - LÍNEA: los " + Reglas.MAX_PREMIOS_LINEA + " primeros en validarla ganan el "
				+ Reglas.PORCENTAJE_PREMIO_LINEA + " % del bote.");
		System.out.println(" - BINGO: el primero en validarlo se lleva el resto del bote y termina la partida.");
		System.out.println("Al completar una línea o el bingo, se reclama solo y el Host te hará una pregunta");
		System.out.println("de sostenibilidad (A, B o C). Si la aciertas, ganas el premio.");
		System.out.println("\n[ESPERANDO] Conectado al Host. Esperando extracción de bolas...");
	}


	// ========== PANTALLA ==========

	/*
	 * DIBUJA LA CABECERA QUE SE REPITE EN TODAS LAS PANTALLAS DE LA PARTIDA:
	 * NOMBRE, NÚMERO DE BOLA, PERDONES GASTADOS, MULTAS ACUMULADAS Y BOTE DE LA PARTIDA.
	 */
	private static void dibujarCabecera(String nombreUsuario, Carton carton, int numeroBola) {
		System.out.println(LINEA);
		System.out.println(" BINGO ALMINGO | Jugador: " + nombreUsuario);
		System.out.println(" Bola " + numeroBola + " de " + Bombo.TOTAL_BOLAS
				+ " | Perdones: " + carton.getPerdonesUsados() + "/" + Reglas.OLVIDOS_PERDONADOS
				+ " | Multas: " + Reglas.formatearEuros(multasEnCentimos(carton)));
		System.out.println(" Bote de la partida: " + textoBote());
		System.out.println(LINEA);
	}

	/*
	 * DEVUELVE EL BOTE ACTUAL EN TEXTO, LEYÉNDOLO DE host/bote.txt (LO PUBLICA EL HOST).
	 * SI TODAVÍA NO ESTÁ PUBLICADO, DEVUELVE "--".
	 */
	private static String textoBote() {
		int bote = GestorFicheros.leerBote();
		if (bote < 0) {
			return "--";
		}
		return Reglas.formatearEuros(bote);
	}

	/*
	 * CALCULA LAS MULTAS ACUMULADAS EN CÉNTIMOS.
	 * LAS PRIMERAS FALSAS ALARMAS (Reglas.FALSAS_ALARMAS_PERDONADAS) NO CUESTAN NADA;
	 * LAS SIGUIENTES CUESTAN Reglas.MULTA_FALSA_ALARMA_CTS CADA UNA.
	 */
	private static int multasEnCentimos(Carton carton) {
		int multables = carton.getFalsasAlarmas() - Reglas.FALSAS_ALARMAS_PERDONADAS;
		if (multables < 0) {
			multables = 0;
		}
		return multables * Reglas.MULTA_FALSA_ALARMA_CTS;
	}

	/*
	 * ESPERA UN POCO A QUE EL HOST ACTUALICE EL BOTE DESPUÉS DE UNA MULTA.
	 * EL HOST RECALCULA EL BOTE CADA SEGUNDO, ASÍ QUE LA PANTALLA DE RESULTADO
	 * PODRÍA DIBUJARSE CON EL BOTE ANTIGUO. AQUÍ SE ESPERA (COMO MUCHO ESPERA_BOTE_MS)
	 * A QUE EL BOTE CAMBIE RESPECTO AL QUE HABÍA ANTES DE LA MULTA.
	 *
	 * @param boteAntes BOTE (EN CÉNTIMOS) QUE HABÍA ANTES DE APLICAR LA RESPUESTA
	 */
	private static void esperarCambioBote(int boteAntes) {
		long limite = System.currentTimeMillis() + ESPERA_BOTE_MS;

		while (System.currentTimeMillis() < limite && GestorFicheros.leerBote() == boteAntes) {
			Utilidades.pausar(200);
		}
	}

	/*
	 * DEVUELVE EN TEXTO LAS (COMO MUCHO) 5 BOLAS ANTERIORES A LA POSICIÓN DADA.
	 * EJEMPLO: SI HAN SALIDO 58, 84 Y 14 Y ESTAMOS EN LA 14, DEVUELVE "58 84".
	 */
	private static String bolasAnteriores(int[] bolas, int indiceActual) {
		int desde = indiceActual - 5;
		if (desde < 0) {
			desde = 0;
		}

		String texto = "";
		for (int i = desde; i < indiceActual; i++) {
			texto = texto + bolas[i] + " ";
		}

		if (texto.equals("")) {
			return "(ninguna todavía)";
		}
		return texto.trim();
	}

	// ========== BUCLE PRINCIPAL DEL JUEGO ==========

	/*
	 * CONSULTA LA PARTIDA CONSTANTEMENTE Y REACCIONA.
	 * TERMINA SI: LA PARTIDA FINALIZA O SE CANCELA, O EL JUGADOR GANA EL BINGO.
	 *
	 * SE LLEVA LA CUENTA DE CUÁNTAS BOLAS SE HAN TRATADO YA (bolasProcesadas).
	 * SI SE JUNTAN VARIAS BOLAS (EL JUGADOR TARDÓ MUCHO EN RESPONDER), SOLO LA
	 * ÚLTIMA SE PREGUNTA; LAS ANTERIORES YA NO TIENEN TIEMPO Y CUENTAN COMO N.
	 *
	 * RECLAMACIONES: LA LÍNEA Y EL BINGO SE RECLAMAN UNA SOLA VEZ CADA UNO, GANE O
	 * PIERDA EL JUGADOR. ASÍ NO SE VUELVE A RECLAMAR EN CADA BOLA SIGUIENTE.
	 */
	private static void iniciarBucleJuego(String nombreUsuario, Carton carton) {
		int bolasProcesadas = 0;

		// YA SE HA RECLAMADO (SE HAYA GANADO O NO) LA LÍNEA / EL BINGO
		boolean lineaResuelta = false;
		boolean bingoResuelto = false;

		// PARA AVISAR SOLO UNA VEZ DE QUE LA PARTIDA ESTÁ EN PAUSA POR UNA RECLAMACIÓN AJENA
		boolean avisoPausa = false;

		boolean juegoActivo = true;

		while (juegoActivo) {
			// leerEstado() DEVUELVE null SI EL FICHERO NO ESTÁ LISTO (EL HOST PUEDE
			// ESTAR ESCRIBIÉNDOLO AHORA MISMO): SE IGNORA Y SE REINTENTA
			Estado estado = GestorFicheros.leerEstado();

			if (estado != null) {
				String fase = estado.getFase();

				// ---- FIN DE LA PARTIDA ----
				if (fase.equals(Estado.FASE_CANCELADA)) {
					System.out.println("\n⚠️ [AVISO] La partida ha sido cancelada por el Host.");
					juegoActivo = false;

				} else if (fase.equals(Estado.FASE_FINALIZADA)) {
					System.out.println("\n🏁 [AVISO] La partida ha finalizado.");
					mostrarResumen(carton);
					juegoActivo = false;

					// ---- OTRO JUGADOR ESTÁ RECLAMANDO: EL HOST TIENE LA PARTIDA EN PAUSA ----
				} else if (fase.equals(Estado.FASE_VALIDANDO)) {
					if (!avisoPausa) {
						System.out.println("\n⏸ Un jugador ha reclamado línea o bingo. "
								+ "El Host está validando: partida en pausa...");
						avisoPausa = true;
					}

					// ---- PARTIDA EN MARCHA: TRATAR LAS BOLAS NUEVAS ----
				} else if (fase.equals(Estado.FASE_JUGANDO)) {
					avisoPausa = false;

					int[] bolas = GestorFicheros.leerBolas();
					int bolasAntes = bolasProcesadas; // PARA SABER SI HUBO BOLAS NUEVAS

					while (juegoActivo && bolasProcesadas < bolas.length) {
						int bola = bolas[bolasProcesadas];
						int pendientes = bolas.length - bolasProcesadas;

						if (pendientes > 1) {
							// BOLA ATRASADA: YA HA SALIDO OTRA DESPUÉS Y SE HA PASADO SU TIEMPO.
							// NO SE PREGUNTA: CUENTA COMO N.
							aplicarRespuesta(nombreUsuario, carton, bola, bolasProcesadas + 1, false,
									"⏰ El " + bola + " salió y no diste respuesta a tiempo: cuenta como N.");
						} else {
							// ES LA ÚLTIMA BOLA: SE PREGUNTA AL JUGADOR
							preguntarBola(nombreUsuario, carton, bolas, bolasProcesadas);
						}
						bolasProcesadas++;

						// ---- LÍNEA: SE RECLAMA UNA SOLA VEZ ----
						if (!lineaResuelta && carton.tieneLinea()) {
							lineaResuelta = true;
							String resultado = reclamar(nombreUsuario, carton, bolasProcesadas, Reglas.TIPO_LINEA);

							if (resultado.equals(Reclamador.PARTIDA_TERMINADA)) {
								mostrarResumen(carton);
								juegoActivo = false;
							}
						}

						// ---- BINGO: SE RECLAMA UNA SOLA VEZ. SI SE GANA, TERMINA LA PARTIDA ----
						if (juegoActivo && !bingoResuelto && carton.tieneBingo()) {
							bingoResuelto = true;
							String resultado = reclamar(nombreUsuario, carton, bolasProcesadas, Reglas.TIPO_BINGO);

							if (resultado.equals(Reglas.RESULTADO_ACIERTO)
									|| resultado.equals(Reclamador.PARTIDA_TERMINADA)) {
								mostrarResumen(carton);
								juegoActivo = false;
							}
							// SI FALLA, LA PARTIDA SIGUE PARA LOS DEMÁS Y ESTE JUGADOR SOLO OBSERVA
						}
					}

					// SI SE HA TRATADO ALGUNA BOLA, AVISAR DE QUE AHORA SE ESPERA LA SIGUIENTE
					if (juegoActivo && bolasProcesadas > bolasAntes) {
						System.out.println("\n⏳ Esperando la siguiente bola...");
					}
				}
			}

			// PAUSA ANTES DE VOLVER A CONSULTAR. ESTÁ FUERA DEL if (estado != null)
			// PARA QUE TAMBIÉN ESPERE CUANDO EL FICHERO NO ESTÁ LISTO.
			if (juegoActivo) {
				Utilidades.pausar(Reglas.INTERVALO_CONSULTA_MS);
			}
		}
	}

	// ========== RECLAMACIONES ==========

	/*
	 * RECLAMA UNA LÍNEA O UN BINGO AL HOST Y MUESTRA EL RESULTADO.
	 *
	 * PASOS:
	 * 1. AVISAR AL JUGADOR DE QUE HA COMPLETADO LA JUGADA.
	 * 2. Reclamador HACE TODO EL PROCESO CON EL HOST (RECLAMO, PREGUNTA, RESPUESTA, RESULTADO).
	 * 3. PANTALLA DE RESULTADO CON EL MENSAJE CORRESPONDIENTE Y EL CARTÓN.
	 *
	 * @param numeroBola NÚMERO DE BOLAS QUE LLEVAN SALIDAS (PARA LA CABECERA)
	 * @param tipo       Reglas.TIPO_LINEA O Reglas.TIPO_BINGO
	 * @return EL RESULTADO DEL HOST (VER Reclamador)
	 */
	private static String reclamar(String nombreUsuario, Carton carton, int numeroBola, String tipo) {
		// 1. AVISO
		if (tipo.equals(Reglas.TIPO_BINGO)) {
			System.out.println("\n🏆 ¡¡¡ HAS COMPLETADO EL BINGO !!!");
		} else {
			System.out.println("\n✨ ¡¡¡ HAS COMPLETADO UNA LÍNEA !!!");
		}

		// 2. PROCESO CON EL HOST (AQUÍ SE ESPERA A QUE EL HOST VALIDE Y SE CONTESTA LA PREGUNTA)
		String resultado = Reclamador.reclamar(nombreUsuario, tipo, teclado);

		// 3. PANTALLA DE RESULTADO
		Utilidades.limpiarPantalla();
		dibujarCabecera(nombreUsuario, carton, numeroBola);
		System.out.println(textoResultado(tipo, resultado) + "\n");
		carton.mostrarCarton();

		return resultado;
	}

	/* DEVUELVE EL MENSAJE QUE SE MUESTRA AL JUGADOR SEGÚN EL RESULTADO DE SU RECLAMACIÓN. */
	private static String textoResultado(String tipo, String resultado) {
		boolean esBingo = tipo.equals(Reglas.TIPO_BINGO);

		if (resultado.equals(Reglas.RESULTADO_ACIERTO)) {
			if (esBingo) {
				return "🏆 ¡¡¡ BINGO VALIDADO !!! Has acertado la pregunta: ¡te llevas el premio del Bingo!";
			}
			return "🎉 ¡LÍNEA VALIDADA! Has acertado la pregunta: ¡ganas un premio de línea!";
		}

		if (resultado.equals(Reglas.RESULTADO_FALLO)) {
			if (esBingo) {
				return "❌ Pregunta fallada o sin contestar: tu bingo no se valida. La partida continúa.";
			}
			return "❌ Pregunta fallada o sin contestar: tu línea no se valida y pierdes el premio de línea.";
		}

		if (resultado.equals(Reglas.RESULTADO_FALSO)) {
			return "🚫 El Host no ha podido comprobar tu jugada con tus respuestas: no se valida.";
		}

		if (resultado.equals(Reglas.RESULTADO_TARDE)) {
			return "⌛ Tu línea es válida, pero ya se han repartido los " + Reglas.MAX_PREMIOS_LINEA
					+ " premios de línea.";
		}

		if (resultado.equals(Reclamador.PARTIDA_TERMINADA)) {
			return "🏁 La partida ha terminado mientras reclamabas.";
		}

		// RESULTADO_SIN_RESPUESTA
		return "⚠️ El Host no ha respondido a tu reclamación. Comprueba que sigue en marcha.";
	}

	// ========== PREGUNTAS S/N ==========

	/*
	 * PANTALLA DE PREGUNTA: PREGUNTA AL JUGADOR SI LA BOLA ESTÁ EN SU CARTÓN Y APLICA LA RESPUESTA.
	 *
	 * PASOS:
	 * 1. LIMPIAR PANTALLA Y MOSTRAR: CABECERA, BOLAS ANTERIORES, BOLA NUEVA Y CARTÓN.
	 * 2. HACER LA PREGUNTA Y ESPERAR LA RESPUESTA COMO MUCHO TIEMPO_RESPUESTA_MS.
	 *    SI NO CONTESTA A TIEMPO, O NO ESCRIBE S/N, CUENTA COMO N.
	 * 3. APLICAR LA RESPUESTA (aplicarRespuesta), QUE DIBUJA LA PANTALLA DE RESULTADO.
	 */
	private static void preguntarBola(String nombreUsuario, Carton carton, int[] bolas, int indice) {
		int bola = bolas[indice];
		int numeroBola = indice + 1;

		// 1. PANTALLA LIMPIA CON LA INFORMACIÓN DE ESTA BOLA
		Utilidades.limpiarPantalla();
		dibujarCabecera(nombreUsuario, carton, numeroBola);
		System.out.println("Bolas anteriores: " + bolasAnteriores(bolas, indice));
		System.out.println("\n📢 ¡HA SALIDO EL " + bola + "!\n");
		carton.mostrarCarton();

		// 2. PREGUNTAR Y ESPERAR LA RESPUESTA CON TIEMPO LÍMITE.
		// ANTES SE BORRA LO QUE EL JUGADOR HUBIERA ESCRITO TARDE A LA BOLA ANTERIOR.
		teclado.descartarPendiente();
		System.out.print("\n¿Está el " + bola + " en tu cartón? (S/N + ENTER, máximo "
				+ (Reglas.TIEMPO_RESPUESTA_MS / 1000) + " s): ");

		String respuesta = teclado.esperarLinea(Reglas.TIEMPO_RESPUESTA_MS);

		boolean dijoSi = false;
		String aviso = ""; // MENSAJE EXTRA SI HUBO PROBLEMA CON LA RESPUESTA
		if (respuesta == null) {
			aviso = "⏰ Tiempo agotado (" + (Reglas.TIEMPO_RESPUESTA_MS / 1000)
					+ " s): no contestaste, cuenta como N.";
		} else if (respuesta.equalsIgnoreCase("S")) {
			dijoSi = true;
		} else if (!respuesta.equalsIgnoreCase("N")) {
			aviso = "Respuesta no válida (solo S o N): cuenta como N.";
		}

		// 3. APLICAR LA RESPUESTA
		aplicarRespuesta(nombreUsuario, carton, bola, numeroBola, dijoSi, aviso);
	}

	/*
	 * PANTALLA DE RESULTADO: APLICA LA RESPUESTA DEL JUGADOR A UNA BOLA (LA HAYA
	 * DADO ÉL O SE LE HAYA ASIGNADO N POR FALTA DE TIEMPO) Y MUESTRA QUÉ HA PASADO.
	 *
	 * PASOS:
	 * 1. ANOTAR EL BOTE ACTUAL Y LA RESPUESTA EN host/respuestas/ (PARA QUE EL HOST
	 *    PUEDA COMPROBARLA Y SUMAR LAS MULTAS AL BOTE).
	 * 2. APLICARLA AL CARTÓN Y PREPARAR EL MENSAJE DE LO QUE HA PASADO.
	 * 3. SI HA HABIDO MULTA, ESPERAR A QUE EL HOST ACTUALICE EL BOTE.
	 * 4. LIMPIAR PANTALLA Y MOSTRAR: CABECERA, AVISO (SI LO HAY), RESULTADO Y CARTÓN ACTUALIZADO.
	 *
	 * @param aviso TEXTO EXTRA QUE SE MUESTRA ANTES DEL RESULTADO ("" SI NO HAY)
	 */
	private static void aplicarRespuesta(String nombreUsuario, Carton carton, int bola, int numeroBola,
			boolean dijoSi, String aviso) {
		// 1. ANOTAR EL BOTE DE ANTES Y LA RESPUESTA PARA QUE EL HOST PUEDA COMPROBARLA
		int boteAntes = GestorFicheros.leerBote();
		GestorFicheros.guardarRespuesta(nombreUsuario, bola, dijoSi);

		// 2. APLICAR LA RESPUESTA AL CARTÓN Y PREPARAR EL MENSAJE
		int resultado = carton.responder(bola, dijoSi);
		String mensaje = "";
		boolean hayMulta = false;

		if (resultado == Carton.RESULTADO_TACHADO) {
			mensaje = "✔ ¡Correcto! Número " + bola + " tachado en tu cartón.";

		} else if (resultado == Carton.RESULTADO_CORRECTO_NO) {
			mensaje = "👍 Correcto: el " + bola + " no está en tu cartón.";

		} else if (resultado == Carton.RESULTADO_OLVIDO_PERDONADO) {
			mensaje = "⚠️ ¡Olvido! El " + bola + " SÍ estaba en tu cartón. Esta vez te lo perdonamos\n"
					+ "   (se tacha solo), pero ya has gastado " + carton.getPerdonesUsados() + " de "
					+ Reglas.OLVIDOS_PERDONADOS + " perdón(es).";

		} else if (resultado == Carton.RESULTADO_OLVIDO) {
			mensaje = "❌ ¡Olvido! El " + bola + " estaba en tu cartón y no lo marcaste.\n"
					+ "   Esa fila ya no podrá dar línea y el Bingo ya no es posible para ti.";

		} else if (resultado == Carton.RESULTADO_FALSA_ALARMA) {
			// getFalsasAlarmas() YA INCLUYE ESTA FALSA ALARMA: LAS PRIMERAS SE PERDONAN
			int numeroFalsa = carton.getFalsasAlarmas();
			if (numeroFalsa <= Reglas.FALSAS_ALARMAS_PERDONADAS) {
				mensaje = "⚠️ Falsa alarma: el " + bola + " NO está en tu cartón. Es la falsa alarma nº "
						+ numeroFalsa + ": esta te la perdonamos.";
			} else {
				mensaje = "💸 Falsa alarma: el " + bola + " NO está en tu cartón. Multa de "
						+ Reglas.formatearEuros(Reglas.MULTA_FALSA_ALARMA_CTS) + " (se suma al bote).";
				hayMulta = true;
			}
		}

		// 3. SI HA HABIDO MULTA, ESPERAR A QUE EL HOST LA SUME AL BOTE
		if (hayMulta) {
			esperarCambioBote(boteAntes);
		}

		// 4. PANTALLA DE RESULTADO. LA CABECERA SE DIBUJA DESPUÉS DE APLICAR LA
		// RESPUESTA PARA QUE YA MUESTRE LOS PERDONES, MULTAS Y BOTE ACTUALIZADOS.
		Utilidades.limpiarPantalla();
		dibujarCabecera(nombreUsuario, carton, numeroBola);
		if (!aviso.equals("")) {
			System.out.println(aviso);
		}
		System.out.println(mensaje + "\n");
		carton.mostrarCarton();
	}

	/* AL TERMINAR LA PARTIDA, MUESTRA EL CARTÓN FINAL Y UN RESUMEN DEL JUGADOR. */
	private static void mostrarResumen(Carton carton) {
		System.out.println("\n=== TU RESUMEN ===");
		carton.mostrarCarton();
		System.out.println("Falsas alarmas: " + carton.getFalsasAlarmas() + " (multa total: "
				+ Reglas.formatearEuros(multasEnCentimos(carton)) + ")");
		System.out.println("Perdones de olvido gastados: " + carton.getPerdonesUsados()
		+ " de " + Reglas.OLVIDOS_PERDONADOS);
		System.out.println("Bote final de la partida: " + textoBote());
		if (!carton.bingoPosible()) {
			System.out.println("Esta partida no pudiste completar el Bingo por un olvido.");
		}
	}
}
