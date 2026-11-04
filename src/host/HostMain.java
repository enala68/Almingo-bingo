package host;

import comun.Bombo;
import comun.Estado;
import comun.Carton;

public class HostMain {
	
	public static void main(String[] args) {
		System.out.println("=== INICIANDO SERVIDOR (HOST) ===");
		
		// INICIALIZAR ELEMENTOS
		Bombo bombo = new Bombo();
		int idPartida = 1;
		// Estado(String fase, int version, int ultimaBola, int idPartida)
		Estado estadoPartida = new Estado("JUGANDO", 1, 0, idPartida);
		
		System.out.println("Partida #" + idPartida  + " creada correctamente.");
        System.out.println("Bombo listo con " + bombo.getBolasRestantes() + " bolas.\n");
        
        // BUCLE PRINCIPAL DE EXTRACCIÓN DE BOLAS
        System.out.println("=== COMIENZA EL JUEGO: ===");
        System.out.println("=== EXTRACCIÓN DE BOLAS ===");
        
     // Simulamos por ejemplo 10 extracciones para ver el flujo en consola
        for (int ronda = 1; ronda <= 10; ronda++) {
            if (bombo.quedanBolas()) {
                int bolaExtraida = bombo.sacarBola();
                
                // Actualizamos el estado de la partida
                estadoPartida.setUltimaBola(bolaExtraida);
                estadoPartida.setVersion(estadoPartida.getVersion() + 1);
                
                System.out.println("Ronda " + ronda + " -> ¡Bola extraída: **" + bolaExtraida + "**! " +
                        "(Fase: " + estadoPartida.getFase() +
                        ", Versión: " + estadoPartida.getVersion() + 
                        ", Faltan: " + bombo.getBolasRestantes() + " bolas)");
                
                try {
                    // Pausa de 1 segundo entre bola y bola para simular ritmo real
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("El hilo fue interrumpido.");
                }
            }
        }
        System.out.println("\n=== FIN DE LA SIMULACIÓN DE PRUEBA ===");
	}
	

	/*public static void main(String[] args) {
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
		
		
		// PRUEBA BOMBO
				System.out.println();
				System.out.println("=== PROBANDO EL BOMBO ===");
				comun.Bombo bombo = new comun.Bombo();
				
				System.out.print("Primeras 5 bolas extraídas: ");
				for (int i = 0; i < 5; i++) {
					System.out.print(bombo.sacarBola() + " ");
				}
				System.out.println("\nBolas restantes en el bombo: " + bombo.getBolasRestantes());
	}*/
}