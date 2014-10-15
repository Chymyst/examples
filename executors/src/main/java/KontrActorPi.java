import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.kontraktor.util.Hoarde;
import org.nustaq.kontraktor.util.Log;

import javax.security.auth.login.LoginContext;
import java.util.concurrent.CountDownLatch;

/**
 * Created by ruedi on 09.10.14.
 */
public class KontrActorPi {

    public static class PiActor extends Actor<PiActor> {
        public void $calculatePiFor(int start, int nrOfElements, Adder adder) {
            double acc = 0.0;
            for (int i = start * nrOfElements; i <= ((start + 1) * nrOfElements - 1); i++) {
                acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
            }
            adder.$add(acc);
        }
    }

    public static class Adder extends Actor<Adder> {
        double pi = 0;
        public void $add(double d) {
            pi += d;
        }
        public void $printPi() {
            System.out.println("PI: "+pi);
        }
    }

    // blocking utility method
    static long calcPi(final int numMessages, int step, final int numActors) throws InterruptedException {
        long tim = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(1);
        Adder adder = Actors.AsActor(Adder.class,70000);

        Hoarde<PiActor> hoarde = new Hoarde<>(numActors, PiActor.class);
        for ( int i = 0; i < numMessages; i+=numActors) {
            int finalI = i;
            hoarde.each( (pia,index) -> pia.$calculatePiFor(finalI + index, step, adder) );
        }
        // trigger latch once all actors have finished
        Actors.yield( hoarde.map( (pia,i) -> pia.$sync() ) ).then( (r,e) -> latch.countDown() );
        latch.await();
        long duration = System.currentTimeMillis() - tim;
        adder.$printPi();

        // clean up
        hoarde.each( (pia) -> pia.$stop() );
        adder.$stop();

        System.out.println("TIM ("+numActors+")"+duration);
        return duration;
    }

    public static void main( String arg[] ) throws InterruptedException {
        final int numMessages = 1000000;
        final int step = 100;
        final int MAX_ACT = 8;
        String results[] = new String[MAX_ACT];

        Log.Lg.$setSeverity(Log.ERROR);
        ElasticScheduler.DEFQSIZE = 60000;

        for ( int numActors = 1; numActors <= MAX_ACT; numActors+=1 ) {
            long sum = 0;
            for ( int ii=0; ii < 20; ii++) {
                long res = calcPi(numMessages, step, numActors);
                if ( ii >= 10 ) {
                    sum+=res;
                }
            }
            results[numActors-1] = "average "+numActors+" threads : "+sum/10;
        }

        for (int i = 0; i < results.length; i++) {
            String result = results[i];
            System.out.println(result);
        }
    }
}
