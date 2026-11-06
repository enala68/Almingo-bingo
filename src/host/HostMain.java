package host;

import comun.Bombo;
import comun.Estado;
import comun.GestorFicheros;
import comun.Utilidades;

/*
 * SERVIDOR (HOST) DEL BINGO
 * 
 * HOST ESCRIBE EN FICHEROS (host/estado.txt y host/bolas.txt) Y LOS JUGADORES LOS LEEN.
 * FLUJO DE LA PARTIDA:
 *
 *   1. PREPARAR CARPETAS Y FICHEROS.
 *   2. CREAR BOMBO Y ESTADO INICIAL (FASE ESPERANDO).
 *   3. ESPERAR 20 SEG A QUE LOS JUGADORES SE CONECTEN.
 *   4. SACAR BOLAS HASTA VACIAR BOMBO (FASE JUGANDO).
 *   5. MARCAR PARTIDA COMO FINALIZADA.
 *
 */
public class HostMain {

	/* ESPERA INICIAL PARA QUE LOS JUGADORES INICIEN SU MAIN (20 SEG). */
	private static final int TIEMPO_ESPERA_INICIAL_MS = 20000;

	/* PAUSA ENTRE BOLAS (0.5 s). */
	private static final int TIEMPO_ESPERA_MS = 500;

	/* IDENTIFICADOR DE LA PARTIDA (de momento fijo). */
	private static final int ID_PARTIDA = 1;

	public static void main(String[] args) {
		System.out.println("=== INICIANDO SERVIDOR (HOST) ===");

		// 1. PREPARAR CARPETAS Y LIMPIAR HISTORIAL DE BOLAS ANTERIOR
		GestorFicheros.crearDirectorios();
		GestorFicheros.reiniciarBolas();

		// 2. CREAR BOMBO Y ESTADO INICIAL
		Bombo bombo = new Bombo();
		Estado estado = new Estado(Estado.FASE_ESPERANDO, 0, 0, ID_PARTIDA);
		GestorFicheros.guardarEstado(estado);

		System.out.println("Partida #" + ID_PARTIDA + " creada correctamente.");
		System.out.println("Bombo listo con " + bombo.getBolasRestantes() + " bolas.");

		// 3. DAR TIEMPO A LOS JUGADORES
		esperarJugadores();

		// 4. SACARA BOLAS HASTA QUE SE ACABE EL BOMBO
		jugarRondas(bombo, estado);

		// 5. CERRAR LA PARTIDA
		finalizarPartida(estado);
	}

	/*
	 * PAUSA INICIAL PARA QUE JUGADORES:
	 * INICIEN MAIN, ESCRIBAN NOMBRE, VEAN SU  ARTÓN ANTES DE LA PRIMERA BOLA.
	 */
	private static void esperarJugadores() {
		System.out.println("\n=== PREPARANDO PARTIDA ===");
		System.out.println("Esperando " + (TIEMPO_ESPERA_INICIAL_MS / 1000)
				+ " segundos para que los jugadores se conecten...");
		Utilidades.pausar(TIEMPO_ESPERA_INICIAL_MS);
	}

	/*
	 * BUCLE PRINCIPAL:
	 * 1. SACA BOLA
	 * 2. ACTUALIZA ESTADO
	 */
	private static void jugarRondas(Bombo bombo, Estado estado) {
		System.out.println("\n=== COMIENZA EL JUEGO ===");

		// PARTIDA EN MARCHA
		estado.setFase(Estado.FASE_JUGANDO);

		int ronda = 1;
		while (bombo.quedanBolas()) {
			int bola = bombo.sacarBola();

			// 1. GUARDAR BOLA EN HISTORIAL
			GestorFicheros.guardarBola(bola);

			// 2. ACTUALIZAR ESTADO (VERSION SUBE +1)
			estado.setUltimaBola(bola);
			estado.incrementarVersion();
			GestorFicheros.guardarEstado(estado);

			System.out.println("Ronda " + ronda + " -> Bola: " + bola
					+ " (Versión: " + estado.getVersion()
					+ ", Faltan: " + bombo.getBolasRestantes() + ")");

			Utilidades.pausar(TIEMPO_ESPERA_MS);
			ronda++;
		}
	}

	/* MARCA ESTADO COMO FINALIZADA. */
	private static void finalizarPartida(Estado estado) {
		estado.setFase(Estado.FASE_FINALIZADA);
		estado.incrementarVersion();
		GestorFicheros.guardarEstado(estado);

		System.out.println("\n=== EL BOMBO SE HA VACIADO - FIN DE LA PARTIDA ===");
	}
}
