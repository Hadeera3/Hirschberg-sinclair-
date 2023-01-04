package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface serverINT extends Remote{
	void review(String msg) throws RemoteException;
	void statusreview(String msg) throws RemoteException;
}
