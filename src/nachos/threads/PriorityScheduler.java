package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
	
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
		    thread.schedulingState = new ThreadState(thread);
		
		return (ThreadState) thread.schedulingState;
	    }
	
	    /**
	     * A <tt>ThreadQueue</tt> that sorts threads by priority.
	     */
	    protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
		    this.transferPriority = transferPriority;
		}
	
		public void waitForAccess(KThread thread) {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    getThreadState(thread).waitForAccess(this);
		}
	
		public void acquire(KThread thread) {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    getThreadState(thread).acquire(this);
		}
	
		public KThread nextThread() {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    //primero saco el thread anterior y todos sus problemas
		    if (lockingThread != null){
		    	if (transferPriority){//si esta permitido transferir prioridad aqui
		    		getThreadState(lockingThread).removeEffectivePriority(index_lockingThread);
		    		index_lockingThread = -1;
		    	}
		    }
		    if (waitQueue.isEmpty()) return null;
		    acquire(waitQueue.poll().thread);
			return lockingThread;
		}
	
		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		protected ThreadState pickNextThread() {
		    return waitQueue.peek();
		}
		
		public void print() {
		    Lib.assertTrue(Machine.interrupt().disabled());
		    // implement me (if you want)
		}
		
		public KThread getLockingThread(){
			return lockingThread;
		}
	
		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		//priority queue de java de tipo thread state en el cual se ordenan los thread a su ingreso
		protected java.util.PriorityQueue<ThreadState> waitQueue = new java.util.PriorityQueue<ThreadState>(8,new MyComparator());
		protected KThread lockingThread = null;
		protected int index_lockingThread = -1;
	    }
	    
    protected class MyComparator implements Comparator<ThreadState> {
		
		@Override
		public int compare(ThreadState ts1, ThreadState ts2) {
			//prioridad efectiva
			int diff = ts1.getEffectivePriority() - ts2.getEffectivePriority();
			if (diff != 0 ) return -1 * diff;//el que tiene mayor prioridad va primero
			
			//si tienen misma prioridad, se retorna el de mayor tiempo en cola
			return Long.signum(ts1.getInitTime()-ts2.getInitTime());			
		}
	}

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
    	
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;
	    this.init_time= Machine.timer().getTime();
	    setPriority(priorityDefault);
	    this.uniqueID = (new Random().nextFloat())+"";
	}
	
	public String toString(){
		return uniqueID+" - "+thread.getName()+" - priority "+priority+" - ePriority "+this.getEffectivePriority();
	}
	
	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
		return Collections.max(effectivePriority).intValue();
	}
	
	public void removeEffectivePriority(int index){
		this.effectivePriority.remove(index);
	}
	
	public void setEffectivePriority(int effectivePriority, int index){
		if (this.effectivePriority.get(index).intValue() < effectivePriority)
			this.effectivePriority.set(index, effectivePriority);
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    //if (this.priority == priority)

	    
	    this.priority = priority;
	    
	    if (effectivePriority.size() > 0)
	    	effectivePriority.set(0, priority);
	    else
	    	effectivePriority.add(priority);
	    
	    for (PriorityQueue pq: this.waitingQueues){
	    	if (pq.transferPriority){
	    		if(pq.lockingThread != null){
	    			getThreadState(pq.lockingThread).setEffectivePriority(getEffectivePriority(), pq.index_lockingThread);
	    		}
	    	}
	    }
	    
	}
	
	public boolean equals(Object o){
		return ((ThreadState) o).uniqueID.equals(this.uniqueID);
	}
	
	public int hashCode(){
		return Integer.valueOf(uniqueID.substring(1));
	}
	
	

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
		if (waitQueue.lockingThread != null)
			if (waitQueue.lockingThread.equals(this.thread)){
				if (waitQueue.transferPriority)
					getThreadState(waitQueue.lockingThread).removeEffectivePriority(waitQueue.index_lockingThread);
				
				waitQueue.lockingThread = null;
				waitQueue.index_lockingThread = -1;
				
			}
		waitQueue.waitQueue.offer(this);
		this.waitingQueues.add(waitQueue);
	    
		if (waitQueue.transferPriority)
	    	if (waitQueue.lockingThread != null)
	    		getThreadState(waitQueue.lockingThread).setEffectivePriority(this.getEffectivePriority(), waitQueue.index_lockingThread); 
	}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {
		/*if (waitQueue.lockingThread != null){
	    	if (waitQueue.transferPriority){//si esta permitido transferir prioridad aqui
	    		getThreadState(waitQueue.lockingThread).removeEffectivePriority(waitQueue.index_lockingThread);
	    		waitQueue.index_lockingThread = -1;
	    	}
	    }*/
		waitQueue.lockingThread = null;
		waitQueue.waitQueue.remove(this);
		this.waitingQueues.remove(waitQueue);
		waitQueue.lockingThread = this.thread;
		if (waitQueue.transferPriority){//si se puede priority inversion
			this.effectivePriority.add(getEffectivePriority());
			waitQueue.index_lockingThread = this.effectivePriority.size()-1;
		}
	}	
	
	public long getInitTime(){
		return init_time;
	}
	
	public void setInitTime(long time){
		this.init_time = time;
	}

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	protected ArrayList<Integer> effectivePriority = new ArrayList<Integer>();
	protected long init_time;
	protected String uniqueID = null;
	protected LinkedHashSet<PriorityQueue> waitingQueues = new LinkedHashSet<PriorityQueue>(); 
    }
}
