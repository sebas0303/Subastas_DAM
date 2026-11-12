package ejercicio1;

/**
 * Representa el recurso compartido de un lote subastado de forma concurrente.
 * <p>
 * <b>Seguridad para hilos (Thread-Safety):</b> Esta interfaz define el contrato
 * de acceso concurrente al lote. Las implementaciones deben documentar si
 * garantizan la exclusión mutua y la visibilidad de memoria entre hilos:
 * <ul>
 *   <li>{@link LoteInseguro}: No es segura para hilos.</li>
 *   <li>{@link LoteSincronizado}: Segura para hilos mediante cerrojo explícito.</li>
 *   <li>{@link LoteAtomico}: Segura para hilos sin cerrojos (lock-free).</li>
 * </ul>
 *
 * @author Sebastián Arquinigo
 * @version 1.0
 */
public interface Lote {

    /**
     * Puja inmutable que asocia un postor con el importe ofrecido.
     * Al ser un {@code record}, sus campos son {@code final} y no pueden
     * modificarse tras su creación, garantizando publicación segura entre hilos.
     *
     * @param postor  nombre identificativo del postor
     * @param importe cantidad ofrecida en euros
     */
    record Puja(String postor, int importe) { }

    /**
     * Intenta registrar una nueva puja sobre el lote.
     * Solo se acepta si el lote sigue abierto y el importe supera
     * estrictamente al de la mejor puja actual.
     *
     * @param postor  nombre del postor que realiza la oferta
     * @param importe cantidad ofrecida en euros
     * @return {@code true} si la puja ha sido aceptada; {@code false} en caso contrario
     */
    boolean pujar(String postor, int importe);

    /**
     * Obtiene la mejor puja registrada hasta el momento (o la oferta de salida
     * con postor {@code null} si aún no hay pujas aceptadas).
     *
     * @return objeto inmutable {@link Puja} con la mejor oferta vigente
     */
    Puja getMejorPuja();

    /**
     * Devuelve el número total de pujas válidas aceptadas durante la subasta.
     *
     * @return contador de pujas aceptadas
     */
    int getPujasAceptadas();

    /**
     * Indica si el lote continúa abierto para recibir ofertas.
     *
     * @return {@code true} si está abierto; {@code false} si ya se ha cerrado
     */
    boolean estaAbierto();

    /**
     * Cierra el lote de forma definitiva para impedir nuevas pujas.
     */
    void cerrar();
}
