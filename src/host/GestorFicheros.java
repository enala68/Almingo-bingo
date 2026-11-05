package host;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import comun.Estado;

public class GestorFicheros {

	private static final String RUTA_ESTADO = "host/estado.txt";
	private static final String RUTA_BOLAS = "host/bolas.txt";

	// GUARDAR ESTADO EN "estado.txt"
	public static void guardarEstado(Estado estado) {
		PrintWriter escritor = null;

		try {
			escritor = new PrintWriter(new File(RUTA_ESTADO));
			String[] lineas = estado.aLineas();
			for (String linea : lineas) {
				escritor.println(linea);
			}
		} catch (Exception e) {
            System.err.println("Error al escribir el fichero de estado: " + e.getMessage());
        }

		// CERRAR FICHERO
		if (escritor != null) {
			escritor.close();
		}
	}

	// AÑADIR BOLA AL HISTORIAL DE "bolas.txt"
	public static void guardarBola(int bola) {
		PrintWriter escritor = null;
		
		try {
			escritor = new PrintWriter(new FileWriter(RUTA_BOLAS, true));
			escritor.println(bola);
		} catch (Exception e) {
            System.err.println("Error al escribir en el fichero de bolas: " + e.getMessage());
        }
		
		if (escritor != null) {
            escritor.close();
        }
	}
}
