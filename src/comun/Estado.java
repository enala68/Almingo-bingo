package comun;

/**
 * ESTADO GLOBAL DE LA PARTIDA
 * ---------------------------------------------------------------------
 * Esta clase es una "fotografía" de la partida en un momento concreto.
 * El Host la escribe en el fichero host/estado.txt cada vez que algo
 * cambia, y los Jugadores la leen para enterarse de lo que ocurre.
 *
 * Se guarda en el fichero como 4 líneas de texto con formato clave=valor:
 *     fase=JUGANDO
 *     version=7
 *     ultimaBola=42
 *     idPartida=1
 *
 * IDEA CLAVE: "version" sube +1 cada vez que el estado cambia. Así, un
 * jugador solo tiene que comparar la versión que ya vio con la del fichero
 * para saber si hay algo nuevo, sin comparar el resto de campos.
 */
public class Estado {

	// ---------------------------------------------------------------
	// CONSTANTES DE FASE
	// Se usan en lugar de escribir el texto "a mano" por todo el código.
	// Si te equivocas al teclear una constante, Java te avisa al compilar;
	// si te equivocas al teclear un texto ("FINALISADA"), el error pasaría
	// desapercibido y el programa se comportaría raro.
	// ---------------------------------------------------------------

	/** El Host ha creado la partida pero aún no ha empezado a sacar bolas. */
	public static final String FASE_ESPERANDO = "ESPERANDO";

	/** Se están sacando bolas del bombo. */
	public static final String FASE_JUGANDO = "JUGANDO";

	/** Reservada: se está validando una reclamación (línea o bingo). */
	public static final String FASE_VALIDANDO = "VALIDANDO";

	/** El bombo se ha vaciado o alguien ha ganado: la partida terminó. */
	public static final String FASE_FINALIZADA = "FINALIZADA";

	/** El Host ha interrumpido la partida manualmente. */
	public static final String FASE_CANCELADA = "CANCELADA";

	/** Número de líneas que ocupa el estado dentro del fichero. */
	public static final int NUM_LINEAS = 4;

	// ---------------------------------------------------------------
	// ATRIBUTOS
	// ---------------------------------------------------------------
	private String fase;      // En qué momento está la partida (ver constantes FASE_*)
	private int version;      // Contador que sube +1 con cada cambio
	private int ultimaBola;   // Última bola extraída (1-90). 0 = todavía ninguna
	private int idPartida;    // Identificador de la partida actual

	// ---------------------------------------------------------------
	// CONSTRUCTORES
	// ---------------------------------------------------------------

	/** Crea un estado "en blanco": esperando, versión 0, sin bolas. */
	public Estado() {
		this(FASE_ESPERANDO, 0, 0, 0);
	}

	/**
	 * Crea un estado con todos sus valores.
	 *
	 * @param fase       fase de la partida (usa las constantes FASE_*)
	 * @param version    número de versión
	 * @param ultimaBola última bola sacada (0 si ninguna)
	 * @param idPartida  identificador de la partida
	 */
	public Estado(String fase, int version, int ultimaBola, int idPartida) {
		this.fase = fase;
		this.version = version;
		this.ultimaBola = ultimaBola;
		this.idPartida = idPartida;
	}

	// ---------------------------------------------------------------
	// GETTERS Y SETTERS
	// ---------------------------------------------------------------
	public String getFase() { return fase; }
	public void setFase(String fase) { this.fase = fase; }

	public int getVersion() { return version; }
	public void setVersion(int version) { this.version = version; }

	public int getUltimaBola() { return ultimaBola; }
	public void setUltimaBola(int ultimaBola) { this.ultimaBola = ultimaBola; }

	public int getIdPartida() { return idPartida; }
	public void setIdPartida(int idPartida) { this.idPartida = idPartida; }

	/**
	 * Método de comodidad: sube la versión en 1.
	 * Se llama cada vez que el Host cambia algo del estado, para que los
	 * jugadores se den cuenta de que hay novedades.
	 */
	public void incrementarVersion() {
		this.version++;
	}

	// ---------------------------------------------------------------
	// CONVERSIÓN A / DESDE TEXTO (para guardar en el fichero)
	// ---------------------------------------------------------------

	/**
	 * Convierte el objeto en un array de 4 líneas de texto listas para
	 * escribirse en el fichero.
	 *
	 * @return array con las líneas "fase=..", "version=..", etc.
	 */
	public String[] aLineas() {
		String[] lineas = new String[NUM_LINEAS];
		lineas[0] = "fase=" + fase;
		lineas[1] = "version=" + version;
		lineas[2] = "ultimaBola=" + ultimaBola;
		lineas[3] = "idPartida=" + idPartida;
		return lineas;
	}

	/**
	 * Reconstruye un objeto Estado a partir de las líneas leídas del fichero.
	 * Es la operación inversa de aLineas().
	 *
	 * Cada línea tiene el formato "clave=valor". Se parte por el primer "="
	 * y según la clave se rellena el atributo correspondiente. Las líneas
	 * nulas o con formato desconocido se ignoran.
	 *
	 * @param lineas array de líneas leídas del fichero
	 * @return el Estado reconstruido
	 * @throws NumberFormatException si un valor numérico no es un número
	 *         válido (p. ej. fichero a medio escribir). Quien llame a este
	 *         método debe estar preparado para capturarla.
	 */
	public static Estado deLineas(String[] lineas) {
		Estado estado = new Estado();

		for (int i = 0; i < lineas.length; i++) {
			// Si una línea no llegó a leerse, pasamos a la siguiente
			if (lineas[i] == null) {
				continue;
			}

			// "version=7".split("=", 2) -> ["version", "7"]
			// El 2 limita a dos trozos: solo se parte por el primer "="
			String[] partes = lineas[i].split("=", 2);

			if (partes.length == 2) {
				String clave = partes[0].trim();
				String valor = partes[1].trim();

				if (clave.equals("fase")) {
					estado.setFase(valor);
				} else if (clave.equals("version")) {
					estado.setVersion(Integer.parseInt(valor));
				} else if (clave.equals("ultimaBola")) {
					estado.setUltimaBola(Integer.parseInt(valor));
				} else if (clave.equals("idPartida")) {
					estado.setIdPartida(Integer.parseInt(valor));
				}
			}
		}
		return estado;
	}
}
