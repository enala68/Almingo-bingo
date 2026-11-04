package comun;

import java.util.Random;

public class Bombo {

	private static final int TOTAL_BOLAS = 90;
	private final int[] BOLAS = new int[TOTAL_BOLAS];
	private final Random random = new Random();
	private int indiceActual;

	// CONSTRUCTOR
	public Bombo() {
		reiniciarBombo();
	}

	// RELLENAR Y MEZCLAR
	public void reiniciarBombo() {

		// RELLENAR
		for (int i = 0; i < TOTAL_BOLAS; i++) {
			BOLAS[i] = i + 1;
		}

		// BARAJAR
		for (int i = TOTAL_BOLAS - 1; i > 0; i--) {
			int indiceRandom = random.nextInt(i + 1);

			// SWAP
			int temp = BOLAS[i];
			BOLAS[i] = BOLAS[indiceRandom];
			BOLAS[indiceRandom] = temp;
		}

		// VOLVER AL INDICE 0
		indiceActual = 0;
	}

	// COMPRUEBA SI QUEDAN BOLAS POR SALIR
	public boolean quedanBolas() {
		return indiceActual < TOTAL_BOLAS;
	}

	// SACA LA SIGUIENTE BOLA
	public int sacarBola() {
		if (!quedanBolas()) {
			return -1;
		}
		return BOLAS[indiceActual++];
	}

	// BOLAS QUE FALTAN POR SALIR
	public int getBolasRestantes() {
		return TOTAL_BOLAS - indiceActual;
	}

}
