package jugador;

import java.io.File;
import java.util.Scanner;

public class JugadorMain {

    // RUTA DEL ESTADO DEL HOST
    private static final String RUTA_ESTADO = "host/estado.txt";

    public static void main(String[] args) {
        System.out.println("=== INICIANDO JUGADOR ===");
        
        Scanner lector = null;
        
        try {
            lector = new Scanner(new File(RUTA_ESTADO));
            
            System.out.println("Leyendo el estado actual de la partida desde el Host...\n");
            
            while (lector.hasNext()) {
                String linea = lector.nextLine();
                System.out.println("Jugador lee -> " + linea);
            }
            
        } catch (Exception e) {
            System.err.println("Error al leer el fichero de estado: " + e.getMessage());
            System.out.println("Asegúrate de que el Host ha ejecutado y creado el fichero primero.");
        } finally {
            if (lector != null) {
                lector.close();
            }
        }
    }
}