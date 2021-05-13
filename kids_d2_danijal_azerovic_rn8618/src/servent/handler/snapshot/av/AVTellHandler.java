package servent.handler.snapshot.av;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ab.ABTellMessage;

public class AVTellHandler implements MessageHandler {

	private Message clientMessage;
	private SnapshotCollector snapshotCollector;
	
	public AVTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
		this.clientMessage = clientMessage;
		this.snapshotCollector = snapshotCollector;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.AB_TELL) {
			ABTellMessage lyTellMessage = (ABTellMessage)clientMessage;
			
//			snapshotCollector.addABSnapshotInfo(
//					lyTellMessage.getOriginalSenderInfo().getId(),
//					lyTellMessage.getABSnapshotResult());
		} else {
			AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
		}

	}

}
