package comun;

/* REGLAS Y CONFIGURACIÓN DEL JUEGO */
public class Reglas {

	/* ESPERA INICIAL DEL HOST PARA CONEXIÓN DE JUGADORES */
	public static final int TIEMPO_ESPERA_INICIAL_MS = 20000;

	/* PAUSA ENTRE BOLA Y BOLA */
	public static final int TIEMPO_ENTRE_BOLAS_MS = 15000;

	/* TIEMPO MÁXIMO PARA CONTESTAR S/N A CADA BOLA */
	public static final int TIEMPO_RESPUESTA_MS = 12000;

	/* INTERVALO DE CONSULTA DE FICHEROS */
	public static final int INTERVALO_CONSULTA_MS = 500;

	/* TIEMPO ESPERA PREGUNTA DE SOSTENIBILIDAD */
	public static final int TIEMPO_PREGUNTA_MS = 30000;

	/* TIEMPO ESPERA MÁXIMO RESPUESTA DEL HOST */
	public static final int TIEMPO_ESPERA_HOST_MS = 45000;

	/* OLVIDOS PERDONADOS POR JUGADOR */
	public static final int OLVIDOS_PERDONADOS = 1;

	/* FALSAS ALARMAS PERDONADAS POR JUGADOR */
	public static final int FALSAS_ALARMAS_PERDONADAS = 1;

	/* TIPO DE RECLAMACIÓN LÍNEA */
	public static final String TIPO_LINEA = "LINEA";

	/* TIPO DE RECLAMACIÓN BINGO */
	public static final String TIPO_BINGO = "BINGO";

	/* RESULTADO ACIERTO */
	public static final String RESULTADO_ACIERTO = "ACIERTO";

	/* RESULTADO FALLO */
	public static final String RESULTADO_FALLO = "FALLO";

	/* RESULTADO FALSO */
	public static final String RESULTADO_FALSO = "FALSO";

	/* RESULTADO TARDE */
	public static final String RESULTADO_TARDE = "TARDE";

	/* RESULTADO SIN RESPUESTA */
	public static final String RESULTADO_SIN_RESPUESTA = "SIN_RESPUESTA";

	/* MÁXIMO DE PREMIOS DE LÍNEA */
	public static final int MAX_PREMIOS_LINEA = 3;

	/* ENTRADA EN CÉNTIMOS POR JUGADOR */
	public static final int ENTRADA_CTS = 100;

	/* MULTA POR FALSA ALARMA EN CÉNTIMOS */
	public static final int MULTA_FALSA_ALARMA_CTS = 50;

	/* PORCENTAJE DE BOTE PARA PREMIO DE LÍNEA */
	public static final int PORCENTAJE_PREMIO_LINEA = 10;

	/* FORMATEAR CÉNTIMOS A EUROS EN TEXTO */
	public static String formatearEuros(int centimos) {
		int euros = centimos / 100;
		int resto = centimos % 100;
		String ceroDelante = "";
		if (resto < 10) {
			ceroDelante = "0";
		}
		return euros + "," + ceroDelante + resto + " €";
	}
}