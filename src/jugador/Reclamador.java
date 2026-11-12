package jugador;

import comun.Estado;
import comun.GestorFicheros;
import comun.Reglas;
import comun.Utilidades;

/*
 * RECLAMADOR (LADO DEL JUGADOR).
 * GESTIONA EL PROCESO DE CANTAR LÍNEA O BINGO CON EL HOST, VÍA FICHEROS DE reclamaciones/:
 *
 *   JUGADOR                                   HOST
 *   -------                                   ----
 *   1. ESCRIBE <nombre>_reclamo.txt   ----->  LO DETECTA Y VALIDA LA JUGADA
 *   2. ESPERA <nombre>_pregunta.txt   <-----  ESCRIBE UNA PREGUNTA (O EL RESULTADO
 *                                             SI ES FALSA O LLEGA TARDE)
 *   3. ESCRIBE <nombre>_respuesta.txt ----->  COMPRUEBA LA RESPUESTA (A, B O C)
 *   4. ESPERA <nombre>_resultado.txt  <-----  ACIERTO, FALLO, FALSO O TARDE
 *
 * MIENTRAS TANTO, EL HOST TIENE LA PARTIDA EN PAUSA (FASE VALIDANDO).
 * NO TOCA FICHEROS DIRECTAMENTE: USA GestorFicheros.
 */
public class Reclamador {

	/* LA PARTIDA TERMINÓ MIENTRAS SE ESPERABA AL HOST. LOS DEMÁS RESULTADOS ESTÁN EN Reglas. */
	public static final String PARTIDA_TERMINADA = "PARTIDA_TERMINADA";

	/* LÍNEA DECORATIVA DE LA PANTALLA DE PREGUNTA. */
	private static final String LINEA = "==================================================";

	/* CADA CUÁNTO SE CONSULTA AL HOST (CORTO: EL HOST BORRA EL RESULTADO ENSEGUIDA). */
	private static final int INTERVALO_MS = 200;

	/* MARGEN: EL JUGADOR TIENE ESTE TIEMPO MENOS QUE EL HOST, POR SI LA CARPETA VA LENTA. */
	private static final int MARGEN_MS = 5000;

	/*
	 * RECLAMA UNA LÍNEA O UN BINGO Y DEVUELVE EL RESULTADO.
	 *
	 * @param nombre  NOMBRE DEL JUGADOR
	 * @param tipo    Reglas.TIPO_LINEA O Reglas.TIPO_BINGO
	 * @param teclado LECTOR DE TECLADO
	 * @return Reglas.RESULTADO_ACIERTO, _FALLO, _FALSO, _TARDE, _SIN_RESPUESTA
	 *         O Reclamador.PARTIDA_TERMINADA
	 */
	public static String reclamar(String nombre, String tipo, LectorTeclado teclado) {

		// 1. LIMPIAR RESTOS Y ENVIAR EL RECLAMO
		GestorFicheros.limpiarFicherosReclamo(nombre);
		GestorFicheros.guardarReclamo(nombre, tipo);
		System.out.println("\n📨 Reclamación de " + tipo + " enviada al Host. Esperando validación...");

		// 2. ESPERAR PREGUNTA O RESULTADO DIRECTO (EL HOST PUEDE ESTAR ATENDIENDO A OTRO)
		String[] pregunta = null;
		String resultado = null;
		long limite = System.currentTimeMillis() + Reglas.TIEMPO_ESPERA_HOST_MS;

		while (pregunta == null && resultado == null && System.currentTimeMillis() < limite) {
			pregunta = leerPreguntaCompleta(nombre);

			if (pregunta == null) {
				resultado = GestorFicheros.leerResultadoReclamo(nombre);
			}

			if (pregunta == null && resultado == null) {
				if (partidaTerminada()) {
					resultado = PARTIDA_TERMINADA;
				} else {
					Utilidades.pausar(INTERVALO_MS);
				}
			}
		}

		// 3. SI HAY PREGUNTA, CONTESTARLA Y ESPERAR EL RESULTADO
		if (pregunta != null) {
			contestarPregunta(nombre, tipo, pregunta, teclado);
			resultado = esperarResultado(nombre);
		}

		// 4. LIMPIAR FICHEROS (SI EL HOST NO RESPONDIÓ, TAMBIÉN RETIRA EL RECLAMO)
		GestorFicheros.limpiarFicherosReclamo(nombre);

		if (resultado == null) {
			return Reglas.RESULTADO_SIN_RESPUESTA;
		}
		return resultado;
	}

	/* LEE LA PREGUNTA (4 LÍNEAS: ENUNCIADO + A, B, C). null SI NO EXISTE O ESTÁ A MEDIAS. */
	private static String[] leerPreguntaCompleta(String nombre) {
		String[] lineas = GestorFicheros.leerPreguntaReclamo(nombre);
		if (lineas != null && lineas.length >= 4) {
			return lineas;
		}
		return null;
	}

	/* MUESTRA LA PREGUNTA Y ENVÍA LA RESPUESTA. SIN RESPUESTA A TIEMPO, NO SE ENVÍA NADA. */
	private static void contestarPregunta(String nombre, String tipo, String[] pregunta, LectorTeclado teclado) {
		int tiempoMs = Reglas.TIEMPO_PREGUNTA_MS - MARGEN_MS;

		// PANTALLA DE LA PREGUNTA
		Utilidades.limpiarPantalla();
		System.out.println(LINEA);
		System.out.println(" 🌱 PREGUNTA DE SOSTENIBILIDAD PARA VALIDAR TU " + tipo);
		System.out.println(LINEA);
		System.out.println("\n" + pregunta[0] + "\n"); // ENUNCIADO
		for (int i = 1; i < pregunta.length; i++) {
			System.out.println("   " + pregunta[i]); // OPCIONES A, B, C
		}

		// LEER RESPUESTA (SI NO ES A, B O C, SE REPITE MIENTRAS QUEDE TIEMPO)
		teclado.descartarPendiente();
		long limite = System.currentTimeMillis() + tiempoMs;
		String letra = null;

		while (letra == null && System.currentTimeMillis() < limite) {
			System.out.print("\nTu respuesta (A, B o C + ENTER, máximo " + (tiempoMs / 1000) + " s): ");

			// TIEMPO RESTANTE (long -> int SIN PROBLEMA)
			int quedan = (int) (limite - System.currentTimeMillis());
			String texto = teclado.esperarLinea(quedan);

			if (texto == null) {
				break; // TIEMPO AGOTADO
			}

			texto = texto.trim().toUpperCase();
			if (texto.equals("A") || texto.equals("B") || texto.equals("C")) {
				letra = texto;
			} else {
				System.out.println("Escribe solo A, B o C.");
			}
		}

		if (letra != null) {
			GestorFicheros.guardarRespuestaReclamo(nombre, letra);
			System.out.println("\n📤 Respuesta enviada. Esperando al Host...");
		} else {
			System.out.println("\n⏰ No has contestado a tiempo. Esperando al Host...");
		}
	}

	/*
	 * ESPERA EL RESULTADO FINAL (HASTA TIEMPO_PREGUNTA_MS, POR SI NO SE CONTESTÓ).
	 *
	 * @return EL RESULTADO, PARTIDA_TERMINADA O null SI NO LLEGÓ NADA
	 */
	private static String esperarResultado(String nombre) {
		long limite = System.currentTimeMillis() + Reglas.TIEMPO_PREGUNTA_MS;

		while (System.currentTimeMillis() < limite) {
			String resultado = GestorFicheros.leerResultadoReclamo(nombre);
			if (resultado != null) {
				return resultado;
			}
			if (partidaTerminada()) {
				return PARTIDA_TERMINADA;
			}
			Utilidades.pausar(INTERVALO_MS);
		}
		return null;
	}

	/* COMPRUEBA SI LA PARTIDA ESTÁ FINALIZADA O CANCELADA. */
	private static boolean partidaTerminada() {
		Estado estado = GestorFicheros.leerEstado();
		if (estado == null) {
			return false;
		}
		return estado.getFase().equals(Estado.FASE_FINALIZADA)
				|| estado.getFase().equals(Estado.FASE_CANCELADA);
	}
}