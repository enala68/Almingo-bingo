package comun;

/* UTILIDADES COMUNES */
public class Utilidades {

	/* DETENER EL PROGRAMA EN MILISEGUNDOS */
	public static void pausar(int milisegundos) {
		try {
			Thread.sleep(milisegundos);
		} catch (InterruptedException e) {
			System.out.println("[AVISO] La espera fue interrumpida.");
		}
	}

	/* BORRAR PANTALLA DE LA TERMINAL */
	public static void limpiarPantalla() {
		try {
			new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
		} catch (Exception e) {
			// IGNORAR SI FALLA LA LIMPIEZA
		}
	}
}