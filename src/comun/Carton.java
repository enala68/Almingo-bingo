package comun;

import java.util.concurrent.ThreadLocalRandom;

public class Carton {
	public static final int FILAS = 3;
	public static final int COLUMNAS = 9;

	private final int[][] matriz = new int[FILAS][COLUMNAS];

	// CONSTRUCTOR. INICIALIZA TODO A O
	public Carton() {
		for (int i = 0; i < FILAS; i++) {
			for (int j = 0; j < COLUMNAS; j++) {
				matriz[i][j] = 0;
			}
		}
	}


	public static Carton generarCarton() {

		Carton carton = new Carton();
		boolean[][] ocupada = new boolean[FILAS][COLUMNAS];

		// GARANTIZAR 5 NUM / FILA. 3 NUM / COLUMNA
		for (int fil = 0; fil < FILAS; fil++) {
			int numsEnFil = 0;
			while (numsEnFil < 5) {
				int col = ThreadLocalRandom.current().nextInt(COLUMNAS);
				if (!ocupada[fil][col]) {
					int numsEnCol = 0;
					for (int i = 0; i < FILAS; i++) {
						if (ocupada[i][col]) {
							numsEnCol++;
						}
					}
					if (numsEnCol < 3) {
						ocupada[fil][col] = true;
						numsEnFil++;
					}
				}
			}
		}

		// RELLENAR COLUMNAS
		for (int col = 0; col < COLUMNAS; col++) {
			int numsEnCol = 0;
			for (int fil = 0; fil < FILAS; fil++) {
				if (ocupada[fil][col]) {
					numsEnCol++;
				}
			}

			if (numsEnCol > 0) {
				// DEFINIR RANGOS
				int min;
				if (col == 0) {
					min = 1;
				} else {
					min = col * 10;
				}

				int max;
				if (col == 8) {
					max = 90;
				} else {
					max = col * 10 + 9;
				}

				// ARRAY TEMPORAL PARA NUMS DE ÉSTA COLUMNA
				int[] numsCol = new int[numsEnCol];

				// GENERACION DE NÚMEROS ÚNICOS
				for (int nums = 0; nums < numsEnCol; nums++) {
					boolean repetido;
					int valor;

					do {
						repetido = false;
						valor = ThreadLocalRandom.current().nextInt(min, max + 1);
						for (int i = 0; i < nums; i++) {
							if (numsCol[i] == valor) {
								repetido = true;
								break;
							}
						}
					} while (repetido); {
						numsCol[nums] = valor;
					}
				}

				// ORDENAR NÚMEROS
				java.util.Arrays.sort(numsCol);

				// RELLENAR CARTÓN
				int i = 0;
				for (int fil = 0; fil < FILAS; fil++) {
					if (ocupada[fil][col]) {
						carton.matriz[fil][col] = numsCol[i++];
					}
				}
			}
		}

		return carton;
	}

	// GETTER
	public int[][] getMatriz() {
		return matriz;
	}

	// MOSTRAR EL CARTÓN POR CONSOLA
	public void mostrarCarton(boolean[][] tachados) {
		System.out.println("+----+----+----+----+----+----+----+----+----+");
		for (int fil = 0; fil < 3; fil++) {
			System.out.print("|");
			for (int j = 0; j < 9; j++) {
				int num = matriz[fil][j];
				if (num == 0) {
					System.out.print(" -- |"); // Hueco vacío
				} else if (tachados != null && tachados[fil][j]) {
					System.out.print(" XX |"); // Número tachado porque ya salió la bola
				} else {
					if (num < 10) {
						System.out.print(" 0" + num + " |");
					} else {
						System.out.print(" " + num + " |");
					}
				}
			}
			System.out.println();
			System.out.println("+----+----+----+----+----+----+----+----+----+");
		}
	}
}
