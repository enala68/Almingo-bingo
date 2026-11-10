package comun;

import java.util.Arrays;
import java.util.Random;

/* CARTÓN DE BINGO EUROPEO */
public class Carton {

	public static final int FILAS = 3;
	public static final int COLUMNAS = 9;

	/* NÚMEROS POR FILA */
	public static final int NUMS_POR_FILA = 5;

	/* RESULTADO TACHADO */
	public static final int RESULTADO_TACHADO = 1;

	/* RESULTADO CORRECTO NO */
	public static final int RESULTADO_CORRECTO_NO = 2;

	/* RESULTADO OLVIDO PERDONADO */
	public static final int RESULTADO_OLVIDO_PERDONADO = 3;

	/* RESULTADO OLVIDO */
	public static final int RESULTADO_OLVIDO = 4;

	/* RESULTADO FALSA ALARMA */
	public static final int RESULTADO_FALSA_ALARMA = 5;

	private static final Random random = new Random();

	private final int[][] matriz = new int[FILAS][COLUMNAS];

	private final boolean[][] tachados = new boolean[FILAS][COLUMNAS];

	private final boolean[][] olvidados = new boolean[FILAS][COLUMNAS];

	private int perdonesUsados = 0;

	private int falsasAlarmas = 0;

	/* CREAR CARTÓN VACÍO */
	public Carton() {
	}

	/* GENERAR CARTÓN NUEVO */
	public static Carton generarCarton() {
		Carton carton = new Carton();

		boolean[][] ocupada = generarMascara();

		for (int col = 0; col < COLUMNAS; col++) {
			int cantidad = contarOcupadasEnColumna(ocupada, col);
			int[] numeros = generarNumerosColumna(col, cantidad);

			int siguiente = 0; 
			for (int fil = 0; fil < FILAS; fil++) {
				if (ocupada[fil][col]) {
					carton.matriz[fil][col] = numeros[siguiente];
					siguiente++;
				}
			}
		}

		return carton;
	}

	/* GENERAR MÁSCARA DE HUECOS */
	private static boolean[][] generarMascara() {
		boolean[][] ocupada = new boolean[FILAS][COLUMNAS];
		int[] numsEnFila = new int[FILAS];

		for (int col = 0; col < COLUMNAS; col++) {
			int fil;
			do {
				fil = random.nextInt(FILAS);
			} while (numsEnFila[fil] >= NUMS_POR_FILA);

			ocupada[fil][col] = true;
			numsEnFila[fil]++;
		}

		for (int fil = 0; fil < FILAS; fil++) {
			while (numsEnFila[fil] < NUMS_POR_FILA) {
				int col = random.nextInt(COLUMNAS);
				if (!ocupada[fil][col]) {
					ocupada[fil][col] = true;
					numsEnFila[fil]++;
				}
			}
		}

		return ocupada;
	}

	/* CONTAR OCUPADAS EN COLUMNA */
	private static int contarOcupadasEnColumna(boolean[][] ocupada, int col) {
		int total = 0;
		for (int fil = 0; fil < FILAS; fil++) {
			if (ocupada[fil][col]) {
				total++;
			}
		}
		return total;
	}

	/* MÍNIMO COLUMNA */
	private static int minimoColumna(int col) {
		return (col == 0) ? 1 : col * 10;
	}

	/* MÁXIMO COLUMNA */
	private static int maximoColumna(int col) {
		return (col == 8) ? 90 : col * 10 + 9;
	}

	/* GENERAR NÚMEROS DE COLUMNA */
	private static int[] generarNumerosColumna(int col, int cantidad) {
		int min = minimoColumna(col);
		int max = maximoColumna(col);
		int[] numeros = new int[cantidad];

		for (int i = 0; i < cantidad; i++) {
			int valor;
			do {
				valor = min + random.nextInt(max - min + 1); 
			} while (contiene(numeros, i, valor));

			numeros[i] = valor;
		}

		Arrays.sort(numeros);
		return numeros;
	}

	/* COMPROBAR SI CONTIENE VALOR */
	private static boolean contiene(int[] array, int hasta, int valor) {
		for (int i = 0; i < hasta; i++) {
			if (array[i] == valor) {
				return true;
			}
		}
		return false;
	}

	/* APLICAR RESPUESTA S/N A UNA BOLA */
	public int responder(int bola, boolean dijoSi) {
		int fil = -1;
		int col = -1;

		if (bola >= 1) { 
			for (int f = 0; f < FILAS; f++) {
				for (int c = 0; c < COLUMNAS; c++) {
					if (matriz[f][c] == bola) {
						fil = f;
						col = c;
					}
				}
			}
		}
		boolean estabaEnCarton = (fil != -1);

		if (estabaEnCarton && dijoSi) {
			tachados[fil][col] = true;
			return RESULTADO_TACHADO;
		}

		if (!estabaEnCarton && !dijoSi) {
			return RESULTADO_CORRECTO_NO;
		}

		if (!estabaEnCarton && dijoSi) {
			falsasAlarmas++;
			return RESULTADO_FALSA_ALARMA;
		}

		if (perdonesUsados < Reglas.OLVIDOS_PERDONADOS) {
			perdonesUsados++;
			tachados[fil][col] = true; 
			return RESULTADO_OLVIDO_PERDONADO;
		}

		olvidados[fil][col] = true; 
		return RESULTADO_OLVIDO;
	}

	/* COMPROBAR SI FILA ESTÁ COMPLETA */
	public boolean filaCompleta(int fil) {
		for (int col = 0; col < COLUMNAS; col++) {
			if (matriz[fil][col] != 0 && !tachados[fil][col]) {
				return false;
			}
		}
		return true;
	}

	/* COMPROBAR SI HAY LÍNEA */
	public boolean tieneLinea() {
		for (int fil = 0; fil < FILAS; fil++) {
			if (filaCompleta(fil)) {
				return true;
			}
		}
		return false;
	}

	/* COMPROBAR SI HAY BINGO */
	public boolean tieneBingo() {
		for (int fil = 0; fil < FILAS; fil++) {
			if (!filaCompleta(fil)) {
				return false;
			}
		}
		return true;
	}

	/* COMPROBAR SI BINGO ES POSIBLE */
	public boolean bingoPosible() {
		for (int fil = 0; fil < FILAS; fil++) {
			for (int col = 0; col < COLUMNAS; col++) {
				if (olvidados[fil][col]) {
					return false;
				}
			}
		}
		return true;
	}

	/* OBTENER MATRIZ */
	public int[][] getMatriz() {
		return matriz;
	}

	/* OBTENER TACHADOS */
	public boolean[][] getTachados() {
		return tachados;
	}

	/* OBTENER PERDONES USADOS */
	public int getPerdonesUsados() {
		return perdonesUsados;
	}

	/* OBTENER FALSAS ALARMAS */
	public int getFalsasAlarmas() {
		return falsasAlarmas;
	}

	/* CONVERTIR CARTÓN A LÍNEAS */
	public String[] aLineas() {
		String[] lineas = new String[FILAS];
		for (int fil = 0; fil < FILAS; fil++) {
			String linea = "";
			for (int col = 0; col < COLUMNAS; col++) {
				linea = linea + matriz[fil][col];
				if (col < COLUMNAS - 1) {
					linea = linea + " ";
				}
			}
			lineas[fil] = linea;
		}
		return lineas;
	}

	/* RECONSTRUIR CARTÓN DESDE LÍNEAS */
	public static Carton deLineas(String[] lineas) {
		if (lineas == null || lineas.length < FILAS) {
			return null;
		}

		Carton carton = new Carton();
		for (int fil = 0; fil < FILAS; fil++) {
			String[] partes = lineas[fil].trim().split("\\s+");

			if (partes.length != COLUMNAS) {
				return null;
			}
			for (int col = 0; col < COLUMNAS; col++) {
				try {
					carton.matriz[fil][col] = Integer.parseInt(partes[col]);
				} catch (NumberFormatException e) {
					return null;
				}
			}
		}
		return carton;
	}

	/* MOSTRAR CARTÓN POR CONSOLA */
	public void mostrarCarton() {
		String separador = "+----+----+----+----+----+----+----+----+----+";

		System.out.println(separador);
		for (int fil = 0; fil < FILAS; fil++) {
			System.out.print("|");
			for (int col = 0; col < COLUMNAS; col++) {
				int num = matriz[fil][col];

				if (num == 0) {
					System.out.print(" -- |");
				} else if (tachados[fil][col]) {
					System.out.print(" XX |");
				} else if (olvidados[fil][col]) {
					System.out.print(" !! |");
				} else if (num < 10) {
					System.out.print(" 0" + num + " |");
				} else {
					System.out.print(" " + num + " |");
				}
			}
			System.out.println();
			System.out.println(separador);
		}
	}
}