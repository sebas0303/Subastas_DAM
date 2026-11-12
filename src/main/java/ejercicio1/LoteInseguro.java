package ejercicio1;

/**
 * <p>
 * <b>Seguridad para hilos:</b> NO segura ({@code NotThreadSafe}). Ninguno de sus
 * atributos ({@code abierto}, {@code mejorPuja}, {@code pujasAceptadas}) está
 * protegido por cerrojos ni variables atómicas. Además, incluye llamadas a
 * {@link Thread#yield()} entre la lectura y la escritura para favorecer cambios
 * de contexto y evidenciar condiciones de carrera (check-then-act y
 * read-modify-write).
 * <p>
 * <b>Bloqueos e interrupciones:</b> Ningún método bloquea ni lanza
 * {@link InterruptedException}.
 *
 * @author Sebastián Arquinigo
 * @version 1.0
 */
public class LoteInseguro implements Lote {

    private boolean abierto;
    private Puja mejorPuja;
    private int pujasAceptadas;

    /**
     * Crea un lote abierto sin sincronización con un precio de salida inicial.
     *
     * @param precioSalida precio mínimo inicial en euros
     */
    public LoteInseguro(int precioSalida) {
        this.abierto = true;
        this.mejorPuja = new Puja(null, precioSalida);
        this.pujasAceptadas = 0;
    }

    @Override
    public boolean pujar(String postor, int importe) {
        if (!abierto) {
            return false;
        }
        Puja actual = this.mejorPuja;
        if (importe <= actual.importe()) {
            return false;
        }

        // Cedemos CPU deliberadamente para exponer la condición de carrera
        Thread.yield();

        this.mejorPuja = new Puja(postor, importe);

        Thread.yield();

        this.pujasAceptadas++;
        return true;
    }

    @Override
    public Puja getMejorPuja() {
        return mejorPuja;
    }

    @Override
    public int getPujasAceptadas() {
        return pujasAceptadas;
    }

    @Override
    public boolean estaAbierto() {
        return abierto;
    }

    @Override
    public void cerrar() {
        this.abierto = false;
    }
}
