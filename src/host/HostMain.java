package host;

import comun.Bombo;
import comun.Estado;
import comun.Carton;

public class HostMain {

	// TIEMPO DE ESPERA INICIAL PARA DAR TIEMPO A LOS JUGADORES (20 SEGUNDOS)
	private static final int TIEMPO_ESPERA_INICIAL_MS = 20000;
	
	// TIEMPO DE ESPERA ENTRE BOLA Y BOLA (5 SEGUNDOS)
	private static final int TIEMPO_ESPERA_MS = 5000;

	public static void main(String[] args) {
		System.out.println("=== INICIANDO SERVIDOR (HOST) ===");

		// 0. PRUEBA PREVIA DEL GESTOR DE FICHEROS
		System.out.println("\n--- PROBANDO GESTOR DE FICHEROS ---");
		Estado estadoPrueba = new Estado("INICIO", 1, 0, 1);
		GestorFicheros.guardarEstado(estadoPrueba);
		GestorFicheros.guardarBola(99);
		System.out.println("Ficheros 'estado.txt' y 'bolas.txt' actualizados de prueba.\n");

		// 1. INICIALIZAR ELEMENTOS DE LA PARTIDA REAL
		Bombo bombo = new Bombo();
		int idPartida = 1;
		Estado estadoPartida = new Estado("JUGANDO", 1, 0, idPartida);

		// GUARDAR ESTADO DE LA PARTIDA
		GestorFicheros.guardarEstado(estadoPartida);

		System.out.println("Partida #" + idPartida + " creada correctamente.");
		System.out.println("Bombo listo con " + bombo.getBolasRestantes() + " bolas.\n");

		// 2. PAUSA INICIAL PARA PERMITIR CONECTAR A LOS CLIENTES
		System.out.println("=== PREPARANDO PARTIDA ===");
		System.out.println("Esperando " + (TIEMPO_ESPERA_INICIAL_MS / 1000) + " segundos para que los jugadores se conecten...");
		
		try {
			Thread.sleep(TIEMPO_ESPERA_INICIAL_MS);
		} catch (InterruptedException e) {
			System.out.println("El hilo de espera inicial fue interrumpido.");
		}

		// 3. BUCLE PRINCIPAL DE EXTRACCIÓN DE BOLAS Y PERSISTENCIA HASTA EL FINAL
		System.out.println("\n=== COMIENZA EL JUEGO: ===");
		System.out.println("=== EXTRACCIÓN Y PERSISTENCIA DE BOLAS ===");

		int ronda = 1;

		// BUCLE HASTA QUE NO QUEDEN BOLAS
		while (bombo.quedanBolas()) {
			int bolaExtraida = bombo.sacarBola();

			// ACTUALIZAR ESTADO DE LA PARTIDA
			estadoPartida.setUltimaBola(bolaExtraida);
			estadoPartida.setVersion(estadoPartida.getVersion() + 1);

			// PERSISTENCIA: GUARDAR CAMBIOS EN FICHEROS
			GestorFicheros.guardarEstado(estadoPartida);
			GestorFicheros.guardarBola(bolaExtraida);

			System.out.println("Ronda " + ronda + " -> ¡Bola extraída: **" + bolaExtraida + "**! " +
					"(Fase: " + estadoPartida.getFase() +
					", Versión: " + estadoPartida.getVersion() + 
					", Faltan: " + bombo.getBolasRestantes() + " bolas) [Ficheros guardados]");

			try {
				// PAUSA CONFIGURABLE ENTRE BOLA Y BOLA
				Thread.sleep(TIEMPO_ESPERA_MS);
			} catch (InterruptedException e) {
				System.out.println("El hilo fue interrumpido.");
			}

			ronda++;
		}

		// CAMBIAR FASE A FINALIZADA CUANDO SE ACABEN LAS BOLAS
		estadoPartida.setFase("FINALIZADA");
		estadoPartida.setVersion(estadoPartida.getVersion() + 1);
		GestorFicheros.guardarEstado(estadoPartida);

		System.out.println("\n=== EL BOMBO SE HA VACIADO - FIN DE LA PARTIDA ===");
	}
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
