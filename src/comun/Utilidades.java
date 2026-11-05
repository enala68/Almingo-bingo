package comun;

/**
 * UTILIDADES COMUNES
 * ---------------------------------------------------------------------
 * Pequeñas herramientas que usan varias clases. De momento solo la pausa,
 * que antes estaba repetida (con su try-catch) en HostMain y JugadorMain.
 */
public class Utilidades {

	/**
	 * Detiene el programa durante unos milisegundos.
	 * Thread.sleep obliga a capturar InterruptedException (error que ocurre
	 * si otro hilo interrumpe la espera). Al capturarla aquí, el resto del
	 * código se mantiene limpio.
	 *
	 * @param milisegundos tiempo de espera (1000 ms = 1 segundo)
	 */
	public static void pausar(int milisegundos) {
		try {
			Thread.sleep(milisegundos);
		} catch (InterruptedException e) {
			System.out.println("[AVISO] La espera fue interrumpida.");
		}
	}
}
