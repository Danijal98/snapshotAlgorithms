package app.snapshot_bitcake;

import app.AppConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class AVBitcakeManager implements BitcakeManager {

	private final AtomicInteger currentAmount = new AtomicInteger(1000);
	private final Map<Integer, Integer> channelTransactions = new ConcurrentHashMap<>();

	private static class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {

		private final int valueToAdd;

		public MapValueUpdater(int valueToAdd) {
			this.valueToAdd = valueToAdd;
		}

		@Override
		public Integer apply(Integer key, Integer oldValue) {
			return oldValue + valueToAdd;
		}
	}

	public void recordChannelTransaction(int neighbor, int amount) {
		channelTransactions.compute(neighbor, new AVBitcakeManager.MapValueUpdater(amount));
	}

	public Map<Integer, Integer> getChannelTransactions() {
		return channelTransactions;
	}

	public void initializeChannelTransactions() {
		for (Integer neighbour: AppConfig.myServentInfo.getNeighbors()) {
			channelTransactions.put(neighbour, 0);
		}
	}

	@Override
	public void takeSomeBitcakes(int amount) {
		currentAmount.getAndAdd(-amount);
	}
	
	@Override
	public void addSomeBitcakes(int amount) {
		currentAmount.getAndAdd(amount);
	}
	
	@Override
	public int getCurrentBitcakeAmount() {
		return currentAmount.get();
	}
	
}
