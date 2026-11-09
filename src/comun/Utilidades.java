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

	/* BORRA LA PANTALLA DEL PROGRAMA. */
	public static void limpiarPantalla() {
		try {
			new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
		} catch (Exception e) {
			// NO SE PUDO LIMPIAR: EL JUEGO FUNCIONA IGUAL, SOLO SE VE MÁS LARGO
		}
	}
}
