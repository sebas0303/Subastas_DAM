package ejercicio2;

import java.util.List;

/**
 * Hilo subastador que saca los lotes de uno en uno en {@link SalaSubastas},
 * espera a que cesen las pujas durante un tiempo fijo para adjudicarlos y
 * cierra la sala al terminar.
 *
 * @author Sebastián Arquinigo
 * @version 1.0
 */
public class Subastador implements Runnable {

    /**
     * Configuración inicial de cada lote a subastar.
     *
     * @param nombre       título del lote
     * @param precioSalida precio mínimo de salida en euros
     */
    public record LoteConfig(String nombre, int precioSalida) { }

    private final SalaSubastas sala;
    private final List<LoteConfig> lotes;
    private final long tiempoSinPujasMs;
    private final long pausaEntreLotesMs;

    /**
     * Crea el subastador de la sala.
     *
     * @param sala              monitor compartido de la sala
     * @param lotes             lista secuencial de lotes a subastar
     * @param tiempoSinPujasMs  tiempo sin pujas para adjudicar (ej. 1500 ms)
     * @param pausaEntreLotesMs tiempo de espera entre lote y lote
     */
    public Subastador(SalaSubastas sala, List<LoteConfig> lotes, long tiempoSinPujasMs, long pausaEntreLotesMs) {
        this.sala = sala;
        this.lotes = lotes;
        this.tiempoSinPujasMs = tiempoSinPujasMs;
        this.pausaEntreLotesMs = pausaEntreLotesMs;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < lotes.size(); i++) {
                LoteConfig lote = lotes.get(i);
                sala.abrirLote(lote.nombre(), lote.precioSalida());
                sala.esperarYAdjudicar(tiempoSinPujasMs);

                if (i < lotes.size() - 1) {
                    Thread.sleep(pausaEntreLotesMs);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.printf("[%s] Subastador interrumpido.%n", Thread.currentThread().getName());
        } finally {
            sala.cerrar();
        }
    }
}
