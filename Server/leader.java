package server;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("serial")
public class leader extends UnicastRemoteObject implements serverINT {

	private static final int PORT = 1234;
	private int size;
	private int tm;
	ArrayList<Integer> nodeID;
	boolean debug;

	
	public void nodeStart() throws IOException
	{
		try 
		{
			
			String left, right;
			nodeID = new ArrayList<Integer>(getsize());
			for (int i = 0; i < getsize(); i++)
			{
				int rand = (int) Math.round(Math.random() * 1000);
				if(nodeID.contains(rand))
					i--;
				else
					nodeID.add(rand);
			}

			Collections.shuffle(nodeID);
			System.out.println("Registered Nodes:");

			for (int i = 0; i < getsize(); i++) 
			{
				String ID = nodeID.get(i).toString();

				if (i == 0) {
					left = nodeID.get(getsize() - 1).toString();
					right = nodeID.get(i + 1).toString();
				} else if (i == getsize() - 1) {
					left = nodeID.get(i - 1).toString();
					right = nodeID.get(0).toString();
				} else {
					left = nodeID.get(i - 1).toString();
					right = nodeID.get(i + 1).toString();
				}
				
				
				ProcessBuilder proc=null;
				if(debug==true)
					proc= new ProcessBuilder("java", "client.NodeImpl",ID, left, right,new Integer(1).toString());
				else if(debug!=true)
					proc= new ProcessBuilder("java", "client.NodeImpl",ID, left, right,new Integer(0).toString());
				proc.directory(new File(System.getProperty("user.dir")+ "/bin"));
				try 
				{
					proc.start();
				}
				catch (IOException e1) {e1.printStackTrace();}	
				boolean done = false;
				
				while (!done) 
				{
					
					try
					{
						LocateRegistry.getRegistry(PORT).lookup(ID.toString());
						done = true;
						review(ID);
					}
					catch (Exception e) 
					{						
						
					}
				}
			}
			System.out.println();
			for (int i = 0; i < getsize(); i++) 
			{
				System.out.print(nodeID.get(i).toString()+"-");
			}
			System.out.println();
			
			for (int i = 0; i < getsize(); i++) 
			{
				new indicator(nodeID.get(i).toString());
			}
			System.out.println();
			System.out.println();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public leader(int size,boolean debug) throws RemoteException{
		try
		{
			setsize(size);
			this.debug=debug;
		}
		catch (Exception ex) {ex.printStackTrace();	}
	}
	
	public int getsize() {
		return size;
	}
	
		
	public void setsize(int size) {
		this.size = size;
	}
	
	public int gettm() {
		return tm;
	}

	public void settm(int tm) {
		this.tm = tm;
	}

	public synchronized void review(String message) throws RemoteException 
	{
			System.out.println(message);
	}
	
	public void statusreview(String message) throws RemoteException 
	{
		
		new controller(this,message);
	}
	public static void main(String[] args) 
	{
		try
		{
			Registry registry;
			int Pros = Integer.parseInt(args[0]);
			leader l=new leader(Pros,false);			
			registry = LocateRegistry.createRegistry(PORT);
			registry.rebind("L",l);			
			l.nodeStart();					
		}
		catch(Exception e)
		{
			System.out.println("Message: "+e.toString());
		}
	}	
}
