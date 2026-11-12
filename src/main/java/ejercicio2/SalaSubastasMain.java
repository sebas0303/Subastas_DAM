package ejercicio2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Clase principal del Ejercicio 2: crea la sala de subastas con varios lotes,
 * lanza el hilo subastador, los hilos postores con {@link Thread.Builder} y un
 * hilo monitor {@code daemon}, y muestra el resumen final de adjudicaciones.
 *
 * @author Sebastián Arquinigo
 * @version 1.0
 */
public class SalaSubastasMain {

    private static final int NUM_POSTORES = 5;
    private static final long TIEMPO_SIN_PUJAS_MS = 1500;
    private static final long PAUSA_ENTRE_LOTES_MS = 1200;
    private static final long INTERVALO_MONITOR_MS = 700;

    public static void main(String[] args) {
        SalaSubastas sala = new SalaSubastas();

        List<Subastador.LoteConfig> lotes = List.of(
                new Subastador.LoteConfig("Lote 1 - Cuadro Óleo Siglo XIX", 150),
                new Subastador.LoteConfig("Lote 2 - Reloj de Bolsillo Oro", 200),
                new Subastador.LoteConfig("Lote 3 - Primera Edición Quijote", 500),
                new Subastador.LoteConfig("Lote 4 - Escultura Bronce Clásica", 120)
        );

        System.out.println("============================================================");
        System.out.println("          BIENVENIDOS A LA SALA DE SUBASTAS DAM");
        System.out.println("============================================================");

        // Creamos el hilo subastador con Thread.Builder y prioridad máxima
        Thread hiloSubastador = Thread.ofPlatform()
                .name("subastador")
                .priority(Thread.MAX_PRIORITY)
                .unstarted(new Subastador(sala, lotes, TIEMPO_SIN_PUJAS_MS, PAUSA_ENTRE_LOTES_MS));

        // Creamos los hilos postores numerados con Thread.Builder
        Thread.Builder.OfPlatform builderPostores = Thread.ofPlatform().name("postor-", 1);
        List<Thread> hilosPostores = new ArrayList<>();
        for (int i = 0; i < NUM_POSTORES; i++) {
            int presupuestoPorLote = ThreadLocalRandom.current().nextInt(320, 651);
            Thread tPostor = builderPostores.unstarted(
                    new PostorSala(sala, presupuestoPorLote, 45, 450));
            hilosPostores.add(tPostor);
        }

        // Creamos el hilo monitor marcado como daemon y con prioridad mínima
        Thread hiloMonitor = Thread.ofPlatform()
                .name("monitor-estados")
                .daemon(true)
                .priority(Thread.MIN_PRIORITY)
                .unstarted(new MonitorEstados(hiloSubastador, hilosPostores, INTERVALO_MONITOR_MS));

        // Iniciamos primero el monitor y los postores, y después el subastador
        hiloMonitor.start();
        for (Thread t : hilosPostores) {
            t.start();
        }
        hiloSubastador.start();

        // Esperamos a que finalicen los hilos de usuario (subastador y postores)
        try {
            hiloSubastador.join();
            for (Thread t : hilosPostores) {
                t.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[main] Hilo principal interrumpido.");
        }

        // Mostramos el resumen final de la sala
        System.out.println("================ RESUMEN FINAL DE LA SALA ================");
        for (SalaSubastas.ResultadoLote res : sala.getHistorialResultados()) {
            if (res.ganador() == null) {
                System.out.printf("- %-34s -> DESIERTO (Salida: %d €)%n",
                        res.nombreLote(), res.importeFinal());
            } else {
                System.out.printf("- %-34s -> Ganador: %-9s | Importe: %d € (%d pujas)%n",
                        res.nombreLote(), res.ganador(), res.importeFinal(), res.totalPujas());
            }
        }
        System.out.println("============================================================");
        // No detenemos manualmente hiloMonitor: al ser daemon, la JVM termina sola.
    }
}
