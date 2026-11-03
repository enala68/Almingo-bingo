package host;

import comun.Estado;
import comun.Carton;

public class HostMain {

	public static void main(String[] args) {
		// 1. Creamos un objeto Estado con valores de prueba
		Estado estadoOriginal = new Estado("JUGANDO", 5, 42, 1);

		System.out.println("=== 1. ESTADO ORIGINAL ===");
		System.out.println("Fase: " + estadoOriginal.getFase());
		System.out.println("Versión: " + estadoOriginal.getVersion());
		System.out.println("Última bola: " + estadoOriginal.getUltimaBola());
		System.out.println("ID Partida: " + estadoOriginal.getIdPartida());

		// 2. Probamos el método aLineas() (convertir a array de texto)
		String[] lineas = estadoOriginal.aLineas();
		System.out.println("\n=== 2. CONVERTIDO A LÍNEAS (Array) ===" );
		for (String linea : lineas) {
			System.out.println(linea);
		}

		// 3. Probamos el método deLineas() (reconstruir el objeto desde el array)
		Estado estadoRecuperado = Estado.deLineas(lineas);
		System.out.println("\n=== 3. OBJETO RECUPERADO DE LAS LÍNEAS ===");
		System.out.println("Fase recuperada: " + estadoRecuperado.getFase());
		System.out.println("Versión recuperada: " + estadoRecuperado.getVersion());
		System.out.println("Última bola recuperada: " + estadoRecuperado.getUltimaBola());
		System.out.println("ID Partida recuperado: " + estadoRecuperado.getIdPartida());

		
		// PRUEBA CARTON
		System.out.println();
		System.out.println("=== PROBANDO LA GENERACIÓN DE UN CARTÓN ===");

		// 1. Generamos el cartón aleatorio
		Carton cartonJugador = Carton.generarCarton();

		// 2. Lo mostramos por pantalla
		cartonJugador.mostrarCarton();
	}
}