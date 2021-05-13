package servent.message.snapshot.ab;

import app.AppConfig;
import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.CausalBroadcastMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.naive.NaiveAskAmountMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ABMarkerMessage extends CausalBroadcastMessage {

	public ABMarkerMessage(ServentInfo sender, ServentInfo receiver, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.AB_MARKER, sender, receiver, "ABMarkerMessage", mapDeepCopy(senderVectorClock));
	}

	private ABMarkerMessage(ServentInfo sender, ServentInfo receiver, Map<Integer, Integer> senderVectorClock, List<ServentInfo> routeList, int messageId) {
		super(MessageType.AB_MARKER, sender, receiver, "ABMarkerMessage", senderVectorClock, routeList, messageId);
	}

	@Override
	public Message makeMeASender() {
		synchronized (AppConfig.sendLock) {
			ServentInfo newRouteItem = AppConfig.myServentInfo;
			List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
			newRouteList.add(newRouteItem);

			return new ABMarkerMessage(getOriginalSenderInfo(), getReceiverInfo(),
					mapDeepCopy(getSenderVectorClock()), newRouteList, getMessageId());
		}
	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			synchronized (AppConfig.sendLock) {
				ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

				return new ABMarkerMessage(getOriginalSenderInfo(), newReceiverInfo,
						mapDeepCopy(getSenderVectorClock()), getRoute(), getMessageId());
			}
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

			return null;
		}
	}

}
