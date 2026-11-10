package comun;

import java.io.File;
import java.util.Random;
import java.util.Scanner;

/* BANCO DE PREGUNTAS DE SOSTENIBILIDAD */
public class BancoPreguntas {

	private static final int MAX_PREGUNTAS = 50;
	private static final String RUTA_PREGUNTAS = "preguntas.txt";

	private final Pregunta[] preguntas;
	private final int total;
	private final Random random = new Random();

	/* CARGAR PREGUNTAS DESDE ARCHIVO LOCAL */
	public BancoPreguntas() {
		Pregunta[] temporal = new Pregunta[MAX_PREGUNTAS];
		int contador = 0;

		File fichero = new File(RUTA_PREGUNTAS);
		if (!fichero.exists()) {
			System.err.println("[AVISO] No se encuentra preguntas.txt en: " + fichero.getAbsolutePath());
			this.preguntas = new Pregunta[0];
			this.total = 0;
			return;
		}

		try (Scanner lector = new Scanner(fichero)) {
			while (lector.hasNextLine() && contador < MAX_PREGUNTAS) {
				String enunciado = leerLineaNoVacia(lector);
				if (enunciado == null) {
					break;
				}
				String opA = leerLineaNoVacia(lector);
				String opB = leerLineaNoVacia(lector);
				String opC = leerLineaNoVacia(lector);
				String correcta = leerLineaNoVacia(lector);

				if (opA == null || opB == null || opC == null || correcta == null) {
					break;
				}

				correcta = correcta.trim().toUpperCase();
				if (!correcta.equals("A") && !correcta.equals("B") && !correcta.equals("C")) {
					System.err.println("[AVISO] Respuesta inválida en preguntas.txt: " + correcta);
					continue;
				}

				temporal[contador] = new Pregunta(enunciado, opA, opB, opC, correcta);
				contador++;
			}
		} catch (Exception e) {
			System.err.println("[AVISO] Error leyendo preguntas.txt: " + e.getMessage());
		}

		this.total = contador;
		this.preguntas = new Pregunta[total];
		for (int i = 0; i < total; i++) {
			this.preguntas[i] = temporal[i];
		}

		System.out.println("Banco de preguntas cargado: " + total + " pregunta(s).");
	}

	/* LEER LÍNEA NO VACÍA */
	private static String leerLineaNoVacia(Scanner lector) {
		while (lector.hasNextLine()) {
			String linea = lector.nextLine().trim();
			if (!linea.isEmpty()) {
				return linea;
			}
		}
		return null;
	}

	/* OBTENER TOTAL DE PREGUNTAS */
	public int getTotal() {
		return total;
	}

	/* ELEGIR PREGUNTA ALEATORIA */
	public Pregunta elegirAleatoria() {
		if (total == 0) {
			return null;
		}
		return preguntas[random.nextInt(total)];
	}
}