package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

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
* @paramtransferPriority<tt>true</tt> if this queue should
*transfer priority from waiting threads
*to the owning thread.
* @returna new priority thread queue.
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
* @paramthreadthe thread whose scheduling state to return.
* @returnthe scheduling state of the specified thread.
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
	
	//si existe un thread dueño del recurso, lo libero
	if (lockHolder != null) 
		getThreadState(lockHolder).release(this);
	
	ThreadState temp = this.pickNextThread();
	
	if (temp == null) return null;
	
	return temp.thread;
}

/**
 * Return the next thread that <tt>nextThread()</tt> would return,
 * without modifying the state of this queue. 
 * 
 * @returnthe next thread that <tt>nextThread()</tt> would
 *return. 
 */ 
 protected ThreadState pickNextThread() {
	
	 //si no existe un thread en espera, se retorna nulo
	if (waitQueue.isEmpty()) return null;
	
	int ref = 0;//variable para revisar el thread con mayor prioridad 
	int actual_ref;
	ThreadState next = null;
	ThreadState temp; 
	for (Iterator i = waitQueue.iterator(); i.hasNext();){
		temp = (ThreadState) i.next();
		actual_ref = temp.tsWeight(this.transferPriority);
		if (actual_ref > ref)
		{
		ref = actual_ref;
		next = temp;
		}
	}
	
	if (ref == 0){
		ThreadState tempThreadState1; 
		for (Iterator i = waitQueue.iterator(); i.hasNext();){
			tempThreadState1 = (ThreadState) i.next();
			if (this.transferPriority)
				tempThreadState1.setCounter( tempThreadState1.getEffectivePriority());
			else
				tempThreadState1.setCounter( tempThreadState1.getPriority() );
		}
		
		ref = 0;
		next = null;
		for(Iterator i = waitQueue.iterator(); i.hasNext();){
			temp = (ThreadState) i.next();
			actual_ref = temp.tsWeight(this.transferPriority);
			if (actual_ref > ref){
				ref = actual_ref;
				next = temp;
			}
		} 
		if (ref == 0)	return null;

	}
	
	return next; 
}

public boolean decreaseCounter(KThread thread)
 {
 return getThreadState(thread).decreaseCounter(); 
 }


public void print() { 
Lib.assertTrue(Machine.interrupt().disabled()); 
// implement me (if you want) 
}

/**
 * <tt>true</tt> if this queue should transfer priority from waiting
 * threads to the owning thread. 
 */ 
 public boolean transferPriority; 
 public LinkedList waitQueue = new LinkedList(); 
public KThread lockHolder = null;
 }

/**
* The scheduling state of a thread. This should include the thread's
* priority, its effective priority, any objects it owns, and the queue
* it's waiting for, if any.
*
* @seenachos.threads.KThread#schedulingState
*/
protected class ThreadState {
 /** 
 * Allocate a new <tt>ThreadState</tt> object and associate it with the
 * specified thread. 
 * 
 * @paramthreadthe thread this state belongs to. 
 */ 
 public ThreadState(KThread thread) { 
 this.thread = thread; 
 
 setPriority(priorityDefault); 
 }

/**
 * Return the priority of the associated thread.
 * 
 * @returnthe priority of the associated thread. 
 */ 
 public int getPriority() { 
 return priority; 
 }

/**
 * Return the effective priority of the associated thread.
 * 
 * @returnthe effective priority of the associated thread. 
 */ 
 public int getEffectivePriority() {
	 //seteamos effective priority a la prioridad actual del thread
	 int effectivePriority = this.priority;
	 
	 //revisamos todas las colas en las que se encuentra esperando nuestro thread
	 for (Iterator i = myQueue.iterator(); i.hasNext();) { 
		PriorityQueue queue = (PriorityQueue) i.next();
		
		//si nuesto thread esta bloqueando actualmente el recurso
		if (queue.lockHolder == this.thread){
			//revisamos todos los threads esperando por este recurso
			//de los que estan esperando, tomamos la prioridad mayor
			for (Iterator j = queue.waitQueue.iterator(); i.hasNext(); ) { 
				ThreadState ts = (ThreadState) i.next();
				//la prioridad efectiva será la mayor priodidad de los threads
				//en todas las colas de espera donde nuestro thread este bloqueando el recurso
				if(ts.getPriority() > effectivePriority)
					effectivePriority = ts.getPriority(); 
			}
		}
	 } 
	 return effectivePriority; 
}

/**
 * Set the priority of the associated thread to the specified value.
 * 
 * @paramprioritythe new priority. 
 */ 
 public void setPriority(int priority) { 
	 if (this.priority == priority) 
	 return; 
	 
	 this.priority = priority; 
 }

/**
 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
 * the associated thread) is invoked on the specified priority queue. 
 * The associated thread is therefore waiting for access to the 
 * resource guarded by <tt>waitQueue</tt>. This method is only called
 * if the associated thread cannot immediately obtain access. 
 * 
 * @paramwaitQueuethe queue that the associated thread is 
 *now waiting on. 
 * 
 * @seenachos.threads.ThreadQueue#waitForAccess 
 */ 
 public void waitForAccess(PriorityQueue waitQueue) { 
	
	 if (this.counter == -1) { 
		 if (waitQueue.transferPriority) 
			 this.counter = this.getEffectivePriority(); 
		 else 
			 this.counter = this.priority; 
	 } 
	 waitQueue.waitQueue.add(this); 
	 this.myQueue.add((PriorityQueue)waitQueue); 
 }

/**
 * Called when the associated thread has acquired access to whatever is
 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
 * <tt>thread</tt> is the associated thread), or as a result of
 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
 * 
 * @seenachos.threads.ThreadQueue#acquire 
 * @seenachos.threads.ThreadQueue#nextThread 
 */ 
 public void acquire(PriorityQueue waitQueue) 
 { 

 Lib.assertTrue(Machine.interrupt().disabled()); 
 Lib.assertTrue(waitQueue.waitQueue.isEmpty()); 
 this.myQueue.add((PriorityQueue) waitQueue); 
 if(waitQueue.transferPriority) 
waitQueue.lockHolder = this.thread;
 } 
 
 
 public int tsWeight(boolean transferPriority) { 
	 if (this.counter == 0) return 0; 
	 
	 if (transferPriority) return this.counter + this.getEffectivePriority(); 
	 
	 return this.counter + this.priority; 
 } 
 
 public void release(PriorityQueue waitQueue) 
 { 
 this.myQueue.remove(waitQueue); 
 waitQueue.lockHolder = null; 
 } 
 
 public void setCounter(int newCounter) 
 { 
if (newCounter < 0)
return;
 this.counter = newCounter; 
 } 
 
 public int getCounter() 
 { 
 return this.counter; 
 } 
 
 public boolean decreaseCounter() { 
	 if (this.counter == 0) return false; 
	 else { 
		 this.counter = this.counter - 1; 
		 if (this.counter == 0) return false;  
	 } 
	 return true; 
 } 


/** The thread with which this object is associated. */ 
 protected KThread thread;
 /** The priority of the associated thread. */ 
 protected int priority; 
 protected int counter = -1; 
 protected LinkedList myQueue = new LinkedList(); 
}
}