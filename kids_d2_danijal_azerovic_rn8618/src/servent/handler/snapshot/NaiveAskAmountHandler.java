package servent.handler.snapshot;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.NaiveAskAmountMessage;
import servent.message.snapshot.NaiveTellAmountMessage;
import servent.message.snapshot.NaiveTokenAmountMessage;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NaiveAskAmountHandler implements MessageHandler {

	private final Message clientMessage;
	private final BitcakeManager bitcakeManager;
	private static final Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public NaiveAskAmountHandler(Message clientMessage, BitcakeManager bitcakeManager) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.NAIVE_ASK_AMOUNT) {
			ServentInfo senderInfo = clientMessage.getOriginalSenderInfo();
			ServentInfo lastSenderInfo = clientMessage.getRoute().size() == 0 ?
					clientMessage.getOriginalSenderInfo() :
					clientMessage.getRoute().get(clientMessage.getRoute().size()-1);
			int currentAmount = bitcakeManager.getCurrentBitcakeAmount();

			if (senderInfo.getId() == AppConfig.myServentInfo.getId()) {
				//We are the sender :o someone bounced this back to us. /ignore
				AppConfig.timestampedStandardPrint("Got own message back. No rebroadcast.");
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

					Message myMessage = null;
					try {
						myMessage = new NaiveTellAmountMessage(clientMessage.getOriginalSenderInfo(), AppConfig.myServentInfo, AppConfig.myServentInfo,
								currentAmount, ((NaiveAskAmountMessage)clientMessage).getSenderVectorClock());
					}catch (Exception e) {
						AppConfig.timestampedErrorPrint(e.getMessage());
					}

					for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
						//My message
						MessageUtil.sendMessage(myMessage.changeReceiver(neighbor).makeMeASender());
					}

					CausalBroadcastShared.addPendingMessage(myMessage);
					CausalBroadcastShared.checkPendingMessages();

					CausalBroadcastShared.addPendingMessage(clientMessage);
					CausalBroadcastShared.checkPendingMessages();

				} else {
					//We already got this from somewhere else. /ignore
//					AppConfig.timestampedStandardPrint("Already had this. No rebroadcast.");
				}
			}


		} else {
			AppConfig.timestampedErrorPrint("Ask amount handler got: " + clientMessage);
		}

	}

}
