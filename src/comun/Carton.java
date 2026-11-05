package comun;

import java.util.Arrays;
import java.util.Random;

/**
 * CARTÓN DE BINGO EUROPEO (90 BOLAS)
 * ---------------------------------------------------------------------
 * Reglas del cartón:
 *   - 3 filas x 9 columnas.
 *   - Cada fila tiene exactamente 5 números y 4 huecos (15 números en total).
 *   - Cada columna tiene entre 1 y 3 números.
 *   - Rangos por columna:  col 0 -> 1-9 | col 1 -> 10-19 | ... |
 *                          col 7 -> 70-79 | col 8 -> 80-90
 *   - Dentro de una columna, los números van ordenados de menor a mayor
 *     de arriba hacia abajo, y no se repiten.
 *
 * REPRESENTACIÓN INTERNA:
 *   - matriz[fila][columna]  : el número que hay en esa casilla (0 = hueco).
 *   - tachados[fila][columna]: true si esa casilla ya está marcada.
 *
 * Esta clase guarda TAMBIÉN los números tachados y sabe comprobar si hay
 * línea o bingo. Así tanto el Jugador (para saber cuándo cantar) como el
 * Host (para validar la reclamación) usan exactamente el mismo código.
 */
public class Carton {

	public static final int FILAS = 3;
	public static final int COLUMNAS = 9;

	/** Números que debe tener cada fila (el resto son huecos). */
	public static final int NUMS_POR_FILA = 5;

	// Generador aleatorio compartido por todos los cartones
	private static final Random random = new Random();

	// Números del cartón (0 = hueco)
	private final int[][] matriz = new int[FILAS][COLUMNAS];

	// Casillas ya tachadas (por defecto, todas false)
	private final boolean[][] tachados = new boolean[FILAS][COLUMNAS];

	/**
	 * Crea un cartón vacío (todo ceros, nada tachado).
	 * Java inicializa los arrays int a 0 y los boolean a false por sí solo,
	 * así que no hace falta recorrerlos para rellenarlos.
	 */
	public Carton() {
	}

	// =================================================================
	// GENERACIÓN DE UN CARTÓN ALEATORIO
	// =================================================================

	/**
	 * Genera un cartón nuevo y válido siguiendo las reglas del bingo europeo.
	 *
	 * Se hace en dos fases:
	 *   1) Decidir DÓNDE van los números (la "máscara" de huecos).
	 *   2) Decidir QUÉ números van en cada columna.
	 *
	 * @return un cartón nuevo listo para jugar
	 */
	public static Carton generarCarton() {
		Carton carton = new Carton();

		// FASE 1: máscara. ocupada[f][c] = true significa "aquí va un número"
		boolean[][] ocupada = generarMascara();

		// FASE 2: para cada columna, generar tantos números como casillas
		// ocupadas tenga, y colocarlos de arriba abajo en las casillas ocupadas
		for (int col = 0; col < COLUMNAS; col++) {
			int cantidad = contarOcupadasEnColumna(ocupada, col);
			int[] numeros = generarNumerosColumna(col, cantidad);

			int siguiente = 0; // índice del próximo número a colocar
			for (int fil = 0; fil < FILAS; fil++) {
				if (ocupada[fil][col]) {
					carton.matriz[fil][col] = numeros[siguiente];
					siguiente++;
				}
			}
		}

		return carton;
	}

	/**
	 * Decide en qué casillas habrá número y en cuáles habrá hueco.
	 *
	 * Garantiza:
	 *   - exactamente 5 números por fila,
	 *   - al menos 1 número por columna (como en un cartón real),
	 *   - como máximo 3 por columna (solo hay 3 filas, es automático).
	 *
	 * @return matriz de booleanos: true = número, false = hueco
	 */
	private static boolean[][] generarMascara() {
		boolean[][] ocupada = new boolean[FILAS][COLUMNAS];
		int[] numsEnFila = new int[FILAS]; // cuántos números lleva cada fila

		// PASO 1: asegurar UN número en cada columna.
		// Para cada columna se elige una fila al azar que aún tenga sitio
		// (menos de 5 números). Con esto ninguna columna queda vacía.
		for (int col = 0; col < COLUMNAS; col++) {
			int fil;
			do {
				fil = random.nextInt(FILAS);
			} while (numsEnFila[fil] >= NUMS_POR_FILA);

			ocupada[fil][col] = true;
			numsEnFila[fil]++;
		}

		// PASO 2: completar cada fila hasta tener 5 números.
		// Se eligen columnas al azar y se ocupan solo si esa casilla
		// todavía está libre en esta fila.
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

	/**
	 * Cuenta cuántas casillas ocupadas hay en una columna de la máscara.
	 *
	 * @param ocupada máscara de huecos
	 * @param col     columna a revisar
	 * @return número de casillas ocupadas (0 a 3)
	 */
	private static int contarOcupadasEnColumna(boolean[][] ocupada, int col) {
		int total = 0;
		for (int fil = 0; fil < FILAS; fil++) {
			if (ocupada[fil][col]) {
				total++;
			}
		}
		return total;
	}

	/**
	 * Primer número permitido en una columna.
	 * Columna 0 -> 1 (no existe el 0); resto -> col*10 (10, 20, 30...).
	 */
	private static int minimoColumna(int col) {
		return (col == 0) ? 1 : col * 10;
	}

	/**
	 * Último número permitido en una columna.
	 * Columna 8 -> 90 (la última columna tiene 11 números: 80..90);
	 * resto -> col*10+9 (9, 19, 29...).
	 */
	private static int maximoColumna(int col) {
		return (col == 8) ? 90 : col * 10 + 9;
	}

	/**
	 * Genera números aleatorios, distintos entre sí y ordenados de menor a
	 * mayor, dentro del rango de una columna.
	 *
	 * @param col      columna para la que se generan (define el rango)
	 * @param cantidad cuántos números hacen falta (1 a 3)
	 * @return array ordenado con los números generados
	 */
	private static int[] generarNumerosColumna(int col, int cantidad) {
		int min = minimoColumna(col);
		int max = maximoColumna(col);
		int[] numeros = new int[cantidad];

		for (int i = 0; i < cantidad; i++) {
			int valor;
			// Se repite el sorteo mientras salga un número que ya tenemos
			do {
				valor = min + random.nextInt(max - min + 1); // entre min y max
			} while (contiene(numeros, i, valor));

			numeros[i] = valor;
		}

		// Ordena de menor a mayor para que en el cartón queden de arriba a abajo
		Arrays.sort(numeros);
		return numeros;
	}

	/**
	 * Comprueba si un valor ya está en las primeras 'hasta' posiciones de un array.
	 * Solo miramos hasta 'hasta' porque las posiciones siguientes aún no se han
	 * rellenado (contienen 0).
	 *
	 * @param array array donde buscar
	 * @param hasta cuántas posiciones (desde la 0) hay que revisar
	 * @param valor valor que se busca
	 * @return true si el valor ya existe
	 */
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

	/**
	 * Intenta tachar una bola en el cartón.
	 *
	 * @param bola número de la bola extraída
	 * @return true si la bola estaba en el cartón y se ha tachado AHORA;
	 *         false si no estaba o ya estaba tachada (así se puede avisar
	 *         al jugador solo la primera vez)
	 */
	public boolean marcarBola(int bola) {
		// Los ceros son huecos, no bolas: ignorarlos evita tachar huecos
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

	/**
	 * Comprueba si una fila concreta está completa: todos sus NÚMEROS
	 * (los huecos se ignoran) están tachados.
	 *
	 * @param fil fila a comprobar (0 a 2)
	 * @return true si la fila está completa
	 */
	public boolean filaCompleta(int fil) {
		for (int col = 0; col < COLUMNAS; col++) {
			// Hay un número (no es hueco) y todavía no está tachado -> incompleta
			if (matriz[fil][col] != 0 && !tachados[fil][col]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return true si al menos UNA fila está completa (hay línea)
	 */
	public boolean tieneLinea() {
		for (int fil = 0; fil < FILAS; fil++) {
			if (filaCompleta(fil)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return true si las TRES filas están completas (hay bingo)
	 */
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

	/** @return la matriz de números (0 = hueco). */
	public int[][] getMatriz() {
		return matriz;
	}

	/** @return la matriz de casillas tachadas. */
	public boolean[][] getTachados() {
		return tachados;
	}

	// =================================================================
	// CONVERSIÓN A / DESDE TEXTO (para guardar en fichero)
	// =================================================================

	/**
	 * Convierte el cartón en 3 líneas de texto, una por fila, con los
	 * números separados por espacios. Ejemplo de una fila:
	 *     "0 12 0 31 0 0 63 74 85"
	 * Los números tachados NO se guardan: se recalculan con las bolas
	 * que ya salieron (host/bolas.txt).
	 *
	 * @return array de 3 líneas de texto
	 */
	public String[] aLineas() {
		String[] lineas = new String[FILAS];
		for (int fil = 0; fil < FILAS; fil++) {
			String linea = "";
			for (int col = 0; col < COLUMNAS; col++) {
				linea = linea + matriz[fil][col];
				if (col < COLUMNAS - 1) {
					linea = linea + " "; // separador entre números (no tras el último)
				}
			}
			lineas[fil] = linea;
		}
		return lineas;
	}

	/**
	 * Reconstruye un cartón a partir de 3 líneas de texto (inversa de aLineas).
	 *
	 * @param lineas líneas leídas del fichero
	 * @return el cartón, o null si el contenido no tiene el formato esperado
	 */
	public static Carton deLineas(String[] lineas) {
		if (lineas == null || lineas.length < FILAS) {
			return null;
		}

		Carton carton = new Carton();
		for (int fil = 0; fil < FILAS; fil++) {
			// "\\s+" = uno o más espacios; trim() quita espacios sobrantes de los extremos
			String[] partes = lineas[fil].trim().split("\\s+");

			if (partes.length != COLUMNAS) {
				return null; // fila con un número incorrecto de columnas
			}
			for (int col = 0; col < COLUMNAS; col++) {
				try {
					carton.matriz[fil][col] = Integer.parseInt(partes[col]);
				} catch (NumberFormatException e) {
					return null; // algo que no era un número
				}
			}
		}
		return carton;
	}

	// =================================================================
	// VISUALIZACIÓN
	// =================================================================

	/**
	 * Dibuja el cartón por consola. Convenciones:
	 *   " -- " = hueco vacío
	 *   " XX " = número ya tachado
	 *   " 07 " = número pendiente (con cero delante si es de una cifra)
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
