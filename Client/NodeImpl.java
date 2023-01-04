package client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import server.serverINT;

import client.Receive;

@SuppressWarnings("serial")
public class NodeImpl extends UnicastRemoteObject implements client.clientIntf
{
	final int MSG_TYPE=0;
	final int SENDER=1;	
	final int MSG_UID=2;
	final int DIRECTION=3;
	final int HOP_COUNT=4;
	final int REPLY_BACK=5;
	
	final int PORT = 1234;
	
	int UID;
	int left;
	int right;
	int totalMsgs;	
	String rightMsg;	
	String leftMsg;					
	boolean contention;	
	boolean leftreplyrecv;
	boolean rightreplyrecv;
	boolean leftreaderrcv;
	boolean rightreaderrcv;
	boolean Electedleader;
	boolean debug;
	boolean replybackrecv;
	Neighbors contender;
	serverIntf superProcess;	
	
	protected NodeImpl(int uid,int leftId, int rightId,boolean debug) throws RemoteException 
	{
		super();	
		UID=uid;
		left=leftId;
		right=rightId;
		contention=false;
		leftreplyrecv=true;
		rightreplyrecv=true;
		replybackrecv=true;
		leftreaderrcv=false;
		rightreaderrcv=false;
		Electedleader=false;
		totalMsgs=0;	//counts only sent messages and relayed messages
		this.debug=debug;
		try 
		{
			superProcess=(serverIntf)LocateRegistry.getRegistry(PORT).lookup("L");
		} catch (NotBoundException e) {	e.printStackTrace();}
	}		
	
	public void send(String msg) throws RemoteException
	{		
		new Receive(this,msg);				
	}  		
	
	public synchronized void message(String msg)
	{
		try 
		{
			String msgIndex[]=msg.split(",");
			if(debug)
				superProcess.review(UID+" Received message "+msg+" at "+UID);		
							
			if(msgIndex[MSG_TYPE].equalsIgnoreCase("msg"))
			{			
				if(msgIndex.length==5)
				{
					
					if(msgIndex[MSG_UID].equals(new Integer(UID).toString()))
					{								

						if(contention)							
						{												
							
							if(msgIndex[DIRECTION].equalsIgnoreCase("r"))
							{

								if(msgIndex[SENDER].equals(new Integer(left).toString()))
								{
 link																
									if(debug)
										superProcess.review(System.currentTimeMillis()+" "+UID+ msg);
									rightreaderrcv=true;									
								}
							}
							else if(msgIndex[DIRECTION].equalsIgnoreCase("l"))
							{

								if(msgIndex[SENDER].equals(new Integer(right).toString()))
								{

									if(debug)
										superProcess.review(UID+msg);
									leftreaderrcv=true;
								}
							}
							if(leftreaderrcv==true && rightreaderrcv==true)
							{

								Electedleader=true;								

								try
								{
									clientIntf leftNode=(clientIntf)LocateRegistry.getRegistry(PORT).lookup(new Integer(left).toString());
									clientIntf rightNode=(clientIntf)LocateRegistry.getRegistry(PORT).lookup(new Integer(right).toString());									
									rightNode.send("leader,"+UID+","+UID);							
									leftNode.send("leader,"+UID+","+UID);
									
									//notify the waiting threads
									Electedleader=true;
									notifyAll();									
									
									//unbind the leader from the registry
									LocateRegistry.getRegistry(1234).unbind(new Integer(UID).toString());
									
									boolean done = false;
									while (!done) 
									{
										try
										{
											LocateRegistry.getRegistry(PORT).lookup(new Integer(UID).toString());																
										}
										catch (Exception e) 
										{						
											done = true;
											break;
										}
									}									
																			
									superProcess.statusreview("status,"+UID+","+UID+","+totalMsgs);
									System.exit(0);
								}
								catch (NotBoundException e) {	e.printStackTrace();}
							}
						}
					}
				
					else
					{
					
						if(Integer.parseInt(msgIndex[MSG_UID])>UID)
						{
						
							if(Integer.parseInt(msgIndex[HOP_COUNT])!=1)
							{
															
								String M="msg,"+UID+","+msgIndex[MSG_UID]+","+msgIndex[DIRECTION]+","+(Integer.parseInt(msgIndex[HOP_COUNT])-1);
								
		
								if(msgIndex[DIRECTION].equalsIgnoreCase("r"))
								{
									try
									{
										clientIntf rightNode=(clientIntf)LocateRegistry.getRegistry(PORT).lookup(new Integer(right).toString());
										rightNode.send(M);
										totalMsgs+=1;
									}
									catch (NotBoundException e) {e.printStackTrace();}
									
									if(debug)
										superProcess.review(UID+M);
								}
							
								else if(msgIndex[DIRECTION].equalsIgnoreCase("l"))
								{																	
									try
									{
										clientIntf leftNode=(clientIntf)LocateRegistry.getRegistry(PORT).lookup(new Integer(left).toString());
										leftNode.send(M);
										totalMsgs+=1;
									}
									catch (NotBoundException e) {e.printStackTrace();}
									
									if(debug)
										superProcess.review(UID+M);
								}
							}
							//if the hop count is 1,  
							else if(Integer.parseInt(msgIndex[HOP_COUNT])==1)
							{
																
								if(msgIndex[DIRECTION].equalsIgnoreCase("r"))
								{									
									String M = "msg,"+UID+","+msgIndex[MSG_UID]+","+"l,1,RB";
									try 
									{
										clientIntf leftNode=(clientIntf)LocateRegistry.getRegistry(PORT).lookup(new Integer(left).toString());
										leftNode.send(M);
										totalMsgs+=1;
									}
									catch (NotBoundException e) {e.printStackTrace();	}
									
									if(debug)
										superProcess.review(UID+M);
								}
								else if(msgIndex[DIRECTION].equalsIgnoreCase("l"))
								{									
									String M = "msg,"+UID+","+msgIndex[MSG_UID]+","+"r,1,RB";
									try
									{
										clientIntf rightNode=(clientIntf)LocateRegistry.getRegistry(PORT).lookup(new Integer(right).toString());
										rightNode.send(M);
										totalMsgs+=1;
									}
									catch (NotBoundException e) {e.printStackTrace();}
									
									if(debug)
										superProcess.review(UID+M);
								} 
							}
						}
						if(Integer.parseInt(msgIndex[MSG_UID])<UID)
						{
							if(debug)
								superProcess.review(UID+" Dropped message "+msg+". My UID > MSG_UID");
						}																							 
					}					
				}
				//if the reply back field is not empty
				else if(msgIndex.length==6 && msgIndex[REPLY_BACK].equalsIgnoreCase("rb"))
				{
					if(contention==true && (msgIndex[MSG_UID].equals(new Integer(UID).toString())))
					{					
						if(msgIndex[SENDER].equals(new Integer(right).toString()))
						{							
							rightreplyrecv=true;							
							if(debug)
								superProcess.review(UID+" Reply Back received on right link and rightReplyBackValue is "+rightreplyrecv+" and leftReplyBackValue is"+leftreplyrecv);
						}
						if(msgIndex[SENDER].equals(new Integer(left).toString()))
						{
							leftreplyrecv=true;
							if(debug)
								superProcess.review(UID+" Reply Back received on left link and rightReplyBackValue is "+rightreplyrecv+" and leftReplyBackValue is"+leftreplyrecv);
						}
						if(rightreplyrecv==true && leftreplyrecv==true)
						{
							replybackrecv=true;
							contention=false;
							if(debug)
								superProcess.review(UID+" Inside wake up process, rightreplyrecv is "+rightreplyrecv+" leftreplyrecv is "+leftreplyrecv);
							if(debug)
								superProcess.review(UID+" Wake up the contender thread for message "+msg);
							
							//wake up the waiting thread
							notifyAll();
						}
					}
					//if a message is a reply back message and the MSG_UID>our UID, we just relay
					//the msg
					else if(!(msgIndex[MSG_UID].equals(new Integer(UID).toString())) && (Integer.parseInt(msgIndex[MSG_UID])>UID))
					{									
						//if the node left to us sent it, then we send it to our right
						if(msgIndex[SENDER].equals(new Integer(left).toString()))
						{									
							String M = "msg,"+UID+","+msgIndex[MSG_UID]+","+msgIndex[DIRECTION]+",1,RB";
							try 
							{
								clientIntf rightNode=(clientIntf)LocateRegistry.getRegistry(PORT).lookup(new Integer(right).toString());
								rightNode.send(M);
								totalMsgs+=1;
							}
							catch (NotBoundException e) {	e.printStackTrace();}
							
							if(debug)
								superProcess.review(UID+M);
						}
						else if(msgIndex[SENDER].equals(new Integer(right).toString()))
						{									
							String M = "msg,"+UID+","+msgIndex[MSG_UID]+","+msgIndex[DIRECTION]+",1,RB";
							try
							{
								clientIntf leftNode=(clientIntf)LocateRegistry.getRegistry(PORT).lookup(new Integer(left).toString());
								leftNode.send(M);
								totalMsgs+=1;
							}
							catch (NotBoundException e) {e.printStackTrace();	}
							
							if(debug)
								superProcess.review(UID+M);
						}
					}							
				}
			}
			if(msgIndex[MSG_TYPE].equalsIgnoreCase("start"))
			{
				contender=new Neighbors(this);
				contender.getneighborThread().start();								
			}
			if(msgIndex[MSG_TYPE].equalsIgnoreCase("leader"))
			{
				if(Electedleader==false)
				{
					Electedleader=true;
					try
					{																
																					
						if(msgIndex[SENDER].equals(new Integer(left).toString()) && !(new Integer(right).toString().equals(msgIndex[2])))
						{
							if(!(msgIndex[SENDER].equals(new Integer(right).toString())))
									((clientIntf)(LocateRegistry.getRegistry(PORT).lookup(new Integer(right).toString()))).send("leader,"+UID+","+msgIndex[2]);							
						}															 
																					  
						if(msgIndex[SENDER].equals(new Integer(right).toString()) && !(new Integer(left).toString().equals(msgIndex[2])))
						{
							if(!(msgIndex[SENDER].equals(new Integer(left).toString())))
								((clientIntf)(LocateRegistry.getRegistry(PORT).lookup(new Integer(left).toString()))).send("leader,"+UID+","+msgIndex[2]);							
						}
						if(debug)
							superProcess.review(UID+" Leader has been elected and the leader is "+msgIndex[2]);
						
						superProcess.statusreview("status,"+UID+","+msgIndex[2]+","+totalMsgs);
						
						//unregister ourselves from the rmi registry 
						LocateRegistry.getRegistry(1234).unbind(new Integer(UID).toString());						
						boolean done = false;
						//wait till we are unbounded
						while (!done) 
						{
							try
							{
								LocateRegistry.getRegistry(PORT).lookup(new Integer(UID).toString());								
							}
							catch (Exception e) 
							{						
								done = true;
								break;
							}
						}					
						
						
						notifyAll();												
						System.exit(0);						
					}
					catch (NotBoundException e) {	superProcess.review(UID+" "+e.toString());e.printStackTrace();}
				}
				else					
				{	
					if(debug)
						superProcess.review(UID+" Received leader message after already receiving it");
					//check if this object is already unbounded
					boolean done = false;
					while (!done) 
					{
						try
						{
							LocateRegistry.getRegistry(PORT).lookup(new Integer(UID).toString());
						}
						catch (Exception e) 
						{						
							done = true;
							break;
						}
					}						
				}
				//same as above
				notifyAll();
				System.exit(0);
			}
		}
		catch (RemoteException e)
		{			
			try 
			{
				superProcess.review("Exception here"+e.toString());
			}
			catch (AccessException e1) {e1.printStackTrace();}
			catch (RemoteException e1) {e1.printStackTrace();}			
		}
	}

	public synchronized void contendForLeadership(int phase)
	{
		try 
		{							
			if(replybackrecv==true && Electedleader!=true)
			{	
				if(debug)
					superProcess.review(UID+" Contending on phase "+phase);
				
				rightMsg="msg,"+UID+","+UID+",r,"+(int)(Math.pow(2,phase));
				leftMsg="msg,"+UID+","+UID+",l,"+(int)(Math.pow(2,phase));
				if(debug)
					superProcess.review(UID+" 's messages are "+leftMsg+" "+rightMsg);
				clientIntf leftNode=(clientIntf)LocateRegistry.getRegistry(PORT).lookup(new Integer(left).toString());			
				clientIntf rightNode=(clientIntf)LocateRegistry.getRegistry(PORT).lookup(new Integer(right).toString());
							
				rightreplyrecv=leftreplyrecv=false;
				replybackrecv=false;
				
				leftNode.send(leftMsg);
				rightNode.send(rightMsg);

				totalMsgs+=2;														
				
				contention=true;
				
				notifyAll();
				
				if(debug)
					superProcess.review(UID);					
			}
			if(replybackrecv==false)
			{			
				//we put this thread on wait
				wait();										
			}
		}
		catch (RemoteException e) {	e.printStackTrace();	}
		catch (InterruptedException e) {	e.printStackTrace(); 	}
		catch (NotBoundException e) {	e.printStackTrace();	}
	}
			
	public static void main(String args[]) throws RemoteException
	{
		try 
		{
			boolean debug=false;
			if(Integer.parseInt(args[3])==1)
				debug=true;
			LocateRegistry.getRegistry(1234).rebind(args[0],new NodeImpl(Integer.parseInt(args[0]),Integer.parseInt(args[1]),Integer.parseInt(args[2]),debug));
		}
		catch (NumberFormatException e) {	e.printStackTrace();}			
	}
	
}
