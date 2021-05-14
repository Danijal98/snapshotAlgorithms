package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import app.snapshot_bitcake.ABBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotType;
import servent.handler.CausalBroadcastHandler;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.ab.ABMarkerHandler;
import servent.handler.snapshot.ab.ABTellHandler;
import servent.handler.snapshot.av.AVMarkerHandler;
import servent.handler.snapshot.av.AVTellHandler;
import servent.handler.snapshot.naive.NaiveAskAmountHandler;
import servent.handler.snapshot.naive.NaiveTellAmountHandler;
import servent.handler.snapshot.NaiveTokenAmountHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;

	private SnapshotCollector snapshotCollector;

	public SimpleServentListener(SnapshotCollector snapshotCollector) {
		this.snapshotCollector = snapshotCollector;
	}
	
	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort());
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}
		
		while (working) {
			try {
				/*
				 * This blocks for up to 1s, after which SocketTimeoutException is thrown.
				 */
				Socket clientSocket = listenerSocket.accept();
				
				//GOT A MESSAGE! <3
				Message clientMessage = MessageUtil.readMessage(clientSocket);
				MessageHandler messageHandler = new NullHandler(clientMessage);
				
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */
				switch (clientMessage.getMessageType()) {
					case CAUSAL_BROADCAST:
						messageHandler = new CausalBroadcastHandler(clientMessage, !AppConfig.IS_CLIQUE);
						break;
					case TRANSACTION:
						messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager(), !AppConfig.IS_CLIQUE);
						break;
					case NAIVE_TOKEN_AMOUNT:
						messageHandler = new NaiveTokenAmountHandler(clientMessage, snapshotCollector, snapshotCollector.getBitcakeManager(), !AppConfig.IS_CLIQUE);
						break;
					case NAIVE_ASK_AMOUNT:
						messageHandler = new NaiveAskAmountHandler(clientMessage, snapshotCollector.getBitcakeManager());
						break;
					case NAIVE_TELL_AMOUNT:
						messageHandler = new NaiveTellAmountHandler(clientMessage, snapshotCollector);
						break;
					case AB_MARKER:
						messageHandler = new ABMarkerHandler(clientMessage, snapshotCollector.getBitcakeManager());
						break;
					case AB_TELL:
						messageHandler = new ABTellHandler(clientMessage, snapshotCollector);
						break;
					case AV_MARKER:
					case AV_TERMINATE:
						messageHandler = new AVMarkerHandler(clientMessage, snapshotCollector.getBitcakeManager());
						break;
					case AV_DONE:
						messageHandler = new AVTellHandler(clientMessage, snapshotCollector);
						break;
				}
				
				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
