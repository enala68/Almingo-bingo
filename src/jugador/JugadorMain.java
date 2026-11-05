package jugador;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;
import comun.Carton;
import comun.Estado;

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
		
		// 4. INICIAR EL BUCLE DE ESCUCHA DEL JUEGO
		iniciarBucleJuego(miCarton, tachados);
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
	
	// BUSCAR Y TACHAR LA BOLA EN EL CARTÓN
	private static void marcarBolaEnCarton(int bolaExtraida, Carton carton, boolean[][] tachados) {
		int[][] matrizCarton = carton.getMatriz();
		
		for (int fila = 0; fila < Carton.FILAS; fila++) {
			for (int columna = 0; columna < Carton.COLUMNAS; columna++) {
				if (matrizCarton[fila][columna] == bolaExtraida) {
					tachados[fila][columna] = true;
					System.out.println("✔ ¡Número " + bolaExtraida + " tachado en tu cartón!");
				}
			}
		}
	}
	
	// --- BUCLE DE ESCUCHA ACTIVA DEL CLIENTE ---
		private static void iniciarBucleJuego(Carton miCarton, boolean[][] tachados) {
			System.out.println("\n[ESPERANDO] Conectado al Host. Esperando extracción de bolas...");
			
			int ultimaVersionVista = 0;
			boolean juegoActivo = true;
			
			while (juegoActivo) {
				File ficheroEstado = new File(RUTA_ESTADO);
				
				if (ficheroEstado.exists()) {
					// ARRAY QUE ALMACENA LAS 4 LINEAS DEL ESTADO
					String[] lineasArray = new String[4];
					
					try (Scanner lectorEstado = new Scanner(ficheroEstado)) {
						int indice = 0;
						while (lectorEstado.hasNextLine() && indice < lineasArray.length) {
							lineasArray[indice] = lectorEstado.nextLine();
							indice++;
						}
						
						// RECONSTRUCCIÓN DEL ESTADO ACTUAL
						Estado estadoActual = Estado.deLineas(lineasArray);
						
						// CANCELACIÓN DE LA PARTIDA
						String faseActual = estadoActual.getFase();

						if (faseActual.equals("CANCELADA")) {
						    System.out.println("\n⚠️ [AVISO] La partida ha sido cancelada por el Host.");
						    juegoActivo = false; // FINALIZA EL PROGRAMA DEL JUGADOR
						    break;
						} else if (faseActual.equals("FINALIZADA")) {
						    System.out.println("\n🏁 [AVISO] La partida ha finalizado.");
						    juegoActivo = false;
						    break;
						}
						
						// COMPROBACIÓN DE NUEVA VERSION DE LA PARTIDA
						if (estadoActual.getVersion() > ultimaVersionVista) {
							ultimaVersionVista = estadoActual.getVersion();
							
							System.out.println("\n--------------------------------------------------");
							System.out.println("📢 [NUEVA BOLA] Partida #" + estadoActual.getIdPartida() + 
									           " | Fase: " + estadoActual.getFase() + 
									           " | ¡Ha salido el **" + estadoActual.getUltimaBola() + "**!");
							
							int bolaExtraida = estadoActual.getUltimaBola();
							
							if (bolaExtraida > 0) {
								// 1. TACHAR BOLA EN CARTÓN
								marcarBolaEnCarton(bolaExtraida, miCarton, tachados);
								
								// 2. MOSTRAR CARTÓN ACTUALIZADO (XX)
								miCarton.mostrarCarton(tachados);
								
								// 3. COMPROBAR SI HAY LINEA O BINGO
								if (comprobarLinea(miCarton, tachados)) {
									System.out.println("✨ ¡¡¡ HAS CANTADO LÍNEA !!! (Responde ésta pregunta de sostenibilidad...)");
								}
								
								if (comprobarBingo(miCarton, tachados)) {
									System.out.println("🏆 ¡¡¡ HAS CANTADO BINGO COMPLETO !!!");
									juegoActivo = false;
								}
							}
						}
						
					} catch (Exception e) {
						// Posible conflicto momentáneo si el Host está escribiendo el archivo en este instante
					}
				}
				
				try {
					// COMPROBAR CAMBIOS EN EL FICHERO CADA 2 SEG
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					System.out.println("Hilo interrumpido.");
					break;
				}
			}
		}
		
		// COMPROBAR SI HAY LÍNEA
		private static boolean comprobarLinea(Carton carton, boolean[][] tachados) {
			int[][] matrizCarton = carton.getMatriz();
			
			for (int fila = 0; fila < Carton.FILAS; fila++) {
				boolean filaCompleta = true;
				for (int columna = 0; columna < Carton.COLUMNAS; columna++) {
					if (matrizCarton[fila][columna] != 0 && !tachados[fila][columna]) {
						filaCompleta = false;
						break;
					}
				}
				if (filaCompleta) {
					return true;
				}
			}
			return false;
		}

		// COMPROBAR SI HAY BINGO
		private static boolean comprobarBingo(Carton carton, boolean[][] tachados) {
			int[][] matrizCarton = carton.getMatriz();
			
			for (int fila = 0; fila < Carton.FILAS; fila++) {
				for (int columna = 0; columna < Carton.COLUMNAS; columna++) {
					if (matrizCarton[fila][columna] != 0 && !tachados[fila][columna]) {
						return false;
					}
				}
			}
			return true;
		}

}