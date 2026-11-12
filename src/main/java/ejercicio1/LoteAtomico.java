package ejercicio1;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * <b>Seguridad para hilos:</b> Segura ({@code ThreadSafe}). Utiliza:
 * <ul>
 *   <li>{@link AtomicReference} sobre el objeto inmutable {@link Puja} para
 *       actualizar atómicamente postor e importe mediante bucles CAS
 *       ({@code compareAndSet}).</li>
 *   <li>{@link AtomicInteger} para contabilizar las pujas aceptadas sin pérdida
 *       de incrementos.</li>
 *   <li>{@link AtomicBoolean} para gestionar la apertura y el cierre del lote
 *       con visibilidad inmediata.</li>
 * </ul>
 * <p>
 * <b>Bloqueos e interrupciones:</b> No utiliza cerrojos bloqueantes ni lanza
 * {@link InterruptedException}.
 *
 * @author Sebastián Arquinigo
 * @version 1.0
 */
public class LoteAtomico implements Lote {

    private final AtomicBoolean abierto;
    private final AtomicReference<Puja> mejorPuja;
    private final AtomicInteger pujasAceptadas;

    /**
     * Crea un lote atómico libre de cerrojos con un precio inicial de salida.
     *
     * @param precioSalida precio mínimo inicial en euros
     */
    public LoteAtomico(int precioSalida) {
        this.abierto = new AtomicBoolean(true);
        this.mejorPuja = new AtomicReference<>(new Puja(null, precioSalida));
        this.pujasAceptadas = new AtomicInteger(0);
    }

    @Override
    public boolean pujar(String postor, int importe) {
        while (abierto.get()) {
            Puja actual = mejorPuja.get();
            if (importe <= actual.importe()) {
                return false;
            }
            Puja nueva = new Puja(postor, importe);
            if (mejorPuja.compareAndSet(actual, nueva)) {
                pujasAceptadas.incrementAndGet();
                return true;
            }
            // Si compareAndSet devuelve false, otro hilo actualizó antes; reintentamos
        }
        return false;
    }

    @Override
    public Puja getMejorPuja() {
        return mejorPuja.get();
    }

    @Override
    public int getPujasAceptadas() {
        return pujasAceptadas.get();
    }

    @Override
    public boolean estaAbierto() {
        return abierto.get();
    }

    @Override
    public void cerrar() {
        abierto.set(false);
    }
}
