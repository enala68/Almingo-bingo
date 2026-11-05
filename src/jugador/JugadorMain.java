package jugador;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;
import comun.Carton;
import comun.Estado;

public class JugadorMain {

	// RUTA DEL ESTADO DEL HOST
	private static final String RUTA_ESTADO = "host/estado.txt";
	private static final String RUTA_BOLAS = "host/bolas.txt";
	private static final String DIRECTORIO_JUGADORES = "jugadores/";

	public static void main(String[] args) {
		Scanner scTeclado;
		String nombreUsuario;
		String rutaMiCarton;
		Carton miCarton;
		File carpetaJugadores;
		File ficheroCarton;
		boolean tachados[][];
		
		System.out.println("=== BIENVENIDO AL BINGO DE ALMINGO ===");
		
		// NOMBRE DE USUARIO
		scTeclado = new Scanner(System.in);
		System.out.print("Introduce tu nombre de usuario: ");
		nombreUsuario = scTeclado.nextLine().trim();
		
		// RUTA PERSONAL DE CARTÓN DEL JUGADOR
		rutaMiCarton = DIRECTORIO_JUGADORES + nombreUsuario + ".txt";
		miCarton = null;
		
		// VERIFICAR QUE EXISTE DIRECTORIO "jugadores"
		carpetaJugadores = new File(DIRECTORIO_JUGADORES);
		if (!carpetaJugadores.exists() ) {
			carpetaJugadores.mkdir();
		}
		// PRIMERA PARTIDA: SE CREA FICHERO
		ficheroCarton = new File(rutaMiCarton);
		
		if (ficheroCarton.exists()) {
			// SI YA JUGÓ ANTES: RECUPERAMOS SU CARTÓN DEL FICHERO
			System.out.println("Bienvenid@ de nuevo, " + nombreUsuario + "! Hemos recuperado tu cartón guardado.");
			
			miCarton = new Carton(); // Creamos una instancia vacía
			int[][] matrizCarton = miCarton.getMatriz();
			
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
				miCarton = Carton.generarCarton(); // Por seguridad si hubiera algún fallo
			}
			
		} else {
			// SI ES UN JUGADOR NUEVO
			System.out.println("¡Bienvenido, " + nombreUsuario + "! Generando tu nuevo cartón...");
			miCarton = Carton.generarCarton();
			
			// GUARDAR EL CARTÓN EN SU FICHERO PERSONAL
			try (PrintWriter escritor = new PrintWriter(ficheroCarton)) {
				int[][] matrizCarton = miCarton.getMatriz();
				
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
		
		// VALIDACION DE ACIERTOS
		tachados = new boolean[3][9];
		
		System.out.println("\nTu cartón oficial:");
        miCarton.mostrarCarton(tachados);
	}
}