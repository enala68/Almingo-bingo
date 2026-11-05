package comun;

import java.util.Random;

/**
 * BOMBO DEL BINGO
 * ---------------------------------------------------------------------
 * Simula el bombo físico: contiene las bolas del 1 al 90 y las va
 * entregando de una en una, en orden aleatorio y sin repetir ninguna.
 *
 * TRUCO DE DISEÑO: en vez de "sacar una bola al azar y quitarla" (lo que
 * obligaría a borrar elementos del array), se BARAJA el array entero una
 * sola vez al principio y después se van entregando las bolas en orden,
 * avanzando un índice. El resultado es el mismo y el código es mucho
 * más simple, porque un array no permite borrar elementos.
 *
 * Ejemplo con 5 bolas:  [3, 5, 1, 4, 2] -> indiceActual=0 -> sale el 3,
 * indiceActual=1 -> sale el 5, y así sucesivamente.
 */
public class Bombo {

	/** Cantidad total de bolas (del 1 al 90). Pública para que otras clases la usen. */
	public static final int TOTAL_BOLAS = 90;

	// Array con las 90 bolas ya barajadas
	private final int[] bolas = new int[TOTAL_BOLAS];

	// Generador de números aleatorios (se usa para barajar)
	private final Random random = new Random();

	// Posición de la próxima bola a entregar. También cuenta cuántas han salido.
	private int indiceActual;

	/** Crea un bombo nuevo, ya lleno y barajado. */
	public Bombo() {
		reiniciarBombo();
	}

	/**
	 * Deja el bombo listo para una partida nueva: rellena el array con los
	 * números 1..90, los baraja y vuelve a poner el índice a 0.
	 */
	public void reiniciarBombo() {

		// PASO 1: rellenar. La posición 0 guarda el 1, la 1 el 2... la 89 el 90
		for (int i = 0; i < TOTAL_BOLAS; i++) {
			bolas[i] = i + 1;
		}

		// PASO 2: barajar (algoritmo de Fisher-Yates).
		// Recorremos de atrás hacia delante; en cada posición i intercambiamos
		// su bola con la de una posición aleatoria entre 0 e i. Garantiza
		// que todas las ordenaciones posibles son igual de probables.
		for (int i = TOTAL_BOLAS - 1; i > 0; i--) {
			int indiceRandom = random.nextInt(i + 1); // número entre 0 e i

			// Intercambio de dos posiciones usando una variable temporal
			int temporal = bolas[i];
			bolas[i] = bolas[indiceRandom];
			bolas[indiceRandom] = temporal;
		}

		// PASO 3: empezamos desde la primera bola
		indiceActual = 0;
	}

	/**
	 * @return true si todavía queda alguna bola por sacar
	 */
	public boolean quedanBolas() {
		return indiceActual < TOTAL_BOLAS;
	}

	/**
	 * Entrega la siguiente bola del bombo.
	 *
	 * @return número de la bola (1-90), o -1 si el bombo ya está vacío
	 */
	public int sacarBola() {
		if (!quedanBolas()) {
			return -1;
		}
		// Devuelve la bola actual y DESPUÉS avanza el índice (indiceActual++)
		return bolas[indiceActual++];
	}

	/**
	 * @return cuántas bolas faltan todavía por salir
	 */
	public int getBolasRestantes() {
		return TOTAL_BOLAS - indiceActual;
	}
}
