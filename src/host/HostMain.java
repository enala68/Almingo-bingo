package host;

import java.io.File;

import comun.BancoPreguntas;
import comun.Bombo;
import comun.Carton;
import comun.Estado;
import comun.GestorFicheros;
import comun.Pregunta;
import comun.Reglas;
import comun.Utilidades;

/*
- SERVIDOR (HOST) DEL BINGO
 *
- EL HOST ESCRIBE EN FICHEROS Y LOS JUGADORES LOS LEEN.
- GESTIONA BOLAS, BOTE, RECLAMACIONES DE LÍNEA/BINGO Y PREGUNTAS DE SOSTENIBILIDAD.
 *
- FLUJO:
- PREPARAR CARPETAS Y LIMPIAR RESTOS.
-   2. ESTADO ESPERANDO + TIEMPO PARA CONECTARSE.
-   3. CONTAR JUGADORES (CARPETAS) Y FORMAR EL BOTE.
-   4. SACAR BOLAS; ENTRE BOLAS ATIENDE RECLAMACIONES (FASE VALIDANDO).
-   5. FINALIZAR CUANDO SE ACABE EL BOMBO O HAYA BINGO VÁLIDO.
 */

public class HostMain {

	private static final int ID_PARTIDA = 1;

	/* TIEMPO MANTENER RESULTADO ANTES DE BORRAR */
	private static final int PAUSA_RESULTADO_MS = 4000;

	/* BOTE ACUMULADO EN CÉNTIMOS */
	private static int boteCts = 0;

	/* PREMIOS DE LÍNEA CONCEDIDOS */
	private static int premiosLineaDados = 0;

	/* BANCO DE PREGUNTAS */
	private static BancoPreguntas banco;

	public static void main(String[] args) {
		System.out.println("=== INICIANDO SERVIDOR (HOST) ===");

		// CONFIGURAR RUTA BASE SI SE INDICA
		if (args.length >= 1 && args[0] != null && !args[0].trim().isEmpty()) {
			GestorFicheros.setRutaBase(args[0]);
			System.out.println("Carpeta compartida: " + GestorFicheros.getRutaBase());
		} else {
			System.out.println("No se indicó carpeta compartida. Usando carpeta local.");
		}

		// 1. PREPARAR DIRECTORIOS Y LIMPIAR
		GestorFicheros.crearDirectorios();
		GestorFicheros.limpiarPartidaAnterior();

		// CARGAR BANCO DE PREGUNTAS
		banco = new BancoPreguntas();
		if (banco.getTotal() == 0) {
			System.out.println("[AVISO] Sin preguntas: las reclamaciones no podrán validarse con pregunta.");
		}

		// 2. ESTADO INICIAL ESPERANDO
		Bombo bombo = new Bombo();
		Estado estado = new Estado(Estado.FASE_ESPERANDO, 0, 0, ID_PARTIDA);
		GestorFicheros.guardarEstado(estado);

		System.out.println("Partida #" + ID_PARTIDA + " creada correctamente.");
		System.out.println("Bombo listo con " + bombo.getBolasRestantes() + " bolas.");

		// 3. ESPERAR JUGADORES Y CALCULAR BOTE
		esperarJugadores();
		calcularBoteInicial();

		// 4. JUGAR RONDAS
		jugarRondas(bombo, estado);

		// 5. FINALIZAR PARTIDA
		finalizarPartida(estado);
	}

	/* ESPERAR CONEXIÓN DE JUGADORES */
	private static void esperarJugadores() {
		System.out.println("\n=== PREPARANDO PARTIDA ===");
		System.out.println("Esperando " + (Reglas.TIEMPO_ESPERA_INICIAL_MS / 1000)
				+ " segundos para que los jugadores se conecten...");
		Utilidades.pausar(Reglas.TIEMPO_ESPERA_INICIAL_MS);
	}

	/* CALCULAR BOTE INICIAL SEGÚN CARTONES */
	private static void calcularBoteInicial() {
		File carpeta = new File(GestorFicheros.getRutaBase().isEmpty()
				? GestorFicheros.DIR_CARTONES_HOST
				: GestorFicheros.getRutaBase() + "\\" + GestorFicheros.DIR_CARTONES_HOST.replace('/', '\\'));

		File[] ficheros = carpeta.listFiles();
		int jugadores = 0;
		if (ficheros != null) {
			for (File f : ficheros) {
				if (f.isFile() && f.getName().endsWith(".txt")) {
					jugadores++;
				}
			}
		}

		boteCts = jugadores * Reglas.ENTRADA_CTS;
		System.out.println("Jugadores detectados: " + jugadores
				+ " | Bote inicial: " + Reglas.formatearEuros(boteCts));
	}

	/* BUCLE PRINCIPAL DE EXTRACCIÓN Y ATENCIÓN DE RECLAMOS */
	private static void jugarRondas(Bombo bombo, Estado estado) {
		System.out.println("\n=== COMIENZA EL JUEGO ===");
		System.out.println("Bote actual: " + Reglas.formatearEuros(boteCts));

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
					+ ", Bote: " + Reglas.formatearEuros(boteCts) + ")");

			// ESPERAR HASTA SIGUIENTE BOLA ATENDIENDO RECLAMOS
			long finEspera = System.currentTimeMillis() + Reglas.TIEMPO_ENTRE_BOLAS_MS;
			while (System.currentTimeMillis() < finEspera && !bingoValidado) {
				long antes = System.currentTimeMillis();
				bingoValidado = atenderReclamos(estado);

				if (!bingoValidado) {
					if (System.currentTimeMillis() - antes > 2000) {
						finEspera = System.currentTimeMillis() + Reglas.TIEMPO_ENTRE_BOLAS_MS;
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

	/* PROCESAR RECLAMACIONES PENDIENTES */
	private static boolean atenderReclamos(Estado estado) {
		String[] pendientes = GestorFicheros.listarReclamosPendientes();
		if (pendientes.length == 0) {
			return false;
		}

		boolean huboBingo = false;
		boolean huboValidacion = false; 

		for (String nombre : pendientes) {

			String tipo = GestorFicheros.leerReclamo(nombre);
			if (tipo == null) {
				continue;
			}

			System.out.println("\n--- Reclamación de: " + nombre + " ---");

			// CAMBIAR A FASE VALIDANDO
			estado.setFase(Estado.FASE_VALIDANDO);
			estado.incrementarVersion();
			GestorFicheros.guardarEstado(estado);
			huboValidacion = true;

			System.out.println("Tipo: " + tipo);

			// VALIDAR JUGADA EN CARTÓN
			boolean jugadaValida = validarJugada(nombre, tipo);

			if (!jugadaValida) {
				System.out.println("Jugada FALSA.");
				GestorFicheros.guardarResultadoReclamo(nombre, Reglas.RESULTADO_FALSO);
				Utilidades.pausar(PAUSA_RESULTADO_MS);
				GestorFicheros.limpiarFicherosReclamo(nombre);
				continue;
			}

			System.out.println("Jugada VALIDADA.");

			// LÍNEA TARDE (FUERA DE PREMIO)
			if (tipo.equals(Reglas.TIPO_LINEA) && premiosLineaDados >= Reglas.MAX_PREMIOS_LINEA) {
				System.out.println("Ya se repartieron los " + Reglas.MAX_PREMIOS_LINEA
						+ " premios de línea. Resultado: TARDE.");
				GestorFicheros.guardarResultadoReclamo(nombre, Reglas.RESULTADO_TARDE);
				Utilidades.pausar(PAUSA_RESULTADO_MS);
				GestorFicheros.limpiarFicherosReclamo(nombre);
				continue;
			}

			// ENVIAR PREGUNTA DE SOSTENIBILIDAD
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

		// VOLVER A JUGANDO SI NO HUBO BINGO
		if (huboValidacion && !huboBingo) {
			estado.setFase(Estado.FASE_JUGANDO);
			estado.incrementarVersion();
			GestorFicheros.guardarEstado(estado);
		}

		return huboBingo;
	}

	/* VALIDAR RESPUESTAS Y ESTADO DEL CARTÓN */
	private static boolean validarJugada(String nombre, String tipo) {
		Carton carton = GestorFicheros.cargarCartonHost(nombre);
		if (carton == null) {
			System.out.println("No se pudo cargar el cartón de " + nombre);
			return false;
		}

		int[] salidas = GestorFicheros.leerBolas();
		String[][] respuestas = GestorFicheros.leerRespuestas(nombre);

		for (int i = 0; i < respuestas.length; i++) {
			try {
				int bola = Integer.parseInt(respuestas[i][0]);
				boolean dijoSi = respuestas[i][1].equals("S");

				if (haSalido(salidas, bola)) {
					carton.responder(bola, dijoSi);
				}
			} catch (NumberFormatException e) {
				// IGNORAR LÍNEA ROTA
			}
		}

		if (tipo.equals(Reglas.TIPO_LINEA)) {
			return carton.tieneLinea();
		}
		if (tipo.equals(Reglas.TIPO_BINGO)) {
			return carton.tieneBingo();
		}
		return false;
	}

	/* COMPROBAR SI LA BOLA YA HA SALIDO */
	private static boolean haSalido(int[] salidas, int bola) {
		for (int i = 0; i < salidas.length; i++) {
			if (salidas[i] == bola) {
				return true;
			}
		}
		return false;
	}

	/* ESPERAR RESPUESTA DEL JUGADOR A LA PREGUNTA */
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

	/* PAGAR PREMIO DE LÍNEA O BINGO */
	private static void pagarPremio(String nombre, String tipo) {
		if (tipo.equals(Reglas.TIPO_LINEA)) {
			int premio = (boteCts * Reglas.PORCENTAJE_PREMIO_LINEA) / 100;
			boteCts -= premio;
			premiosLineaDados++;
			System.out.println(">>> PREMIO LÍNEA para " + nombre + ": "
					+ Reglas.formatearEuros(premio)
					+ " (premio " + premiosLineaDados + "/" + Reglas.MAX_PREMIOS_LINEA + ")");
			System.out.println("    Bote restante: " + Reglas.formatearEuros(boteCts));
		} else if (tipo.equals(Reglas.TIPO_BINGO)) {
			int premio = boteCts;
			boteCts = 0;
			System.out.println(">>> PREMIO BINGO para " + nombre + ": "
					+ Reglas.formatearEuros(premio));
			System.out.println("    Bote restante: " + Reglas.formatearEuros(boteCts));
		}
	}

	/* FINALIZAR PARTIDA Y MOSTRAR RESUMEN */
	private static void finalizarPartida(Estado estado) {
		estado.setFase(Estado.FASE_FINALIZADA);
		estado.incrementarVersion();
		GestorFicheros.guardarEstado(estado);

		System.out.println("\n=== FIN DE LA PARTIDA ===");
		System.out.println("Bote final no repartido: " + Reglas.formatearEuros(boteCts));
	}
}