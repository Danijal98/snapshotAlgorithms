package servent.message.snapshot.av;

import app.AppConfig;
import app.ServentInfo;
import servent.message.CausalBroadcastMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ab.ABMarkerMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AVMarkerMessage extends CausalBroadcastMessage {

	public AVMarkerMessage(ServentInfo sender, ServentInfo receiver, int collectorId, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.AV_MARKER, sender, receiver, String.valueOf(collectorId), mapDeepCopy(senderVectorClock));
	}

	private AVMarkerMessage(ServentInfo sender, ServentInfo receiver, Map<Integer, Integer> senderVectorClock, List<ServentInfo> routeList, int messageId) {
		super(MessageType.AV_MARKER, sender, receiver, "ASK", senderVectorClock, routeList, messageId);
	}

	@Override
	public Message makeMeASender() {
		synchronized (AppConfig.sendLock) {
			ServentInfo newRouteItem = AppConfig.myServentInfo;
			List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
			newRouteList.add(newRouteItem);

			return new AVMarkerMessage(getOriginalSenderInfo(), getReceiverInfo(),
					mapDeepCopy(getSenderVectorClock()), newRouteList, getMessageId());
		}
	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			synchronized (AppConfig.sendLock) {
				ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

				return new AVMarkerMessage(getOriginalSenderInfo(), newReceiverInfo,
						mapDeepCopy(getSenderVectorClock()), getRoute(), getMessageId());
			}
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

			return null;
		}
	}

}
