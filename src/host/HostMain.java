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
 * SERVIDOR (HOST) DEL BINGO
 *
 * EL HOST ESCRIBE EN FICHEROS Y LOS JUGADORES LOS LEEN.
 * GESTIONA BOLAS, BOTE, RECLAMACIONES DE LÍNEA/BINGO Y PREGUNTAS DE SOSTENIBILIDAD.
 *
 * FLUJO:
 *   1. PREPARAR CARPETAS Y LIMPIAR RESTOS.
 *   2. ESTADO ESPERANDO + TIEMPO PARA CONECTARSE.
 *   3. CONTAR JUGADORES (CARTONES) Y FORMAR EL BOTE CON LAS ENTRADAS.
 *   4. SACAR BOLAS; ENTRE BOLAS ATIENDE RECLAMACIONES (FASE VALIDANDO) Y ACTUALIZA EL BOTE CON LAS MULTAS DE LOS JUGADORES.
 *   5. FINALIZAR CUANDO SE ACABE EL BOMBO O HAYA BINGO VÁLIDO.
 *
 * EL BOTE:
 *   BOTE = ENTRADAS + MULTAS - PREMIOS YA REPARTIDOS
 *   - ENTRADAS: Reglas.ENTRADA_CTS POR CADA JUGADOR (SE FIJAN AL EMPEZAR).
 *   - MULTAS: EL HOST REPITE LAS RESPUESTAS S/N DE CADA JUGADOR SOBRE LA COPIA DE
 *     SU CARTÓN Y CUENTA SUS FALSAS ALARMAS. ASÍ NO SE FÍA DE LO QUE DIGA EL JUGADOR.
 *   - CADA VEZ QUE CAMBIA, SE PUBLICA EN host/bote.txt PARA QUE LOS JUGADORES LO VEAN.
 */
public class HostMain {

	private static final int ID_PARTIDA = 1;

	/*
	 * TIEMPO QUE EL HOST MANTIENE EL RESULTADO DE UNA RECLAMACIÓN ANTES DE BORRAR LOS
	 * FICHEROS (4 SEG). ASÍ EL JUGADOR TIENE TIEMPO DE LEERLO AUNQUE LA CARPETA
	 * COMPARTIDA VAYA LENTA.
	 */
	private static final int PAUSA_RESULTADO_MS = 4000;

	/* CADA CUÁNTO SE RECALCULAN LAS MULTAS Y SE ACTUALIZA EL BOTE (1 SEG). */
	private static final int INTERVALO_BOTE_MS = 1000;

	// ---------------------------------------------------------------
	// DINERO (TODO EN CÉNTIMOS)
	// ---------------------------------------------------------------

	/* ENTRADAS: UNA POR JUGADOR. SE FIJA UNA SOLA VEZ, AL EMPEZAR LA PARTIDA. */
	private static int entradasCts = 0;

	/* MULTAS DE TODOS LOS JUGADORES. SE RECALCULA CADA INTERVALO_BOTE_MS. */
	private static int multasCts = 0;

	/* PREMIOS QUE YA SE HAN REPARTIDO (SE RESTAN DEL BOTE). */
	private static int premiosPagadosCts = 0;

	/* ÚLTIMO BOTE PUBLICADO EN host/bote.txt (-1 = NINGUNO TODAVÍA). SIRVE PARA PUBLICAR SOLO SI CAMBIA. */
	private static int botePublicadoCts = -1;

	/* HORA (EN MILISEGUNDOS) DE LA ÚLTIMA ACTUALIZACIÓN DEL BOTE. */
	private static long ultimaActualizacionBote = 0;

	/* PREMIOS DE LINEA QUE SE HAN CONCEDIDO */
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

	/*
	 * CUENTA LOS JUGADORES (UN CARTÓN POR JUGADOR) Y FORMA EL BOTE CON SUS ENTRADAS.
	 * AL FINAL LO PUBLICA PARA QUE LOS JUGADORES YA LO VEAN EN LA PRIMERA BOLA.
	 */
	private static void calcularBoteInicial() {
		int jugadores = GestorFicheros.listarJugadores().length;

		entradasCts = jugadores * Reglas.ENTRADA_CTS;
		System.out.println("Jugadores detectados: " + jugadores
				+ " | Entradas: " + Reglas.formatearEuros(entradasCts));

		actualizarBote();
	}

	/* DEVUELVE EL BOTE ACTUAL EN CÉNTIMOS: ENTRADAS + MULTAS - PREMIOS REPARTIDOS. */
	private static int calcularBote() {
		return entradasCts + multasCts - premiosPagadosCts;
	}

	/*
	 * RECALCULA LAS MULTAS DE TODOS LOS JUGADORES Y, SI EL BOTE HA CAMBIADO, LO PUBLICA
	 * EN host/bote.txt Y LO ESCRIBE EN LA CONSOLA DEL HOST.
	 */
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
	 * CALCULA LAS MULTAS DE TODOS LOS JUGADORES.
	 * PARA CADA JUGADOR SE REPITEN SUS RESPUESTAS S/N SOBRE LA COPIA DE SU CARTÓN Y SE
	 * CUENTAN SUS FALSAS ALARMAS. LAS PRIMERAS (Reglas.FALSAS_ALARMAS_PERDONADAS) NO
	 * CUESTAN NADA; LAS SIGUIENTES CUESTAN Reglas.MULTA_FALSA_ALARMA_CTS CADA UNA.
	 * ES EL MISMO CÁLCULO QUE HACE EL JUGADOR PARA MOSTRAR SUS MULTAS.
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

	/* SACA BOLAS Y ENTRE BOLAS ATIENDE RECLAMACIONES Y ACTUALIZA EL BOTE. */
	private static void jugarRondas(Bombo bombo, Estado estado) {
		System.out.println("\n=== COMIENZA EL JUEGO ===");
		System.out.println("Bote actual: " + Reglas.formatearEuros(calcularBote()));

		estado.setFase(Estado.FASE_JUGANDO);
		GestorFicheros.guardarEstado(estado);

		int ronda = 1;
		boolean bingoValidado = false;

		while (bombo.quedanBolas() && !bingoValidado) {
			// ATENDER POSIBLES RECLAMOS ANTES DE SACAR LA SIGUIENTE BOLA
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

			// ESPERA HASTA LA SIGUIENTE BOLA, ATENDIENDO RECLAMACIONES Y ACTUALIZANDO
			// EL BOTE MIENTRAS TANTO (LOS JUGADORES VAN RESPONDIENDO Y PUEDEN SER MULTADOS)
			long finEspera = System.currentTimeMillis() + Reglas.TIEMPO_ENTRE_BOLAS_MS;
			while (System.currentTimeMillis() < finEspera && !bingoValidado) {
				long antes = System.currentTimeMillis();
				bingoValidado = atenderReclamos(estado);

				if (!bingoValidado) {
					// SI HA TARDADO MÁS DE 2 SEGUNDOS, SE HA VALIDADO UNA RECLAMACIÓN Y LA
					// PARTIDA HA ESTADO EN PAUSA: SE DA A LOS JUGADORES EL TIEMPO COMPLETO
					// HASTA LA SIGUIENTE BOLA
					if (System.currentTimeMillis() - antes > 2000) {
						finEspera = System.currentTimeMillis() + Reglas.TIEMPO_ENTRE_BOLAS_MS;
					}

					// ACTUALIZAR EL BOTE CON LAS MULTAS NUEVAS (COMO MUCHO UNA VEZ POR SEGUNDO)
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

	/* PROCESA RECLAMOS PENDIENTES EN ORDEN DE LLEGADA. */
	private static boolean atenderReclamos(Estado estado) {
		String[] pendientes = GestorFicheros.listarReclamosPendientes();
		if (pendientes.length == 0) {
			return false;
		}

		boolean huboBingo = false;
		boolean huboValidacion = false; // SE PUSO LA PARTIDA EN PAUSA PARA VALIDAR ALGO

		for (String nombre : pendientes) {

			// LEER EL TIPO ANTES DE PONER LA PARTIDA EN PAUSA.
			// SI EL JUGADOR TODAVÍA ESTÁ ESCRIBIENDO EL FICHERO, SE LEE VACÍO (null):
			// NO SE BORRA NADA Y SE REINTENTA EN LA SIGUIENTE CONSULTA.
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

			// VALIDAR RESPUESTAS SOBRE LA COPIA DEL CARTON
			boolean jugadaValida = validarJugada(nombre, tipo);

			if (!jugadaValida) {
				System.out.println("Jugada FALSA.");
				GestorFicheros.guardarResultadoReclamo(nombre, Reglas.RESULTADO_FALSO);
				Utilidades.pausar(PAUSA_RESULTADO_MS);
				GestorFicheros.limpiarFicherosReclamo(nombre);
				continue;
			}

			System.out.println("Jugada VALIDADA.");

			// LINEA FUERA DE LOS 3 PRIMEROS PREMIOS (SIN PREMIO PERO VALIDADA)
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

		// VOLVER A JUGANDO SI SE HA VALIDADO ALGO Y NO HUBO BINGO
		if (huboValidacion && !huboBingo) {
			estado.setFase(Estado.FASE_JUGANDO);
			estado.incrementarVersion();
			GestorFicheros.guardarEstado(estado);
		}

		return huboBingo;
	}

	/*
	 * REPRODUCE LAS RESPUESTAS S/N DEL JUGADOR SOBRE LA COPIA DEL CARTÓN DEL HOST
	 * Y COMPRUEBA LINEA/BINGO.
	 */
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
	 * APLICA AL CARTÓN TODAS LAS RESPUESTAS S/N QUE EL JUGADOR HA DADO, EN ORDEN.
	 * DESPUÉS, EL CARTÓN QUEDA IGUAL QUE EL DEL JUGADOR (TACHADOS, OLVIDOS, FALSAS ALARMAS).
	 * SOLO SE TIENEN EN CUENTA LAS RESPUESTAS A BOLAS QUE HAN SALIDO DE VERDAD DEL BOMBO:
	 * ASÍ UN JUGADOR NO PUEDE INVENTARSE RESPUESTAS A NÚMEROS QUE TODAVÍA NO HAN SALIDO.
	 * LA USAN validarJugada (LÍNEA/BINGO) Y recalcularMultas (BOTE).
	 *
	 * @param salidas BOLAS QUE HAN SALIDO HASTA AHORA
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
				// LÍNEA ROTA EN EL FICHERO DE RESPUESTAS: SE IGNORA
			}
		}
	}

	/* COMPRUEBA SI UNA BOLA HA SALIDO YA DEL BOMBO (ESTÁ EN LA LISTA DE BOLAS SALIDAS). */
	private static boolean haSalido(int[] salidas, int bola) {
		for (int i = 0; i < salidas.length; i++) {
			if (salidas[i] == bola) {
				return true;
			}
		}
		return false;
	}

	/** ESPERA HASTA TIEMPO_PREGUNTA_MS A QUE EL JUGADOR ESCRIBA SU RESPUESTA A/B/C. */
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
	 * PAGA EL PREMIO DE LA LINEA (10% DEL BOTE) O BINGO (TODO EL BOTE RESTANTE).
	 * ANTES DE REPARTIR SE ACTUALIZA EL BOTE PARA INCLUIR LAS ÚLTIMAS MULTAS,
	 * Y DESPUÉS SE VUELVE A PUBLICAR YA SIN EL PREMIO.
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
		// ÚLTIMA ACTUALIZACIÓN: INCLUYE LAS MULTAS DE LAS ÚLTIMAS RESPUESTAS
		actualizarBote();

		estado.setFase(Estado.FASE_FINALIZADA);
		estado.incrementarVersion();
		GestorFicheros.guardarEstado(estado);

		System.out.println("\n=== FIN DE LA PARTIDA ===");
		System.out.println("Bote final no repartido: " + Reglas.formatearEuros(calcularBote()));
	}
}
