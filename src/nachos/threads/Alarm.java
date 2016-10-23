package nachos.threads;

import nachos.machine.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
	//KThread.currentThread().yield();
        long currentTime = Machine.timer().getTime();
        boolean intStatus = Machine.interrupt().disable();

        ArrayList<Integer> indices = new ArrayList<Integer>();

        if (!waitQueue.isEmpty()){
            Tupla tupla = waitQueue.get(0);
            if (tupla.getWakeTime() <= currentTime){
                KThread thread = tupla.getThread();
                if (thread != null){
                    thread.ready();
                }
                waitQueue.remove(0);
            }
            
        }

        /*
        for (int i = 0; i < waitQueue.size(); i++){
            if (waitQueue.get(i).getWakeTime() <= currentTime){
                indices.add(i);
            }
        }

        Collections.sort(indices,Collections.reverseOrder());
        System.out.println(waitQueue);

        for (int i = 0;i<indices.size(); i++){

            KThread thread = waitQueue.get(indices.get(i)).getThread();
            if (thread != null){
                thread.ready();
            }
            waitQueue.remove(indices.get(i));
        }*/

        KThread.yield();
        Machine.interrupt().restore(intStatus);

    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        long wakeTime = Machine.timer().getTime() + x;
        KThread thread = KThread.currentThread();
        Tupla tupla = new Tupla(thread, wakeTime);
        boolean intStatus = Machine.interrupt().disable();
        waitQueue.add(tupla);
        thread.sleep();
        Machine.interrupt().restore(intStatus);
    }

    private class Tupla{
        public Tupla (KThread thread, long waketime){
            this.thread = thread;
            this.waketime = waketime;
        }        
        public KThread getThread(){
            return this.thread;
        }
        public long getWakeTime(){
            return this.waketime;
        }

        public String toString(){
            return ""+this.waketime;
        }

        private KThread thread;
        private long waketime;

    }

    private ArrayList<Tupla> waitQueue = new ArrayList<Tupla>();
}
