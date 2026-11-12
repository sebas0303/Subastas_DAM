package ejercicio2;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Hilo postor que participa en todos los lotes de {@link SalaSubastas} con un
 * presupuesto máximo independiente por cada lote.
 *
 * @author Sebastián Arquinigo
 * @version 1.0
 */
public class PostorSala implements Runnable {

    private final SalaSubastas sala;
    private final int presupuestoPorLote;
    private final int incrementoMax;
    private final int pausaMaxMs;

    /**
     * Crea un postor para la sala de subastas.
     *
     * @param sala               monitor compartido de la sala
     * @param presupuestoPorLote presupuesto máximo disponible para cada lote
     * @param incrementoMax      incremento aleatorio máximo sobre el precio actual
     * @param pausaMaxMs         pausa máxima entre intentos de puja (ms)
     */
    public PostorSala(SalaSubastas sala, int presupuestoPorLote, int incrementoMax, int pausaMaxMs) {
        this.sala = sala;
        this.presupuestoPorLote = presupuestoPorLote;
        this.incrementoMax = incrementoMax;
        this.pausaMaxMs = pausaMaxMs;
    }

    @Override
    public void run() {
        String nombre = Thread.currentThread().getName();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Espera sin consumir CPU hasta que salga un lote o cierre la sala
                String loteActual = sala.esperarLote();
                if (loteActual == null) {
                    break; // Sala cerrada
                }

                while (!Thread.currentThread().isInterrupted()) {
                    SalaSubastas.EstadoLote estado = sala.getEstadoLoteActual();
                    if (!estado.abierto() || !loteActual.equals(estado.nombreLote())) {
                        break; // Este lote ya se cerró o adjudicó
                    }

                    // Un postor no puede superar su propia puja
                    if (nombre.equals(estado.mejorPostor())) {
                        Thread.sleep(random.nextInt(150, pausaMaxMs + 150));
                        continue;
                    }

                    int oferta = estado.precioActual() + random.nextInt(10, incrementoMax + 1);
                    if (oferta > presupuestoPorLote) {
                        System.out.printf("[%s] Se retira de '%s' (oferta %d € > presupuesto %d €)%n",
                                nombre, loteActual, oferta, presupuestoPorLote);
                        sala.esperarCierreLote(loteActual);
                        break;
                    }

                    sala.pujar(nombre, loteActual, oferta);
                    Thread.sleep(random.nextInt(150, pausaMaxMs + 150));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
