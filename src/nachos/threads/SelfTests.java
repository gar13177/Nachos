package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

public class SelfTests {
	
	public SelfTests(){
		
	}

	public void testPriorityScheduler() {

		ThreadQueue tq1 = ThreadedKernel.scheduler.newThreadQueue(true), 
				tq2 = ThreadedKernel.scheduler.newThreadQueue(true), 
				tq3 = ThreadedKernel.scheduler.newThreadQueue(true);
		KThread kt_1 = new KThread(), 
				kt_2 = new KThread(), 
				kt_3 = new KThread(), 
				kt_4 = new KThread();
		
		boolean status = Machine.interrupt().disable();
		
		tq1.waitForAccess(kt_1);
		tq2.waitForAccess(kt_2);
		tq3.waitForAccess(kt_3);
		
		tq1.acquire(kt_2);
		tq2.acquire(kt_3);
		tq3.acquire(kt_4);
		
		ThreadedKernel.scheduler.setPriority(kt_1, 6);
		
		System.out.println(ThreadedKernel.scheduler.getEffectivePriority(kt_4));
		Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_4)==6);
		
		KThread kt_5 = new KThread();
		
		ThreadedKernel.scheduler.setPriority(kt_5, 7);
		
		tq1.waitForAccess(kt_5);
		System.out.println(ThreadedKernel.scheduler.getEffectivePriority(kt_4));
		Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_4)==7);
		
		tq1.nextThread();
		System.out.println(ThreadedKernel.scheduler.getEffectivePriority(kt_4));
		Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_4)==1);
		
		Machine.interrupt().restore(status);
	}
	
	public void testBoat(){
		new Boat().selfTest();
	}
}
