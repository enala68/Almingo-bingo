package jugador;

import java.util.Scanner;
import comun.Utilidades;

/*
 * LECTOR DE TECLADO CON TIEMPO LÍMITE
 * ---------------------------------------------------------------------
 * PROBLEMA: UN Scanner NORMAL (nextLine) SE QUEDA PARADO HASTA QUE EL USUARIO
 * PULSA ENTER. MIENTRAS ESTÁ PARADO, EL PROGRAMA NO PUEDE HACER NADA MÁS, ASÍ
 * QUE NO PUEDE CONTAR EL TIEMPO NI PASAR A LA SIGUIENTE BOLA.
 *
 * SOLUCIÓN: ESTA CLASE ES UN "HILO" (Thread), ES DECIR, UN TRABAJADOR EN SEGUNDO
 * PLANO. SU ÚNICO TRABAJO ES QUEDARSE ESPERANDO AL TECLADO. CUANDO EL USUARIO
 * ESCRIBE UNA LÍNEA Y PULSA ENTER, EL HILO LA GUARDA EN UNA VARIABLE.
 * MIENTRAS TANTO, EL PROGRAMA PRINCIPAL ES LIBRE: MIRA CADA 50 MILISEGUNDOS SI
 * YA HAY UNA LÍNEA GUARDADA O SI SE HA ACABADO EL TIEMPO.
 *
 * USO:
 *   LectorTeclado teclado = new LectorTeclado();
 *   teclado.start();                          // ARRANCA EL HILO (UNA SOLA VEZ)
 *   String nombre = teclado.leerLinea();      // ESPERA SIN LÍMITE DE TIEMPO
 *   String resp = teclado.esperarLinea(12000);// ESPERA COMO MUCHO 12 SEG (null SI NO HUBO RESPUESTA)
 *
 * "synchronized" EN LOS MÉTODOS QUE TOCAN lineaPendiente SIRVE PARA QUE EL HILO
 * DEL TECLADO Y EL PROGRAMA PRINCIPAL NO LA USEN EXACTAMENTE A LA VEZ.
 */
public class LectorTeclado extends Thread {

	// ÚNICO Scanner DEL PROGRAMA. SOLO LO USA ESTE HILO. NO SE CIERRA A PROPÓSITO:
	// CERRARLO CERRARÍA System.in.
	private final Scanner scanner = new Scanner(System.in);

	// ÚLTIMA LÍNEA ESCRITA POR EL USUARIO QUE TODAVÍA NO SE HA RECOGIDO (null = NINGUNA)
	private String lineaPendiente = null;

	/*
	 * CONSTRUCTOR. setDaemon(true) HACE QUE ESTE HILO NO IMPIDA QUE EL PROGRAMA
	 * TERMINE: SI EL JUEGO ACABA, EL PROGRAMA SE CIERRA AUNQUE EL HILO SIGA
	 * ESPERANDO AL TECLADO.
	 */
	public LectorTeclado() {
		setDaemon(true);
	}

	/*
	 * ESTO ES LO QUE HACE EL HILO CUANDO SE LLAMA A start().
	 * REPITE PARA SIEMPRE: ESPERAR UNA LÍNEA (AQUÍ SE QUEDA PARADO, PERO SOLO ESTE
	 * HILO) Y GUARDARLA. SI EL TECLADO SE CIERRA, nextLine() DA ERROR Y EL HILO ACABA.
	 */
	@Override
	public void run() {
		try {
			while (true) {
				String linea = scanner.nextLine();
				guardarLinea(linea.trim());
			}
		} catch (Exception e) {
			// TECLADO CERRADO: EL HILO TERMINA
		}
	}

	/* GUARDA LA LÍNEA ESCRITA POR EL USUARIO (SI HABÍA OTRA SIN RECOGER, SE PIERDE). */
	private synchronized void guardarLinea(String linea) {
		lineaPendiente = linea;
	}

	/* DEVUELVE LA LÍNEA PENDIENTE Y LA BORRA (DEVUELVE null SI NO HABÍA NINGUNA). */
	private synchronized String recogerLinea() {
		String linea = lineaPendiente;
		lineaPendiente = null;
		return linea;
	}

	/*
	 * TIRA A LA BASURA LO QUE EL USUARIO HAYA ESCRITO Y NADIE HAYA RECOGIDO.
	 * SE LLAMA ANTES DE CADA PREGUNTA PARA QUE UNA RESPUESTA TARDÍA A LA BOLA
	 * ANTERIOR NO SE CUENTE COMO RESPUESTA A LA BOLA NUEVA.
	 */
	public void descartarPendiente() {
		recogerLinea();
	}

	/*
	 * ESPERA SIN LÍMITE DE TIEMPO A QUE EL USUARIO ESCRIBA UNA LÍNEA Y LA DEVUELVE.
	 * SE USA PARA EL NOMBRE DE USUARIO.
	 */
	public String leerLinea() {
		String linea = null;
		while (linea == null) {
			linea = recogerLinea();
			if (linea == null) {
				Utilidades.pausar(50); // NO HAY NADA TODAVÍA: ESPERAMOS UN POCO Y MIRAMOS DE NUEVO
			}
		}
		return linea;
	}

	/*
	 * ESPERA COMO MUCHO 'milisegundos' A QUE EL USUARIO ESCRIBA UNA LÍNEA.
	 *
	 * @return LA LÍNEA ESCRITA, O null SI SE ACABÓ EL TIEMPO SIN QUE ESCRIBIERA
	 */
	public String esperarLinea(int milisegundos) {
		// HORA LÍMITE = AHORA + TIEMPO PERMITIDO
		long limite = System.currentTimeMillis() + milisegundos;

		while (System.currentTimeMillis() < limite) {
			String linea = recogerLinea();
			if (linea != null) {
				return linea;
			}
			Utilidades.pausar(50);
		}
		return null; // TIEMPO AGOTADO
	}
}