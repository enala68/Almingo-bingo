package comun;

/*
 * UTILIDADES COMUNES
 * FUNCIONES QUE USAN VARIAS CLASES.
 */
public class Utilidades {

	/*
	 * DETIENE EL PROGRAMA UNOS MILISEGUNDOS (1000 = 1 SEGUNDO).
	 * Thread.sleep OBLIGA A CAPTURAR InterruptedException (SI ALGO INTERRUMPE LA ESPERA).
	 */
	public static void pausar(int milisegundos) {
		try {
			Thread.sleep(milisegundos);
		} catch (InterruptedException e) {
			System.out.println("[AVISO] La espera fue interrumpida.");
		}
	}

	/*
	 * BORRA LA PANTALLA DE LA CONSOLA DE WINDOWS (EL COMANDO "cls").
	 * SE LANZA UN PROCESO "cmd /c cls". inheritIO() HACE QUE ESE PROCESO USE
	 * NUESTRA MISMA VENTANA, ASÍ QUE LO QUE BORRA ES LA PANTALLA DEL PROGRAMA.
	 *
	 * SI NO SE PUEDE (POR EJEMPLO, LA CONSOLA INTEGRADA DE ECLIPSE NO LO
	 * PERMITE) NO PASA NADA: SIMPLEMENTE LA PANTALLA NO SE BORRA.
	 */
	public static void limpiarPantalla() {
		try {
			new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
		} catch (Exception e) {
			// NO SE PUDO LIMPIAR: EL JUEGO FUNCIONA IGUAL, SOLO SE VE MÁS LARGO
		}
	}
}
