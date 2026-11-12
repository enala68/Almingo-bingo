package jugador;

import comun.Bombo;
import comun.Carton;
import comun.Estado;
import comun.GestorFicheros;
import comun.Reglas;
import comun.Utilidades;

/*
 * CLIENTE (JUGADOR) DEL BINGO.
 *
 * FLUJO:
 *   1. PEDIR NOMBRE DE USUARIO.
 *   2. ESPERAR A QUE EL HOST CREE UNA PARTIDA (SOLO SE ENTRA ANTES DE LA PRIMERA BOLA).
 *   3. RECUPERAR EL CARTÓN GUARDADO O GENERAR UNO NUEVO.
 *   4. MOSTRAR CARTÓN Y REGLAS.
 *   5. VIGILAR host/bolas.txt Y host/estado.txt:
 *        - POR CADA BOLA NUEVA, PREGUNTAR S/N AL JUGADOR.
 *        - AL COMPLETAR LÍNEA O BINGO, RECLAMAR AL HOST (Reclamador).
 *        - TERMINAR SI LA PARTIDA SE CANCELA O FINALIZA.
 *
 * PANTALLA: DOS PANTALLAS LIMPIAS POR BOLA, SIEMPRE CON UN ÚNICO CARTÓN:
 *   - PREGUNTA:  CABECERA + BOLAS ANTERIORES + BOLA NUEVA + CARTÓN + PREGUNTA.
 *   - RESULTADO: CABECERA + QUÉ HA PASADO + CARTÓN ACTUALIZADO.
 *
 * REPARTO DEL TRABAJO:
 *   - Carton:          LÓGICA DEL CARTÓN.
 *   - GestorFicheros:  LECTURA Y ESCRITURA DE FICHEROS.
 *   - Reglas:          TIEMPOS Y CONSTANTES.
 *   - LectorTeclado:   TECLADO CON TIEMPO LÍMITE (HILO).
 *   - Reclamador:      RECLAMAR LÍNEA/BINGO Y CONTESTAR LA PREGUNTA.
 *   - JugadorMain:     COORDINA TODO.
 */
public class JugadorMain {

	/* LÍNEA DECORATIVA DE LA CABECERA. */
	private static final String LINEA = "==================================================";

	/* ESPERA MÁXIMA A QUE EL HOST ACTUALICE EL BOTE TRAS UNA MULTA (2,5 SEG). */
	private static final int ESPERA_BOTE_MS = 2500;

	/* ÚNICO LECTOR DE TECLADO (NOMBRE, S/N Y A/B/C). SE ARRANCA UNA VEZ EN main(). */
	private static final LectorTeclado teclado = new LectorTeclado();

	public static void main(String[] args) {
		System.out.println("=== BIENVENIDO AL BINGO DE ALMINGO ===");

		// 0. RUTA BASE (CARPETA COMPARTIDA), PRIMER ARGUMENTO
		//    EJEMPLO: java jugador.JugadorMain "\\192.168.0.112\BingoCompartido"
		if (args.length >= 1 && args[0] != null && !args[0].trim().isEmpty()) {
			GestorFicheros.setRutaBase(args[0]);
			System.out.println("Conectado a: " + GestorFicheros.getRutaBase());
		} else {
			System.out.println("No se indicó carpeta compartida. Usando carpeta local.");
		}

		// CREAR CARPETAS QUE FALTEN (POR SI ARRANCA ANTES QUE EL HOST)
		GestorFicheros.crearDirectorios();

		// ARRANCAR EL HILO DEL TECLADO
		teclado.start();

		// 1. NOMBRE DE USUARIO
		String nombreUsuario = pedirNombreUsuario();

		// 2. ESPERAR A LA PARTIDA (EL CARTÓN SE CREA DESPUÉS DE QUE EL HOST LIMPIE)
		esperarPartidaActiva();

		// 3. CARTÓN (RECUPERADO O NUEVO)
		Carton miCarton = obtenerOCrearCarton(nombreUsuario);

		// 4. MOSTRAR CARTÓN Y REGLAS
		System.out.println("\nTu cartón:");
		miCarton.mostrarCarton();
		explicarReglas();

		// 5. BUCLE DE LA PARTIDA
		iniciarBucleJuego(nombreUsuario, miCarton);
	}

	// ========== NOMBRE DE USUARIO ==========

	/* PIDE EL NOMBRE HASTA QUE SEA VÁLIDO (SE USA PARA CREAR FICHEROS). */
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

	/* NO VACÍO Y SOLO LETRAS, NÚMEROS O "_". */
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
	 * ESPERA A QUE EL HOST CREE UNA PARTIDA QUE AÚN NO HAYA EMPEZADO.
	 *   - ESPERANDO: SE PUEDE ENTRAR.
	 *   - JUGANDO: YA EMPEZÓ, HAY QUE ESPERAR A LA SIGUIENTE.
	 *   - FINALIZADA, CANCELADA O SIN FICHERO: NO HAY PARTIDA.
	 */
	private static void esperarPartidaActiva() {
		boolean puedeEntrar = false;
		String ultimoAviso = ""; // EVITA REPETIR EL MISMO AVISO

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

	/* DEVUELVE EL CARTÓN GUARDADO (RECONSTRUIDO) SI SE PUEDE LEER; SI NO, GENERA UNO NUEVO. */
	private static Carton obtenerOCrearCarton(String nombreUsuario) {
		Carton carton = null;

		if (GestorFicheros.existeCarton(nombreUsuario)) {
			carton = GestorFicheros.cargarCarton(nombreUsuario);

			if (carton != null) {
				System.out.println("Bienvenid@ de nuevo, " + nombreUsuario
						+ "! Hemos recuperado tu cartón guardado.");

				// RECONSTRUIR TACHADOS, PERDONES Y FALSAS ALARMAS
				reproducirRespuestasGuardadas(carton, nombreUsuario);
			} else {
				System.out.println("[AVISO] Tu cartón guardado está dañado. Se generará uno nuevo.");
			}
		}

		// SIN CARTÓN VÁLIDO: CREAR UNO NUEVO
		if (carton == null) {
			System.out.println("¡Bienvenid@, " + nombreUsuario + "! Generando tu cartón...");
			carton = Carton.generarCarton();
			GestorFicheros.guardarCarton(nombreUsuario, carton);
		}

		return carton;
	}

	/* REAPLICA AL CARTÓN LAS RESPUESTAS GUARDADAS. */
	private static void reproducirRespuestasGuardadas(Carton carton, String nombreUsuario) {
		String[][] respuestas = GestorFicheros.leerRespuestas(nombreUsuario);
		int[] salidas = GestorFicheros.leerBolas();

		for (int i = 0; i < respuestas.length; i++) {
			try {
				int bola = Integer.parseInt(respuestas[i][0]);
				boolean dijoSi = respuestas[i][1].equals("S");

				// SOLO SI LA BOLA HA SALIDO DE VERDAD
				if (haSalido(salidas, bola)) {
					carton.responder(bola, dijoSi);
				}
			} catch (NumberFormatException e) {
				// LÍNEA CORRUPTA: SE IGNORA
			}
		}
	}

	/* COMPRUEBA SI UNA BOLA YA HA SALIDO. */
	private static boolean haSalido(int[] salidas, int bola) {
		for (int i = 0; i < salidas.length; i++) {
			if (salidas[i] == bola) {
				return true;
			}
		}
		return false;
	}

	/* MUESTRA LAS REGLAS DEL JUEGO. */
	private static void explicarReglas() {
		// CÓDIGOS DE COLOR ANSI
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

	/* CABECERA COMÚN: NOMBRE, BOLA, PERDONES, MULTAS Y BOTE. */
	private static void dibujarCabecera(String nombreUsuario, Carton carton, int numeroBola) {
		System.out.println(LINEA);
		System.out.println(" BINGO ALMINGO | Jugador: " + nombreUsuario);
		System.out.println(" Bola " + numeroBola + " de " + Bombo.TOTAL_BOLAS
				+ " | Perdones: " + carton.getPerdonesUsados() + "/" + Reglas.OLVIDOS_PERDONADOS
				+ " | Multas: " + Reglas.formatearEuros(multasEnCentimos(carton)));
		System.out.println(" Bote de la partida: " + textoBote());
		System.out.println(LINEA);
	}

	/* BOTE EN TEXTO (host/bote.txt). "--" SI NO ESTÁ PUBLICADO. */
	private static String textoBote() {
		int bote = GestorFicheros.leerBote();
		if (bote < 0) {
			return "--";
		}
		return Reglas.formatearEuros(bote);
	}

	/* MULTAS ACUMULADAS EN CÉNTIMOS (LAS PRIMERAS FALSAS ALARMAS SON GRATIS). */
	private static int multasEnCentimos(Carton carton) {
		int multables = carton.getFalsasAlarmas() - Reglas.FALSAS_ALARMAS_PERDONADAS;
		if (multables < 0) {
			multables = 0;
		}
		return multables * Reglas.MULTA_FALSA_ALARMA_CTS;
	}

	/*
	 * ESPERA (MÁX. ESPERA_BOTE_MS) A QUE EL BOTE CAMBIE TRAS UNA MULTA.
	 *
	 * @param boteAntes BOTE EN CÉNTIMOS ANTES DE LA RESPUESTA
	 */
	private static void esperarCambioBote(int boteAntes) {
		long limite = System.currentTimeMillis() + ESPERA_BOTE_MS;

		while (System.currentTimeMillis() < limite && GestorFicheros.leerBote() == boteAntes) {
			Utilidades.pausar(200);
		}
	}

	/* TEXTO CON LAS (COMO MUCHO) 5 BOLAS ANTERIORES A LA POSICIÓN DADA. */
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
	 * CONSULTA LA PARTIDA Y REACCIONA. TERMINA SI FINALIZA, SE CANCELA O GANA EL BINGO.
	 * SI SE ACUMULAN BOLAS, SOLO SE PREGUNTA LA ÚLTIMA; LAS DEMÁS CUENTAN COMO N.
	 * LÍNEA Y BINGO SE RECLAMAN UNA SOLA VEZ CADA UNO.
	 */
	private static void iniciarBucleJuego(String nombreUsuario, Carton carton) {
		int bolasProcesadas = 0;

		// YA RECLAMADOS (GANADOS O NO)
		boolean lineaResuelta = false;
		boolean bingoResuelto = false;

		// AVISAR UNA SOLA VEZ DE LA PAUSA POR RECLAMACIÓN AJENA
		boolean avisoPausa = false;

		boolean juegoActivo = true;

		while (juegoActivo) {
			// null = FICHERO NO LISTO: SE REINTENTA
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

					// ---- OTRO JUGADOR RECLAMA: PARTIDA EN PAUSA ----
				} else if (fase.equals(Estado.FASE_VALIDANDO)) {
					if (!avisoPausa) {
						System.out.println("\n⏸ Un jugador ha reclamado línea o bingo. "
								+ "El Host está validando: partida en pausa...");
						avisoPausa = true;
					}

					// ---- PARTIDA EN MARCHA: TRATAR BOLAS NUEVAS ----
				} else if (fase.equals(Estado.FASE_JUGANDO)) {
					avisoPausa = false;

					int[] bolas = GestorFicheros.leerBolas();
					int bolasAntes = bolasProcesadas;

					while (juegoActivo && bolasProcesadas < bolas.length) {
						int bola = bolas[bolasProcesadas];
						int pendientes = bolas.length - bolasProcesadas;

						if (pendientes > 1) {
							// BOLA ATRASADA: SIN PREGUNTA, CUENTA COMO N
							aplicarRespuesta(nombreUsuario, carton, bola, bolasProcesadas + 1, false,
									"⏰ El " + bola + " salió y no diste respuesta a tiempo: cuenta como N.");
						} else {
							// ÚLTIMA BOLA: SE PREGUNTA
							preguntarBola(nombreUsuario, carton, bolas, bolasProcesadas);
						}
						bolasProcesadas++;

						// ---- LÍNEA (UNA SOLA VEZ) ----
						if (!lineaResuelta && carton.tieneLinea()) {
							lineaResuelta = true;
							String resultado = reclamar(nombreUsuario, carton, bolasProcesadas, Reglas.TIPO_LINEA);

							if (resultado.equals(Reclamador.PARTIDA_TERMINADA)) {
								mostrarResumen(carton);
								juegoActivo = false;
							}
						}

						// ---- BINGO (UNA SOLA VEZ). SI SE GANA, TERMINA ----
						if (juegoActivo && !bingoResuelto && carton.tieneBingo()) {
							bingoResuelto = true;
							String resultado = reclamar(nombreUsuario, carton, bolasProcesadas, Reglas.TIPO_BINGO);

							if (resultado.equals(Reglas.RESULTADO_ACIERTO)
									|| resultado.equals(Reclamador.PARTIDA_TERMINADA)) {
								mostrarResumen(carton);
								juegoActivo = false;
							}
							// SI FALLA, EL JUGADOR SOLO OBSERVA
						}
					}

					// AVISAR DE QUE SE ESPERA LA SIGUIENTE BOLA
					if (juegoActivo && bolasProcesadas > bolasAntes) {
						System.out.println("\n⏳ Esperando la siguiente bola...");
					}
				}
			}

			// PAUSA ENTRE CONSULTAS (TAMBIÉN SI EL FICHERO NO ESTÁ LISTO)
			if (juegoActivo) {
				Utilidades.pausar(Reglas.INTERVALO_CONSULTA_MS);
			}
		}
	}

	// ========== RECLAMACIONES ==========

	/*
	 * RECLAMA LÍNEA O BINGO AL HOST Y MUESTRA EL RESULTADO.
	 *
	 * @param numeroBola BOLAS SALIDAS (PARA LA CABECERA)
	 * @param tipo       Reglas.TIPO_LINEA O Reglas.TIPO_BINGO
	 * @return RESULTADO DEL HOST (VER Reclamador)
	 */
	private static String reclamar(String nombreUsuario, Carton carton, int numeroBola, String tipo) {
		// 1. AVISO
		if (tipo.equals(Reglas.TIPO_BINGO)) {
			System.out.println("\n🏆 ¡¡¡ HAS COMPLETADO EL BINGO !!!");
		} else {
			System.out.println("\n✨ ¡¡¡ HAS COMPLETADO UNA LÍNEA !!!");
		}

		// 2. PROCESO CON EL HOST (VALIDACIÓN Y PREGUNTA)
		String resultado = Reclamador.reclamar(nombreUsuario, tipo, teclado);

		// 3. PANTALLA DE RESULTADO
		Utilidades.limpiarPantalla();
		dibujarCabecera(nombreUsuario, carton, numeroBola);
		System.out.println(textoResultado(tipo, resultado) + "\n");
		carton.mostrarCarton();

		return resultado;
	}

	/* MENSAJE PARA EL JUGADOR SEGÚN EL RESULTADO DE LA RECLAMACIÓN. */
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
	 * PANTALLA DE PREGUNTA: PREGUNTA SI LA BOLA ESTÁ EN EL CARTÓN Y APLICA LA RESPUESTA.
	 * SIN RESPUESTA A TIEMPO O NO VÁLIDA, CUENTA COMO N.
	 */
	private static void preguntarBola(String nombreUsuario, Carton carton, int[] bolas, int indice) {
		int bola = bolas[indice];
		int numeroBola = indice + 1;

		// 1. PANTALLA LIMPIA CON LA BOLA
		Utilidades.limpiarPantalla();
		dibujarCabecera(nombreUsuario, carton, numeroBola);
		System.out.println("Bolas anteriores: " + bolasAnteriores(bolas, indice));
		System.out.println("\n📢 ¡HA SALIDO EL " + bola + "!\n");
		carton.mostrarCarton();

		// 2. PREGUNTAR CON TIEMPO LÍMITE (ANTES SE DESCARTA LO ESCRITO TARDE)
		teclado.descartarPendiente();
		System.out.print("\n¿Está el " + bola + " en tu cartón? (S/N + ENTER, máximo "
				+ (Reglas.TIEMPO_RESPUESTA_MS / 1000) + " s): ");

		String respuesta = teclado.esperarLinea(Reglas.TIEMPO_RESPUESTA_MS);

		boolean dijoSi = false;
		String aviso = ""; // MENSAJE EXTRA SI HUBO PROBLEMA
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
	 * PANTALLA DE RESULTADO: APLICA LA RESPUESTA A LA BOLA Y MUESTRA QUÉ HA PASADO.
	 *
	 * @param aviso TEXTO EXTRA ANTES DEL RESULTADO ("" SI NO HAY)
	 */
	private static void aplicarRespuesta(String nombreUsuario, Carton carton, int bola, int numeroBola,
			boolean dijoSi, String aviso) {
		// 1. ANOTAR BOTE Y RESPUESTA (EL HOST LA COMPRUEBA)
		int boteAntes = GestorFicheros.leerBote();
		GestorFicheros.guardarRespuesta(nombreUsuario, bola, dijoSi);

		// 2. APLICAR AL CARTÓN Y PREPARAR MENSAJE
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
			// getFalsasAlarmas() YA INCLUYE ESTA FALSA ALARMA
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

		// 3. SI HAY MULTA, ESPERAR AL BOTE DEL HOST
		if (hayMulta) {
			esperarCambioBote(boteAntes);
		}

		// 4. PANTALLA DE RESULTADO (CABECERA DESPUÉS DE APLICAR, YA ACTUALIZADA)
		Utilidades.limpiarPantalla();
		dibujarCabecera(nombreUsuario, carton, numeroBola);
		if (!aviso.equals("")) {
			System.out.println(aviso);
		}
		System.out.println(mensaje + "\n");
		carton.mostrarCarton();
	}

	/* AL TERMINAR, MUESTRA EL CARTÓN FINAL Y EL RESUMEN DEL JUGADOR. */
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