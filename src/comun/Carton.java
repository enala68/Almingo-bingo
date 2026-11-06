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
 *   - matriz[fila][columna]    : NÚMERO QUE HAY EN ESA CASILLA (0 = HUECO).
 *   - tachados[fila][columna]  : TRUE SI ESA CASILLA YA ESTÁ MARCADA.
 *   - olvidados[fila][columna] : TRUE SI EL NÚMERO SALIÓ Y EL JUGADOR NO LO MARCÓ (Y NO SE LE PERDONÓ).
 *
 * MECÁNICA S/N: EL JUGADOR NO TACHA A MANO. CADA VEZ QUE SALE UNA BOLA
 * CONTESTA S O N Y EL MÉTODO responder() DECIDE QUÉ PASA (VER MÁS ABAJO).
 *
 * LA CLASE Carton GUARDA LOS TACHADOS Y COMPRUEBA LÍNEA Y BINGO. ASÍ:
 *   - EL JUGADOR SABE CUÁNDO CANTAR.
 *   - EL HOST PUEDE REPETIR LAS RESPUESTAS SOBRE LA COPIA DEL CARTÓN
 *     Y COMPROBAR QUE LA RECLAMACIÓN ES REAL, USANDO EL MISMO CÓDIGO.
 */
public class Carton {

	public static final int FILAS = 3;
	public static final int COLUMNAS = 9;

	/* NÚMEROS DE CADA FILA (EL RESTO SON HUECOS). */
	public static final int NUMS_POR_FILA = 5;

	// ---------------------------------------------------------------
	// RESULTADOS POSIBLES DE responder()
	// ---------------------------------------------------------------

	/* LA BOLA ESTABA Y EL JUGADOR DIJO S: SE TACHA. */
	public static final int RESULTADO_TACHADO = 1;

	/* LA BOLA NO ESTABA Y EL JUGADOR DIJO N: CORRECTO, NO PASA NADA. */
	public static final int RESULTADO_CORRECTO_NO = 2;

	/* LA BOLA ESTABA Y DIJO N, PERO TENÍA UN PERDÓN: SE TACHA SOLA. */
	public static final int RESULTADO_OLVIDO_PERDONADO = 3;

	/* LA BOLA ESTABA Y DIJO N SIN PERDÓN: NO SE TACHA, SE PIERDE ESA FILA. */
	public static final int RESULTADO_OLVIDO = 4;

	/* LA BOLA NO ESTABA Y DIJO S: MULTA. */
	public static final int RESULTADO_FALSA_ALARMA = 5;

	// GENERADOR ALEATORIO
	private static final Random random = new Random();

	// NÚMEROS DEL CARTÓN (0 = HUECO)
	private final int[][] matriz = new int[FILAS][COLUMNAS];

	// CASILLAS YA TACHADAS (POR DEFECTO ESTÁN EN FALSE)
	private final boolean[][] tachados = new boolean[FILAS][COLUMNAS];

	// NÚMEROS OLVIDADOS SIN PERDÓN (POR DEFECTO EN FALSE)
	private final boolean[][] olvidados = new boolean[FILAS][COLUMNAS];

	// CUÁNTOS PERDONES DE OLVIDO SE HAN GASTADO YA
	private int perdonesUsados = 0;

	// CUÁNTAS VECES HA DICHO S CUANDO LA BOLA NO ESTABA
	private int falsasAlarmas = 0;

	/* CREA CARTÓN VACÍO (TODO 0, SIN TACHADOS). */
	public Carton() {
	}

	// =================================================================
	// GENERACIÓN DE UN CARTÓN ALEATORIO
	// =================================================================

	/*
	 * GENERA CARTÓN NUEVO EN 2 FASES:
	 *   1) DECIDIR DÓNDE VAN LOS NÚMEROS.
	 *   2) DECIDIR QUÉ NÚMEROS VAN EN CADA COLUMNA.
	 */
	public static Carton generarCarton() {
		Carton carton = new Carton();

		// FASE 1: MÁSCARA. ocupada[f][c] = true SIGNIFICA "AQUÍ VA UN NÚMERO".
		boolean[][] ocupada = generarMascara();

		// FASE 2: PARA CADA COLUMNA SE GENERAN TANTOS NÚMEROS COMO CASILLAS OCUPADAS
		// TENGA Y SE COLOCAN DE ARRIBA A ABAJO EN ESAS CASILLAS.
		for (int col = 0; col < COLUMNAS; col++) {
			int cantidad = contarOcupadasEnColumna(ocupada, col);
			int[] numeros = generarNumerosColumna(col, cantidad);

			int siguiente = 0; // ÍNDICE DEL PRÓXIMO NÚMERO A COLOCAR
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
	 * DECIDE EN QUÉ CASILLAS HABRÁ NÚMERO Y EN CUÁLES HUECO.
	 *
	 * GARANTIZA:
	 *   - 5 NÚMEROS POR FILA,
	 *   - AL MENOS UN NÚMERO POR COLUMNA,
	 *   - COMO MÁXIMO 3 POR COLUMNA (SOLO HAY 3 FILAS, ES AUTOMÁTICO).
	 */
	private static boolean[][] generarMascara() {
		boolean[][] ocupada = new boolean[FILAS][COLUMNAS];
		int[] numsEnFila = new int[FILAS];

		// PASO 1: ASEGURAR 1 NÚMERO EN CADA COLUMNA.
		// PARA CADA COLUMNA SE ELIGE UNA FILA AL AZAR QUE AÚN TENGA SITIO,
		// ASÍ NINGUNA COLUMNA QUEDA VACÍA.
		for (int col = 0; col < COLUMNAS; col++) {
			int fil;
			do {
				fil = random.nextInt(FILAS);
			} while (numsEnFila[fil] >= NUMS_POR_FILA);

			ocupada[fil][col] = true;
			numsEnFila[fil]++;
		}

		// PASO 2: COMPLETAR CADA FILA HASTA TENER 5 NÚMEROS.
		// SE ELIGEN COLUMNAS AL AZAR Y SE OCUPAN SOLO SI ESA CASILLA TODAVÍA ESTÁ LIBRE EN ESA FILA.
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

	/* CUENTA CUÁNTAS CASILLAS OCUPADAS HAY EN UNA COLUMNA DE LA MÁSCARA. */
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
	 * PRIMER NÚMERO PERMITIDO EN UNA COLUMNA.
	 * COLUMNA 0 -> 1; RESTO -> col*10 (10, 20, 30...).
	 */
	private static int minimoColumna(int col) {
		return (col == 0) ? 1 : col * 10;
	}

	/*
	 * ÚLTIMO NÚMERO PERMITIDO EN UNA COLUMNA.
	 * COLUMNA 8 -> 90 (LA ÚLTIMA COLUMNA TIENE 11 NÚMEROS: 80..90);
	 * RESTO -> col*10+9 (9, 19, 29...).
	 */
	private static int maximoColumna(int col) {
		return (col == 8) ? 90 : col * 10 + 9;
	}

	/* GENERA NÚMEROS ALEATORIOS, DISTINTOS Y ORDENADOS, DENTRO DEL RANGO DE LA COLUMNA. */
	private static int[] generarNumerosColumna(int col, int cantidad) {
		int min = minimoColumna(col);
		int max = maximoColumna(col);
		int[] numeros = new int[cantidad];

		for (int i = 0; i < cantidad; i++) {
			int valor;
			// SE REPITE EL SORTEO MIENTRAS SALGA UN NÚMERO QUE YA TENGAMOS
			do {
				valor = min + random.nextInt(max - min + 1); // ENTRE MIN Y MAX
			} while (contiene(numeros, i, valor));

			numeros[i] = valor;
		}

		// ORDENA DE MENOR A MAYOR (QUEDARÁN DE ARRIBA A ABAJO)
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
	// JUEGO: RESPONDER S/N, LÍNEA Y BINGO
	// =================================================================

	/*
	 * APLICA LA RESPUESTA DEL JUGADOR A UNA BOLA. ES EL CORAZÓN DE LA MECÁNICA S/N.
	 *
	 *   ¿ESTABA LA BOLA?   RESPUESTA   QUÉ PASA
	 *   ----------------   ---------   ------------------------------------------
	 *   SÍ                 S           SE TACHA                    (RESULTADO_TACHADO)
	 *   NO                 N           NADA, SIGUE                 (RESULTADO_CORRECTO_NO)
	 *   SÍ                 N           OLVIDO: SI QUEDA PERDÓN SE TACHA SOLA
	 *                                  (RESULTADO_OLVIDO_PERDONADO); SI NO, NO SE
	 *                                  TACHA Y SE PIERDE ESA FILA (RESULTADO_OLVIDO)
	 *   NO                 S           FALSA ALARMA: MULTA         (RESULTADO_FALSA_ALARMA)
	 *
	 * POR QUÉ UN OLVIDO "PIERDE LA FILA": EL NÚMERO OLVIDADO SE QUEDA SIN TACHAR
	 * PARA SIEMPRE, ASÍ QUE ESA FILA NUNCA ESTARÁ COMPLETA (NO HAY LÍNEA POR ELLA)
	 * Y EL CARTÓN NUNCA ESTARÁ ENTERO (NO HAY BINGO). NO HACE FALTA CÓDIGO EXTRA.
	 *
	 * @param bola   NÚMERO DE LA BOLA QUE HA SALIDO
	 * @param dijoSi TRUE SI EL JUGADOR CONTESTÓ S; FALSE SI CONTESTÓ N O NO CONTESTÓ
	 * @return UNA DE LAS CONSTANTES RESULTADO_*
	 */
	public int responder(int bola, boolean dijoSi) {
		// 1. BUSCAR LA BOLA EN EL CARTÓN. SI NO APARECE, fil Y col SE QUEDAN EN -1.
		int fil = -1;
		int col = -1;

		if (bola >= 1) { // EL 0 ES UN HUECO, NO UNA BOLA
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

		// 2. DECIDIR SEGÚN ESTÉ O NO ESTÉ, Y SEGÚN LO QUE CONTESTÓ
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

		// 3. SOLO QUEDA: ESTABA EN EL CARTÓN Y DIJO N (OLVIDO)
		if (perdonesUsados < Reglas.OLVIDOS_PERDONADOS) {
			perdonesUsados++;
			tachados[fil][col] = true; // PERDONADO: SE TACHA SOLO
			return RESULTADO_OLVIDO_PERDONADO;
		}

		olvidados[fil][col] = true; // SIN PERDÓN: SE QUEDA SIN TACHAR
		return RESULTADO_OLVIDO;
	}

	/* COMPRUEBA SI UNA FILA ESTÁ COMPLETAMENTE TACHADA (LOS HUECOS SE IGNORAN). */
	public boolean filaCompleta(int fil) {
		for (int col = 0; col < COLUMNAS; col++) {
			// SI HAY UN NÚMERO SIN TACHAR -> FILA INCOMPLETA
			if (matriz[fil][col] != 0 && !tachados[fil][col]) {
				return false;
			}
		}
		return true;
	}

	/* DEVUELVE TRUE SI AL MENOS UNA FILA ESTÁ COMPLETA (HAY LÍNEA). */
	public boolean tieneLinea() {
		for (int fil = 0; fil < FILAS; fil++) {
			if (filaCompleta(fil)) {
				return true;
			}
		}
		return false;
	}

	/* DEVUELVE TRUE SI LAS 3 FILAS ESTÁN COMPLETAS (HAY BINGO). */
	public boolean tieneBingo() {
		for (int fil = 0; fil < FILAS; fil++) {
			if (!filaCompleta(fil)) {
				return false;
			}
		}
		return true;
	}

	/*
	 * DEVUELVE TRUE SI TODAVÍA SE PUEDE COMPLETAR EL BINGO, ES DECIR, SI NO
	 * HAY NINGÚN NÚMERO OLVIDADO. SIRVE PARA AVISAR AL JUGADOR.
	 */
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

	// =================================================================
	// GETTERS
	// =================================================================

	/** OBTIENE LA MATRIZ DE NÚMEROS (0 = HUECO). */
	public int[][] getMatriz() {
		return matriz;
	}

	/** OBTIENE LA MATRIZ DE CASILLAS TACHADAS. */
	public boolean[][] getTachados() {
		return tachados;
	}

	/** CUÁNTOS PERDONES DE OLVIDO SE HAN GASTADO. */
	public int getPerdonesUsados() {
		return perdonesUsados;
	}

	/** CUÁNTAS FALSAS ALARMAS (S SIN TENER LA BOLA) LLEVA EL JUGADOR. */
	public int getFalsasAlarmas() {
		return falsasAlarmas;
	}

	// =================================================================
	// CONVERSIÓN A FICHERO / DESDE TEXTO
	// =================================================================

	/*
	 * CONVIERTE EL CARTÓN EN 3 LÍNEAS DE TEXTO CON LOS NÚMEROS SEPARADOS POR ESPACIOS.
	 * LOS TACHADOS NO SE GUARDAN: SE RECALCULAN REPITIENDO LAS RESPUESTAS DEL JUGADOR.
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

	/* RECONSTRUYE UN CARTÓN A PARTIR DE 3 LÍNEAS (OPERACIÓN INVERSA DE aLineas). */
	public static Carton deLineas(String[] lineas) {
		if (lineas == null || lineas.length < FILAS) {
			return null;
		}

		Carton carton = new Carton();
		for (int fil = 0; fil < FILAS; fil++) {
			// "\\s+" = UNO O MÁS ESPACIOS; trim() QUITA LOS ESPACIOS SOBRANTES DE LOS EXTREMOS
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
	 * DIBUJA EL CARTÓN POR CONSOLA:
	 *   " -- " = HUECO VACÍO
	 *   " XX " = NÚMERO TACHADO
	 *   " !! " = NÚMERO OLVIDADO (SALIÓ Y NO SE MARCÓ: ESA FILA YA NO PUEDE DAR LÍNEA)
	 *   " 07 " = NÚMERO PENDIENTE DE SALIR
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
