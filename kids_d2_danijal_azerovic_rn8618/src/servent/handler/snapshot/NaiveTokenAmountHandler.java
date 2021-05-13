package servent.handler.snapshot;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.NaiveTokenAmountMessage;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NaiveTokenAmountHandler implements MessageHandler {

	private Message clientMessage;
	private SnapshotCollector snapshotCollector;
	private BitcakeManager bitcakeManager;
	private boolean doRebroadcast = false;

	private static Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public NaiveTokenAmountHandler(Message clientMessage, SnapshotCollector snapshotCollector, BitcakeManager bitcakeManager, boolean doRebroadcast) {
		this.clientMessage = clientMessage;
		this.snapshotCollector = snapshotCollector;
		this.bitcakeManager = bitcakeManager;
		this.doRebroadcast = doRebroadcast;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.NAIVE_TOKEN_AMOUNT) {
			ServentInfo senderInfo = clientMessage.getOriginalSenderInfo();
			ServentInfo lastSenderInfo = clientMessage.getRoute().size() == 0 ?
					clientMessage.getOriginalSenderInfo() :
					clientMessage.getRoute().get(clientMessage.getRoute().size()-1);

			int neighborAmount = Integer.parseInt(clientMessage.getMessageText());

			if(AppConfig.myServentInfo.getId() == ((NaiveTokenAmountMessage)clientMessage).getOriginalDestination().getId()){
				snapshotCollector.addNaiveSnapshotInfo(
						"node" + lastSenderInfo.getId(), Integer.parseInt(clientMessage.getMessageText()));
			}else{
				if (doRebroadcast) {
					if (senderInfo.getId() == AppConfig.myServentInfo.getId()) {
						//We are the sender :o someone bounced this back to us. /ignore
						AppConfig.timestampedStandardPrint("Got own message back. No rebroadcast.");
					} else {
						//Try to put in the set. Thread safe add ftw.
						boolean didPut = receivedBroadcasts.add(clientMessage);

						if (didPut) {
							//New message for us. Rebroadcast it.
//							AppConfig.timestampedStandardPrint("Rebroadcasting... " + receivedBroadcasts.size());

							CausalBroadcastShared.addPendingMessage(clientMessage);
							CausalBroadcastShared.checkPendingMessages();

							//Rebroadcast received message to all neighbours
							Message myMessage = new NaiveTokenAmountMessage(clientMessage.getOriginalSenderInfo(), AppConfig.myServentInfo, null,
									bitcakeManager.getCurrentBitcakeAmount(), mapDeepCopy(((NaiveTokenAmountMessage)clientMessage).getSenderVectorClock()));

							for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
								//Same message, different receiver, and add us to the route table.
								MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
//							MessageUtil.sendMessage(msg.changeReceiverAndMessage(neighbor, String.valueOf(bitcakeManager.getCurrentBitcakeAmount())));
								//My message
								MessageUtil.sendMessage(myMessage.changeReceiver(neighbor));
							}

						} else {
							//We already got this from somewhere else. /ignore
							AppConfig.timestampedStandardPrint("Already had this. No rebroadcast.");
						}
					}
				}
			}

		} else {
			AppConfig.timestampedErrorPrint("Token amount handler got: " + clientMessage);
		}

	}

	private ConcurrentHashMap<Integer, Integer> mapDeepCopy (Map<Integer, Integer> original) {
		ConcurrentHashMap<Integer, Integer> copy = new ConcurrentHashMap<>();
		for (Map.Entry<Integer, Integer> entry : original.entrySet())
		{
			copy.put(entry.getKey(), entry.getValue());
		}
		return copy;
	}

}
