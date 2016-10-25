package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    
    static int adultos_oahu, ninos_oahu, adultos_molokai, ninos_molokai; 
    static boolean passenger;
    static Communicator finalizar;
    static Lock lock_variables, adulto_nuevo, lock_ninos_esperando, lock_bote, lock_recuperacion, lock_adulto_dormido;
    static Condition2 esperar_ninos, avisar_ninos, nuevo_nino_de_recuperacion, adulto_del_otro_lado;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	lock_variables = new Lock();
	adulto_nuevo = new Lock();
	esperar_ninos = new Condition2(adulto_nuevo);//esperar a que todos los niños esten en molokai
	lock_ninos_esperando = new Lock();
	avisar_ninos = new Condition2(lock_ninos_esperando);
	lock_bote = new Lock();
	lock_recuperacion = new Lock();
	nuevo_nino_de_recuperacion = new Condition2(lock_recuperacion);
	finalizar = new Communicator();
	
	adultos_oahu = 0; 
    adultos_molokai = 0; 
    ninos_oahu = 0; 
    ninos_molokai = 0; 
    passenger = false; 
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.
    
    for(int i = 0; i < adults; i++) { 
        KThread t = new KThread(new Runnable() { 
            @Override 
            public void run() { 
                AdultItinerary(); 
            } 
        }); 
        t.setName("Adulto: " + i); 
        t.fork(); 
    } 
    for(int i = 0; i < children; i++) { 
        KThread t = new KThread(new Runnable() { 
            @Override 
            public void run() { 
                ChildItinerary(); 
            } 
        }); 
        t.setName("Nino: " + i); 
        t.fork(); 
    } 
    
    finalizar.listen();
    
    if (adultos_oahu + ninos_oahu == 0) System.out.println("Todos pasaron");
    else 
    	System.out.println("Quedaron "+ninos_oahu+" ninos y "+adultos_oahu+" adultos");

	/*Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();*/

    }

    static void AdultItinerary()
    {
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
    	System.out.println("Nuevo Adulto");
    	lock_variables.acquire();
    	adultos_oahu++;
    	lock_variables.release();
    	adulto_nuevo.acquire();
    	esperar_ninos.sleep();
    	adulto_nuevo.release();
    	
    	bg.AdultRowToMolokai();
    	adultos_oahu--;
    	
    	lock_ninos_esperando.acquire();
    	avisar_ninos.wake();
    	lock_ninos_esperando.release();
    }

    static void ChildItinerary()
    {
    	System.out.println("Nuevo Niño");
    	lock_variables.acquire();
    	ninos_oahu++;
    	lock_variables.release();
    	
    	ChildFromOAhuToMolokai();
    }
    
    static void ChildFromOAhuToMolokai(){
    	while(true) { 
        	//si hay un  niño, que empieze a moverse con el bote
            lock_bote.acquire(); 
            if(!passenger && ninos_oahu > 0) { 
            	//quiere decir que todavía queda espacio para ser pasajero,entonces lo tomo
            	
                passenger = true; 
                bg.ChildRideToMolokai(); 
                ninos_oahu--; 
                ninos_molokai++; 
                //como es pasajero, simplemente llega a Molokai
                lock_bote.release(); 
 
                WorkingChild();//si el niño llega al otro lado, sera un trabajador
            } else { 
                passenger = false; 
                bg.ChildRowToMolokai(); 
                ninos_oahu--; 
                ninos_molokai++; 
                if(ninos_oahu > 0) {//si faltan niños y el cuate regresó solito, que regrese por mas niños
                    bg.ChildRowToOahu(); 
                    ninos_molokai--; 
                    ninos_oahu++; 
                    lock_bote.release(); 
                }else{
                	AdultsReturn();
                	return;
                }
            }
    	}
                
    }
    
    static void AdultsReturn(){
    	bg.ChildRowToOahu(); 
        ninos_molokai--; 
        ninos_oahu++; 
        while (adultos_oahu >0 ){
        	adulto_nuevo.acquire();
        	esperar_ninos.wake();
        	adulto_nuevo.release();
        	
        	lock_recuperacion.acquire();
        	nuevo_nino_de_recuperacion.sleep();
        	lock_recuperacion.release();
        	
        	bg.ChildRowToMolokai();
        	bg.ChildRowToOahu();        	
        }
        bg.ChildRowToMolokai();
        ninos_molokai++;
        ninos_oahu--;
        finalizar.speak(1);
        return;        
    }
    
    static void WorkingChild(){
    	while(true) { 
    		lock_ninos_esperando.acquire(); 
            //por si es necesario regresar, lo dejo dormido
            avisar_ninos.sleep(); 
            lock_ninos_esperando.release(); 
            //si es necesario regresar, regreso
            bg.ChildRowToOahu(); 
            lock_recuperacion.acquire(); 
            //del otro lado, despierto a otro niño para regresar
            nuevo_nino_de_recuperacion.wake(); 
            lock_recuperacion.release(); 
            bg.ChildRideToMolokai(); 
        } 
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
