package servent.handler.snapshot.av;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ab.ABTellMessage;
import servent.message.snapshot.av.AVTellMessage;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AVTellHandler implements MessageHandler {

	private final Message clientMessage;
	private final SnapshotCollector snapshotCollector;

	private static final Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public AVTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
		this.clientMessage = clientMessage;
		this.snapshotCollector = snapshotCollector;
	}

	@Override
	public void run() {
		AVTellMessage avTellMessage = (AVTellMessage)clientMessage;
		if (clientMessage.getMessageType() == MessageType.AV_DONE) {

			if(AppConfig.myServentInfo.getId() == ((AVTellMessage)clientMessage).getOriginalDestination().getId()){
				snapshotCollector.addAVSnapshotInfo(avTellMessage.getOriginalSenderInfo().getId(),
						Integer.parseInt(avTellMessage.getMessageText()));
				return;
			}

			//rebroadcast
			boolean didPut = receivedBroadcasts.add(clientMessage);

			if (didPut) {
				//New message for us. Rebroadcast it.
				//AppConfig.timestampedStandardPrint("Rebroadcasting... " + receivedBroadcasts.size());

				CausalBroadcastShared.addPendingMessage(clientMessage);
				CausalBroadcastShared.checkPendingMessages();

				for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
					//Same message, different receiver, and add us to the route table.
					MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
				}

			} else {
				//We already got this from somewhere else. /ignore
//				AppConfig.timestampedStandardPrint("Already had this. No rebroadcast.");
			}
		} else {
			AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
		}

	}

}
