import java.util.Random;
import java.util.concurrent.Semaphore;

public class Main {
    // Set rider arrival mean time to 30 seconds
    static float riderArrivalMean = 30f * 1000;
    // Set bus arrival mean time to 20 minutes
    static float busArrivalMean = 20 * 60f * 1000;

    public static void main(String[] args) {
        // Create rider generator to generate riders
        riderGenerator rG = new riderGenerator(riderArrivalMean);
        // Create bus generator to generate busses
        busGenerator bG = new busGenerator(busArrivalMean);

        // Create the generator threads
        Thread rGT = new Thread(rG);
        Thread bGT = new Thread(bG);

        // Start the generator threads
        rGT.start();
        bGT.start();
    }
}

class busStop {
    /*
    busStop class for holding shared variables
     */

    // Index of the current rider
    public static int riderIndex = 1;

    // Index of the current bus
    public static int busIndex = 1;

    // Shared variable to hold the number of riders waiting to board the bus
    public static int riders = 0;

    // Mutex used to protect riders variable
    public static Semaphore mutex = new Semaphore(1);

    // Semaphore to allow riders to enter the bus stop. If more than 50 are in the bus stop, next thread can't enter
    public static Semaphore inWaiting = new Semaphore(50);

    // Semaphore to signal whether a person can board the bus(Can only board when the bus arrives at the bus stop)
    public static Semaphore busArrived = new Semaphore(0);

    // Semaphore to signal whether all waiting riders has boarded the bus
    public static Semaphore fullyBoarded = new Semaphore(0);

    // Increment rider index
    public static void riderIndexIncrement() {
        busStop.riderIndex++;
    }

    // Increment bus index
    public static void busIndexIncrement() {
        busStop.busIndex++;
    }
}

class riderGenerator implements Runnable {
    static Random random;
    float riderArrivalMean;

    riderGenerator(float riderArrivalMean) {
        this.riderArrivalMean = riderArrivalMean;
        random = new Random();
    }

    @Override
    public void run() {
        while (true) {

            // Create rider
            rider passenger = new rider(busStop.riderIndex);
            Thread passengerThread = new Thread(passenger);

            // Start rider thread
            passengerThread.start();
            try {
                // Sleep to obtain the specific inter arrival time mean
                Thread.sleep(this.calcRiderSleepTime(riderArrivalMean, random));
                busStop.riderIndexIncrement();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Calculate thread sleeping time
    private long calcRiderSleepTime(float riderArrivalMean, Random random) {
        float lambda = 1 / riderArrivalMean;
        return Math.round(-Math.log(1 - random.nextFloat()) / lambda);
    }
}

class rider implements Runnable {
    int riderIndex;

    rider(int riderIndex) {
        this.riderIndex = riderIndex;
    }

    @Override
    public void run() {
        try {
            // Enter the bus stop. Only 50 can enter the bus stop at a time
            busStop.inWaiting.acquire();
            System.out.println("Rider Number: " + this.riderIndex + " entered the bus stop");

            // If bus has not arrived, increment the number of riders waiting to board the bus
            // If a bus has arrived at the bus stop, the thread won't be able to get this mutex as the bus has it
            busStop.mutex.acquire();
            busStop.riders++;
            busStop.mutex.release();

            // Sleep till the bus arrive
            busStop.busArrived.acquire();

            // When the bus arrives one rider will be awakened. He enters the bus and allow one more rider to enter the bus
            busStop.inWaiting.release();
            board_bus();

            // No need to lock this section as only one thread can go to this area at a time
            busStop.riders--;

            if (0 == busStop.riders) {
                System.out.println("All riders got in. Bus departing...");
                // If all riders have boarded, wake the bus thread to depart
                busStop.fullyBoarded.release();
            }
            else {
                // When he boards the bus, allow one more rider to board the bus by releasing this semaphoer once
                busStop.busArrived.release();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    void board_bus() {
        System.out.println("Rider Number: " + this.riderIndex + " boarded");
    }

}

class busGenerator implements Runnable {
    static Random random;
    float busArrivalMean;

    busGenerator(float busArrivalMean) {
        this.busArrivalMean = busArrivalMean;
        random = new Random();
    }

    @Override
    public void run() {
        while (true) {

            // Generate new bus
            bus driver = new bus(busStop.busIndex);
            Thread driverThread = new Thread(driver);

            // Start bus thread
            driverThread.start();

            try {
                // Sleep to obtain the specific inter arrival time mean
                Thread.sleep(this.calcBusSleepTime(busArrivalMean, random));
                busStop.busIndexIncrement();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Calculate thread sleep time
    private long calcBusSleepTime(float busArrivalMean, Random random) {
        float lambda = 1 / busArrivalMean;
        return Math.round(-Math.log(1 - random.nextFloat()) / lambda);
    }

}

class bus implements Runnable {
    int busIndex;

    bus(int busIndex) {
        this.busIndex = busIndex;
    }

    @Override
    public void run() {
        try {

            // Allow only the riders who were there when the bus arrived to board the bus
            busStop.mutex.acquire();
            System.out.println("Bus arrived at the station. " + busStop.riders + " riders waiting to board the bus");


            if (busStop.riders > 0) {
                // Awake a rider waiting to board the bus
                busStop.busArrived.release();

                // Sleep until all waiting riders have boarded the bus
                busStop.fullyBoarded.acquire();
            }
            else {
                System.out.println("Bus leaving because 0 riders in bus stop");
            }

            // Allow other riders to wait for the next bus and depart
            busStop.mutex.release();
            depart();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void depart() {
        System.out.println("Bus " + this.busIndex + " departed");
    }

}