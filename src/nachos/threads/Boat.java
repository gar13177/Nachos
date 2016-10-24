package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
	static BoatGrader bg; 
    static Communicator prueba; 
 
    static Lock lock_nueva_persona, lock_bote, lock3, lock1, lock2; 
    static int adultos_oahu, ninos_oahu, adultos_molokai, childrenMolokai; 
    static boolean hay_pasajero; 
    static Condition2 todos_los_ninos, necesito_regresar, duermase; 
    
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
	prueba = new Communicator(); 
    lock_nueva_persona = new Lock(); 
    lock_bote = new Lock(); 
    lock3 = new Lock(); 
    todos_los_ninos = new Condition2(lock3); 
    lock1 = new Lock(); 
    necesito_regresar = new Condition2(lock1); 
    lock2 = new Lock(); 
    duermase = new Condition2(lock2); 

    adultos_oahu = 0; 
    adultos_molokai = 0; 
    ninos_oahu = 0; 
    childrenMolokai = 0; 
    hay_pasajero = false; 
	
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

    prueba.listen(); //con esto espero a que todos hayan terminado de hacer lo que hayan tratado de hacer
    
    if (adultos_oahu != 0) System.out.println("Adultos pendejos: " + adultos_oahu);
    if (ninos_oahu != 0 ) System.out.println("Niños pendejos: " + ninos_oahu );;
    if (adultos_oahu+ninos_oahu == 0) System.out.println("Gracias a dios");

    }

    static void AdultItinerary()
    {
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
    	System.out.println("Nuevo adulto"); 
    	lock_nueva_persona.acquire(); 
        adultos_oahu++; //bloqueo el recurso y adhiero un adulto
        lock_nueva_persona.release(); 
 
        lock3.acquire(); //necesito que el adulto se duerma hasta que todos los niños esten del otro lado
        todos_los_ninos.sleep(); 
        lock3.release(); 
        //si ya estan todos los niños del otro lado, entonces un niño tuvo que haber regresado
        //si el adulto se despierta, lo primero que debe hacer es ir a Molokai
        bg.AdultRowToMolokai();
        adultos_oahu--; 
        lock1.acquire();
        //si ya pase el adulto, necesito que un niño regrese a verificar que no queda nadie
        necesito_regresar.wake(); 
        lock1.release(); 
    }

    static void ChildItinerary()
    {
    	System.out.println("A child is initialized."); 
        lock_nueva_persona.acquire(); 
        ninos_oahu++; //aumento la cantidad de niños en oahu
        lock_nueva_persona.release(); 
 
        //ThreadedKernel.alarm.waitUntil(500); 
 
        while(true) { 
        	//si hay un  niño, que empieze a moverse con el bote
            lock_bote.acquire(); 
            if(!hay_pasajero && ninos_oahu > 0) { 
            	//quiere decir que todavía queda espacio para ser pasajero,entonces lo tomo
                hay_pasajero = true; //momento, por que no le puse  lock a esto? pero funciona?
                bg.ChildRideToMolokai(); 
                ninos_oahu--; 
                childrenMolokai++; 
                //como es pasajero, simplemente llega a Molokai
                lock_bote.release(); 
 
                while(true) { 
                    lock1.acquire(); 
                    //por si es necesario regresar, lo dejo dormido
                    necesito_regresar.sleep(); 
                    lock1.release(); 
                    //si es necesario regresar, regreso
                    bg.ChildRowToOahu(); 
                    lock2.acquire(); 
                    //del otro lado, despierto a otro niño para regresar
                    duermase.wake(); 
                    lock2.release(); 
                    bg.ChildRideToMolokai(); 
                } 
            } else { 
                hay_pasajero = false; 
                bg.ChildRowToMolokai(); 
                ninos_oahu--; 
                childrenMolokai++; 
                if(ninos_oahu > 0) { 
                    bg.ChildRowToOahu(); 
                    childrenMolokai--; 
                    ninos_oahu++; 
                    lock_bote.release(); 
                } else {//si llego a este punto es porque ya pase a todos los niños al otro lado
                	//primero necesito que regrese un niño a revisar si no hay adultos
                    bg.ChildRowToOahu(); 
                    childrenMolokai--; 
                    ninos_oahu++; 
                    while(adultos_oahu > 0) { //si hay adultos en oahu
                    	//primero le aviso a un adulto que se vaya al otro lado
                        lock3.acquire(); 
                        todos_los_ninos.wake(); 
                        lock3.release(); 
                        lock2.acquire(); 
                        //una vez el adulto ya fue al otro lado, espero a que regrese un niño
                        duermase.sleep(); 
                        lock2.release(); 
                        bg.ChildRowToMolokai();
                        //como en teoria puede ser el mismo niño el que lleva y trae la barca para volver a revisar si hay alguien
                        //dejo este childrowtooahu para que haga un viaje para "chequear"
                        bg.ChildRowToOahu(); 
                    } 
                    bg.ChildRowToMolokai(); 
                    childrenMolokai++; 
                    ninos_oahu--; 
                    prueba.speak(1); //si llega aca ya termino
                    return ; 
                } 
            } 
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
