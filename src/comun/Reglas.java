package comun;

/*
 * REGLAS Y CONFIGURACIÓN DEL JUEGO
 * ---------------------------------------------------------------------
 * AQUÍ ESTÁN TODOS LOS NÚMEROS Y TEXTOS FIJOS QUE SE PUEDEN AJUSTAR DEL JUEGO
 * (TIEMPOS, DINERO, PERDONES, RECLAMACIONES). ASÍ, SI QUEREMOS CAMBIAR ALGO
 * (POR EJEMPLO, HACER LA PARTIDA MÁS RÁPIDA PARA PROBAR) SOLO SE TOCA UN SITIO.
 *
 * PARA PROBAR RÁPIDO: TIEMPO_ENTRE_BOLAS_MS = 3000 Y TIEMPO_RESPUESTA_MS = 2000.
 * IMPORTANTE: TIEMPO_RESPUESTA_MS TIENE QUE SER MENOR QUE TIEMPO_ENTRE_BOLAS_MS.
 */
public class Reglas {

	// =================================================================
	// TIEMPOS (EN MILISEGUNDOS: 1000 = 1 SEGUNDO)
	// =================================================================

	/* ESPERA DEL HOST AL PRINCIPIO PARA QUE LOS JUGADORES SE CONECTEN. */
	public static final int TIEMPO_ESPERA_INICIAL_MS = 20000;

	/* PAUSA DEL HOST ENTRE UNA BOLA Y LA SIGUIENTE. */
	public static final int TIEMPO_ENTRE_BOLAS_MS = 15000;

	/* TIEMPO QUE TIENE EL JUGADOR PARA CONTESTAR S/N A CADA BOLA. */
	public static final int TIEMPO_RESPUESTA_MS = 12000;

	/* CADA CUÁNTO MIRAN LOS PROGRAMAS SI HAY NOVEDADES EN LOS FICHEROS (MEDIO SEGUNDO). */
	public static final int INTERVALO_CONSULTA_MS = 500;

	/*
	 * TIEMPO QUE TIENE EL HOST ESPERANDO LA RESPUESTA A LA PREGUNTA DE SOSTENIBILIDAD (30 SEG).
	 * EL JUGADOR TIENE 5 SEGUNDOS MENOS PARA ASEGURAR QUE SU RESPUESTA LLEGA A TIEMPO.
	 */
	public static final int TIEMPO_PREGUNTA_MS = 30000;

	/*
	 * TIEMPO QUE ESPERA EL JUGADOR A QUE EL HOST ATIENDA SU RECLAMACIÓN (45 SEG).
	 * ES LARGO A PROPÓSITO: SI OTRO JUGADOR ESTÁ SIENDO VALIDADO, HAY QUE ESPERAR SU TURNO.
	 */
	public static final int TIEMPO_ESPERA_HOST_MS = 45000;

	// =================================================================
	// OLVIDOS Y FALSAS ALARMAS
	// =================================================================

	/*
	 * CUÁNTOS OLVIDOS SE PERDONAN POR JUGADOR Y PARTIDA.
	 * UN OLVIDO ES CUANDO LA BOLA ESTABA EN EL CARTÓN Y EL JUGADOR DICE N
	 * (O NO CONTESTA). SI SE PERDONA, EL NÚMERO SE TACHA SOLO.
	 */
	public static final int OLVIDOS_PERDONADOS = 1;

	/*
	 * CUÁNTAS FALSAS ALARMAS SE PERDONAN POR JUGADOR Y PARTIDA.
	 * UNA FALSA ALARMA ES DECIR S CUANDO LA BOLA NO ESTÁ EN EL CARTÓN.
	 * LA PRIMERA NO TIENE PENALIZACIÓN; A PARTIR DE LA SEGUNDA SE REGISTRA
	 * (LAS MULTAS NO VAN AL BOTE).
	 */
	public static final int FALSAS_ALARMAS_PERDONADAS = 1;

	// =================================================================
	// RECLAMACIONES (LÍNEA Y BINGO)
	// =================================================================

	/* TIPOS DE RECLAMACIÓN. SE ESCRIBEN TAL CUAL EN EL FICHERO <jugador>_reclamo.txt. */
	public static final String TIPO_LINEA = "LINEA";
	public static final String TIPO_BINGO = "BINGO";

	/*
	 * POSIBLES RESULTADOS DE UNA RECLAMACIÓN (SE ESCRIBEN EN <jugador>_resultado.txt):
	 *   ACIERTO       -> JUGADA REAL Y PREGUNTA ACERTADA: GANA EL PREMIO.
	 *   FALLO         -> JUGADA REAL, PERO PREGUNTA FALLADA O SIN CONTESTAR: PIERDE EL PREMIO.
	 *   FALSO         -> EL HOST NO HA PODIDO COMPROBAR LA JUGADA: PIERDE EL PREMIO.
	 *   TARDE         -> YA SE HAN REPARTIDO LOS PREMIOS DE LÍNEA (SE VALIDA PARA SEGUIR A BINGO).
	 *   SIN_RESPUESTA -> EL HOST NO RESPONDIÓ A TIEMPO (SOLO LO USA EL JUGADOR).
	 */
	public static final String RESULTADO_ACIERTO = "ACIERTO";
	public static final String RESULTADO_FALLO = "FALLO";
	public static final String RESULTADO_FALSO = "FALSO";
	public static final String RESULTADO_TARDE = "TARDE";
	public static final String RESULTADO_SIN_RESPUESTA = "SIN_RESPUESTA";

	/* MÁXIMO DE PREMIOS DE LÍNEA QUE SE REPARTEN (LOS 3 PRIMEROS). */
	public static final int MAX_PREMIOS_LINEA = 3;

	// =================================================================
	// DINERO (EN CÉNTIMOS PARA EVITAR ERRORES DE DECIMALES: 100 = 1 EURO)
	// =================================================================

	/* LO QUE PAGA CADA JUGADOR AL ENTRAR EN FASE ESPERANDO (VA AL BOTE). */
	public static final int ENTRADA_CTS = 100;

	/*
	 * MULTA POR FALSA ALARMA (A PARTIR DE LA SEGUNDA).
	 * DE MOMENTO NO VA AL BOTE; SE MANTIENE POR SI SE QUIERE MOSTRAR O USAR DESPUÉS.
	 */
	public static final int MULTA_FALSA_ALARMA_CTS = 50;

	/* PORCENTAJE DEL BOTE QUE SE LLEVA CADA UNO DE LOS 3 PRIMEROS EN VALIDAR LÍNEA. */
	public static final int PORCENTAJE_PREMIO_LINEA = 10;

	/*
	 * CONVIERTE CÉNTIMOS EN TEXTO DE EUROS. EJEMPLO: 50 -> "0,50 €", 1250 -> "12,50 €".
	 * SE HACE CON DIVISIÓN ENTERA (/) Y RESTO (%), SIN USAR DECIMALES.
	 */
	public static String formatearEuros(int centimos) {
		int euros = centimos / 100;
		int resto = centimos % 100;
		String ceroDelante = "";
		if (resto < 10) {
			ceroDelante = "0"; // PARA QUE 5 CÉNTIMOS SE VEA "05" Y NO "5"
		}
		return euros + "," + ceroDelante + resto + " €";
	}
}