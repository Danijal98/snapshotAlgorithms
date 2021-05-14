package servent.handler.snapshot.ab;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.ABBitcakeManager;
import app.snapshot_bitcake.ABSnapshotResult;
import app.snapshot_bitcake.BitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ab.ABMarkerMessage;
import servent.message.snapshot.ab.ABTellMessage;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ABMarkerHandler implements MessageHandler {

	private final Message clientMessage;
	private final BitcakeManager bitcakeManager;
	private static final Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public ABMarkerHandler(Message clientMessage, BitcakeManager bitcakeManager) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = bitcakeManager;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.AB_MARKER) {
			ServentInfo senderInfo = clientMessage.getOriginalSenderInfo();
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

					ABSnapshotResult abSnapshotResult = new ABSnapshotResult(clientMessage.getOriginalSenderInfo().getId(), currentAmount, ((ABBitcakeManager) bitcakeManager).getSent(), ((ABBitcakeManager)bitcakeManager).getReceived());
//					Message myMessage = new ABTellMessage(clientMessage.getOriginalSenderInfo(), AppConfig.myServentInfo, AppConfig.myServentInfo,
//							abSnapshotResult, ((ABMarkerMessage)clientMessage).getSenderVectorClock());

					Message myMessage = new ABTellMessage(clientMessage.getOriginalSenderInfo(), AppConfig.myServentInfo, AppConfig.myServentInfo,
							abSnapshotResult, CausalBroadcastShared.getVectorClock());

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
