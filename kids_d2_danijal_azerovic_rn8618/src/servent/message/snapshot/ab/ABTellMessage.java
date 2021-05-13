package servent.message.snapshot.ab;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.ABSnapshotResult;
import servent.message.CausalBroadcastMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ABTellMessage extends CausalBroadcastMessage {

	private final ABSnapshotResult abSnapshotResult;
	private final ServentInfo originalDestination;
	
	public ABTellMessage(ServentInfo originalDestination, ServentInfo sender, ServentInfo receiver, ABSnapshotResult abSnapshotResult, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.AB_TELL, sender, receiver, "AB_TELL", mapDeepCopy(senderVectorClock));
		this.abSnapshotResult = abSnapshotResult;
		this.originalDestination = originalDestination;
	}
	
	private ABTellMessage(ServentInfo originalDestination, ServentInfo sender, ServentInfo receiver, List<ServentInfo> routeList,
						  int messageId, ABSnapshotResult abSnapshotResult, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.AB_TELL, sender, receiver, "AB_TELL", mapDeepCopy(senderVectorClock), routeList, messageId);
		this.abSnapshotResult = abSnapshotResult;
		this.originalDestination = originalDestination;
	}

	public ABSnapshotResult getABSnapshotResult() {
		return abSnapshotResult;
	}

	public ServentInfo getOriginalDestination() {
		return originalDestination;
	}

	@Override
	public Message makeMeASender() {
		ServentInfo newRouteItem = AppConfig.myServentInfo;

		List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
		newRouteList.add(newRouteItem);

		return new ABTellMessage(getOriginalDestination(), getOriginalSenderInfo(),
				getReceiverInfo(), newRouteList, getMessageId(), getABSnapshotResult(), getSenderVectorClock());
	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

			return new ABTellMessage(getOriginalDestination(), getOriginalSenderInfo(), newReceiverInfo,
					getRoute(), getMessageId(), getABSnapshotResult(), getSenderVectorClock());
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

			return null;
		}
	}

}
