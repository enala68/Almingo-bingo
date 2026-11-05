package jugador;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;
import comun.Carton;

public class JugadorMain {

	private static final String RUTA_ESTADO = "host/estado.txt";
	private static final String RUTA_BOLAS = "host/bolas.txt";
	private static final String DIRECTORIO_JUGADORES = "jugadores/";

	public static void main(String[] args) {
		System.out.println("=== BIENVENIDO AL BINGO DE ALMINGO ===");
		
		// 1. OBTENER NOMBRE DE USUARIO
		String nombreUsuario = pedirNombreUsuario();
		
		// 2. RECUPERAR O CREAR NUEVO CARTON
		Carton miCarton = obtenerOCrearCarton(nombreUsuario);
		
		// 3. INICIALIZAR LA MATRIZ DE TACHADOS Y MOSTRAR EL CARTÓN
		boolean[][] tachados = new boolean[3][9];
		mostrarCartonOficial(miCarton, tachados);
		
		// Próximamente aquí añadiremos el bucle de escucha del Host...
	}

	// PEDIR NOMBRE AL USUARIO
	private static String pedirNombreUsuario() {
		Scanner scTeclado = new Scanner(System.in);
		System.out.print("Introduce tu nombre de usuario: ");
		return scTeclado.nextLine().trim();
	}

	// OBTIENE O GENERA UN NUEVO CARTÓN
	private static Carton obtenerOCrearCarton(String nombreUsuario) {
		String rutaMiCarton = DIRECTORIO_JUGADORES + nombreUsuario + ".txt";
		File carpetaJugadores = new File(DIRECTORIO_JUGADORES);
		
		// VERIFICAR Y CREAR DIRECTORIO 'jugadores' SI NO EXISTE
		if (!carpetaJugadores.exists()) {
			carpetaJugadores.mkdir();
		}
		
		File ficheroCarton = new File(rutaMiCarton);
		Carton carton = null;
		
		if (ficheroCarton.exists()) {
			// CARGAR CARTÓN EXISTENTE
			System.out.println("Bienvenid@ de nuevo, " + nombreUsuario + "! Hemos recuperado tu cartón guardado.");
			carton = new Carton();
			int[][] matrizCarton = carton.getMatriz();
			
			try (Scanner lectorCarton = new Scanner(ficheroCarton)) {
				for (int fila = 0; fila < Carton.FILAS; fila++) {
					for (int columna = 0; columna < Carton.COLUMNAS; columna++) {
						if (lectorCarton.hasNextInt()) {
							matrizCarton[fila][columna] = lectorCarton.nextInt();
						}
					}
				}
			} catch (Exception e) {
				System.err.println("Error al leer el cartón guardado: " + e.getMessage());
				carton = Carton.generarCarton(); // Plan B si falla la lectura
			}
			
		} else {
			// GENERAR NUEVO CARTÓN Y GUARDARLO
			System.out.println("¡Bienvenido, " + nombreUsuario + "! Generando tu nuevo cartón...");
			carton = Carton.generarCarton();
			
			try (PrintWriter escritor = new PrintWriter(ficheroCarton)) {
				int[][] matrizCarton = carton.getMatriz();
				for (int fila = 0; fila < Carton.FILAS; fila++) {
					for (int columna = 0; columna < Carton.COLUMNAS; columna++) {
						escritor.print(matrizCarton[fila][columna] + " ");
					}
					escritor.println();
				}
				System.out.println("[INFO] Cartón guardado correctamente en " + rutaMiCarton);
			} catch (Exception e) {
				System.err.println("Error al guardar el cartón: " + e.getMessage());
			}
		}
		
		return carton;
	}

	// MOSTRAR CARTON
	private static void mostrarCartonOficial(Carton carton, boolean[][] tachados) {
		System.out.println("\nTu cartón:");
		carton.mostrarCarton(tachados);
	}
}