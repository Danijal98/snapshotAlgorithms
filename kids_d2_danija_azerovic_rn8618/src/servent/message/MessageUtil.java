package servent.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import app.AppConfig;
import app.ServentInfo;

/**
 * For now, just the read and send implementation, based on Java serializing.
 * Not too smart. Doesn't even check the neighbor list, so it actually allows cheating.
 * @author bmilojkovic
 *
 */
public class MessageUtil {

	/**
	 * Normally this should be true, because it helps with debugging.
	 * Flip this to false to disable printing every message send / receive.
	 */
	private static final boolean MESSAGE_UTIL_PRINTING = true;
	
	public static Message readMessage(Socket socket) {
		
		Message clientMessage = null;
		
		try {
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
	
			clientMessage = (Message) ois.readObject();
			
			socket.close();
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Error in reading socket on " +
					socket.getInetAddress() + ":" + socket.getPort());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		if (MESSAGE_UTIL_PRINTING) {
			AppConfig.timestampedStandardPrint("Got message " + clientMessage);
		}
		
		return clientMessage;
		
		
	}
	
	public static void sendMessage(Message message) {
		
		/*
		 * A random sleep before sending.
		 * It is important to take regular naps for health reasons.
		 */
		try {
			Thread.sleep((long)(Math.random() * 1000) + 500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		ServentInfo receiverInfo = message.getReceiverInfo();
		
		if (MESSAGE_UTIL_PRINTING) {
			AppConfig.timestampedStandardPrint("Sending message " + message);
		}
		
		try {
			Socket sendSocket = new Socket(receiverInfo.getIpAddress(), receiverInfo.getListenerPort());
			
			ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
			
			oos.writeObject(message);
			
			oos.flush();
			
			sendSocket.close();
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't send message: " + message.toString());
		}
	}
}
