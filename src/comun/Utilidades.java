package comun;

/*
 * UTILIDADES COMUNES
 * FUNCIONES MULTICLASES.
 */
public class Utilidades {

	/*
	 * DETIENE EL PROGRAMA
	 * Thread.sleep CAPTURA InterruptedException (SI OTRA FUNCION INTERRUMPE LA ESPERA).
	 */
	public static void pausar(int milisegundos) {
		try {
			Thread.sleep(milisegundos);
		} catch (InterruptedException e) {
			System.out.println("[AVISO] La espera fue interrumpida.");
		}
	}
}
