package servent.handler.snapshot;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.NaiveTellAmountMessage;
import servent.message.snapshot.NaiveTokenAmountMessage;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NaiveTellAmountHandler implements MessageHandler {

	private final Message clientMessage;
	private final SnapshotCollector snapshotCollector;

	private static final Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public NaiveTellAmountHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
		this.clientMessage = clientMessage;
		this.snapshotCollector = snapshotCollector;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.NAIVE_TELL_AMOUNT) {

			int neighborAmount = Integer.parseInt(clientMessage.getMessageText());

			if(AppConfig.myServentInfo.getId() == ((NaiveTellAmountMessage)clientMessage).getOriginalDestination().getId()){

				snapshotCollector.addNaiveSnapshotInfo("node"+ clientMessage.getOriginalSenderInfo().getId(), neighborAmount);

			}else{
				boolean didPut = receivedBroadcasts.add(clientMessage);

				if (didPut) {
					//New message for us. Rebroadcast it.
//					AppConfig.timestampedStandardPrint("Rebroadcasting... " + receivedBroadcasts.size());

					CausalBroadcastShared.addPendingMessage(clientMessage);
					CausalBroadcastShared.checkPendingMessages();

					for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
						//Same message, different receiver, and add us to the route table.
						MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
					}

				} else {
					//We already got this from somewhere else. /ignore
					AppConfig.timestampedStandardPrint("Already had this. No rebroadcast.");
				}
			}

		} else {
			AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
		}

	}

}
