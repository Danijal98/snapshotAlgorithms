package app.snapshot_bitcake;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Snapshot result for servent with id serventId.
 * The amount of bitcakes on that servent is written in recordedAmount.
 * The channel messages are recorded in giveHistory and getHistory.
 * In Lai-Yang, the initiator has to reconcile the differences between
 * individual nodes, so we just let him know what we got and what we gave
 * and let him do the rest.
 * 
 * @author bmilojkovic
 *
 */
public class ABSnapshotResult implements Serializable {

	private final int serventId;
	private final int recordedAmount;
	private final Map<Integer, Integer> sent;
	private final Map<Integer, Integer> received;

	public ABSnapshotResult(int serventId, int recordedAmount,
							Map<Integer, Integer> sent, Map<Integer, Integer> received) {
		this.serventId = serventId;
		this.recordedAmount = recordedAmount;
		this.sent = new ConcurrentHashMap<>(sent);
		this.received = new ConcurrentHashMap<>(received);
	}
	public int getServentId() {
		return serventId;
	}
	public int getRecordedAmount() {
		return recordedAmount;
	}
	public Map<Integer, Integer> getSent() {
		return sent;
	}
	public Map<Integer, Integer> getReceived() {
		return received;
	}
}
