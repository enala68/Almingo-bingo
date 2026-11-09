package host;

import comun.Bombo;
import comun.Estado;
import comun.GestorFicheros;
import comun.Reglas;
import comun.Utilidades;

/*
 * SERVIDOR (HOST) DEL BINGO
 *
 * EL HOST ESCRIBE EN FICHEROS (host/estado.txt Y host/bolas.txt) Y LOS JUGADORES LOS LEEN.
 * LOS TIEMPOS SE CONFIGURAN EN LA CLASE Reglas.
 *
 * FLUJO DE LA PARTIDA:
 *   0. RUTA CARPETA COMPARTIDA
 *   1. PREPARAR CARPETAS Y LIMPIAR RESTOS DE LA PARTIDA ANTERIOR.
 *   2. CREAR BOMBO Y ESTADO INICIAL (FASE ESPERANDO).
 *   3. ESPERAR A QUE LOS JUGADORES SE CONECTEN (SOLO PUEDEN ENTRAR EN ESTA FASE).
 *   4. SACAR BOLAS HASTA VACIAR EL BOMBO (FASE JUGANDO).
 *   5. MARCAR LA PARTIDA COMO FINALIZADA.
 */
public class HostMain {

	/* IDENTIFICADOR DE LA PARTIDA (DE MOMENTO FIJO). */
	private static final int ID_PARTIDA = 1;

	public static void main(String[] args) {
		System.out.println("=== INICIANDO SERVIDOR (HOST) ===");

		// 0. RUTA BASE (CARPETA COMPARTIDA). SE PASA COMO PRIMER ARGUMENTO.
		//    EJ: java host.HostMain "\\192.168.0.112\BingoCompartido"
		if (args.length >= 1 && args[0] != null && !args[0].trim().isEmpty()) {
			GestorFicheros.setRutaBase(args[0]);
			System.out.println("Carpeta compartida: " + GestorFicheros.getRutaBase());
		} else {
			System.out.println("No se indicó carpeta compartida. Usando carpeta local.");
		}

		// 1. PREPARAR CARPETAS Y LIMPIAR LOS RESTOS DE LA PARTIDA ANTERIOR
		GestorFicheros.crearDirectorios();
		GestorFicheros.limpiarPartidaAnterior();

		// 2. CREAR BOMBO Y ESTADO INICIAL
		Bombo bombo = new Bombo();
		Estado estado = new Estado(Estado.FASE_ESPERANDO, 0, 0, ID_PARTIDA);
		GestorFicheros.guardarEstado(estado);

		System.out.println("Partida #" + ID_PARTIDA + " creada correctamente.");
		System.out.println("Bombo listo con " + bombo.getBolasRestantes() + " bolas.");

		// 3. DAR TIEMPO A LOS JUGADORES
		esperarJugadores();

		// 4. SACAR BOLAS HASTA QUE SE ACABE EL BOMBO
		jugarRondas(bombo, estado);

		// 5. CERRAR LA PARTIDA
		finalizarPartida(estado);
	}

	/*
	 * PAUSA INICIAL PARA QUE LOS JUGADORES:
	 * ARRANQUEN SU PROGRAMA, ESCRIBAN SU NOMBRE Y VEAN SU CARTÓN ANTES DE LA PRIMERA BOLA.
	 * UNA VEZ EMPIEZA EL JUEGO, YA NO SE PUEDE ENTRAR A LA PARTIDA.
	 */
	private static void esperarJugadores() {
		System.out.println("\n=== PREPARANDO PARTIDA ===");
		System.out.println("Esperando " + (Reglas.TIEMPO_ESPERA_INICIAL_MS / 1000)
				+ " segundos para que los jugadores se conecten...");
		Utilidades.pausar(Reglas.TIEMPO_ESPERA_INICIAL_MS);
	}

	/*
	 * BUCLE PRINCIPAL. EN CADA RONDA:
	 * 1. SACA UNA BOLA DEL BOMBO.
	 * 2. LA GUARDA EN bolas.txt (PRIMERO LA BOLA...).
	 * 3. ACTUALIZA Y GUARDA EL ESTADO (...Y DESPUÉS EL ESTADO, PARA QUE CUANDO
	 *    UN JUGADOR VEA LA VERSIÓN NUEVA, LA BOLA YA ESTÉ EN EL HISTORIAL).
	 * 4. ESPERA ANTES DE SACAR LA SIGUIENTE.
	 */
	private static void jugarRondas(Bombo bombo, Estado estado) {
		System.out.println("\n=== COMIENZA EL JUEGO ===");

		// PARTIDA EN MARCHA
		estado.setFase(Estado.FASE_JUGANDO);

		int ronda = 1;
		while (bombo.quedanBolas()) {
			int bola = bombo.sacarBola();

			// 1. GUARDAR LA BOLA EN EL HISTORIAL
			GestorFicheros.guardarBola(bola);

			// 2. ACTUALIZAR EL ESTADO (LA VERSIÓN SUBE +1)
			estado.setUltimaBola(bola);
			estado.incrementarVersion();
			GestorFicheros.guardarEstado(estado);

			System.out.println("Ronda " + ronda + " -> Bola: " + bola
					+ " (Versión: " + estado.getVersion()
					+ ", Faltan: " + bombo.getBolasRestantes() + ")");

			Utilidades.pausar(Reglas.TIEMPO_ENTRE_BOLAS_MS);
			ronda++;
		}
	}

	/* MARCA LA PARTIDA COMO FINALIZADA Y SUBE LA VERSIÓN PARA QUE LOS JUGADORES SE ENTEREN. */
	private static void finalizarPartida(Estado estado) {
		estado.setFase(Estado.FASE_FINALIZADA);
		estado.incrementarVersion();
		GestorFicheros.guardarEstado(estado);

		System.out.println("\n=== EL BOMBO SE HA VACIADO - FIN DE LA PARTIDA ===");
	}
}
