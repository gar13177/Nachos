package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

public class SelfTests {
	
	public SelfTests(){
		
	}

	
	public void testBoat(){
		new Boat().selfTest();
	}
	
	public static void testPriorityScheduler(){
        KThread t1, t2, t3; 
        final Lock lock; 
        final Condition2 condition; 
        /* 
        * Case 1: Tests priority scheduler without donation 
        * 
        * This runs t1 with priority 7, and t2 with priority 4. 
        * 
        */ 
        /*
        System.out.println("Case 1:"); 

        t1 = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().getName() + " started working"); 
                for (int i = 0; i < 10; ++i){
                    System.out.println(KThread.currentThread().getName() + " working " + i); 
                    KThread.yield(); 
                } 
                System.out.println(KThread.currentThread().getName() + " finished working"); 
            }}); 
        t2 = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().getName() + " started working"); 
                for (int i = 0; i < 10; ++i) {
                    System.out.println(KThread.currentThread().getName() + " working " + i); 
                    KThread.yield(); 
                } 
                System.out.println(KThread.currentThread().getName() + " finished working"); 
            }});
        selfTestRun(t1, 7, t2, 4);*/
        //System.out.println(KThread.getCurrentThread()); 

        /*
        * Case 2: Tests priority scheduler without donation, altering 
        * priorities of threads after they've started running 
        * 
        * This runs t1 with priority 7, and t2 with priority 4, but 
        * half-way through t1's process its priority is lowered to 2. 
        * 
        */ 
        /*
        System.out.println("Case 2:"); 
        t1 = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().getName() + " started working");
                for (int i = 0; i < 10; ++i){
                    System.out.println(KThread.currentThread().getName() + " working " + i);
                    KThread.yield();
                    if (i == 4){
                        System.out.println(KThread.currentThread().getName() + " reached 1/2 way, changing priority");
                        boolean int_state = Machine.interrupt().disable();
                        ThreadedKernel.scheduler.setPriority(2);
                        Machine.interrupt().restore(int_state);
                    }
                }
                System.out.println(KThread.currentThread().getName() + " finished working");
            }});
        t2 = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().getName() + " started working");
                for (int i = 0; i < 10; ++i){
                    System.out.println(KThread.currentThread().getName() + " working " + i);
                    KThread.yield();
                }
                System.out.println(KThread.currentThread().getName() + " finished working");
            }}); 
        selfTestRun(t1, 7, t2, 4); 

        /* 
        * Case 3: Tests priority donation 
        * 
        * This runs t1 with priority 7, t2 with priority 6 and t3 with 
        * priority 4. t1 will wait on a lock, and while t2 would normally 
        * then steal all available CPU, priority donation will ensure that 
        * t3 is given control in order to help unlock t1. 
        * 
        */ 

        System.out.println("Case 3:"); 
        lock = new Lock(); 
        condition = new Condition2(lock); 
        t1 = new KThread(new Runnable() {
            public void run(){
                lock.acquire();
                System.out.println(KThread.currentThread().getName() + " active");
                lock.release();
            }}); 

        t2 = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().getName() + " started working");
                for (int i = 0; i < 3; ++i){
                    System.out.println(KThread.currentThread().getName() + " working " + i);
                    KThread.yield();
                }
                System.out.println(KThread.currentThread().getName() + " finished working");
            }}); 

        t3 = new KThread(new Runnable(){
            public void run(){
                lock.acquire();
                boolean int_state = Machine.interrupt().disable();
                ThreadedKernel.scheduler.setPriority(2);
                Machine.interrupt().restore(int_state);
                KThread.yield();
                // t1.acquire() will now have to realise that t3 owns the lock it wants to obtain
                // so program execution will continue here. 
                System.out.println(KThread.currentThread().getName() + " active ('a' wants its lock back so we are here)"); 
                lock.release();
                KThread.yield(); 
                lock.acquire();
                System.out.println(KThread.currentThread().getName() + " active-again (should be after 'a' and 'b' done)"); 
                lock.release();
            }}); 

        selfTestRun(t1, 6, t2, 4, t3, 7); 

    }

    public static void selfTestRun(KThread t1, int t1p, KThread t2, int t2p) {
        boolean int_state;
        int_state = Machine.interrupt().disable();
        ThreadedKernel.scheduler.setPriority(t1, t1p);
        ThreadedKernel.scheduler.setPriority(t2, t2p); 
        
        t1.setName("a").fork();
        t2.setName("b").fork();
        t1.join();
        t2.join(); 
        Machine.interrupt().restore(int_state);
    } 

    public static void selfTestRun(KThread t1, int t1p, KThread t2, int t2p, KThread t3, int t3p) {
        boolean int_state;
        int_state = Machine.interrupt().disable();

        ThreadedKernel.scheduler.setPriority(t1, t1p);
        ThreadedKernel.scheduler.setPriority(t2, t2p);
        ThreadedKernel.scheduler.setPriority(t3, t3p);
        
        
        t1.setName("a").fork();
        t2.setName("b").fork();
        t3.setName("c").fork();
        t1.join();
        t2.join();
        t3.join();
        Machine.interrupt().restore(int_state);
    } 
}
