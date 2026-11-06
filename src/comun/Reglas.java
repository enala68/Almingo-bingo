package comun;

/*
 * REGLAS Y CONFIGURACIÓN DEL JUEGO
 * ---------------------------------------------------------------------
 * AQUÍ ESTÁN TODOS LOS NÚMEROS QUE SE PUEDEN AJUSTAR DEL JUEGO (TIEMPOS,
 * DINERO, PERDONES). ASÍ, SI QUEREMOS CAMBIAR ALGO (POR EJEMPLO, HACER LA
 * PARTIDA MÁS RÁPIDA PARA PROBAR) SOLO SE TOCA UN SITIO Y NO HAY QUE
 * BUSCAR NÚMEROS REPARTIDOS POR TODO EL CÓDIGO.
 *
 * PARA PROBAR RÁPIDO: TIEMPO_ENTRE_BOLAS_MS = 1500 Y TIEMPO_RESPUESTA_MS = 1000.
 * IMPORTANTE: TIEMPO_RESPUESTA_MS TIENE QUE SER MENOR QUE TIEMPO_ENTRE_BOLAS_MS,
 * SI NO, LA SIGUIENTE BOLA SALDRÍA MIENTRAS EL JUGADOR TODAVÍA CONTESTA.
 */
public class Reglas {

	// =================================================================
	// TIEMPOS (EN MILISEGUNDOS: 1000 = 1 SEGUNDO)
	// =================================================================

	/* ESPERA DEL HOST AL PRINCIPIO PARA QUE LOS JUGADORES SE CONECTEN (20 SEG). */
	public static final int TIEMPO_ESPERA_INICIAL_MS = 20000;

	/* PAUSA DEL HOST ENTRE UNA BOLA Y LA SIGUIENTE (15 SEG). */
	public static final int TIEMPO_ENTRE_BOLAS_MS = 15000;

	/* TIEMPO QUE TIENE EL JUGADOR PARA CONTESTAR S/N A CADA BOLA (12 SEG). */
	public static final int TIEMPO_RESPUESTA_MS = 12000;

	/* CADA CUÁNTO MIRA EL JUGADOR SI HAY BOLA NUEVA (MEDIO SEGUNDO). */
	public static final int INTERVALO_CONSULTA_MS = 500;

	// =================================================================
	// OLVIDOS
	// =================================================================

	/*
	 * CUÁNTOS OLVIDOS SE PERDONAN POR JUGADOR Y PARTIDA.
	 * UN OLVIDO ES CUANDO LA BOLA ESTABA EN EL CARTÓN Y EL JUGADOR DICE N
	 * (O NO CONTESTA). SI SE PERDONA, EL NÚMERO SE TACHA SOLO.
	 */
	public static final int OLVIDOS_PERDONADOS = 1;

	// =================================================================
	// DINERO (EN CÉNTIMOS PARA EVITAR ERRORES DE DECIMALES: 100 = 1 EURO)
	// SE USARÁ CUANDO SE IMPLEMENTE EL BOTE.
	// =================================================================

	/* LO QUE PAGA CADA JUGADOR AL ENTRAR (VA AL BOTE). */
	public static final int ENTRADA_CTS = 100;

	/* MULTA POR DECIR S CUANDO LA BOLA NO ESTABA (VA AL BOTE). */
	public static final int MULTA_FALSA_ALARMA_CTS = 50;

	/* PORCENTAJE DEL BOTE QUE SE LLEVA EL PRIMERO EN CANTAR LÍNEA. */
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
