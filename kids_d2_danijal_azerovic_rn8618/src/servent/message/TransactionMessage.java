package servent.message;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.ABBitcakeManager;
import app.snapshot_bitcake.BitcakeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a bitcake transaction. We are sending some bitcakes to another node.
 * 
 * @author bmilojkovic
 *
 */
public class TransactionMessage extends CausalBroadcastMessage {

	private final transient BitcakeManager bitcakeManager;
	private final ServentInfo originalDestination;
	
	public TransactionMessage(ServentInfo originalDestination, ServentInfo sender, ServentInfo receiver, int amount,
							  BitcakeManager bitcakeManager, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount), mapDeepCopy(senderVectorClock));
		this.bitcakeManager = bitcakeManager;
		this.originalDestination = originalDestination;
	}

	private TransactionMessage(ServentInfo originalDestination, ServentInfo sender, ServentInfo receiver, String amount, BitcakeManager bitcakeManager,
								   Map<Integer, Integer> senderVectorClock, List<ServentInfo> routeList, int messageId) {
		super(MessageType.TRANSACTION, sender, receiver, amount, mapDeepCopy(senderVectorClock), routeList, messageId);
		this.bitcakeManager = bitcakeManager;
		this.originalDestination = originalDestination;
	}

	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}

	public ServentInfo getOriginalDestination() {
		return originalDestination;
	}

	@Override
	public Message makeMeASender() {
		ServentInfo newRouteItem = AppConfig.myServentInfo;

		List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
		newRouteList.add(newRouteItem);

		return new TransactionMessage(getOriginalDestination(), getOriginalSenderInfo(), getReceiverInfo(), getMessageText(), getBitcakeManager(),
				getSenderVectorClock(), newRouteList, getMessageId());
	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

			return new TransactionMessage(getOriginalDestination(), getOriginalSenderInfo(), newReceiverInfo, getMessageText(), getBitcakeManager(),
					getSenderVectorClock(), getRoute(), getMessageId());
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

			return null;
		}
	}

	@Override
	public Message changeReceiverAndMessage(Integer newReceiverId, String message) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

			return new TransactionMessage(getOriginalDestination(), getOriginalSenderInfo(), newReceiverInfo, message, getBitcakeManager(),
					getSenderVectorClock(), getRoute(), getMessageId());
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

			return null;
		}
	}

	/**
	 * We want to take away our amount exactly as we are sending, so our snapshots don't mess up.
	 * This method is invoked by the sender just before sending
	 */
	@Override
	public void sendEffect() {
		if((AppConfig.myServentInfo.getId() == getOriginalSenderInfo().getId()) && getReceiverInfo().getId() == originalDestination.getId()) {
			int amount = Integer.parseInt(getMessageText());
			bitcakeManager.takeSomeBitcakes(amount);

			if (bitcakeManager instanceof ABBitcakeManager) {
				ABBitcakeManager abBitcakeManager = (ABBitcakeManager) bitcakeManager;
				synchronized (AppConfig.bitcakeLock) {
					abBitcakeManager.recordSentTransaction(getReceiverInfo().getId(), amount);
				}
			}
		}
	}

	@Override
	public Message changeOriginalDestination(ServentInfo newOriginalDestination) {
		return new TransactionMessage(newOriginalDestination, getOriginalSenderInfo(), getReceiverInfo(), getMessageText(), getBitcakeManager(),
				getSenderVectorClock(), getRoute(), getMessageId());
	}

}