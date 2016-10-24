package nachos.threads;

import nachos.machine.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;

/**
 * A scheduler that chooses threads based on their priorities.
 * 
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 * 
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fashion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
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
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer priority from
	 *            waiting threads to the owning thread.
	 * @return a new priority thread queue.
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

		Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);
		
		ThreadState ts = getThreadState(thread);
		
		//To make sure we don't do unnecessary calculation
		if (priority != ts.getPriority())
			ts.setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable(), val = true;

		KThread thread = KThread.currentThread();
		
		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			val = false;
		else
			setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return val;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable(), val = true;
		
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			val = false;
		else
			setPriority(thread, priority - 1);
		
		Machine.interrupt().restore(intStatus);
		return val;
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
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
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
			if (waitQueue.isEmpty()) return null;
			
			//se mueve el siguiente thread en cola para obtener el recurso
			acquire(waitQueue.poll().thread);

			return theThread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			return waitQueue.peek();
		}

		public void print() {
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		boolean transferPriority;
		
		//priority queue de java de tipo thread state en el cual se ordenan los thread a su ingreso
		private java.util.PriorityQueue<ThreadState> waitQueue = new java.util.PriorityQueue<ThreadState>(8,new MyComparator(this));
		
		//thread que bloquea actualmente el recurso
		private KThread theThread = null;
	}
	
	protected class MyComparator implements Comparator<ThreadState> {
		protected MyComparator(nachos.threads.PriorityScheduler.PriorityQueue pq) {
			actualQueue = pq;
		}
		
		@Override
		public int compare(ThreadState ts1, ThreadState ts2) {
			//prioridad efectiva
			int diff = ts1.getEffectivePriority() - ts2.getEffectivePriority();
			if (diff != 0 ) return -1 * diff;//el que tiene mayor prioridad va primero
			
			//si tienen misma prioridad, se retorna el de mayor tiempo en cola
			return Long.signum(ts1.waiting.get(actualQueue)-ts2.waiting.get(actualQueue));			
		}
		
		private nachos.threads.PriorityScheduler.PriorityQueue actualQueue;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread
		 *            the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			
			effectivePriority = priorityDefault;
			setPriority(priorityDefault);
		}

		
		/**
		 * metodo para eliminar un queue de la cola de queues donde ha sido posible obtener el recurso
		 * se llama si se ha terminado de utilizar el recurso
		 * 
		 * @param waitQueue
		 */
		private void detach(PriorityQueue waitQueue) {
			/**
			 * la idea es la siguiente:
			 * si se desea remover el thread actual de una cola, quiere decir que actualmente bloquea el recurso
			 * esto implica que el threa actual es "theThread", por lo que si es posible removerlo, entonces 
			 * se actualiza "waitQueue", seteando nuevamente "theThread" a null
			 */
			if (acquired.remove(waitQueue)) {
				waitQueue.theThread = null;
				/**
				 * como se quita el thread actual de la cola, es necesario volver a calcular el
				 * effectivePriority.
				 * 
				 */
				onChange();
			}
		}

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			return effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value. <p>
		 * @param priority
		 *            the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;
			
			this.priority = priority;
			//como cambia la prioridad, vuelvo a actualizar effectivePriority
			onChange();
		}

		/**
		 * metodo para actualizar la prioridad
		 * solamente se llama tras un cambio de prioridad o
		 * cambios en cascada
		 */
		protected void onChange() {
			//primero quito threadstate actual de todos los waiting para actualizarlo
			for (PriorityQueue p : waiting.keySet())
				p.waitQueue.remove(this);
			
			int temp = priority;//nueva prioridad efectiva
			//si no llega a existir un motivo por el que la prioridad efectiva
			//sea distinta de la prioridad actual, se mantendra la prioridad 
			//actual como efectiva
			
			//para todos los queue donde se ha conseguido ser "theThread"
			for (PriorityQueue p : acquired) {
				//si esta habilitado transferPriority
				if (p.transferPriority) {
					//se obtiene el thread proximo en cola
					ThreadState nextThread = p.pickNextThread();
					if (nextThread != null) {//si existe uno proximo en cola
						int ep = nextThread.getEffectivePriority();
						
						//si el siguiente en cola tiene mayor prioridad que la prioridad actual
						//entonces se cambia
						if (ep > temp)
							temp = ep;
					}
				}
			}
			
			boolean setNewEffectivePriority = temp != effectivePriority;//si ha cambiado effectivepriority
			//quiere decir que necesita hacer el cambio
			
			effectivePriority = temp;//se cambia effectivePriority por la prioridad nueva
			
			//vuelvo a agregar threadstate a los waiting queue
			//ahora ya tiene definido un nuevo effectivepriority
			for (PriorityQueue p : waiting.keySet())
				p.waitQueue.add(this);

			/*//no estoy seguro si esto esta sirviendo bien
			if (setNewEffectivePriority)//si es necesario cambiar el effective priority
				for (PriorityQueue p : waiting.keySet()) {
					if (p.transferPriority && p.theThread != null)
						getThreadState(p.theThread).onChange();
				}*/
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param priorityQ
		 *            the queue that the associated thread is now waiting on.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		void waitForAccess(PriorityQueue waitQueue) {
			if (!waiting.containsKey(waitQueue)) {
				/**
				 * hay que quitarlo de acquired si ya esta adentro
				 */
				detach(waitQueue);
				
				//se agrega a la lista de espera, con el tiempo de ingreso
				waiting.put(waitQueue, Machine.timer().getTime());
				waitQueue.waitQueue.add(this);//agrego threadstate a la lista de espera del priorityqueue
				
				if (waitQueue.theThread != null) {//si existe algun thread con el recurso de waitqueue
					//debo actualizar su tiempo efectivo porque acabo de agregar un nuevo threadstate a la cola
					getThreadState(waitQueue.theThread).onChange();
				}
			}
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		void acquire(PriorityQueue waitQueue) {
			//primero quito "theThread" actual para liberar el recurso
			if (waitQueue.theThread != null) {
				getThreadState(waitQueue.theThread).detach(waitQueue);
			}
			
			//quito el thread actual si existe en la lista de espera de priorityqueue
			waitQueue.waitQueue.remove(this);
			
			//cambio el thread actual por "theThread" para ser dueño del recurso
			waitQueue.theThread = this.thread;
			acquired.add(waitQueue);//agrego waitQueue a los acquired
			waiting.remove(waitQueue);//quito watiQueue de los queues en espera
			
			onChange();//actualizo el effectivePriority
		}

		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority, effectivePriority;
		
		//priority queues que el thread actual ha logrado capturar
		protected HashSet<nachos.threads.PriorityScheduler.PriorityQueue> acquired = new HashSet<nachos.threads.PriorityScheduler.PriorityQueue>();
		//priority queues en los que el thread actual esta esperando
		protected HashMap<nachos.threads.PriorityScheduler.PriorityQueue,Long> waiting = new HashMap<nachos.threads.PriorityScheduler.PriorityQueue,Long>();
	}
}