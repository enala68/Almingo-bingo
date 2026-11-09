package comun;

/* PREGUNTA DE SOSTENIBILIDAD */
public class Pregunta {

	private final String enunciado;
	private final String opcionA;
	private final String opcionB;
	private final String opcionC;
	private final String correcta; 

	public Pregunta(String enunciado, String opcionA, String opcionB, String opcionC, String correcta) {
		this.enunciado = enunciado;
		this.opcionA = opcionA;
		this.opcionB = opcionB;
		this.opcionC = opcionC;
		this.correcta = correcta.trim().toUpperCase();
	}

	/* OBTENER ENUNCIADO */
	public String getEnunciado() {
		return enunciado;
	}

	/* OBTENER OPCIÓN A */
	public String getOpcionA() {
		return opcionA;
	}

	/* OBTENER OPCIÓN B */
	public String getOpcionB() {
		return opcionB;
	}

	/* OBTENER OPCIÓN C */
	public String getOpcionC() {
		return opcionC;
	}

	/* OBTENER OPCIÓN CORRECTA */
	public String getCorrecta() {
		return correcta;
	}

	/* COMPROBAR SI LA LETRA ES CORRECTA */
	public boolean esCorrecta(String letra) {
		if (letra == null) {
			return false;
		}
		return correcta.equals(letra.trim().toUpperCase());
	}

	/* CONVERTIR PREGUNTA A LÍNEAS PARA EL JUGADOR */
	public String[] aLineasParaJugador() {
		return new String[] {
			enunciado,
			"A) " + opcionA,
			"B) " + opcionB,
			"C) " + opcionC
		};
	}
}