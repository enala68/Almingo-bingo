package host;

import comun.BancoPreguntas;
import comun.Bombo;
import comun.Carton;
import comun.Estado;
import comun.GestorFicheros;
import comun.Pregunta;
import comun.Reglas;
import comun.Utilidades;

/*
 * SERVIDOR (HOST) DEL BINGO.
 * ESCRIBE EN FICHEROS Y LOS JUGADORES LOS LEEN.
 * GESTIONA BOLAS, BOTE, RECLAMACIONES Y PREGUNTAS DE SOSTENIBILIDAD.
 *
 * FLUJO:
 *   1. PREPARAR CARPETAS.
 *   2. ESPERAR JUGADORES.
 *   3. FORMAR EL BOTE CON LAS ENTRADAS.
 *   4. SACAR BOLAS Y ATENDER RECLAMACIONES.
 *   5. FINALIZAR (BOMBO VACÍO O BINGO VÁLIDO).
 *
 * BOTE = ENTRADAS + MULTAS - PREMIOS REPARTIDOS.
 * EL HOST RECALCULA LAS MULTAS REPITIENDO LAS RESPUESTAS S/N DE CADA JUGADOR.
 * EL BOTE SE PUBLICA EN host/bote.txt CADA VEZ QUE CAMBIA.
 */
public class HostMain {

	private static final int ID_PARTIDA = 1;

	/* PAUSA PARA QUE EL JUGADOR LEA EL RESULTADO (4 SEG). */
	private static final int PAUSA_RESULTADO_MS = 4000;

	/* INTERVALO DE ACTUALIZACIÓN DEL BOTE (1 SEG). */
	private static final int INTERVALO_BOTE_MS = 1000;

	// ---------------------------------------------------------------
	// DINERO (EN CÉNTIMOS)
	// ---------------------------------------------------------------

	/* ENTRADAS: UNA POR JUGADOR, FIJADAS AL EMPEZAR. */
	private static int entradasCts = 0;

	/* MULTAS DE TODOS LOS JUGADORES. */
	private static int multasCts = 0;

	/* PREMIOS YA REPARTIDOS. */
	private static int premiosPagadosCts = 0;

	/* ÚLTIMO BOTE PUBLICADO (-1 = NINGUNO). */
	private static int botePublicadoCts = -1;

	/* HORA DE LA ÚLTIMA ACTUALIZACIÓN DEL BOTE. */
	private static long ultimaActualizacionBote = 0;

	/* PREMIOS DE LÍNEA CONCEDIDOS. */
	private static int premiosLineaDados = 0;

	/* BANCO DE PREGUNTAS. */
	private static BancoPreguntas banco;

	public static void main(String[] args) {
		System.out.println("=== INICIANDO SERVIDOR (HOST) ===");

		// 0. RUTA BASE (CARPETA COMPARTIDA)
		if (args.length >= 1 && args[0] != null && !args[0].trim().isEmpty()) {
			GestorFicheros.setRutaBase(args[0]);
			System.out.println("Carpeta compartida: " + GestorFicheros.getRutaBase());
		} else {
			System.out.println("No se indicó carpeta compartida. Usando carpeta local.");
		}

		// 1. PREPARAR
		GestorFicheros.crearDirectorios();
		GestorFicheros.limpiarPartidaAnterior();

		// BANCO DE PREGUNTAS
		banco = new BancoPreguntas();
		if (banco.getTotal() == 0) {
			System.out.println("[AVISO] Sin preguntas: las reclamaciones no podrán validarse con pregunta.");
		}

		// 2. ESTADO INICIAL
		Bombo bombo = new Bombo();
		Estado estado = new Estado(Estado.FASE_ESPERANDO, 0, 0, ID_PARTIDA);
		GestorFicheros.guardarEstado(estado);

		System.out.println("Partida #" + ID_PARTIDA + " creada correctamente.");
		System.out.println("Bombo listo con " + bombo.getBolasRestantes() + " bolas.");

		// 3. ESPERAR JUGADORES Y FORMAR BOTE
		esperarJugadores();
		calcularBoteInicial();

		// 4. JUEGO
		jugarRondas(bombo, estado);

		// 5. FIN
		finalizarPartida(estado);
	}

	private static void esperarJugadores() {
		System.out.println("\n=== PREPARANDO PARTIDA ===");
		System.out.println("Esperando " + (Reglas.TIEMPO_ESPERA_INICIAL_MS / 1000)
				+ " segundos para que los jugadores se conecten...");
		Utilidades.pausar(Reglas.TIEMPO_ESPERA_INICIAL_MS);
	}

	// =================================================================
	// BOTE
	// =================================================================

	/* CUENTA JUGADORES, FORMA EL BOTE CON SUS ENTRADAS Y LO PUBLICA. */
	private static void calcularBoteInicial() {
		int jugadores = GestorFicheros.listarJugadores().length;

		entradasCts = jugadores * Reglas.ENTRADA_CTS;
		System.out.println("Jugadores detectados: " + jugadores
				+ " | Entradas: " + Reglas.formatearEuros(entradasCts));

		actualizarBote();
	}

	/* BOTE ACTUAL EN CÉNTIMOS. */
	private static int calcularBote() {
		return entradasCts + multasCts - premiosPagadosCts;
	}

	/* RECALCULA MULTAS Y PUBLICA EL BOTE SI HA CAMBIADO. */
	private static void actualizarBote() {
		ultimaActualizacionBote = System.currentTimeMillis();

		recalcularMultas();
		int bote = calcularBote();

		if (bote != botePublicadoCts) {
			GestorFicheros.guardarBote(bote);
			botePublicadoCts = bote;
			System.out.println("[BOTE] Bote actualizado: " + Reglas.formatearEuros(bote)
					+ " (multas acumuladas: " + Reglas.formatearEuros(multasCts) + ")");
		}
	}

	/*
	 * CALCULA LAS MULTAS DE TODOS LOS JUGADORES CONTANDO SUS FALSAS ALARMAS.
	 * LAS PRIMERAS (Reglas.FALSAS_ALARMAS_PERDONADAS) SON GRATIS.
	 */
	private static void recalcularMultas() {
		String[] jugadores = GestorFicheros.listarJugadores();
		int[] salidas = GestorFicheros.leerBolas();
		int total = 0;

		for (int i = 0; i < jugadores.length; i++) {
			Carton carton = GestorFicheros.cargarCartonHost(jugadores[i]);

			if (carton != null) {
				reproducirRespuestas(carton, jugadores[i], salidas);

				int multables = carton.getFalsasAlarmas() - Reglas.FALSAS_ALARMAS_PERDONADAS;
				if (multables > 0) {
					total = total + multables * Reglas.MULTA_FALSA_ALARMA_CTS;
				}
			}
		}

		multasCts = total;
	}

	// =================================================================
	// RONDAS
	// =================================================================

	/* SACA BOLAS, ATIENDE RECLAMACIONES Y ACTUALIZA EL BOTE. */
	private static void jugarRondas(Bombo bombo, Estado estado) {
		System.out.println("\n=== COMIENZA EL JUEGO ===");
		System.out.println("Bote actual: " + Reglas.formatearEuros(calcularBote()));

		estado.setFase(Estado.FASE_JUGANDO);
		GestorFicheros.guardarEstado(estado);

		int ronda = 1;
		boolean bingoValidado = false;

		while (bombo.quedanBolas() && !bingoValidado) {
			// ATENDER RECLAMOS ANTES DE SACAR BOLA
			bingoValidado = atenderReclamos(estado);
			if (bingoValidado) {
				break;
			}

			int bola = bombo.sacarBola();

			GestorFicheros.guardarBola(bola);
			estado.setUltimaBola(bola);
			estado.setFase(Estado.FASE_JUGANDO);
			estado.incrementarVersion();
			GestorFicheros.guardarEstado(estado);

			System.out.println("Ronda " + ronda + " -> Bola: " + bola
					+ " (Versión: " + estado.getVersion()
					+ ", Faltan: " + bombo.getBolasRestantes()
					+ ", Bote: " + Reglas.formatearEuros(calcularBote()) + ")");

			// ESPERA A LA SIGUIENTE BOLA ATENDIENDO RECLAMOS Y ACTUALIZANDO EL BOTE
			long finEspera = System.currentTimeMillis() + Reglas.TIEMPO_ENTRE_BOLAS_MS;
			while (System.currentTimeMillis() < finEspera && !bingoValidado) {
				long antes = System.currentTimeMillis();
				bingoValidado = atenderReclamos(estado);

				if (!bingoValidado) {
					// SI TARDÓ MÁS DE 2 SEG HUBO VALIDACIÓN: SE REINICIA LA ESPERA
					if (System.currentTimeMillis() - antes > 2000) {
						finEspera = System.currentTimeMillis() + Reglas.TIEMPO_ENTRE_BOLAS_MS;
					}

					// ACTUALIZAR BOTE (COMO MUCHO UNA VEZ POR SEGUNDO)
					if (System.currentTimeMillis() - ultimaActualizacionBote >= INTERVALO_BOTE_MS) {
						actualizarBote();
					}

					Utilidades.pausar(Reglas.INTERVALO_CONSULTA_MS);
				}
			}

			ronda++;
		}

		if (bingoValidado) {
			System.out.println("\n=== BINGO VALIDADO - FIN DE LA PARTIDA ===");
		}
	}

	// =================================================================
	// RECLAMACIONES
	// =================================================================

	/* PROCESA LOS RECLAMOS PENDIENTES EN ORDEN DE LLEGADA. */
	private static boolean atenderReclamos(Estado estado) {
		String[] pendientes = GestorFicheros.listarReclamosPendientes();
		if (pendientes.length == 0) {
			return false;
		}

		boolean huboBingo = false;
		boolean huboValidacion = false;

		for (String nombre : pendientes) {

			// SI EL JUGADOR AÚN ESTÁ ESCRIBIENDO EL FICHERO (null), SE REINTENTA DESPUÉS
			String tipo = GestorFicheros.leerReclamo(nombre);
			if (tipo == null) {
				continue;
			}

			System.out.println("\n--- Reclamación de: " + nombre + " ---");

			// PASAR A VALIDANDO
			estado.setFase(Estado.FASE_VALIDANDO);
			estado.incrementarVersion();
			GestorFicheros.guardarEstado(estado);
			huboValidacion = true;

			System.out.println("Tipo: " + tipo);

			// VALIDAR SOBRE LA COPIA DEL CARTÓN
			boolean jugadaValida = validarJugada(nombre, tipo);

			if (!jugadaValida) {
				System.out.println("Jugada FALSA.");
				GestorFicheros.guardarResultadoReclamo(nombre, Reglas.RESULTADO_FALSO);
				Utilidades.pausar(PAUSA_RESULTADO_MS);
				GestorFicheros.limpiarFicherosReclamo(nombre);
				continue;
			}

			System.out.println("Jugada VALIDADA.");

			// LÍNEA VÁLIDA PERO SIN PREMIO (YA SE REPARTIERON TODOS)
			if (tipo.equals(Reglas.TIPO_LINEA) && premiosLineaDados >= Reglas.MAX_PREMIOS_LINEA) {
				System.out.println("Ya se repartieron los " + Reglas.MAX_PREMIOS_LINEA
						+ " premios de línea. Resultado: TARDE.");
				GestorFicheros.guardarResultadoReclamo(nombre, Reglas.RESULTADO_TARDE);
				Utilidades.pausar(PAUSA_RESULTADO_MS);
				GestorFicheros.limpiarFicherosReclamo(nombre);
				continue;
			}

			// PREGUNTA DE SOSTENIBILIDAD
			Pregunta pregunta = (banco != null) ? banco.elegirAleatoria() : null;
			if (pregunta == null) {
				System.out.println("[AVISO] No hay pregunta disponible. Se da por FALLO.");
				GestorFicheros.guardarResultadoReclamo(nombre, Reglas.RESULTADO_FALLO);
				Utilidades.pausar(PAUSA_RESULTADO_MS);
				GestorFicheros.limpiarFicherosReclamo(nombre);
				continue;
			}

			GestorFicheros.guardarPreguntaReclamo(nombre, pregunta.aLineasParaJugador());
			System.out.println("Pregunta enviada. Esperando respuesta ("
					+ (Reglas.TIEMPO_PREGUNTA_MS / 1000) + " s)...");

			String respuesta = esperarRespuestaJugador(nombre, Reglas.TIEMPO_PREGUNTA_MS);

			if (respuesta != null && pregunta.esCorrecta(respuesta)) {
				System.out.println("Respuesta CORRECTA (" + respuesta + ").");
				GestorFicheros.guardarResultadoReclamo(nombre, Reglas.RESULTADO_ACIERTO);
				pagarPremio(nombre, tipo);

				if (tipo.equals(Reglas.TIPO_BINGO)) {
					huboBingo = true;
				}
			} else {
				if (respuesta == null) {
					System.out.println("Sin respuesta a tiempo.");
				} else {
					System.out.println("Respuesta INCORRECTA (" + respuesta + "). Correcta: "
							+ pregunta.getCorrecta());
				}
				GestorFicheros.guardarResultadoReclamo(nombre, Reglas.RESULTADO_FALLO);
			}

			Utilidades.pausar(PAUSA_RESULTADO_MS);
			GestorFicheros.limpiarFicherosReclamo(nombre);

			if (huboBingo) {
				break;
			}
		}

		// VOLVER A JUGANDO SI HUBO VALIDACIÓN SIN BINGO
		if (huboValidacion && !huboBingo) {
			estado.setFase(Estado.FASE_JUGANDO);
			estado.incrementarVersion();
			GestorFicheros.guardarEstado(estado);
		}

		return huboBingo;
	}

	/* REPRODUCE LAS RESPUESTAS DEL JUGADOR SOBRE SU CARTÓN Y COMPRUEBA LÍNEA/BINGO. */
	private static boolean validarJugada(String nombre, String tipo) {
		Carton carton = GestorFicheros.cargarCartonHost(nombre);
		if (carton == null) {
			System.out.println("No se pudo cargar el cartón de " + nombre);
			return false;
		}

		reproducirRespuestas(carton, nombre, GestorFicheros.leerBolas());

		if (tipo.equals(Reglas.TIPO_LINEA)) {
			return carton.tieneLinea();
		}
		if (tipo.equals(Reglas.TIPO_BINGO)) {
			return carton.tieneBingo();
		}
		return false;
	}

	/*
	 * APLICA AL CARTÓN LAS RESPUESTAS S/N DEL JUGADOR, EN ORDEN.
	 * SOLO CUENTAN LAS BOLAS QUE HAN SALIDO DE VERDAD DEL BOMBO.
	 *
	 * @param salidas BOLAS SALIDAS HASTA AHORA
	 */
	private static void reproducirRespuestas(Carton carton, String nombre, int[] salidas) {
		String[][] respuestas = GestorFicheros.leerRespuestas(nombre);

		for (int i = 0; i < respuestas.length; i++) {
			try {
				int bola = Integer.parseInt(respuestas[i][0]);
				boolean dijoSi = respuestas[i][1].equals("S");

				if (haSalido(salidas, bola)) {
					carton.responder(bola, dijoSi);
				}
			} catch (NumberFormatException e) {
				// LÍNEA ROTA: SE IGNORA
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

	/** ESPERA LA RESPUESTA A/B/C DEL JUGADOR HASTA tiempoMs. */
	private static String esperarRespuestaJugador(String nombre, int tiempoMs) {
		long limite = System.currentTimeMillis() + tiempoMs;
		while (System.currentTimeMillis() < limite) {
			String resp = GestorFicheros.leerRespuestaReclamo(nombre);
			if (resp != null) {
				return resp;
			}
			Utilidades.pausar(Reglas.INTERVALO_CONSULTA_MS);
		}
		return null;
	}

	/*
	 * PAGA EL PREMIO: LÍNEA (% DEL BOTE) O BINGO (TODO EL BOTE).
	 * ACTUALIZA EL BOTE ANTES Y DESPUÉS DE REPARTIR.
	 */
	private static void pagarPremio(String nombre, String tipo) {
		actualizarBote();
		int bote = calcularBote();

		if (tipo.equals(Reglas.TIPO_LINEA)) {
			int premio = (bote * Reglas.PORCENTAJE_PREMIO_LINEA) / 100;
			premiosPagadosCts = premiosPagadosCts + premio;
			premiosLineaDados++;
			System.out.println(">>> PREMIO LÍNEA para " + nombre + ": "
					+ Reglas.formatearEuros(premio)
					+ " (premio " + premiosLineaDados + "/" + Reglas.MAX_PREMIOS_LINEA + ")");

		} else if (tipo.equals(Reglas.TIPO_BINGO)) {
			int premio = bote;
			premiosPagadosCts = premiosPagadosCts + premio;
			System.out.println(">>> PREMIO BINGO para " + nombre + ": "
					+ Reglas.formatearEuros(premio));
		}

		actualizarBote();
		System.out.println("    Bote restante: " + Reglas.formatearEuros(calcularBote()));
	}

	private static void finalizarPartida(Estado estado) {
		// ÚLTIMA ACTUALIZACIÓN DEL BOTE
		actualizarBote();

		estado.setFase(Estado.FASE_FINALIZADA);
		estado.incrementarVersion();
		GestorFicheros.guardarEstado(estado);

		System.out.println("\n=== FIN DE LA PARTIDA ===");
		System.out.println("Bote final no repartido: " + Reglas.formatearEuros(calcularBote()));
	}
}