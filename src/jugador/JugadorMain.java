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

	/* LONGITUD LÍNEAS DECORATIVA. */
	private static final String LINEA = "==================================================";

	/* HILO LECTOR DE TECLADO */
	private static final LectorTeclado teclado = new LectorTeclado();

	public static void main(String[] args) {
		System.out.println("=== BIENVENIDO AL BINGO DE ALMINGO ===");

		// CONFIGURAR RUTA BASE SI SE INDICA
		if (args.length >= 1 && args[0] != null && !args[0].trim().isEmpty()) {
			GestorFicheros.setRutaBase(args[0]);
			System.out.println("Conectado a: " + GestorFicheros.getRutaBase());
		} else {
			System.out.println("No se indicó carpeta compartida. Usando carpeta local.");
		}

		// 1. CREAR DIRECTORIOS NECESARIOS
		GestorFicheros.crearDirectorios();

		// 2. INICIAR HILO DE TECLADO
		teclado.start();

		// 3. PEDIR NOMBRE
		String nombreUsuario = pedirNombreUsuario();

		// 4. ESPERAR PARTIDA ACTIVA
		esperarPartidaActiva();

		// 5. OBTENER O CREAR CARTÓN
		Carton miCarton = obtenerOCrearCarton(nombreUsuario);

		// 6. MOSTRAR CARTÓN Y REGLAS
		System.out.println("\nTu cartón:");
		miCarton.mostrarCarton();
		explicarReglas();

		// 7. INICIAR BUCLE JUEGO
		iniciarBucleJuego(nombreUsuario, miCarton);
	}

	// ========== NOMBRE DE USUARIO ==========

	/* PEDIR NOMBRE VÁLIDO */
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

	/* VALIDAR CARACTERES DEL NOMBRE */
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

	/* ESPERAR A QUE LA PARTIDA ESTÉ EN FASE ESPERANDO */
	private static void esperarPartidaActiva() {
		boolean puedeEntrar = false;
		String ultimoAviso = ""; 

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

	/* CARGAR CARTÓN GUARDADO O GENERAR UNO NUEVO */
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

		if (carton == null) {
			System.out.println("¡Bienvenid@, " + nombreUsuario + "! Generando tu cartón...");
			carton = Carton.generarCarton();
			GestorFicheros.guardarCarton(nombreUsuario, carton);
		}

		return carton;
	}

	/* IMPRIMIR EXPLICACIÓN DE REGLAS */
	private static void explicarReglas() {
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
				+ Reglas.formatearEuros(Reglas.MULTA_FALSA_ALARMA_CTS) + " cada una.");
		System.out.println("\nPREMIOS (se reparte el bote formado por la entrada de cada jugador):");
		System.out.println(" - LÍNEA: los " + Reglas.MAX_PREMIOS_LINEA + " primeros en validarla ganan el "
				+ Reglas.PORCENTAJE_PREMIO_LINEA + " % del bote.");
		System.out.println(" - BINGO: el primero en validarlo se lleva el resto del bote y termina la partida.");
		System.out.println("Al completar una línea o el bingo, se reclama solo y el Host te hará una pregunta");
		System.out.println("de sostenibilidad (A, B o C). Si la aciertas, ganas el premio.");
		System.out.println("\n[ESPERANDO] Conectado al Host. Esperando extracción de bolas...");
	}

	// ========== PANTALLA ==========

	/* DIBUJAR CABECERA DE DATOS */
	private static void dibujarCabecera(String nombreUsuario, Carton carton, int numeroBola) {
		System.out.println(LINEA);
		System.out.println(" BINGO ALMINGO | Jugador: " + nombreUsuario);
		System.out.println(" Bola " + numeroBola + " de " + Bombo.TOTAL_BOLAS
				+ " | Perdones: " + carton.getPerdonesUsados() + "/" + Reglas.OLVIDOS_PERDONADOS
				+ " | Multas: " + Reglas.formatearEuros(multasEnCentimos(carton)));
		System.out.println(LINEA);
	}

	/* CALCULAR MULTAS EN CÉNTIMOS */
	private static int multasEnCentimos(Carton carton) {
		int multables = carton.getFalsasAlarmas() - Reglas.FALSAS_ALARMAS_PERDONADAS;
		if (multables < 0) {
			multables = 0;
		}
		return multables * Reglas.MULTA_FALSA_ALARMA_CTS;
	}

	/* OBTENER HISTORIAL DE BOLAS ANTERIORES */
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

	/* BUCLE DE ESCUCHA Y PROCESAMIENTO DE ESTADOS */
	private static void iniciarBucleJuego(String nombreUsuario, Carton carton) {
		int bolasProcesadas = 0;

		boolean lineaResuelta = false;
		boolean bingoResuelto = false;

		boolean avisoPausa = false;

		boolean juegoActivo = true;

		while (juegoActivo) {
			Estado estado = GestorFicheros.leerEstado();

			if (estado != null) {
				String fase = estado.getFase();

				// PARTIDA CANCELADA
				if (fase.equals(Estado.FASE_CANCELADA)) {
					System.out.println("\n⚠️ [AVISO] La partida ha sido cancelada por el Host.");
					juegoActivo = false;

				// PARTIDA FINALIZADA
				} else.equals(Estado.FASE_FINALIZADA)) {
					System.out.println("\n🏁 [AVISO] La partida ha finalizado.");
					mostrarResumen(carton);
					juegoActivo = false;

				// PARTIDA EN PAUSA POR VALIDACIÓN DE OTRO JUGADOR
				} else if (fase.equals(Estado.FASE_VALIDANDO)) {
					if (!avisoPausa) {
						System.out.println("\n⏸ Un jugador ha reclamado línea o bingo. "
								+ "El Host está validando: partida en pausa...");
						avisoPausa = true;
					}

				// PARTIDA EN JUEGO
				} else if (fase.equals(Estado.FASE_JUGANDO)) {
					avisoPausa = false;

					int[] bolas = GestorFicheros.leerBolas();
					int bolasAntes = bolasProcesadas; 

					while (juegoActivo && bolasProcesadas < bolas.length) {
						int bola = bolas[bolasProcesadas];
						int pendientes = bolas.length - bolasProcesadas;

						if (pendientes > 1) {
							// BOLA ATRASADA
							aplicarRespuesta(nombreUsuario, carton, bola, bolasProcesadas + 1, false,
									"⏰ El " + bola + " salió y no diste respuesta a tiempo: cuenta como N.");
						} else {
							// ÚLTIMA BOLA
							preguntarBola(nombreUsuario, carton, bolas, bolasProcesadas);
						}
						bolasProcesadas++;

						// RECLAMAR LÍNEA
						if (!lineaResuelta && carton.tieneLinea()) {
							lineaResuelta = true;
							String resultado = reclamar(nombreUsuario, carton, bolasProcesadas, Reglas.TIPO_LINEA);

							if (resultado.equals(Reclamador.PARTIDA_TERMINADA)) {
								mostrarResumen(carton);
								juegoActivo = false;
							}
						}

						// RECLAMAR BINGO
						if (juegoActivo && !bingoResuelto && carton.tieneBingo()) {
							bingoResuelto = true;
							String resultado = reclamar(nombreUsuario, carton, bolasProcesadas, Reglas.TIPO_BINGO);

							if (resultado.equals(Reglas.RESULTADO_ACIERTO)
									|| resultado.equals(Reclamador.PARTIDA_TERMINADA)) {
								mostrarResumen(carton);
								juegoActivo = false;
							}
						}
					}

					// AVISAR ESPERA SIGUIENTE BOLA
					if (juegoActivo && bolasProcesadas > bolasAntes) {
						System.out.println("\n⏳ Esperando la siguiente bola...");
					}
				}
			}

			// PAUSA ENTRE CONSULTAS
			if (juegoActivo) {
				Utilidades.pausar(Reglas.INTERVALO_CONSULTA_MS);
			}
		}
	}

	// ========== RECLAMACIONES ==========

	/* GESTIONAR RECLAMACIÓN DE LÍNEA O BINGO */
	private static String reclamar(String nombreUsuario, Carton carton, int numeroBola, String tipo) {
		// AVISO EN PANTALLA
		if (tipo.equals(Reglas.TIPO_BINGO)) {
			System.out.println("\n🏆 ¡¡¡ HAS COMPLETADO EL BINGO !!!");
		} else {
			System.out.println("\n✨ ¡¡¡ HAS COMPLETADO UNA LÍNEA !!!");
		}

		// PROCESAR RECLAMO CON HOST
		String resultado = Reclamador.reclamar(nombreUsuario, tipo, teclado);

		// MOSTRAR RESULTADO
		Utilidades.limpiarPantalla();
		dibujarCabecera(nombreUsuario, carton, numeroBola);
		System.out.println(textoResultado(tipo, resultado) + "\n");
		carton.mostrarCarton();

		return resultado;
	}

	/* OBTENER TEXTO DESCRIPTIVO DEL RESULTADO */
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

		return "⚠️ El Host no ha respondido a tu reclamación. Comprueba que sigue en marcha.";
	}

	// ========== PREGUNTAS S/N ==========

	/* PREGUNTAR AL JUGADOR SI TIENE LA BOLA */
	private static void preguntarBola(String nombreUsuario, Carton carton, int[] bolas, int indice) {
		int bola = bolas[indice];
		int numeroBola = indice + 1;

		// MOSTRAR PANTALLA DE BOLA
		Utilidades.limpiarPantalla();
		dibujarCabecera(nombreUsuario, carton, numeroBola);
		System.out.println("Bolas anteriores: " + bolasAnteriores(bolas, indice));
		System.out.println("\n📢 ¡HA SALIDO EL " + bola + "!\n");
		carton.mostrarCarton();

		// DESCARTAR ENTRADAS PENDIENTES Y LEER RESPUESTA
		teclado.descartarPendiente();
		System.print("\n¿Está el " + bola + " en tu cartón? (S/N + ENTER, máximo "
				+ (Reglas.TIEMPO_RESPUESTA_MS / 1000) + " s): ");

		String respuesta = teclado.esperarLinea(Reglas.TIEMPO_RESPUESTA_MS);

		boolean dijoSi = false;
		String aviso = ""; 
		if (respuesta == null) {
			aviso = "⏰ Tiempo agotado (" + (Reglas.TIEMPO_RESPUESTA_MS / 1000)
					+ " s): no contestaste, cuenta como N.";
		} else if (respuesta.equalsIgnoreCase("S")) {
			dijoSi = true;
		} else if (!respuesta.equalsIgnoreCase("N")) {
			aviso = "Respuesta no válida (solo S o N): cuenta como N.";
		}

		// APLICAR RESPUESTA
		aplicarRespuesta(nombreUsuario, carton, bola, numeroBola, dijoSi, aviso);
	}

	/* GUARDAR RESPUESTA Y ACTUALIZAR CARTÓN */
	private static void aplicarRespuesta(String nombreUsuario, Carton carton, int bola, int numeroBola,
			boolean dijoSi, String aviso) {
		// GUARDAR EN FICHERO
		GestorFicheros.guardarRespuesta(nombreUsuario, bola, dijoSi);

		// ACTUALIZAR CARTÓN
		int resultado = carton.responder(bola, dijoSi);
		String mensaje = "";

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
			int numeroFalsa = carton.getFalsasAlarmas();
			if (numeroFalsa <= Reglas.FALSAS_ALARMAS_PERDONADAS) {
				mensaje = "⚠️ Falsa alarma: el " + bola + " NO está en tu cartón. Es la falsa alarma nº "
						+ numeroFalsa + ": esta te la perdonamos.";
			} else {
				mensaje = "💸 Falsa alarma: el " + bola + " NO está en tu cartón. Multa de "
						+ Reglas.formatearEuros(Reglas.MULTA_FALSA_ALARMA_CTS) + ".";
			}
		}

		// MOSTRAR PANTALLA DE RESULTADO
		Utilidades.limpiarPantalla();
		dibujarCabecera(nombreUsuario, carton, numeroBola);
		if (!aviso.equals("")) {
			System.out.println(aviso);
		}
		System.out.println(mensaje + "\n");
		carton.mostrarCarton();
	}

	/* MOSTRAR RESUMEN FINAL AL TERMINAR */
	private static void mostrarResumen(Carton carton) {
		System.out.println("\n=== TU RESUMEN ===");
		carton.mostrarCarton();
		System.out.println("Falsas alarmas: " + carton.getFalsasAlarmas() + " (multa total: "
				+ Reglas.formatearEuros(multasEnCentimos(carton)) + ")");
		System.out.println("Perdones de olvido gastados: " + carton.getPerdonesUsados()
				+ " de " + Reglas.OLVIDOS_PERDONADOS);
		if (!carton.bingoPosible()) {
			System.out.println("Esta partida no pudiste completar el Bingo por un olvido.");
		}
	}
}