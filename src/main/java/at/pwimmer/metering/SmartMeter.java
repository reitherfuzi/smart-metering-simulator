package at.pwimmer.metering;

import at.pwimmer.metering.tasks.MeterSimulator;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmartMeter {
    private static final int DEFAULT_PORT = 10038;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        UUID uuid = UUID.randomUUID();

        if(args.length == 1) {
            port = Integer.parseInt(args[0]);
        }
        else if(args.length == 2) {
            uuid = UUID.fromString(args[1]);
        }

        System.out.println("##########################################");
        System.out.println("##        Smart-Meter-Simulator         ##");
        System.out.println("## ------------------------------------ ##");
        System.out.println("## " + uuid.toString() + " ##");
        System.out.println("##########################################");

        final ExecutorService exService = Executors.newSingleThreadExecutor();
        final MeterSimulator simulator = new MeterSimulator(uuid, port);
        exService.execute(simulator);

        Runtime.getRuntime().addShutdownHook(new Thread(exService::shutdownNow));
        Runtime.getRuntime().addShutdownHook(new Thread(simulator::shutdown));
    }
}
