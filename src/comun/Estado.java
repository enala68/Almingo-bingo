package comun;

import java.util.List;
import java.util.Map;

public class Estado {
	// EN QUE FASE ESTAMOS? (ESPERANDO, JUGANDO, VALIDANDO, FINALIZADA
	// QUE VERSION ES? INT QUE INDIQUE EL ESTADO
	// CUÁL HA SIDO LA ÚLTIMA BOLA? INT (1-90)
	// ID DE LA PARTIDA ACTUAL
	
	// ATRIBUTOS
	private String fase;
	private int version;
	private int ultimaBola;
	private int idPartida;
	
	// CONSTRUCTORES
	public Estado() {
		this.fase = "ESPERANDO";
		this.version = 0;
		this.ultimaBola = 0;
		this.idPartida = 0;
	}
	
	public Estado(String fase, int version, int ultimaBola, int idPartida) {
		this.fase = fase;
		this.version = version;
		this.ultimaBola = ultimaBola;
		this.idPartida = idPartida;
	}
	
	// GETTERS AND SETTERS
	// FASE
	public void setFase(String fase) {
		this.fase = fase;
	}
	
	public String getFase() {
		return fase;
	}
	
	// VERSION
	public void setVersion(int version) {
		this.version = version;
	}
	
	public int getVersion() {
		return version;
	}
	
	// ULTIMABOLA
	public void setUltimaBola(int ultimaBola) {
		this.ultimaBola = ultimaBola;
	}
	
	public int getUltimaBola() {
		return ultimaBola;
	}
	
	// ID PARTIDA
	public void setIdPartida(int idPartida) {
		this.idPartida = idPartida;
	}
	
	public int getIdPartida() {
		return idPartida;
	}
	
	// CONVERSIONES PARA LEER Y ESCRIBIR EN EL .TXT DEL HOST
	// CONVERTIR OBJETO EN ARRAY DE LINEAS
	public String[] aLineas() {
		String[] lineas = new String[4];
		lineas[0] = "fase=" + fase;
		lineas[1] = "version=" + version;
		lineas[2] = "ultimaBola=" + ultimaBola;
		lineas[3] = "idPartida=" + idPartida;
		
		return lineas;
	}
	
	// CONVERTIR ARRAY D LINEAS EN OBJETO
	public static Estado deLineas(String[] lineas) {
		Estado estado = new Estado();
		
		for(int i = 0; i < lineas.length; i++) {
			String linea = lineas[i];
			
			// CONTROL DE ERRORES
			if (lineas[i] == null) {
				System.out.println("Error. falta elemento en Estado de la partida");
				continue;
			}
			// DIVIDIR TEXTO EN "="
			String[] partes = linea.split("=", 2);
			
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
			
		} return estado;
	}

}
