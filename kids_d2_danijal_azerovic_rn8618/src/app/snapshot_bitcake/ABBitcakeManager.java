package app.snapshot_bitcake;

import app.AppConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class ABBitcakeManager implements BitcakeManager {

    private final AtomicInteger currentAmount = new AtomicInteger(1000);

    public void takeSomeBitcakes(int amount) {
        currentAmount.getAndAdd(-amount);
    }

    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
    }

    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }

    private final Map<Integer, Integer> sent = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> received = new ConcurrentHashMap<>();

    public ABBitcakeManager() {
        for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
            sent.put(neighbor, 0);
            received.put(neighbor, 0);
        }
    }

    public Map<Integer, Integer> getSent() {
        return sent;
    }

    public Map<Integer, Integer> getReceived() {
        return received;
    }

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

    public void recordSentTransaction(int neighbor, int amount) {
        sent.compute(neighbor, new MapValueUpdater(amount));
    }

    public void recordReceivedTransaction(int neighbor, int amount) {
        received.compute(neighbor, new MapValueUpdater(amount));
    }
}
