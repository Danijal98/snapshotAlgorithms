package servent.handler.snapshot.ab;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ab.ABTellMessage;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ABTellHandler implements MessageHandler {

	private final Message clientMessage;
	private final SnapshotCollector snapshotCollector;

	private static final Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public ABTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
		this.clientMessage = clientMessage;
		this.snapshotCollector = snapshotCollector;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.AB_TELL) {
			ABTellMessage abTellMessage = (ABTellMessage)clientMessage;
			if(AppConfig.myServentInfo.getId() == ((ABTellMessage)clientMessage).getOriginalDestination().getId()) {
				snapshotCollector.addABSnapshotInfo(abTellMessage.getOriginalSenderInfo().getId(), abTellMessage.getABSnapshotResult());
			}else {
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
