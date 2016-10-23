package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param   word    the integer to transfer.
     */
    public void speak(int word) {
        //one thread has something to say.  
        communicatorLock.acquire();
        //if nobody is listening or if something has already been said, speaker goes to sleep
        while (listening == 0 || message != null) {
            speaker.sleep();
        }
        //make a new message and wake up someone to listen
        message = new Integer(word);
        listener.wake();
        communicatorLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return  the integer transferred.
     */
    public int listen() {
        //there's another thread listening
        communicatorLock.acquire();
        listening++;
        //if there's not a message for the listener to hear, wake up a speaker and then immediately goes to sleep
        while (message == null) {
            speaker.wake();
            listener.sleep();
        }
        //this thread hears the message and there is one fewer listener
        int receivedMessage = message.intValue();
        message = null;
        listening--;
        communicatorLock.release();
        return receivedMessage;
    }

    

    private Integer message = null;
    private int listening = 0;
    private Lock communicatorLock = new Lock();
    private Condition2 listener = new Condition2(communicatorLock);
    private Condition2 speaker = new Condition2(communicatorLock);
}