package ejercicio2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Monitor seguro para hilos que gestiona una sala de subastas secuencial con
 * múltiples lotes. Encapsula toda la sincronización mediante un {@link ReentrantLock}
 * y dos objetos {@link Condition}.
 * <p>
 * <b>Seguridad para hilos:</b> Segura ({@code ThreadSafe}). Todos los atributos
 * de estado de la sala y del lote activo están protegidos por {@code cerrojo}:
 * <ul>
 *   <li>{@code salaCerrada}, {@code loteAbierto}, {@code nombreLoteActual},
 *       {@code precioActual}, {@code mejorPostorActual}, {@code pujasLoteActual},
 *       {@code nuevaPujaRecibida} y {@code historialResultados}.</li>
 * </ul>
 * <p>
 * <b>Condiciones de espera:</b>
 * <ul>
 *   <li>{@code hayLoteAbierto}: donde esperan los postores sin consumir CPU cuando
 *       no hay lote abierto o cuando ya han alcanzado su presupuesto en el lote actual.</li>
 *   <li>{@code actividadPuja}: donde espera el subastador con tiempo máximo
 *       ({@link Condition#await(long, TimeUnit)}), reiniciando la cuenta cada vez
 *       que se acepta una puja.</li>
 * </ul>
 * <p>
 * <b>Bloqueos e interrupciones:</b> Los métodos {@link #esperarLote()},
 * {@link #esperarCierreLote(String)} y {@link #esperarYAdjudicar(long)} suspenden
 * al hilo llamante y lanzan {@link InterruptedException} si el hilo es interrumpido.
 *
 * @author Sebastián Arquinigo
 * @version 1.0
 */
public class SalaSubastas {

    /**
     * Instantánea inmutable del estado del lote activo en un instante dado.
     *
     * @param nombreLote   nombre del lote actual (o {@code null} si no hay ninguno)
     * @param precioActual importe más alto actual
     * @param mejorPostor  postor que va ganando (o {@code null} si nadie ha pujado)
     * @param abierto      indica si el lote sigue admitiendo pujas
     */
    public record EstadoLote(
            String nombreLote,
            int precioActual,
            String mejorPostor,
            boolean abierto) { }

    /**
     * Resultado final tras cerrar y adjudicar un lote.
     *
     * @param nombreLote   nombre del lote subastado
     * @param ganador      nombre del postor ganador, o {@code null} si quedó desierto
     * @param importeFinal importe de adjudicación (o precio de salida si quedó desierto)
     * @param totalPujas   número de pujas aceptadas para ese lote
     */
    public record ResultadoLote(
            String nombreLote,
            String ganador,
            int importeFinal,
            int totalPujas) { }

    private final ReentrantLock cerrojo = new ReentrantLock();
    private final Condition hayLoteAbierto = cerrojo.newCondition();
    private final Condition actividadPuja = cerrojo.newCondition();

    private boolean salaCerrada = false;
    private boolean loteAbierto = false;
    private String nombreLoteActual = null;
    private int precioActual = 0;
    private String mejorPostorActual = null;
    private int pujasLoteActual = 0;
    private boolean nuevaPujaRecibida = false;

    private final List<ResultadoLote> historialResultados = new ArrayList<>();

    /**
     * Abre un nuevo lote para subasta y despierta a todos los postores en espera.
     *
     * @param nombreLote   descripción o título del lote
     * @param precioSalida precio mínimo inicial en euros
     */
    public void abrirLote(String nombreLote, int precioSalida) {
        cerrojo.lock();
        try {
            if (salaCerrada) {
                return;
            }
            this.nombreLoteActual = nombreLote;
            this.precioActual = precioSalida;
            this.mejorPostorActual = null;
            this.pujasLoteActual = 0;
            this.nuevaPujaRecibida = false;
            this.loteAbierto = true;

            System.out.printf("%n[%s] >>> ABIERTO: %s (Salida: %d €) <<<%n",
                    Thread.currentThread().getName(), nombreLote, precioSalida);
            hayLoteAbierto.signalAll();
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * El subastador espera hasta que transcurra {@code tiempoSinPujasMs} sin que
     * entre ninguna puja nueva. Cada puja aceptada reinicia la cuenta atrás.
     *
     * @param tiempoSinPujasMs tiempo máximo de inactividad antes de adjudicar (ms)
     * @return resultado definitivo del lote adjudicado
     * @throws InterruptedException si el hilo subastador es interrumpido durante la espera
     */
    public ResultadoLote esperarYAdjudicar(long tiempoSinPujasMs) throws InterruptedException {
        cerrojo.lock();
        try {
            while (loteAbierto && !salaCerrada) {
                nuevaPujaRecibida = false;
                boolean despertadoAntes = actividadPuja.await(tiempoSinPujasMs, TimeUnit.MILLISECONDS);
                if (!despertadoAntes && !nuevaPujaRecibida) {
                    // Expiró el tiempo máximo sin nuevas pujas
                    break;
                }
            }

            loteAbierto = false;
            ResultadoLote resultado = new ResultadoLote(
                    nombreLoteActual, mejorPostorActual, precioActual, pujasLoteActual);
            historialResultados.add(resultado);

            if (resultado.ganador() == null) {
                System.out.printf("[%s] <<< ADJUDICACIÓN: %s queda DESIERTO >>>%n",
                        Thread.currentThread().getName(), resultado.nombreLote());
            } else {
                System.out.printf("[%s] <<< ADJUDICACIÓN: %s vendido a %s por %d € (%d pujas) >>>%n",
                        Thread.currentThread().getName(), resultado.nombreLote(),
                        resultado.ganador(), resultado.importeFinal(), resultado.totalPujas());
            }
            return resultado;
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Bloquea al hilo postor sin consumir CPU hasta que haya un lote abierto
     * o se cierre la sala de subastas.
     *
     * @return nombre del lote abierto, o {@code null} si la sala se ha cerrado
     * @throws InterruptedException si el postor es interrumpido mientras espera
     */
    public String esperarLote() throws InterruptedException {
        cerrojo.lock();
        try {
            while (!loteAbierto && !salaCerrada) {
                hayLoteAbierto.await();
            }
            return salaCerrada ? null : nombreLoteActual;
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Hace esperar al postor sin consumir CPU hasta que termine el lote indicado
     * (por ejemplo, si ya superó su presupuesto para ese lote) o cierre la sala.
     *
     * @param loteActual nombre del lote en curso del que el postor se retira
     * @throws InterruptedException si el postor es interrumpido mientras espera
     */
    public void esperarCierreLote(String loteActual) throws InterruptedException {
        cerrojo.lock();
        try {
            while (loteAbierto && loteActual.equals(nombreLoteActual) && !salaCerrada) {
                hayLoteAbierto.await();
            }
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Devuelve una foto consistente del estado actual del lote.
     *
     * @return {@link EstadoLote} actual
     */
    public EstadoLote getEstadoLoteActual() {
        cerrojo.lock();
        try {
            return new EstadoLote(nombreLoteActual, precioActual, mejorPostorActual, loteAbierto && !salaCerrada);
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Intenta registrar una puja sobre el lote indicado.
     * Rechaza la oferta si la sala o el lote están cerrados, si pertenece a un lote
     * anterior ya adjudicado, si el postor intenta superar su propia puja o si el
     * importe no supera el precio vigente.
     *
     * @param postor       nombre del postor
     * @param loteObjetivo nombre del lote por el que cree estar pujando
     * @param importe      cantidad ofrecida en euros
     * @return {@code true} si la puja es válida y aceptada; {@code false} en caso contrario
     */
    public boolean pujar(String postor, String loteObjetivo, int importe) {
        cerrojo.lock();
        try {
            if (salaCerrada || !loteAbierto) {
                return false;
            }
            if (loteObjetivo == null || !loteObjetivo.equals(nombreLoteActual)) {
                return false; // Puja tardía para un lote ya adjudicado
            }
            if (postor.equals(mejorPostorActual)) {
                return false; // Un postor no puede superar su propia puja
            }
            if (importe <= precioActual) {
                return false;
            }

            precioActual = importe;
            mejorPostorActual = postor;
            pujasLoteActual++;
            nuevaPujaRecibida = true;

            System.out.printf("[%s] Puja aceptada en '%s': %d €%n",
                    postor, nombreLoteActual, importe);

            // Avisamos al subastador para que reinicie la cuenta atrás
            actividadPuja.signal();
            return true;
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Cierra definitivamente la sala de subastas y despierta a todos los hilos
     * que pudieran estar esperando en las condiciones.
     */
    public void cerrar() {
        cerrojo.lock();
        try {
            loteAbierto = false;
            salaCerrada = true;
            System.out.printf("%n[%s] === SALA DE SUBASTAS CERRADA ===%n",
                    Thread.currentThread().getName());
            hayLoteAbierto.signalAll();
            actividadPuja.signalAll();
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Devuelve una copia inmutable del historial de lotes adjudicados.
     *
     * @return lista con los resultados de todos los lotes
     */
    public List<ResultadoLote> getHistorialResultados() {
        cerrojo.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(historialResultados));
        } finally {
            cerrojo.unlock();
        }
    }
}
