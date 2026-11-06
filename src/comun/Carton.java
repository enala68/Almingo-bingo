package comun;

import java.util.Arrays;
import java.util.Random;

/*
 * CARTÓN DE BINGO EUROPEO (90 BOLAS)
 * ---------------------------------------------------------------------
 * REGLAS DEL CARTÓN:
 *   - 3 FILAS x 9 COLUMNAS.
 *   - CADA FILA TIENE 5 NÚMEROS Y 4 HUECOS (15 NÚMEROS EN TOTAL).
 *   - CADA COLUMNA TIENE ENTRE 1 Y 3 NÚMEROS.
 *   - RANGOS POR COLUMNA:  col 0 -> 1-9 | col 1 -> 10-19 | ... |
 *                          col 7 -> 70-79 | col 8 -> 80-90
 *   - EN CADA COLUMNA LOS NÚMEROS VAN ORDENADOS DE MENOR A MAYOR DE ARRIBA HACIA ABAJO SIN REPETIRSE.
 *
 * REPRESENTACIÓN INTERNA:
 *   - matriz[fila][columna]  : NÚMERO QUE HAY EN ESA CASILLA (0 = HUECO).
 *   - tachados[fila][columna]: TRUE SI ESA CASILLA YA ESTÁ MARCADA.
 *
 * CLASE Carton GUARDA NÚMEROS TACHADOS Y COMPRUEBA LINEA Y BINGO. ASÍ:
 * JUGADOR SABE CUANDO CANTAR
 * HOST VALIDA RECLAMACIÓN.
 */
public class Carton {

	public static final int FILAS = 3;
	public static final int COLUMNAS = 9;

	/* NÚMEROS DE CADA FILA (RESTO SON HUECOS). */
	public static final int NUMS_POR_FILA = 5;

	// GENERADOR ALEATORIO
	private static final Random random = new Random();

	// NÚMEROS DEL CARTÓN (0 = HUECO)
	private final int[][] matriz = new int[FILAS][COLUMNAS];

	// CASILLAS YA TACHADAS (POR DEFECTO ESTAN EN FALSE)
	private final boolean[][] tachados = new boolean[FILAS][COLUMNAS];

	/* CREA CARTÓN VACIO (TODO 0, SIN TACHADOS). */
	public Carton() {
	}

	// =================================================================
	// GENERACIÓN DE UN CARTÓN ALEATORIO
	// =================================================================

	/*
	 * GENERA CARTÓN NUEVO EN 2 FASES:
	 *   1) DECIDIR DÓNDE VAN LOS NÚMEROS.
	 *   2) DECIDIR QUE NÚMEROS VAN EN CADA COLUMNA.
	 */
	public static Carton generarCarton() {
		Carton carton = new Carton();

		// FASE 1: MÁSCARA. ocupada[f][c] = true SIGNIFICA "AQUÍ VA UN NÚMERO".
		boolean[][] ocupada = generarMascara();

		// FASE 2: PARA CADA COLUMNA GENERAR TÁNTOS NÚMEROS COMO CASILLAS OCUPADAS TENGA Y COLOCARLOS DE ARRIBA A ABAJO EN LAS CASILLAS OCUPADAS.
		for (int col = 0; col < COLUMNAS; col++) {
			int cantidad = contarOcupadasEnColumna(ocupada, col);
			int[] numeros = generarNumerosColumna(col, cantidad);

			int siguiente = 0; // INDICE DEL PRÓXIMO NÚMERO A COLOCAR
			for (int fil = 0; fil < FILAS; fil++) {
				if (ocupada[fil][col]) {
					carton.matriz[fil][col] = numeros[siguiente];
					siguiente++;
				}
			}
		}

		return carton;
	}

	/*
	 * DECIDE EN QUE CASILLAS HABRÁ NÚMERO Y EN CUÁLES HUECO.
	 *
	 * GARANTIZA:
	 *   - 5 NÚMEROS POR FILA,
	 *   - AL MENOS UN NÚMERO POR COLUMNA,
	 *   - COMO MÁXIMO 3 POR COLUMNA.
	 */
	private static boolean[][] generarMascara() {
		boolean[][] ocupada = new boolean[FILAS][COLUMNAS];
		int[] numsEnFila = new int[FILAS];

		// PASO 1: ASEGURAR 1 NÚMERO EN CADA COLUMNA.
		// PARA CADA COLUMNA SE ELIGE UNA FILA AL AZAR QUE AÚN TENGA SITIO PARA QUE NINGUNA COLUMNA QUEDE VACÍA.
		for (int col = 0; col < COLUMNAS; col++) {
			int fil;
			do {
				fil = random.nextInt(FILAS);
			} while (numsEnFila[fil] >= NUMS_POR_FILA);

			ocupada[fil][col] = true;
			numsEnFila[fil]++;
		}

		// PASO 2: COMPLETAR CADA FILA HASTA TENER 5 NÚMEROS
		// SE ELIGEN COLUMNAS AL AZAR Y SE OCUPAN SOLO SI ESA CASILLA TODAVÍA ESTÁ LIBRE EN ESA FILASa.
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

	/* CUENTA CUÁNTAS CASILLAS OCUPADAS HAY EN UNA COLUMNA DE LA MÁSCARA */
	private static int contarOcupadasEnColumna(boolean[][] ocupada, int col) {
		int total = 0;
		for (int fil = 0; fil < FILAS; fil++) {
			if (ocupada[fil][col]) {
				total++;
			}
		}
		return total;
	}

	/*
	 * PRIMER NÚMERO PERMITIDO EN UNA COLUMNA
	 * Columna 0 -> 1; resto -> col*10 (10, 20, 30...).
	 */
	private static int minimoColumna(int col) {
		return (col == 0) ? 1 : col * 10;
	}

	/*
	 * ÚLTIMO NÚMERO PERMITIDO EN UNA COLUMNA
	 * Columna 8 -> 90 (LA ÚLTIMA COLUMNA TIENE 11 NÚMEROS: 80..90);
	 * RESTO -> col*10+9 (9, 19, 29...).
	 */
	private static int maximoColumna(int col) {
		return (col == 8) ? 90 : col * 10 + 9;
	}

	/* GENERA NÚMEROS ALEATORIOS ORDENADOS DENTRO DEL RANGO DE LA COLUMNA. */
	private static int[] generarNumerosColumna(int col, int cantidad) {
		int min = minimoColumna(col);
		int max = maximoColumna(col);
		int[] numeros = new int[cantidad];

		for (int i = 0; i < cantidad; i++) {
			int valor;
			// SE REPITE EL RANDOM MIENTRAS SALGA UN NÚMERO QUE YA TENGAMOS
			do {
				valor = min + random.nextInt(max - min + 1); // ENTRE MIN Y MAX
			} while (contiene(numeros, i, valor));

			numeros[i] = valor;
		}

		// ORDENA DE ARRIBA A ABAJO
		Arrays.sort(numeros);
		return numeros;
	}

	/* COMPRUEBA SI UN VALOR ESTÁ EN LAS PRIMERAS 'hasta' POSICIONES DEL ARRAY. */
	private static boolean contiene(int[] array, int hasta, int valor) {
		for (int i = 0; i < hasta; i++) {
			if (array[i] == valor) {
				return true;
			}
		}
		return false;
	}

	// =================================================================
	// JUEGO: TACHAR, LÍNEA Y BINGO
	// =================================================================

	/* INTENTA TACHAR UNA BOLA EN EL CARTÓN. */
	public boolean marcarBola(int bola) {
		// LOS 0 SON HUECOS, NO BOLAS
		if (bola < 1) {
			return false;
		}

		for (int fil = 0; fil < FILAS; fil++) {
			for (int col = 0; col < COLUMNAS; col++) {
				if (matriz[fil][col] == bola && !tachados[fil][col]) {
					tachados[fil][col] = true;
					return true;
				}
			}
		}
		return false;
	}

	/* COMPRUEBA FILA COMPLETAMENTE TACHADA */
	public boolean filaCompleta(int fil) {
		for (int col = 0; col < COLUMNAS; col++) {
			// SI HAY UN NÚMERO SIN TACHAR-> INCOMPLETA
			if (matriz[fil][col] != 0 && !tachados[fil][col]) {
				return false;
			}
		}
		return true;
	}

	/* RETURN TRUE SI AL MENOS UNA LINEA ESTA COMPLETA (HAY LINEA) */
	public boolean tieneLinea() {
		for (int fil = 0; fil < FILAS; fil++) {
			if (filaCompleta(fil)) {
				return true;
			}
		}
		return false;
	}

	/* RETURN TRUE SI LAS 3 LINEAS ESTÁN COMPLETAS (HAY BINGO) */
	public boolean tieneBingo() {
		for (int fil = 0; fil < FILAS; fil++) {
			if (!filaCompleta(fil)) {
				return false;
			}
		}
		return true;
	}

	// =================================================================
	// GETTERS
	// =================================================================

	/** OBTIENE MATRIZ DE NÚMEROS (0 = HUECO). */
	public int[][] getMatriz() {
		return matriz;
	}

	/** OBTIENE MATRIZ DE CASILLAS TACHADAS */
	public boolean[][] getTachados() {
		return tachados;
	}

	// =================================================================
	// CONVERSIÓN A FICHERO / DESDE TEXTO
	// =================================================================

	/*
	 * CONVIERTE CARTÓN EN 3 LÍNEAS DE TEXTO CON LOS NÚMEROS SEPARADOS POR ESPACIOS.
	 * LOS NÚMEROS TACHADOS NO SE GUARDAN, SE RECALCULAN CON LAS BOLAS QUE VAN SALIENDO.
	 */
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

	/* RECONSTRUYE CARTÓN A PARTIR DE 3 LINEAS (DE aLineas). */
	public static Carton deLineas(String[] lineas) {
		if (lineas == null || lineas.length < FILAS) {
			return null;
		}

		Carton carton = new Carton();
		for (int fil = 0; fil < FILAS; fil++) {
			// "\\s+" = UNO O MÁS ESPACIOS; trim() QUITA ESPACIOS SOBRANTES DE ESTREMOS
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

	// =================================================================
	// VISUALIZACIÓN
	// =================================================================

	/*
	 * DIBUJA CARTÓN POR CONSOLA:
	 *   " -- " = HUECO VACIO
	 *   " XX " = NÚMERO TACHADO
	 *   " 07 " = NÚMERO PENDIENTE DE TACHAR
	 */
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
