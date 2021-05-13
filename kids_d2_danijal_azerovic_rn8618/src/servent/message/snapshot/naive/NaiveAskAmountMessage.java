package servent.message.snapshot.naive;

import app.AppConfig;
import app.ServentInfo;
import servent.message.CausalBroadcastMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NaiveAskAmountMessage extends CausalBroadcastMessage {

	public NaiveAskAmountMessage(ServentInfo sender, ServentInfo receiver, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.NAIVE_ASK_AMOUNT, sender, receiver, "ASK", senderVectorClock);
	}

	private NaiveAskAmountMessage(ServentInfo sender, ServentInfo receiver, Map<Integer, Integer> senderVectorClock, List<ServentInfo> routeList, int messageId) {
		super(MessageType.NAIVE_ASK_AMOUNT, sender, receiver, "ASK", senderVectorClock, routeList, messageId);
	}

	@Override
	public Message makeMeASender() {
		synchronized (AppConfig.sendLock) {
			ServentInfo newRouteItem = AppConfig.myServentInfo;
			List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
			newRouteList.add(newRouteItem);

			return new NaiveAskAmountMessage(getOriginalSenderInfo(), getReceiverInfo(),
					mapDeepCopy(getSenderVectorClock()), newRouteList, getMessageId());
		}
	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			synchronized (AppConfig.sendLock) {
				ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

				return new NaiveAskAmountMessage(getOriginalSenderInfo(), newReceiverInfo,
						mapDeepCopy(getSenderVectorClock()), getRoute(), getMessageId());
			}
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

			return null;
		}
	}

	@Override
	public String toString() {
		return "ASK MESSAGE -> TEXT: " + this.getMessageText() + " ORIGINAL SENDER: " + this.getOriginalSenderInfo() + " RECEIVER: " + this.getReceiverInfo();
	}
}