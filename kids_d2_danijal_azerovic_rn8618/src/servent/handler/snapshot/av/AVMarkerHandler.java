package servent.handler.snapshot.av;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.AVBitcakeManager;
import app.snapshot_bitcake.BitcakeManager;
import servent.handler.MessageHandler;
import servent.message.CausalBroadcastMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.av.AVMarkerMessage;
import servent.message.snapshot.av.AVTellMessage;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AVMarkerHandler implements MessageHandler {

	private final Message clientMessage;
	private final BitcakeManager bitcakeManager;
	private static final Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public AVMarkerHandler(Message clientMessage, BitcakeManager bitcakeManager) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public void run() {
		if ((clientMessage.getMessageType() == MessageType.AV_MARKER) || (clientMessage.getMessageType() == MessageType.AV_TERMINATE)) {
			ServentInfo senderInfo = clientMessage.getOriginalSenderInfo();
			int currentAmount = bitcakeManager.getCurrentBitcakeAmount();

			if (senderInfo.getId() == AppConfig.myServentInfo.getId()) {
				//We are the sender :o someone bounced this back to us. /ignore
//				AppConfig.timestampedStandardPrint("Got own message back. No rebroadcast.");
			} else {
				//Try to put in the set. Thread safe add ftw.
				boolean didPut = receivedBroadcasts.add(clientMessage);

				if (didPut) {
					//New message for us. Rebroadcast it.
//					AppConfig.timestampedStandardPrint("Rebroadcasting... " + receivedBroadcasts.size());

					for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
						//Same message, different receiver, and add us to the route table.
						MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
					}
					CausalBroadcastShared.addPendingMessage(clientMessage);
					CausalBroadcastShared.checkPendingMessages();


					//send result with type done
//					Message myMessage = new AVTellMessage(MessageType.AV_DONE, clientMessage.getOriginalSenderInfo(), AppConfig.myServentInfo, AppConfig.myServentInfo,
//							currentAmount, ((AVMarkerMessage)clientMessage).getSenderVectorClock());

					if(clientMessage.getMessageType() == MessageType.AV_MARKER) {

						Message myMessage = new AVTellMessage(MessageType.AV_DONE, clientMessage.getOriginalSenderInfo(), AppConfig.myServentInfo, AppConfig.myServentInfo,
								currentAmount, CausalBroadcastShared.getVectorClock());

						synchronized (AppConfig.sendLock) {
							for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
								//My message
								MessageUtil.sendMessage(myMessage.changeReceiver(neighbor).makeMeASender());
							}
							AppConfig.tokenClock = ((CausalBroadcastMessage) clientMessage).getSenderVectorClock();
							((AVBitcakeManager)bitcakeManager).initializeChannelTransactions();
							AppConfig.done.getAndSet(true);
						}

						CausalBroadcastShared.addPendingMessage(myMessage);
						CausalBroadcastShared.checkPendingMessages();

					}else if(clientMessage.getMessageType() == MessageType.AV_TERMINATE) {
						AVBitcakeManager avBitcakeManager = (AVBitcakeManager) bitcakeManager;
						AppConfig.timestampedStandardPrint("Channel transactions: " + avBitcakeManager.getChannelTransactions().toString());
						AppConfig.done.getAndSet(false);
					}

				} else {
					//We already got this from somewhere else. /ignore
//					AppConfig.timestampedStandardPrint("Already had this. No rebroadcast.");
				}
			}

		} else{
				AppConfig.timestampedErrorPrint("Ask amount handler got: " + clientMessage);

		}
	}

}
