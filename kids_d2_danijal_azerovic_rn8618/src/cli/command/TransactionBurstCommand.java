package cli.command;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import servent.message.Message;
import servent.message.TransactionMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionBurstCommand implements CLICommand {

	private static final int TRANSACTION_COUNT = 5;
	private static final int BURST_WORKERS = 10;
	private static final int MAX_TRANSFER_AMOUNT = 10;
	
	private BitcakeManager bitcakeManager;
	
	public TransactionBurstCommand(BitcakeManager bitcakeManager) {
		this.bitcakeManager = bitcakeManager;
	}
	
	private class TransactionBurstWorker implements Runnable {
		
		@Override
		public void run() {
			Map<Integer, Integer> myClock = CausalBroadcastShared.getVectorClock();
//			Message transactionMessage = new TransactionMessage(AppConfig.myServentInfo,
//					AppConfig.myServentInfo, AppConfig.myServentInfo, 0, bitcakeManager, mapDeepCopy(myClock));

			for (int i = 0; i < TRANSACTION_COUNT; i++) {
				for (int neighbor : AppConfig.myServentInfo.getNeighbors()) {
					ServentInfo neighborInfo = AppConfig.getInfoById(neighbor);

					Message transactionMessage = new TransactionMessage(neighborInfo,
							AppConfig.myServentInfo, AppConfig.myServentInfo, 0, bitcakeManager, mapDeepCopy(myClock));
					
					int amount = 1 + (int)(Math.random() * MAX_TRANSFER_AMOUNT);
					
					/*
					 * The message itself will reduce our bitcake count as it is being sent.
					 * The sending might be delayed, so we want to make sure we do the
					 * reducing at the right time, not earlier.
					 */
					Message msg = ((TransactionMessage)transactionMessage.changeReceiverAndMessage(neighbor, String.valueOf(amount))).changeOriginalDestination(neighborInfo);
					MessageUtil.sendMessage(msg);
					CausalBroadcastShared.addPendingMessage(msg);
					CausalBroadcastShared.checkPendingMessages();
				}

			}

//			CausalBroadcastShared.addPendingMessage(transactionMessage);
//			CausalBroadcastShared.checkPendingMessages();

		}
	}
	
	@Override
	public String commandName() {
		return "transaction_burst";
	}

	@Override
	public void execute(String args) {
		for (int i = 0; i < BURST_WORKERS; i++) {
			Thread t = new Thread(new TransactionBurstWorker());
			
			t.start();
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
