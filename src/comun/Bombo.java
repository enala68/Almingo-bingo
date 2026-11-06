package comun;

import java.util.Random;

/**
 * BOMBO DEL BINGO
 * ---------------------------------------------------------------------
 * BOMBO CON 90 BOLAS ENTREGADAS EN ORDEN ALEATORIO DE UNA EN UNA SIN REPETICIONES.
 * DISEÑO: SE BARAJA EL ARRAY ENTERO AL PRINCIPIO Y DESPUÉS SE VAN ENTREGANDO LAS BOLAS EN ORDEN.
 */
public class Bombo {

	// TOTAL DE BOLAS (1-90).
	public static final int TOTAL_BOLAS = 90;

	// ARRAY CON LAS BOLAS YA BARAJADAS
	private final int[] bolas = new int[TOTAL_BOLAS];

	// GENERADOR DE NÚMEROS ALEATORIOS
	private final Random random = new Random();

	// POSICION DE LA PRÓXIMA BOLA A ENTREGAR (CUÁNTAS HAN SALIDO).
	private int indiceActual;

	/** CREA BOMBO NUEVO, LLENO Y BARAJADO. */
	public Bombo() {
		reiniciarBombo();
	}

	/* BOMBO RELLENO, BARAJADO Y CON INDICE A 0 */
	public void reiniciarBombo() {

		// PASO 1: RELLENAR LA POSICIÓN (LA 0 ES LA 1, LA 1 ES LA 2, ETC)
		for (int i = 0; i < TOTAL_BOLAS; i++) {
			bolas[i] = i + 1;
		}

		// PASO 2: BARAJAR:
		// SE RECORRE HACIA ATRÁS. EN CADA POSICIÓN i SE INTERCAMBIA SU BOLA POR UNA ALEATORIA ENTRE 0 E i.
		for (int i = TOTAL_BOLAS - 1; i > 0; i--) {
			int indiceRandom = random.nextInt(i + 1); // NÚMEROS ENTRE 0 e i.

			// INTERCAMBIO DE POSICIONES
			int temporal = bolas[i];
			bolas[i] = bolas[indiceRandom];
			bolas[indiceRandom] = temporal;
		}

		// PASO 3: EMPEZAMOS DESDE LA PRIMERA BOLA
		indiceActual = 0;
	}

	/* RETURN TRUE SI QUEDAN BOLAS POR SACAR */
	public boolean quedanBolas() {
		return indiceActual < TOTAL_BOLAS;
	}

	/* ENTREGA LA SIGUIENTE BOLA DEL BOMBO. */
	public int sacarBola() {
		if (!quedanBolas()) {
			return -1;
		}
		// DEVUELVE LA BOLA ACTUAL Y AVANZA EL INDICE.
		return bolas[indiceActual++];
	}

	/* RETURN DE CUANTAS BOLAS FALTAN POR SALIR */
	public int getBolasRestantes() {
		return TOTAL_BOLAS - indiceActual;
	}
}
