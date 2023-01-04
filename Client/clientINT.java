package client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface clientINT	extends Remote
{
	void send(String msg) throws RemoteException;	
}
