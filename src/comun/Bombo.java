package comun;

import java.util.Random;

/* BOMBO DEL BINGO */
public class Bombo {

	/* TOTAL DE BOLAS */
	public static final int TOTAL_BOLAS = 90;

	private final int[] bolas = new int[TOTAL_BOLAS];

	private final Random random = new Random();

	private int indiceActual;

	/* CREAR BOMBO NUEVO */
	public Bombo() {
		reiniciarBombo();
	}

	/* REINICIAR Y BARAJAR BOMBO */
	public void reiniciarBombo() {
		for (int i = 0; i < TOTAL_BOLAS; i++) {
			bolas[i] = i + 1;
		}

		for (int i = TOTAL_BOLAS - 1; i > 0; i--) {
			int indiceRandom = random.nextInt(i + 1); 

			int temporal = bolas[i];
			bolas[i] = bolas[indiceRandom];
			bolas[indiceRandom] = temporal;
		}

		indiceActual = 0;
	}

	/* COMPROBAR SI QUEDAN BOLAS */
	public boolean quedanBolas() {
		return indiceActual < TOTAL_BOLAS;
	}

	/* SACAR SIGUIENTE BOLA */
	public int sacarBola() {
		if (!quedanBolas()) {
			return -1;
		}
		return bolas[indiceActual++];
	}

	/* OBTENER BOLAS RESTANTES */
	public int getBolasRestantes() {
		return TOTAL_BOLAS - indiceActual;
	}
}