package at.pwimmer.metering.tasks;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MeterSimulator implements Runnable {
    private static final int DEFAULT_MAX_CLIENTS = 5;

    private final UUID uuid;
    private final int port;
    private final int maxClients;

    private volatile boolean running = true;

    public MeterSimulator(UUID meterUuid, int serverPort, int maxClients) {
        this.uuid = meterUuid;
        this.port = serverPort;
        this.maxClients = maxClients;
    }

    public MeterSimulator(UUID uuid, int serverPort) {
        this.uuid = uuid;
        this.port = serverPort;
        this.maxClients = DEFAULT_MAX_CLIENTS;
    }

    @Override
    public void run() {
        final ScheduledExecutorService exService = Executors.newScheduledThreadPool(maxClients);
        List<ExporterTask> exporters = new ArrayList<>();

        try(ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout(10000);

            System.out.println("The Meter-Simulator awaits clients on [0.0.0.0:"+port+"]");

            while(running && !Thread.currentThread().isInterrupted()) {
                if(exporters.size() < maxClients) {
                    acceptClient(exService, exporters, serverSocket);
                }
                else {
                    TimeUnit.MILLISECONDS.sleep(500);
                }

                exporters = checkRunningExporters(exporters);
            }

            exService.shutdownNow();
            System.out.println("The Meter-Simulator stopped and will shutdown!");
        }
        catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.err.println("The Meter-Simulator has been interrupted while waiting - Termination!");
        }
        catch(IOException ex) {
            System.err.println("Failed to initialize the Server-Socket on [0.0.0.0::"+port+"]");
        }
        finally {
            terminateAll(exService, exporters);
        }
    }

    private List<ExporterTask> checkRunningExporters(List<ExporterTask> exporters) {
        return exporters.stream().filter(p -> p.isRunning()).collect(Collectors.toList());
    }

    public void shutdown() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    private void acceptClient(ScheduledExecutorService exService, List<ExporterTask> exporters, ServerSocket serverSocket) throws IOException {
        try {
            final Socket client = serverSocket.accept();
            final ExporterTask task = new ExporterTask(uuid, client);
            exService.scheduleAtFixedRate(task, 0, 30, TimeUnit.SECONDS);
            exporters.add(task);
            System.out.println("A new client has connected via " + client.toString());
        }
        catch(SocketTimeoutException ex) {
            // DO NOTHING
        }
    }

    private void terminateAll(ScheduledExecutorService exService, List<ExporterTask> exporters) {
        if(exService != null)       exService.shutdown();
        if(exporters != null)       exporters.stream().forEach(ExporterTask::shutdown);

        if(exService != null) {
            try {
                if(!exService.awaitTermination(4L, TimeUnit.SECONDS)) {
                    exService.shutdownNow();
                }
            }
            catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
                exService.shutdownNow();
            }
        }
    }
}
