import java.util.concurrent.Semaphore;
import java.util.Random;

public class Main {
    static float riderArrivalMean = 30f; //20s *1000
    static float busArrivalMean = 20 * 60f; // *1000

    public static void main(String[] args) {
        riderGenerator rG = new riderGenerator(riderArrivalMean);
        busGenerator bG = new busGenerator(busArrivalMean);
        Thread rGT = new Thread(rG);
        Thread bGT = new Thread(bG);

        rGT.start();
        bGT.start();
    }
}

class BusStop {
    public static int riderIndex = 0;
    public static int riders = 0;
    public static Semaphore mutex = new Semaphore(1);
    public static Semaphore inWaiting = new Semaphore(50); // multiplex
    public static Semaphore busArrived = new Semaphore(0); // bus
    public static Semaphore fullyBoarded = new Semaphore(0); //allAboard
    public static int busIndex = 1;
}

class riderGenerator implements Runnable{
    float riderArrivalMean;
    static Random random;

    riderGenerator(float riderArrivalMean){
        this.riderArrivalMean = riderArrivalMean;
        this.random = new Random();
    }

    @Override
    public void run() {
        while (true) {
            rider passenger = new rider();
            Thread passengerThread = new Thread(passenger);
            passengerThread.start();
            try {
                //Thread.sleep(this.calcRiderSleepTime(riderArrivalMean,random));
                Thread.sleep(1000);
                BusStop.riderIndex ++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private long calcRiderSleepTime(float riderArrivalMean, Random random){
        float lambda = 1 / riderArrivalMean;
        return Math.round(-Math.log(1 - random.nextFloat()) / lambda);
    }
}

class rider implements Runnable {

    rider(){

    }

    @Override
    public void run() {
        try {
            BusStop.inWaiting.acquire();
            System.out.println("inWaiting");
                BusStop.mutex.acquire();
                    System.out.println("b mutex " + BusStop.riders);
                    BusStop.riders ++;
                    System.out.println("a mutex " + BusStop.riders);
                BusStop.mutex.release();
                //System.out.println("mutex released " + riders);
            BusStop.busArrived.acquire();
            System.out.println("busArrived signal received");
            BusStop.inWaiting.release();
            //System.out.println("inWaiting unlocked");
            board_bus();
            System.out.println("faaaaaaaaaaaaaaaaaaaaaaaaaark");
            BusStop.riders --; // only one can enter this area as busArrived is upped only once

            if (0 == BusStop.riders){
                System.out.println("All riders got in. Bus departing...");
                BusStop.fullyBoarded.release();
            }
            else {
                System.out.println("Current rider num" + Integer.toString(BusStop.riders));
                BusStop.busArrived.release();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    void board_bus(){
        System.out.println("Rider " + BusStop.riderIndex +  " boarded the bus");
    }
}

class busGenerator implements Runnable{
    static Random random;
    float busArrivalMean;

    busGenerator(float busArrivalMean){
        this.busArrivalMean = busArrivalMean;
        this.random = new Random();
    }

    @Override
    public void run() {
        while (true) {
            bus driver = new bus();
            Thread driverThread = new Thread(driver);
            driverThread.start();

            try {
                //Thread.sleep(this.calcBusSleepTime(busArrivalMean, random));
                Thread.sleep(5000);
                BusStop.busIndex ++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private long calcBusSleepTime(float busArrivalMean, Random random){
        float lambda = 1 / busArrivalMean;
        return Math.round(-Math.log(1 - random.nextFloat()) / lambda);
    }
}

class bus implements Runnable{

    bus(){

    }

    @Override
    public void run() {
        try {
            BusStop.mutex.acquire();
            System.out.println("Bus arrived at the station.Current rider count" + BusStop.riders);
            if (BusStop.riders > 0){
                System.out.println("rider count  "+ Integer.toString(BusStop.riders));

                BusStop.fullyBoarded.acquire();
            }
            else {
                System.out.println("bus leaving because 0 riders in bus");
            }
            BusStop.busArrived.release();
            BusStop.mutex.release();
            depart();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void depart(){
        System.out.println("Bus " + BusStop.busIndex + " departed");
    }
}