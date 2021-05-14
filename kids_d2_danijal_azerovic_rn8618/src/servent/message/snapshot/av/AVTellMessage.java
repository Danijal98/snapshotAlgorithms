package servent.message.snapshot.av;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.ABSnapshotResult;
import servent.message.CausalBroadcastMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AVTellMessage extends CausalBroadcastMessage {

	private final ServentInfo originalDestination;

	public AVTellMessage(MessageType messageType, ServentInfo originalDestination, ServentInfo sender, ServentInfo receiver, int amount, Map<Integer, Integer> senderVectorClock) {
		super(messageType, sender, receiver, String.valueOf(amount), mapDeepCopy(senderVectorClock));
		this.originalDestination = originalDestination;
	}
	
	private AVTellMessage(MessageType messageType, ServentInfo originalDestination, ServentInfo sender, ServentInfo receiver, List<ServentInfo> routeList,
						  int messageId, String amount, Map<Integer, Integer> senderVectorClock) {
		super(messageType, sender, receiver, amount, mapDeepCopy(senderVectorClock), routeList, messageId);
		this.originalDestination = originalDestination;
	}

	public ServentInfo getOriginalDestination() {
		return originalDestination;
	}

	@Override
	public Message makeMeASender() {
		ServentInfo newRouteItem = AppConfig.myServentInfo;

		List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
		newRouteList.add(newRouteItem);

		return new AVTellMessage(getMessageType(), getOriginalDestination(), getOriginalSenderInfo(),
				getReceiverInfo(), newRouteList, getMessageId(), getMessageText(), getSenderVectorClock());
	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

			return new AVTellMessage(getMessageType(), getOriginalDestination(), getOriginalSenderInfo(), newReceiverInfo,
					getRoute(), getMessageId(), getMessageText(), getSenderVectorClock());
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

			return null;
		}
	}

}
