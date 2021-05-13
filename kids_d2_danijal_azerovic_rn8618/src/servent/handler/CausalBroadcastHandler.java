package servent.handler;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the CAUSAL_BROADCAST message. Fairly simple, as we assume that we are
 * in a clique. We add the message to a pending queue, and let the check on the queue
 * take care of the rest.
 * @author bmilojkovic
 *
 */
public class CausalBroadcastHandler implements MessageHandler {

	private Message clientMessage;
	private boolean doRebroadcast = false;

	private static Set<Message> receivedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public CausalBroadcastHandler(Message clientMessage, boolean doRebroadcast) {
		this.clientMessage = clientMessage;
		//We want to rebroadcast the message if we are not in a clique.
		this.doRebroadcast = doRebroadcast;
	}
	
	@Override
	public void run() {
		//Sanity check.
		if (clientMessage.getMessageType() == MessageType.CAUSAL_BROADCAST) {
			ServentInfo senderInfo = clientMessage.getOriginalSenderInfo();
			ServentInfo lastSenderInfo = clientMessage.getRoute().size() == 0 ?
					clientMessage.getOriginalSenderInfo() :
					clientMessage.getRoute().get(clientMessage.getRoute().size()-1);

			/*
			 * The standard read message already prints out that we got a msg.
			 * However, we also want to see who sent this to us directly, besides from
			 * seeing the original owner - if we are not in a clique, this might
			 * not be the same node.
			 */
			String text = String.format("Got %s from %s broadcast by %s",
					clientMessage.getMessageText(), lastSenderInfo, senderInfo);

			AppConfig.timestampedStandardPrint(text);


			if (doRebroadcast) {
				if (senderInfo.getId() == AppConfig.myServentInfo.getId()) {
					//We are the sender :o someone bounced this back to us. /ignore
					AppConfig.timestampedStandardPrint("Got own message back. No rebroadcast.");
				} else {
					//Try to put in the set. Thread safe add ftw.
					boolean didPut = receivedBroadcasts.add(clientMessage);

					if (didPut) {
						//New message for us. Rebroadcast it.
						AppConfig.timestampedStandardPrint("Rebroadcasting... " + receivedBroadcasts.size());

						CausalBroadcastShared.addPendingMessage(clientMessage);
						CausalBroadcastShared.checkPendingMessages();

						for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
							//Same message, different receiver, and add us to the route table.
							MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
						}

					} else {
						//We already got this from somewhere else. /ignore
						AppConfig.timestampedStandardPrint("Already had this. No rebroadcast.");
					}
				}
			}


		}
	}

}
