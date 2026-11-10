package jugador;

import comun.Estado;
import comun.GestorFicheros;
import comun.Reglas;
import comun.Utilidades;

/*
 * RECLAMADOR (LADO DEL JUGADOR)
 * ---------------------------------------------------------------------
 * SE ENCARGA DE TODO EL PROCESO DE CANTAR LÍNEA O BINGO, HABLANDO CON EL HOST
 * A TRAVÉS DE LOS FICHEROS DE LA CARPETA reclamaciones/:
 *
 *   JUGADOR                                   HOST
 *   -------                                   ----
 *   1. escribe <nombre>_reclamo.txt   ----->  LO DETECTA Y VALIDA LA JUGADA
 *                                             (REPITE LAS RESPUESTAS S/N DEL JUGADOR
 *                                              SOBRE LA COPIA DE SU CARTÓN)
 *   2. espera <nombre>_pregunta.txt   <-----  SI LA JUGADA ES REAL, ESCRIBE UNA PREGUNTA
 *                                             DE SOSTENIBILIDAD (O DIRECTAMENTE EL RESULTADO
 *                                             SI NO ES REAL / LLEGA TARDE)
 *   3. escribe <nombre>_respuesta.txt ----->  COMPRUEBA LA RESPUESTA (A, B O C)
 *   4. espera <nombre>_resultado.txt  <-----  ACIERTO, FALLO, FALSO O TARDE
 *
 * MIENTRAS DURA TODO ESTO, EL HOST TIENE LA PARTIDA EN PAUSA (FASE VALIDANDO).
 * ESTA CLASE NO TOCA FICHEROS DIRECTAMENTE: USA SIEMPRE GestorFicheros.
 */
public class Reclamador {

	/*
	 * RESULTADO ESPECIAL: LA PARTIDA TERMINÓ MIENTRAS SE ESPERABA AL HOST
	 * (POR EJEMPLO, OTRO JUGADOR HA GANADO EL BINGO).
	 * LOS DEMÁS RESULTADOS (ACIERTO, FALLO, FALSO, TARDE, SIN_RESPUESTA) ESTÁN EN Reglas.
	 */
	public static final String PARTIDA_TERMINADA = "PARTIDA_TERMINADA";

	/* LÍNEA DECORATIVA DE LA PANTALLA DE LA PREGUNTA. */
	private static final String LINEA = "==================================================";

	/*
	 * CADA CUÁNTO SE MIRA SI EL HOST HA RESPONDIDO. ES MÁS CORTO QUE EN EL RESTO DEL
	 * PROGRAMA PORQUE EL HOST BORRA EL FICHERO DE RESULTADO POCO DESPUÉS DE ESCRIBIRLO
	 * Y NO QUEREMOS PERDERNOS EL RESULTADO.
	 */
	private static final int INTERVALO_MS = 200;

	/*
	 * MARGEN DE SEGURIDAD: EL JUGADOR TIENE ESTE TIEMPO MENOS QUE EL HOST PARA CONTESTAR,
	 * ASÍ SU RESPUESTA LLEGA A TIEMPO AUNQUE LA CARPETA COMPARTIDA VAYA UN POCO LENTA.
	 */
	private static final int MARGEN_MS = 5000;

	/*
	 * RECLAMA UNA LÍNEA O UN BINGO Y DEVUELVE EL RESULTADO.
	 *
	 * @param nombre  NOMBRE DEL JUGADOR
	 * @param tipo    Reglas.TIPO_LINEA O Reglas.TIPO_BINGO
	 * @param teclado LECTOR DE TECLADO (PARA CONTESTAR LA PREGUNTA)
	 * @return Reglas.RESULTADO_ACIERTO, _FALLO, _FALSO, _TARDE, _SIN_RESPUESTA
	 *         O Reclamador.PARTIDA_TERMINADA
	 */
	public static String reclamar(String nombre, String tipo, LectorTeclado teclado) {

		// 1. LIMPIAR POSIBLES RESTOS PROPIOS Y ENVIAR EL RECLAMO
		GestorFicheros.limpiarFicherosReclamo(nombre);
		GestorFicheros.guardarReclamo(nombre, tipo);
		System.out.println("\n📨 Reclamación de " + tipo + " enviada al Host. Esperando validación...");

		// 2. ESPERAR A QUE EL HOST ESCRIBA LA PREGUNTA (JUGADA REAL) O DIRECTAMENTE
		//    EL RESULTADO (JUGADA FALSA O LLEGADA TARDE). SE ESPERA MUCHO PORQUE EL
		//    HOST PUEDE ESTAR VALIDANDO A OTRO JUGADOR ANTES QUE A NOSOTROS.
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

		// 4. LIMPIAR NUESTROS FICHEROS. SI EL HOST NO RESPONDIÓ, ESTO TAMBIÉN RETIRA
		//    EL RECLAMO PARA QUE NO LO ATIENDA CUANDO YA NO ESTAMOS ESPERANDO.
		GestorFicheros.limpiarFicherosReclamo(nombre);

		if (resultado == null) {
			return Reglas.RESULTADO_SIN_RESPUESTA;
		}
		return resultado;
	}

	/*
	 * LEE LA PREGUNTA QUE EL HOST HA ESCRITO PARA ESTE JUGADOR.
	 * UNA PREGUNTA COMPLETA TIENE 4 LÍNEAS: ENUNCIADO + OPCIONES A, B Y C.
	 * SI EL FICHERO NO EXISTE O SE LEE A MEDIAS (EL HOST LO ESTÁ ESCRIBIENDO), DEVUELVE null.
	 */
	private static String[] leerPreguntaCompleta(String nombre) {
		String[] lineas = GestorFicheros.leerPreguntaReclamo(nombre);
		if (lineas != null && lineas.length >= 4) {
			return lineas;
		}
		return null;
	}

	/*
	 * MUESTRA LA PREGUNTA, LEE LA RESPUESTA (A, B O C) Y LA ENVÍA AL HOST.
	 * SI EL JUGADOR NO CONTESTA A TIEMPO, NO SE ENVÍA NADA Y EL HOST DARÁ FALLO.
	 */
	private static void contestarPregunta(String nombre, String tipo, String[] pregunta, LectorTeclado teclado) {
		int tiempoMs = Reglas.TIEMPO_PREGUNTA_MS - MARGEN_MS;

		// PANTALLA DE LA PREGUNTA
		Utilidades.limpiarPantalla();
		System.out.println(LINEA);
		System.out.println(" 🌱 PREGUNTA DE SOSTENIBILIDAD PARA VALIDAR TU " + tipo);
		System.out.println(LINEA);
		System.out.println("\n" + pregunta[0] + "\n"); // ENUNCIADO
		for (int i = 1; i < pregunta.length; i++) {
			System.out.println("   " + pregunta[i]); // OPCIONES "A) ...", "B) ...", "C) ..."
		}

		// LEER LA RESPUESTA. SI ESCRIBE ALGO QUE NO SEA A, B O C, SE LE VUELVE A PEDIR
		// MIENTRAS QUEDE TIEMPO.
		teclado.descartarPendiente();
		long limite = System.currentTimeMillis() + tiempoMs;
		String letra = null;

		while (letra == null && System.currentTimeMillis() < limite) {
			System.out.print("\nTu respuesta (A, B o C + ENTER, máximo " + (tiempoMs / 1000) + " s): ");

			// TIEMPO QUE QUEDA (long -> int: ES UN NÚMERO PEQUEÑO, NO HAY PROBLEMA)
			int quedan = (int) (limite - System.currentTimeMillis());
			String texto = teclado.esperarLinea(quedan);

			if (texto == null) {
				break; // SE ACABÓ EL TIEMPO
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
	 * ESPERA EL RESULTADO FINAL DEL HOST.
	 * SI EL JUGADOR NO CONTESTÓ, EL HOST ESPERA TODO SU TIEMPO ANTES DE DAR FALLO,
	 * POR ESO SE ESPERA HASTA TIEMPO_PREGUNTA_MS.
	 *
	 * @return EL RESULTADO, Reclamador.PARTIDA_TERMINADA SI LA PARTIDA ACABÓ,
	 *         O null SI NO LLEGÓ NADA A TIEMPO
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

	/* COMPRUEBA SI EL HOST HA DADO LA PARTIDA POR FINALIZADA O CANCELADA. */
	private static boolean partidaTerminada() {
		Estado estado = GestorFicheros.leerEstado();
		if (estado == null) {
			return false;
		}
		return estado.getFase().equals(Estado.FASE_FINALIZADA)
				|| estado.getFase().equals(Estado.FASE_CANCELADA);
	}
}
