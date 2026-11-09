package comun;

/* ESTADO GLOBAL DE LA PARTIDA */
public class Estado {

	/* FASE ESPERANDO */
	public static final String FASE_ESPERANDO = "ESPERANDO";

	/* FASE JUGANDO */
	public static final String FASE_JUGANDO = "JUGANDO";

	/* FASE VALIDANDO */
	public static final String FASE_VALIDANDO = "VALIDANDO";

	/* FASE FINALIZADA */
	public static final String FASE_FINALIZADA = "FINALIZADA";

	/* FASE CANCELADA */
	public static final String FASE_CANCELADA = "CANCELADA";

	/* NÚMERO DE LÍNEAS DE ESTADO */
	public static final int NUM_LINEAS = 4;

	private String fase;      
	private int version;      
	private int ultimaBola;   
	private int idPartida;    

	/* CREAR ESTADO EN BLANCO */
	public Estado() {
		this(FASE_ESPERANDO, 0, 0, 0);
	}

	/* CREAR ESTADO COMPLETO */
	public Estado(String fase, int version, int ultimaBola, int idPartida) {
		this.fase = fase;
		this.version = version;
		this.ultimaBola = ultimaBola;
		this.idPartida = idPartida;
	}

	/* OBTENER FASE */
	public String getFase() { return fase; }

	/* ESTABLECER FASE */
	public void setFase(String fase) { this.fase = fase; }

	/* OBTENER VERSIÓN */
	public int getVersion() { return version; }

	/* ESTABLECER VERSIÓN */
	public void setVersion(int version) { this.version = version; }

	/* OBTENER ÚLTIMA BOLA */
	public int getUltimaBola() { return ultimaBola; }

	/* ESTABLECER ÚLTIMA BOLA */
	public void setUltimaBola(int ultimaBola) { this.ultimaBola = ultimaBola; }

	/* OBTENER ID DE PARTIDA */
	public int getIdPartida() { return idPartida; }

	/* ESTABLECER ID DE PARTIDA */
	public void setIdPartida(int idPartida) { this.idPartida = idPartida; }

	/* INCREMENTAR VERSIÓN EN 1 */
	public void incrementarVersion() {
		this.version++;
	}

	/* CONVERTIR ESTADO A LÍNEAS DE TEXTO */
	public String[] aLineas() {
		String[] lineas = new String[NUM_LINEAS];
		lineas[0] = "fase=" + fase;
		lineas[1] = "version=" + version;
		lineas[2] = "ultimaBola=" + ultimaBola;
		lineas[3] = "idPartida=" + idPartida;
		return lineas;
	}

	/* RECONSTRUIR ESTADO DESDE LÍNEAS */
	public static Estado deLineas(String[] lineas) {
		Estado estado = new Estado();

		for (int i = 0; i < lineas.length; i++) {
			if (lineas[i] == null) {
				continue;
			}

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