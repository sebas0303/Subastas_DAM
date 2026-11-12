package ejercicio1;

import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * <b>Seguridad para hilos:</b> Segura ({@code ThreadSafe}). Todos los atributos
 * mutables ({@code abierto}, {@code mejorPuja}, {@code pujasAceptadas}) están
 * protegidos por el cerrojo {@code cerrojo}, liberado siempre en un bloque
 * {@code finally}. Esto garantiza atomicidad en la comprobación y actualización
 * conjunta de la puja y el contador, así como visibilidad de memoria.
 * <p>
 * <b>Bloqueos e interrupciones:</b> Los métodos pueden bloquear brevemente al
 * hilo llamante mientras adquieren {@code cerrojo}, pero no realizan esperas
 * interrumpibles ni lanzan {@link InterruptedException}.
 *
 * @author Sebastián Arquinigo
 * @version 1.0
 */
public class LoteSincronizado implements Lote {

    private final ReentrantLock cerrojo = new ReentrantLock();
    private boolean abierto;
    private Puja mejorPuja;
    private int pujasAceptadas;

    /**
     * Crea un lote sincronizado con un precio inicial de salida.
     *
     * @param precioSalida precio mínimo inicial en euros
     */
    public LoteSincronizado(int precioSalida) {
        this.abierto = true;
        this.mejorPuja = new Puja(null, precioSalida);
        this.pujasAceptadas = 0;
    }

    @Override
    public boolean pujar(String postor, int importe) {
        cerrojo.lock();
        try {
            if (!abierto || importe <= mejorPuja.importe()) {
                return false;
            }
            mejorPuja = new Puja(postor, importe);
            pujasAceptadas++;
            return true;
        } finally {
            cerrojo.unlock();
        }
    }

    @Override
    public Puja getMejorPuja() {
        cerrojo.lock();
        try {
            return mejorPuja;
        } finally {
            cerrojo.unlock();
        }
    }

    @Override
    public int getPujasAceptadas() {
        cerrojo.lock();
        try {
            return pujasAceptadas;
        } finally {
            cerrojo.unlock();
        }
    }

    @Override
    public boolean estaAbierto() {
        cerrojo.lock();
        try {
            return abierto;
        } finally {
            cerrojo.unlock();
        }
    }

    @Override
    public void cerrar() {
        cerrojo.lock();
        try {
            abierto = false;
        } finally {
            cerrojo.unlock();
        }
    }
}
