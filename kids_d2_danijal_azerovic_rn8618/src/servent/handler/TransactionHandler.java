package servent.handler;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.ABBitcakeManager;
import app.snapshot_bitcake.BitcakeManager;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TransactionMessage;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionHandler implements MessageHandler {

	private final Message clientMessage;
	private final BitcakeManager bitcakeManager;
	private boolean doRebroadcast = false;

	private static final Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public TransactionHandler(Message clientMessage, BitcakeManager bitcakeManager, boolean doRebroadcast) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
		this.doRebroadcast = doRebroadcast;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			ServentInfo senderInfo = clientMessage.getOriginalSenderInfo();
			ServentInfo lastSenderInfo = clientMessage.getRoute().size() == 0 ?
					clientMessage.getOriginalSenderInfo() :
					clientMessage.getRoute().get(clientMessage.getRoute().size()-1);


			String amountString = clientMessage.getMessageText();

			int amountNumber = 0;
			try {
				amountNumber = Integer.parseInt(amountString);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
				return;
			}


			if (doRebroadcast) {
				if (senderInfo.getId() == AppConfig.myServentInfo.getId()) {
					//We are the sender :o someone bounced this back to us. /ignore
					AppConfig.timestampedStandardPrint("Got own message back. No rebroadcast.");
				} else {
					//Try to put in the set. Thread safe add ftw.
					boolean didPut = receivedBroadcasts.add(clientMessage);

					if (didPut) {
						//New message for us. Rebroadcast it.
//						AppConfig.timestampedStandardPrint("Rebroadcasting... " + receivedBroadcasts.size());

						if(AppConfig.myServentInfo.getId() == ((TransactionMessage)clientMessage).getOriginalDestination().getId()) {
							bitcakeManager.addSomeBitcakes(amountNumber);
							if (bitcakeManager instanceof ABBitcakeManager) {
								ABBitcakeManager abBitcakeManager = (ABBitcakeManager) bitcakeManager;
								synchronized (AppConfig.bitcakeLock) {
									abBitcakeManager.recordReceivedTransaction(clientMessage.getOriginalSenderInfo().getId(), amountNumber);
								}
							}
						}

						CausalBroadcastShared.addPendingMessage(clientMessage);
						CausalBroadcastShared.checkPendingMessages();

						for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
							//Same message, different receiver, and add us to the route table.
							MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
						}

					} else {
						//We already got this from somewhere else. /ignore
//						AppConfig.timestampedStandardPrint("Already had this. No rebroadcast.");
					}
				}
			}

		} else {
			AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
		}
	}

}
