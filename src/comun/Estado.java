package comun;

/**
 * ESTADO GLOBAL DE LA PARTIDA
 * ---------------------------------------------------------------------
 * HOST ESCRIBE ESTADO EN host/estado.txt CADA VEZ QUE ALGO CAMBIA Y JUGADORES LO LEEN.
 *
 * FORMATO DE GUARDADO (clave=valor):
 *     fase=JUGANDO
 *     version=7
 *     ultimaBola=42
 *     idPartida=1
 *
 * "version" SUBE +1 CADA VEZ QUE ESTADO CAMBIA PARA QUE JUGADOR SEPA SI HAY ALGO NUEVO.
 */
public class Estado {

	// ---------------------------------------------------------------
	// CONSTANTES DE FASE:
	// ---------------------------------------------------------------
	
	/* HOST HA CREADO PARTIDA PERO AÚN NO EMPIEZA A SACAR BOLAS. */
	public static final String FASE_ESPERANDO = "ESPERANDO";

	/* SE ESTÁN SACANDO BOLAS. */
	public static final String FASE_JUGANDO = "JUGANDO";

	/* VALIDANDO UNA RECLAMACIÓN (LINEA O BINGO). */
	public static final String FASE_VALIDANDO = "VALIDANDO";

	/* PARTIDA TERMINÓ. */
	public static final String FASE_FINALIZADA = "FINALIZADA";

	/* HOST INTERRUMPE PARTIDA MANUALMENTE. */
	public static final String FASE_CANCELADA = "CANCELADA";

	/* NUMERO DE LINEAS DE ESTADO EN FICHERO. */
	public static final int NUM_LINEAS = 4;

	// ---------------------------------------------------------------
	// ATRIBUTOS
	// ---------------------------------------------------------------
	private String fase;      // MOMENTO EN QUE SE ENCUENTRA LA PARTIDA
	private int version;      // CONTADOR DE CAMBIOS DE LA PARTIDA (+1)
	private int ultimaBola;   // ÚLTIMA BOLA EXTRAIDA (1-90). 0 = NINGUNA TODAVÍA
	private int idPartida;    // ID DE LA PARTIDA ACTUAL

	// ---------------------------------------------------------------
	// CONSTRUCTORES
	// ---------------------------------------------------------------

	/* CREA ESTADO EN BLANCO: esperando, versión 0, sin bolas. */
	public Estado() {
		this(FASE_ESPERANDO, 0, 0, 0);
	}

	/* CREA ESTADO COMPLETO */
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

	/* INCREMENTA VERSION EN 1 */
	public void incrementarVersion() {
		this.version++;
	}

	// ---------------------------------------------------------------
	// CONVERSIÓN A LINEAS PARA ESCRIBIR EN FICHERO
	// ---------------------------------------------------------------

	/* ESCRIBE ARRAY DE 4 LINEAS */
	public String[] aLineas() {
		String[] lineas = new String[NUM_LINEAS];
		lineas[0] = "fase=" + fase;
		lineas[1] = "version=" + version;
		lineas[2] = "ultimaBola=" + ultimaBola;
		lineas[3] = "idPartida=" + idPartida;
		return lineas;
	}

	/* LEE ARRAY DE 4 LINEAS */
	public static Estado deLineas(String[] lineas) {
		Estado estado = new Estado();

		for (int i = 0; i < lineas.length; i++) {
			// SI UNA LINEA NO LLEGÓ A LEERSE PASA A LA SIGUIENTE
			if (lineas[i] == null) {
				continue;
			}

			// "version=7".split("=", 2) -> ["version", "7"]
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
