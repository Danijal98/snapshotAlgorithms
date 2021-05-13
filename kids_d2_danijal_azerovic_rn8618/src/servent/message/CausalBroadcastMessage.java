package servent.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.AppConfig;
import app.ServentInfo;

/**
 * Has all the fancy stuff from {@link BasicMessage}, with an
 * added vector clock.
 * 
 * Think about the repercussions of invoking <code>changeReceiver</code> or
 * <code>makeMeASender</code> on this without overriding it.
 * @author bmilojkovic
 *
 */
public class CausalBroadcastMessage extends BasicMessage {

	private final Map<Integer, Integer> senderVectorClock;
	
	public CausalBroadcastMessage(ServentInfo senderInfo, ServentInfo receiverInfo, String messageText,
			Map<Integer, Integer> senderVectorClock) {
		super(MessageType.CAUSAL_BROADCAST, senderInfo, receiverInfo, messageText);

		this.senderVectorClock = senderVectorClock;
	}

	public CausalBroadcastMessage(MessageType messageType, ServentInfo senderInfo, ServentInfo receiverInfo, String messageText,
								  Map<Integer, Integer> senderVectorClock) {
		super(messageType, senderInfo, receiverInfo, messageText);

		this.senderVectorClock = senderVectorClock;
	}

	public CausalBroadcastMessage(ServentInfo senderInfo, ServentInfo receiverInfo, String messageText,
								  Map<Integer, Integer> senderVectorClock, List<ServentInfo> routeList, int messageId) {
		super(MessageType.CAUSAL_BROADCAST, senderInfo, receiverInfo, routeList, messageText, messageId);

		this.senderVectorClock = senderVectorClock;
	}

	public CausalBroadcastMessage(MessageType messageType, ServentInfo senderInfo, ServentInfo receiverInfo, String messageText,
								  Map<Integer, Integer> senderVectorClock, List<ServentInfo> routeList, int messageId) {
		super(messageType, senderInfo, receiverInfo, routeList, messageText, messageId);

		this.senderVectorClock = senderVectorClock;
	}
	
	public Map<Integer, Integer> getSenderVectorClock() {
		return senderVectorClock;
	}

	@Override
	public Message makeMeASender() {
		ServentInfo newRouteItem = AppConfig.myServentInfo;

		List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
		newRouteList.add(newRouteItem);

		return new CausalBroadcastMessage(getOriginalSenderInfo(), getReceiverInfo(),
				getMessageText(), getSenderVectorClock(), newRouteList, getMessageId());
	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

			return new CausalBroadcastMessage(getOriginalSenderInfo(), newReceiverInfo,
					getMessageText(), getSenderVectorClock(), getRoute(), getMessageId());
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

			return null;
		}
	}

	public static ConcurrentHashMap<Integer, Integer> mapDeepCopy (Map<Integer, Integer> original) {
		ConcurrentHashMap<Integer, Integer> copy = new ConcurrentHashMap<>();
		for (Map.Entry<Integer, Integer> entry : original.entrySet())
		{
			copy.put(entry.getKey(), entry.getValue());
		}
		return copy;
	}

}
