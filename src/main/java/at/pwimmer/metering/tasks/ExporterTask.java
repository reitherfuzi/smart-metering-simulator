package at.pwimmer.metering.tasks;

import at.pwimmer.metering.interfaces.Taskable;
import at.pwimmer.metering.models.EnergyMetric;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.UUID;

public class ExporterTask implements Taskable {
    private final ObjectMapper mapper = new ObjectMapper();

    private final UUID uuid;
    private final Socket socket;

    private volatile boolean running = true;

    public ExporterTask(UUID uuid, Socket client) {
        this.uuid = uuid;
        this.socket = client;
    }

    @Override
    public void run() {
        try {
            if(running && !Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                final byte[] data = getRandomMetrics();
                final OutputStream stream = socket.getOutputStream();

                if(data != null && stream != null) {
                    stream.write(data);
                    stream.flush();
                }
                else {
                    System.err.println("The random metrics or the Output-Stream is null!");
                    shutdown();
                }
            }
            else {
                shutdown();         // Shutdown this task, if not already done.
                socket.close();     // And also close the passed socket.
            }
        }
        catch(JsonProcessingException ex) {
            System.err.println("Failed to map the Energy-Metric into a byte-array! <" + ex.getMessage() + ">");
        }
        catch(IOException ex) {
            System.err.println("Failed to transmit the random metrics to " + socket.toString() + "<"+ ex.getMessage() + ">");
            shutdown();
        }
    }

    @Override
    public void shutdown() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private byte[] getRandomMetrics() throws JsonProcessingException {
        long total = (long) (Math.random() * 100000);
        long min = (long) (Math.random() * 10);
        long max = (long) (Math.random() * 1000);

        final EnergyMetric metric = new EnergyMetric(uuid, ZonedDateTime.now(ZoneOffset.UTC), total, min, max);
        return mapper.writeValueAsBytes(metric);
    }
}
