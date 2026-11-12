package ejercicio1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


/**
 * Clase principal del Ejercicio 1: simula la subasta concurrente de un único lote
 * y verifica la integridad de los datos tras el cierre.
 *
 * @author Sebastián Arquinigo
 * @version 1.0
 */
public class Subasta {

    /** Implementación usada si no se pasa argumento: "inseguro", "sincronizado" o "atomico". */
    private static final String IMPLEMENTACION_POR_DEFECTO = "inseguro";

    private static final int NUM_POSTORES = 8;
    private static final int PRECIO_SALIDA = 100;
    private static final int DURACION_SUBASTA_MS = 3000;
    private static final int INCREMENTO_MAX = 20;
    private static final int PAUSA_MAX_MS = 30;

    public static void main(String[] args) {
        String modo = (args.length > 0) ? args[0].trim().toLowerCase() : IMPLEMENTACION_POR_DEFECTO;
        Lote lote = crearLote(modo, PRECIO_SALIDA);

        System.out.println("============================================================");
        System.out.printf(" INICIO SUBASTA DAM | Modo: %s | Precio salida: %d €%n",
                lote.getClass().getSimpleName(), PRECIO_SALIDA);
        System.out.println("============================================================");

        ThreadFactory factory = Thread.ofPlatform().name("postor-", 1).factory();
        ExecutorService ejecutor = Executors.newFixedThreadPool(NUM_POSTORES, factory);
        List<Future<Postor.Resultado>> futuros = new ArrayList<>();

        try {
            for (int i = 1; i <= NUM_POSTORES; i++) {
                int presupuesto = ThreadLocalRandom.current().nextInt(350, 1201);
                Postor postor = new Postor("Postor-" + i, presupuesto, lote, INCREMENTO_MAX, PAUSA_MAX_MS);
                futuros.add(ejecutor.submit(postor));
            }

            // Esperamos el tiempo fijado para la subasta
            Thread.sleep(DURACION_SUBASTA_MS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[main] Subasta interrumpida prematuramente.");
        } finally {
            // Cerramos el lote e interrumpimos a los postores que estén dormidos
            lote.cerrar();
            System.out.println("[main] ¡Tiempo agotado! Lote CERRADO.");
            ejecutor.shutdownNow();
            try {
                if (!ejecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    System.err.println("[main] Algunos hilos tardaron en finalizar.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Recogemos los resultados de todos los Future
        List<Postor.Resultado> resultados = new ArrayList<>();
        for (Future<Postor.Resultado> f : futuros) {
            try {
                resultados.add(f.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                System.err.println("[main] Error en tarea postor: " + e.getMessage());
            }
        }

        mostrarResumenYComprobarIntegridad(lote, resultados);
    }

    private static Lote crearLote(String modo, int precioSalida) {
        return switch (modo) {
            case "sincronizado" -> new LoteSincronizado(precioSalida);
            case "atomico" -> new LoteAtomico(precioSalida);
            case "inseguro" -> new LoteInseguro(precioSalida);
            default -> {
                System.out.println("Modo desconocido '" + modo + "', usando LoteInseguro.");
                yield new LoteInseguro(precioSalida);
            }
        };
    }

    private static void mostrarResumenYComprobarIntegridad(Lote lote, List<Postor.Resultado> resultados) {
        Lote.Puja pujaGanadora = lote.getMejorPuja();
        int pujasEnLote = lote.getPujasAceptadas();

        System.out.println("-------------------- RESUMEN DE SUBASTA --------------------");
        if (pujasEnLote == 0 || pujaGanadora.postor() == null) {
            System.out.println("Resultado: Lote desierto.");
        } else {
            System.out.printf("Resultado: Lote adjudicado a %s por %d €%n",
                    pujaGanadora.postor(), pujaGanadora.importe());
        }

        System.out.println("Postor     | Presupuesto | Pujas aceptadas | Mayor puja");
        System.out.println("---------------------------------------------------------");

        int sumaPujasPostores = 0;
        int maxPujaPostores = 0;
        String postorMaxPuja = null;

        for (Postor.Resultado r : resultados) {
            System.out.printf("%-10s | %9d € | %15d | %8d €%n",
                    r.nombre(), r.presupuestoMaximo(), r.pujasAceptadas(), r.mayorPujaAceptada());
            sumaPujasPostores += r.pujasAceptadas();
            if (r.mayorPujaAceptada() > maxPujaPostores) {
                maxPujaPostores = r.mayorPujaAceptada();
                postorMaxPuja = r.nombre();
            }
        }

        // Comprobación de las 3 condiciones de integridad
        boolean cond1 = (sumaPujasPostores == pujasEnLote);
        boolean cond2 = (pujasEnLote == 0) || (pujaGanadora.importe() == maxPujaPostores);
        boolean cond3 = (pujasEnLote == 0) || (pujaGanadora.postor() != null && pujaGanadora.postor().equals(postorMaxPuja));

        System.out.println("---------------- COMPROBACIÓN DE INTEGRIDAD ----------------");
        System.out.printf("1. Suma pujas postores (%d) == Pujas en lote (%d): %s%n",
                sumaPujasPostores, pujasEnLote, cond1 ? "OK" : "FALLA");
        System.out.printf("2. Importe adjudicado (%d €) == Mayor puja postores (%d €): %s%n",
                pujaGanadora.importe(), maxPujaPostores, cond2 ? "OK" : "FALLA");
        System.out.printf("3. Ganador (%s) == Postor mayor puja (%s): %s%n",
                pujaGanadora.postor(), postorMaxPuja, cond3 ? "OK" : "FALLA");
        System.out.println("VEREDICTO FINAL: " + ((cond1 && cond2 && cond3) ? "OK" : "ERROR"));
        System.out.println("============================================================");
    }
}
