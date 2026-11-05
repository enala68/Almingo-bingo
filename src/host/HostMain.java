package host;

import comun.Bombo;
import comun.Estado;
import comun.GestorFicheros;
import comun.Utilidades;

/**
 * SERVIDOR (HOST) DEL BINGO
 * ---------------------------------------------------------------------
 * Dirige la partida. El flujo completo es:
 *
 *   1. Preparar carpetas y ficheros.
 *   2. Crear el bombo y el estado inicial (fase ESPERANDO).
 *   3. Esperar unos segundos a que los jugadores se conecten.
 *   4. Sacar bolas una a una (fase JUGANDO) hasta vaciar el bombo.
 *   5. Marcar la partida como FINALIZADA.
 *
 * La comunicación con los jugadores es indirecta: el Host solo escribe
 * ficheros (host/estado.txt y host/bolas.txt) y los jugadores los leen.
 *
 * Cada paso está en su propio método para que main() se lea como un
 * índice de lo que hace el programa.
 */
public class HostMain {

	/** Espera inicial para que los jugadores arranquen su programa (20 s). */
	private static final int TIEMPO_ESPERA_INICIAL_MS = 20000;

	/** Pausa entre una bola y la siguiente (5 s). */
	private static final int TIEMPO_ESPERA_MS = 500;

	/** Identificador de la partida (de momento fijo). */
	private static final int ID_PARTIDA = 1;

	public static void main(String[] args) {
		System.out.println("=== INICIANDO SERVIDOR (HOST) ===");

		// 1. Preparar carpetas y limpiar el historial de bolas anterior
		GestorFicheros.crearDirectorios();
		GestorFicheros.reiniciarBolas();

		// 2. Crear bombo y estado inicial
		Bombo bombo = new Bombo();
		Estado estado = new Estado(Estado.FASE_ESPERANDO, 0, 0, ID_PARTIDA);
		GestorFicheros.guardarEstado(estado);

		System.out.println("Partida #" + ID_PARTIDA + " creada correctamente.");
		System.out.println("Bombo listo con " + bombo.getBolasRestantes() + " bolas.");

		// 3. Dar tiempo a los jugadores
		esperarJugadores();

		// 4. Sacar bolas hasta que se acabe el bombo
		jugarRondas(bombo, estado);

		// 5. Cerrar la partida
		finalizarPartida(estado);
	}

	/**
	 * Pausa inicial. Durante este tiempo los jugadores pueden arrancar
	 * JugadorMain, escribir su nombre y ver su cartón antes de que salga
	 * la primera bola. El estado sigue en ESPERANDO.
	 */
	private static void esperarJugadores() {
		System.out.println("\n=== PREPARANDO PARTIDA ===");
		System.out.println("Esperando " + (TIEMPO_ESPERA_INICIAL_MS / 1000)
				+ " segundos para que los jugadores se conecten...");
		Utilidades.pausar(TIEMPO_ESPERA_INICIAL_MS);
	}

	/**
	 * Bucle principal: en cada ronda saca una bola, actualiza el estado y
	 * lo guarda en los ficheros. Se repite hasta que el bombo está vacío.
	 *
	 * IMPORTANTE - ORDEN DE GUARDADO: primero se guarda la bola en
	 * bolas.txt y DESPUÉS el estado. Los jugadores detectan novedades al
	 * ver que sube la versión del estado; si el estado se guardara antes,
	 * podrían leer la nueva versión cuando la bola aún no está en bolas.txt.
	 *
	 * @param bombo  bombo del que se extraen las bolas
	 * @param estado estado de la partida (se modifica en cada ronda)
	 */
	private static void jugarRondas(Bombo bombo, Estado estado) {
		System.out.println("\n=== COMIENZA EL JUEGO ===");

		// A partir de aquí la partida está en marcha
		estado.setFase(Estado.FASE_JUGANDO);

		int ronda = 1;
		while (bombo.quedanBolas()) {
			int bola = bombo.sacarBola();

			// 1) Guardar la bola en el historial
			GestorFicheros.guardarBola(bola);

			// 2) Actualizar y guardar el estado (la versión sube +1)
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

	/**
	 * Marca la partida como FINALIZADA. Se sube la versión para que los
	 * jugadores noten el cambio de fase.
	 *
	 * @param estado estado de la partida
	 */
	private static void finalizarPartida(Estado estado) {
		estado.setFase(Estado.FASE_FINALIZADA);
		estado.incrementarVersion();
		GestorFicheros.guardarEstado(estado);

		System.out.println("\n=== EL BOMBO SE HA VACIADO - FIN DE LA PARTIDA ===");
	}
}
