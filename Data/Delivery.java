import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;


class drone{
	int id;
	int [] position = {0,0};// initial position 0,0
	int currentClk = 0;
	int prevClk=0;
	int maxLoad ;
	order curOrder ; // current order being fulfilled by the drone
	static ArrayList<order> prevOrders = new ArrayList<order>(); // prev orders fulfilled by the drone	-may not be needed	
	
	drone(int i){
		id = i;
	}
	
	void load(warehouse wh){		
		//Loads the products from warehouse to the drone for the current order
		// does 3 tasks - 1.removes the loaded product from the list
		int loadCounter = 0;
	    ArrayList<Integer> Products = new ArrayList<Integer>(); // duplicating remaining product list
	    for(int i=0;i<curOrder.pendingProducts.size();i++) {
			Products.add(curOrder.pendingProducts.get(i));
		}		
		for(int i =0;i<Products.size();i++){
			int product = Products.get(i);
			int count = wh.pAvail.get(product);

			if(count != 0){
				curOrder.pendingProducts.remove(curOrder.pendingProducts.indexOf(product));    // equivalent to loading product onto the drone
				wh.pAvail.set(product, count--);
				loadCounter++;
			}			
			if(loadCounter == this.maxLoad) {
				break;
			}
		}
		curOrder.droneUsed.add(this); 
		currentClk ++; // adding 1 ,distance already added
	}	
	
	int deliver(){
		/*delivers the current order, updates the partial fulfilled and fulfilled flags
		*/
		//1.update clock value indicating delivery
		//updates availability of the drone
		currentClk++;		
		//2. flag update
		if(curOrder.pendingProducts.size() == 0) {
			curOrder.fulfilled = true;
			curOrder.partialFulfilled = false;
			curOrder.clockEnd = this.currentClk;
			updateAvailability();
			return 1;
		}
		else {
			curOrder.partialFulfilled = true;
		}			
		curOrder.clockEnd = this.currentClk;

		prevOrders.add(curOrder);
		updateAvailability();
		return 0;
	}
	
	private void updateAvailability() {
		// inserts currents timestamp and drone object in SortedMap
		SortedMap<Integer, ArrayList<drone>> map = Delivery.availableDrones;
		int availTime = currentClk;
		
		ArrayList<drone> list;
		if(map.containsKey(availTime)) {
			//if timestamp already exists; add drone to the list
		    list = map.get(availTime);
			list.add(this);
		}else {
			// insert new key value pair
		    list = new ArrayList<drone>();
			list.add(this);
		    map.put(availTime, list);
		}
	}

	public int goToLocation(int[] loc) {
		// updates the position of drone and the clock
		int turnCount = Delivery.getDistance(position,loc);
		this.position = loc;
		currentClk = currentClk + turnCount;
		return turnCount;
	}	
}

class warehouse{	
	int id;
	int[] loc = {0,0}; // location of warehouse row =index 0;column = index 1
    ArrayList<Integer> pAvail = new ArrayList<Integer>(); // availability of each product in the warehouse
    
    public warehouse(int i) {
		id=i;
	}
}

class order{
	int id;
	int[] deliveryLoc = {0,0}; // location of delivery row =index 0;column = index 1
	int pCount =0; // # of ordered products
	int status =0; // 0 = not completed by default------------> why not make status char???
	int clockBeg = 0; // clock value when the order was first processed
	int clockEnd = 0; // clock value when the order was fulfilled
	int localClock = 0;//gives number of turns taken by that drone
	drone curDrone; // drone curretly fulfilling the order
	boolean partialFulfilled = false;//needed???? ----> can be merged with status variable
	boolean fulfilled = false;
	ArrayList<drone> droneUsed =  new ArrayList<drone>(); // can change this later to include extra information
    ArrayList<Integer> pTypes = new ArrayList<Integer>(); // types of products ordered
    ArrayList<Integer> pendingProducts = new ArrayList<Integer>(); // products id's left in case of partial delivery    
    
	order(int i){
		id =i;
	}
    public void createPendingList() {
		// create a duplicate product list
		for(int i=0;i<pCount;i++) {
			pendingProducts.add(pTypes.get(i));
		}		
	}
}
public class Delivery {
	static int rows=0;
	static int columns=0;
	static int droneCount=0;
	static int orderCount=0;
	static int globalClk =0; // measured in number of turns
	static int simDeadline=0;	
	static int droneMaxLoad=0;
	static int productCount=0;
	static int warehouseCount=0;
	static double score = 0;
	static boolean pendingOrders = true; // pending orders flag,set to 1 by default	
	static ArrayList<order>  orders = new ArrayList<order>(); //list of incomplete orders	
	static ArrayList<drone>  drones = new ArrayList<drone>();
	static ArrayList<order>  completedOrders = new ArrayList<order>();	
	static ArrayList<Integer> productWeights = new ArrayList<Integer>();
	static ArrayList<warehouse> warehouses = new ArrayList<warehouse>();
	static ArrayList<Integer> completionTurn = new ArrayList<Integer>(); // list of completion time of each order
	static SortedMap<Integer, ArrayList<drone>> availableDrones = new TreeMap<Integer, ArrayList<drone>>();
	
	/* files for output
    fileObjects[0]=drone ;fileObjects[1]=order;fileObjects[0]=warehouse
    fileObjects[4]=command file */
	static ArrayList<FileWriter> fileObjects = new ArrayList<FileWriter>(); 
	
	
	public static void main(String[] args) {
		
		System.out.println("Enter the input file Option\n1.busyDay \n2.motherOfWarehouses\n3.redundancy\n");
		Scanner inp = new Scanner(System.in);
		int File = inp.nextInt();
		inp.close();
		String inpFile = null;
		if(File == 1) {
			inpFile = "data/busy_day.in";
		}
		else if(File == 2) {
			inpFile = "data/mother_of_all_warehouses.in";
		}
		else if(File == 3) {
			inpFile = "data/redundancy.in";
		}	
		readInput(inpFile);
		fileObjects = createOutputFiles();
		pendingOrders = assignOrdersInitial();
		while(pendingOrders) {
			pendingOrders = assignNext();
		}
		closeFiles(fileObjects);
		System.out.println("Number of turns taken to complete simulation ->"+globalClk);
		calculateScore(globalClk);
		System.out.println("SCORE -> "+score);

	}

	private static void calculateScore(double T) {
		// calculates the score (T-t)/T*100
		for(int i=0;i<orderCount;i++) {
			double t = completionTurn.get(i);
			double temp =((T-t)/T)*100;
			score += Math.ceil(temp);
		}
	}

	private static boolean assignNext() {
		// Assigns the remaining orders to the drone which is free
		drone drone = whichDrone();
		ArrayList<Integer> nextPair = getOrder(drone);
		completeOrder(drone.id,nextPair);
		if(orders.size() == 0) {
			return false;
		}
		return true;	
	}

	private static drone whichDrone() {
		// returns the earliest available drone
		//SortedMap<Integer, ArrayList<drone>> map = Delivery3.availableDrones;
		//int availTime = currentClk;
		int time= availableDrones.firstKey();
		drone selDrone;
		ArrayList<drone> dList = availableDrones.get(time);
		if(dList.size()>1) {
			//if multiple drone available,choose the first drone in the list and remove the entry from value list
			 selDrone = dList.get(0);
			 dList.remove(0);
		}else {
			// remove the timeStamp entry from the Map itself
			 selDrone = dList.get(0);
			 availableDrones.remove(time);
		}
	
		return selDrone;
	}

	private static void closeFiles(ArrayList<FileWriter> f) {
		//closes the output files
		try {
			f.get(0).close();
			f.get(1).close();
			f.get(2).close();
			f.get(3).close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private static ArrayList<FileWriter> createOutputFiles() {
		// Creates output files and inserts the header
		FileWriter dFile = null;
		FileWriter oFile = null;
		FileWriter wFile = null;
		FileWriter cFile = null;


		ArrayList<FileWriter> obj = new ArrayList<FileWriter>();
		String space = " ";
		try {
			String details = "\nNumber of Drones->"+droneCount+"\nNumber of Orders->"+orderCount+"\nNumber of warehouses->"+warehouseCount+"\n\n"; 

			//File for drone stats
			dFile = new FileWriter("drone.txt");
			obj.add(dFile);
			String line = "DroneID"+space+"Pos[0]"+space+ "Pos[1]"+space+"Order"+space+"LastClk"+"\n";    
			dFile.write(details);
			dFile.write(line);

			//File for order stats
			oFile = new FileWriter("order.txt");
			obj.add(oFile);
			line = "order"+space+"Loc[0]"+space+"Loc[1]"+space+"totTurns"+space+"DroneID"+space+"partial"+space+"fulfilled"+"\n";
			oFile.write(details);
			oFile.write(line);

			//File for order stats if needed
			wFile = new FileWriter("warehouse.txt");
			obj.add(wFile);	
	        line = "WarehouseID"+space+"location[0]"+space+ "location[1]"+"\n";
			wFile.write(details);
			wFile.write(line);

			cFile = new FileWriter("commands.txt");
			obj.add(cFile);	
	        line = "This File has commands given to the drone \n";
			cFile.write(details);
			cFile.write(line);

		} catch (IOException e) {
			e.printStackTrace();
		}
	
		return obj;
	}

	private static boolean assignOrdersInitial() {
		// This method will assign orders to drones each time that it is called
		for(int droneId=0;droneId<droneCount;droneId++) {	
			ArrayList<Integer> nextPair = getOrder(drones.get(droneId));
			completeOrder(droneId,nextPair);
			if(orders.size() == 0) {
				return false;
			}
		}
		if(orders.size() == 0) {
			return false;
		}	
		
		return true;	
	}

	private static void completeOrder(int droneId, ArrayList<Integer> nextPair) {
		// does the necessary task after a order has been assigned to a drone
		int localTurnClk=0; // why keeping? don't know
		drone drone=drones.get(droneId);
		warehouse wh = warehouses.get(nextPair.get(1));
		order od = orders.get(nextPair.get(0));
		od.curDrone = drone;
		int[] whLoc = wh.loc;
		drone.maxLoad = droneMaxLoad;
		drone.curOrder = od;	
		drone.prevClk = drone.currentClk;
		
		//goes to the warehouse and collects products
		if(!od.fulfilled && !od.partialFulfilled) {
			od.clockBeg = drone.currentClk;
			//od.createPendingList(); // only called when the order is being processed for the first time
		}
		if(Arrays.equals(drone.position,whLoc)) {
			// already at the warehouse
			drone.load(wh);
			localTurnClk += 1;
		}else {
			localTurnClk += drone.goToLocation(whLoc); // add the turns to local clk
			drone.load(wh);
			localTurnClk += 1;
		}		
		//deliver the order	
		
		// go to location
		localTurnClk += drone.goToLocation(od.deliveryLoc);
		int status = drone.deliver();
		if(status == 1) {
			completedOrders.add(od);
			orders.remove(od);  // remove order from the list of orders if fulfilled
			completionTurn.set(od.id,od.clockEnd-1); 
		}
		localTurnClk += 1;
		od.localClock = localTurnClk;
		updateGlobalClk(drone);
		writeToFile(drone);
		writeToFile(od);
	//	writeToFile(wh);
		writeToFile(od,drone,wh);
		
	}
	private static void writeToFile(order od, drone drone, warehouse wh) {
		// Writes the commands to the drone
		//String tab = "\t";

		String line ="Turn "+drone.prevClk+": Drone "+drone.id+"-> warehouse "+wh.id+" -> Order "+od.id+" : Turn "+drone.currentClk+"\n";

		 try {
				fileObjects.get(3).write(line);
				fileObjects.get(3).flush();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		
	}

	@SuppressWarnings("unused")
	private static void writeToFile(warehouse wh) {
		// writes out statistics of warehouse
		String tab = "\t";
		String line = wh.id+" "+wh.loc[0]+" "+wh.loc[1];
        try {
			fileObjects.get(2).write(line);
			fileObjects.get(2).flush();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private static void writeToFile(order od) {
		// writes orders to file
		String tab = "\t";
	//	String line = od.id+" "+od.deliveryLoc[0]+" "+od.deliveryLoc[1]+" "+(od.clockEnd-od.clockBeg)+" "+od.curDrone.id+" "+od.partialFulfilled+" "+od.fulfilled+"\n"; 
		String line = od.id+tab+od.deliveryLoc[0]+tab+od.deliveryLoc[1]+tab+od.localClock+tab+od.curDrone.id+tab+od.partialFulfilled+tab+od.fulfilled+"\n"; 

		try {
			fileObjects.get(1).write(line);
			fileObjects.get(1).flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void writeToFile(drone drone)  {
		// drone statistics to file after completion of each order
		String tab = "\t";
       // System.out.println(drone.id+tab+drone.position[0]+tab+drone.position[1]+tab+drone.curOrder.id+tab+drone.currentClk); 

		 String line = drone.id+tab+drone.position[0]+tab+drone.position[1]+tab+drone.curOrder.id+tab+drone.currentClk+"\n";
	        try {
				fileObjects.get(0).write(line);
				fileObjects.get(0).flush();
			} catch (IOException e) {
				e.printStackTrace();
			}        
	}	
	
	private static void updateGlobalClk(drone drone) {
		// checks if the global clock needs to be updated and updates it
		//also checks if the simulation deadline is reached
		if(drone.currentClk > globalClk) {
			globalClk = drone.currentClk;
		}
		
		if(globalClk> simDeadline) {
			System.out.println("Deadline reached : Simulation ended");
		}
	}

	private static ArrayList<Integer> getOrder(drone dr) {
		/* finds the order with least cost from the current position of the drone
		 * 
		 * returns the next order assigned to the drone
		 * returns array list of type objects will contain two elements
		 * owPair[0] -> order,owPair[1] -> warehouse
		 * 	*/
		
		//Order Id used here is the local array index
		int lastMin =1000000000;
		ArrayList<Integer> owPair = new ArrayList<Integer>();
		owPair.add(0);
	    owPair.add(0);
		for (int whId=0; whId < warehouseCount ;whId++) {
			for(int orderId=0;orderId< orders.size();orderId++) {
				int min = getDistance(warehouses.get(whId).loc,dr.position)+getDistance(warehouses.get(whId).loc,orders.get(orderId).deliveryLoc);				
				if(min < lastMin) {
					if(containsItem(orders.get(orderId).pendingProducts,warehouses.get(whId).pAvail)) {
						lastMin = min;
					    owPair.set(0,orderId);
					    owPair.set(1,whId);
					}				
				}
			}			
		}
		return owPair;	
	}

	private static boolean containsItem(ArrayList<Integer> need, ArrayList<Integer> have) {
		/* checks if the selected warehouse contains atleast one item from the order
		 * returns true if atleast one product exists
		 */
		if(warehouseCount == 1){
			return true;
		}
		for(int i=0;i<need.size();i++) {
			int product = need.get(i);
			for(int j=0;j<have.size();j++) {
				if(have.get(product)>0) {
					return true;
				}
			}
		}			
		return false;
	}
	
	static int getDistance(int[] x, int[] y ) {
		// returns the distance between two locations in number of turns
		double temp1=0;
		double temp2=0;
		temp1 =Math.pow(Math.abs(x[0]-y[0]), 2);
		temp2 =Math.pow(Math.abs(x[1]-y[1]), 2);		
		int distance =(int) Math.ceil( Math.sqrt(temp1+temp2));		// # of turns 
		return  distance;
	}

	private static void readInput(String fileName) {
		// Method to read the input file into data structures
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line;	
			//First line - Reading parameters of simulation
			line = br.readLine();
			String[] token =line.split(" ");
			rows = Integer.parseInt(token[0].trim());
			columns = Integer.parseInt(token[1].trim());
			droneCount = Integer.parseInt(token[2].trim());
			for(int i =0;i<droneCount;i++) {
				drones.add(new drone(i));
			}
			
			simDeadline = Integer.parseInt(token[3].trim());
			droneMaxLoad = Integer.parseInt(token[4].trim());
			//Second line - # of product types
			line = br.readLine();
			productCount = Integer.parseInt(line);
			// third line - weights of all products
			line = br.readLine();
			StringTokenizer tokens = new StringTokenizer(line," ");
			while (tokens.hasMoreTokens()) {
				productWeights.add(Integer.parseInt(tokens.nextToken().trim()));
				
			}
			// Warehouses and Availability of individual product types
			//4th line = # of warehouses and 2 lines for each warehouse describing
			line = br.readLine();
			warehouseCount = Integer.parseInt(line);
			for(int i =0;i < warehouseCount;i++) {
				warehouse w = new warehouse(i);
				// first line - row and column of the warehouse
				line = br.readLine();
				String[] pos = line.split(" ");
				w.loc[0] = Integer.parseInt(pos[0]);
				w.loc[1] = Integer.parseInt(pos[1]);
				//second line - available products in the warehouse
				line = br.readLine();
				StringTokenizer wp_tokens= new StringTokenizer(line," ");
				while (wp_tokens.hasMoreTokens()) {
					w.pAvail.add(Integer.parseInt(wp_tokens.nextToken().trim()));					
				}
				warehouses.add(w);
				
			}
			// # of orders then next 3 lines per order
			line = br.readLine();
			orderCount = Integer.parseInt(line);
			//Order details
			for(int i=0;i<orderCount;i++) {
				order o = new order(i);
				//first line -delivery location
				line = br.readLine();
				String[] loc = line.split(" ");
				o.deliveryLoc[0] = Integer.parseInt(loc[0]);
				o.deliveryLoc[1] = Integer.parseInt(loc[1]);
				//second line - # of ordered products
				line = br.readLine();
				o.pCount = Integer.parseInt(line);
				//Third line - the product types ordered
				line = br.readLine();
				StringTokenizer op_tokens= new StringTokenizer(line," ");
				while (op_tokens.hasMoreTokens()) {
					o.pTypes.add(Integer.parseInt(op_tokens.nextToken().trim()));
					completionTurn.add(0);
				}
				o.createPendingList(); 
				orders.add(o);	
			}			
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
