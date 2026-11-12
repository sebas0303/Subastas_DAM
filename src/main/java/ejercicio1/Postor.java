package ejercicio1;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementa {@link Callable} para devolver un resumen individual al finalizar.
 *
 * @author Sebastián Arquinigo
 * @version 1.0
 */
public class Postor implements Callable<Postor.Resultado> {

    /**
     * Registro inmutable con las estadísticas finales de un postor.
     *
     * @param nombre            identificador del postor
     * @param presupuestoMaximo presupuesto límite del postor en euros
     * @param pujasAceptadas    cantidad de pujas aceptadas por el lote
     * @param mayorPujaAceptada importe más alto aceptado a este postor (0 si ninguna)
     */
    public record Resultado(
            String nombre,
            int presupuestoMaximo,
            int pujasAceptadas,
            int mayorPujaAceptada) { }

    private final String nombre;
    private final int presupuestoMaximo;
    private final Lote lote;
    private final int incrementoMax;
    private final int pausaMaxMs;

    /**
     * Construye un nuevo postor.
     *
     * @param nombre            nombre del postor
     * @param presupuestoMaximo presupuesto máximo dispuesto a pagar
     * @param lote              recurso compartido sobre el que puja
     * @param incrementoMax     incremento aleatorio máximo sobre la puja actual
     * @param pausaMaxMs        pausa aleatoria máxima entre pujas en milisegundos
     */
    public Postor(String nombre, int presupuestoMaximo, Lote lote, int incrementoMax, int pausaMaxMs) {
        this.nombre = nombre;
        this.presupuestoMaximo = presupuestoMaximo;
        this.lote = lote;
        this.incrementoMax = incrementoMax;
        this.pausaMaxMs = pausaMaxMs;
    }

    @Override
    public Resultado call() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int misPujasAceptadas = 0;
        int miMayorPuja = 0;

        while (lote.estaAbierto() && !Thread.currentThread().isInterrupted()) {
            Lote.Puja mejorActual = lote.getMejorPuja();
            int incremento = random.nextInt(1, incrementoMax + 1);
            int oferta = mejorActual.importe() + incremento;

            if (oferta > presupuestoMaximo) {
                System.out.printf("[%s] (%s) Se retira: oferta %d € supera su presupuesto (%d €)%n",
                        Thread.currentThread().getName(), nombre, oferta, presupuestoMaximo);
                break;
            }

            if (lote.pujar(nombre, oferta)) {
                misPujasAceptadas++;
                miMayorPuja = oferta;
                System.out.printf("[%s] (%s) Puja ACEPTADA: %d €%n",
                        Thread.currentThread().getName(), nombre, oferta);
            }

            try {
                Thread.sleep(random.nextInt(5, pausaMaxMs + 1));
            } catch (InterruptedException e) {
                // Restauramos el estado de interrupción y salimos ordenadamente
                Thread.currentThread().interrupt();
                break;
            }
        }

        return new Resultado(nombre, presupuestoMaximo, misPujasAceptadas, miMayorPuja);
    }
}
