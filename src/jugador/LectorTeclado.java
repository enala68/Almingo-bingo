package jugador;

import java.util.Scanner;
import comun.Utilidades;

/* LECTOR DE TECLADO CON TIEMPO LÍMITE */
public class LectorTeclado extends Thread {

	/* SCANNER DE ENTRADA ESTÁNDAR */
	private final Scanner scanner = new Scanner(System.in);

	/* ÚLTIMA LÍNEA PENDIENTE */
	private String lineaPendiente = null;

	/* CONFIGURAR HILO COMO DAEMON */
	public LectorTeclado() {
		setDaemon(true);
	}

	/* BUCLE PRINCIPAL DEL HILO PARA ESCUCHAR EL TECLADO */
	@Override
	public void run() {
		try {
			while (true) {
				String linea = scanner.nextLine();
				guardarLinea(linea.trim());
			}
		} catch (Exception e) {
			// TERMINAR HILO SI FALLA EL TECLADO
		}
	}

	/* GUARDAR LÍNEA ESCRITA */
	private synchronized void guardarLinea(String linea) {
		lineaPendiente = linea;
	}

	/* RECOGER Y BORRAR LÍNEA PENDIENTE */
	private synchronized String recogerLinea() {
		String linea = lineaPendiente;
		lineaPendiente = null;
		return linea;
	}

	/* DESCARTAR LÍNEA ANTIGUA O NO RECOGIDA */
	public void descartarPendiente() {
		recogerLinea();
	}

	/* LEER LÍNEA SIN LÍMITE DE TIEMPO */
	public String leerLinea() {
		String linea = null;
		while (linea == null) {
			linea = recogerLinea();
			if (linea == null) {
				Utilidades.pausar(50);
			}
		}
		return linea;
	}

	/* ESPERAR LÍNEA CON TIEMPO LÍMITE EN MILISEGUNDOS */
	public String esperarLinea(int milisegundos) {
		long limite = System.currentTimeMillis() + milisegundos;

		while (System.currentTimeMillis() < limite) {
			String linea = recogerLinea();
			if (linea != null) {
				return linea;
			}
			Utilidades.pausar(50);
		}
		return null;
	}
}