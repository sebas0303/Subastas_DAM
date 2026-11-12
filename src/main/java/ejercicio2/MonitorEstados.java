package ejercicio2;

import java.util.List;

/**
 * Hilo demonio (daemon) que monitoriza periódicamente el estado ({@link Thread.State})
 * del subastador y de todos los postores en una única línea por muestra.
 * Al ser daemon, la JVM finalizará automáticamente cuando terminen los hilos de usuario.
 *
 * @author Sebastián Arquinigo
 * @version 1.0
 */
public class MonitorEstados implements Runnable {

    private final Thread hiloSubastador;
    private final List<Thread> hilosPostores;
    private final long intervaloMs;

    /**
     * Crea el monitor de estados de hilos.
     *
     * @param hiloSubastador referencia al hilo del subastador
     * @param hilosPostores  lista con las referencias a los hilos postores
     * @param intervaloMs    periodo de muestreo en milisegundos (ej. 700 ms)
     */
    public MonitorEstados(Thread hiloSubastador, List<Thread> hilosPostores, long intervaloMs) {
        this.hiloSubastador = hiloSubastador;
        this.hilosPostores = hilosPostores;
        this.intervaloMs = intervaloMs;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            StringBuilder sb = new StringBuilder("[Monitor] ");
            sb.append(hiloSubastador.getName()).append("=").append(hiloSubastador.getState());
            for (Thread p : hilosPostores) {
                sb.append(" | ").append(p.getName()).append("=").append(p.getState());
            }
            System.out.println(sb);

            try {
                Thread.sleep(intervaloMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
