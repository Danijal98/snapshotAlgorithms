package servent.message.snapshot;

import app.AppConfig;
import app.ServentInfo;
import servent.message.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NaiveTokenAmountMessage extends CausalBroadcastMessage {

	private final ServentInfo originalDestination;

	public NaiveTokenAmountMessage(ServentInfo originalDestination, ServentInfo sender, ServentInfo receiver, int amount, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.NAIVE_TOKEN_AMOUNT, sender, receiver, String.valueOf(amount), mapDeepCopy(senderVectorClock));
		this.originalDestination = originalDestination;
	}

	private NaiveTokenAmountMessage(ServentInfo originalDestination, ServentInfo sender, ServentInfo receiver, String amount, Map<Integer, Integer> senderVectorClock, List<ServentInfo> routeList, int messageId) {
		super(MessageType.NAIVE_TOKEN_AMOUNT, sender, receiver, String.valueOf(amount), mapDeepCopy(senderVectorClock), routeList, messageId);
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

		return new NaiveTokenAmountMessage(getOriginalDestination(), getOriginalSenderInfo(), getReceiverInfo(),
				getMessageText(), getSenderVectorClock(), newRouteList, getMessageId());
	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

			return new NaiveTokenAmountMessage(getOriginalDestination(), getOriginalSenderInfo(), newReceiverInfo,
					getMessageText(), getSenderVectorClock(), getRoute(), getMessageId());
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

			return null;
		}
	}

	@Override
	public Message changeReceiverAndMessage(Integer newReceiverId, String message) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

			return new NaiveTokenAmountMessage(getOriginalDestination(), getOriginalSenderInfo(), newReceiverInfo, message,
					getSenderVectorClock(), getRoute(), getMessageId());
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

			return null;
		}
	}
}
