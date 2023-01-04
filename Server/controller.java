package server;

import java.rmi.RemoteException;

public class controller implements Runnable 
{
	leader lead;
	String message;
	
	public synchronized void run()
	{
		try
		{
			String msgIndex[]=message.split(",");
			
			if(msgIndex[0].equalsIgnoreCase("status"))
			{
				lead.setTM(Integer.parseInt(msgIndex[3])+lead.getTM());
				lead.review(msgIndex[1]+" chooses "+msgIndex[2]);
			}
		}  
		catch (Exception e)
		{
			try
			{
				lead.review(e.toString());				
			}
			catch (RemoteException e1) {e1.printStackTrace();}
		}
	}
	
	public controller(leader lead,String message)
	{
		this.lead=lead;
		this.message=message;
		new Thread(this,"handlestatus").start();
	}
}
