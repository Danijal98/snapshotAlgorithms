package app.snapshot_bitcake;

import app.AppConfig;
import app.CausalBroadcastShared;
import servent.message.CausalBroadcastMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ab.ABMarkerMessage;
import servent.message.snapshot.av.AVMarkerMessage;
import servent.message.snapshot.naive.NaiveAskAmountMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 *
 * @author bmilojkovic
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

    private volatile boolean working = true;

    private final AtomicBoolean collecting = new AtomicBoolean(false);

    private final Map<String, Integer> collectedNaiveValues = new ConcurrentHashMap<>();
    private final Map<Integer, ABSnapshotResult> collectedABValues = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> collectedAVValues = new ConcurrentHashMap<>();

    private SnapshotType snapshotType = SnapshotType.NAIVE;

    private BitcakeManager bitcakeManager;

    public SnapshotCollectorWorker(SnapshotType snapshotType) {
        this.snapshotType = snapshotType;

        switch (snapshotType) {
            case NAIVE:
                bitcakeManager = new NaiveBitcakeManager();
                break;
            case ACHARYA_BADRINATH:
                bitcakeManager = new ABBitcakeManager();
                break;
            case ALAGAR_VENKATESAN:
                bitcakeManager = new AVBitcakeManager();
                break;
            case NONE:
                AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
                System.exit(0);
        }
    }

    @Override
    public BitcakeManager getBitcakeManager() {
        return bitcakeManager;
    }

    @Override
    public void run() {
        while (working) {

            /*
             * Not collecting yet - just sleep until we start actual work, or finish
             */
            while (collecting.get() == false) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (working == false) {
                    return;
                }
            }

            /*
             * Collecting is done in three stages:
             * 1. Send messages asking for values
             * 2. Wait for all the responses
             * 3. Print result
             */

            //1 send asks
            switch (snapshotType) {
                case NAIVE:
                    Map<Integer, Integer> naiveClock = CausalBroadcastShared.getVectorClock();
                    Message naiveAskMessage = new NaiveAskAmountMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, CausalBroadcastMessage.mapDeepCopy(naiveClock));

                    for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                        Message msg = naiveAskMessage.changeReceiver(neighbor);
                        MessageUtil.sendMessage(msg);
                    }

                    CausalBroadcastShared.addPendingMessage(naiveAskMessage);
                    CausalBroadcastShared.checkPendingMessages();

                    collectedNaiveValues.put("node" + AppConfig.myServentInfo.getId(), bitcakeManager.getCurrentBitcakeAmount());
                    break;
                case ACHARYA_BADRINATH:
                    Map<Integer, Integer> abClock = CausalBroadcastShared.getVectorClock();
                    Message abMessage = new ABMarkerMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, CausalBroadcastMessage.mapDeepCopy(abClock));

                    for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                        Message msg = abMessage.changeReceiver(neighbor);
                        MessageUtil.sendMessage(msg);
                    }

                    CausalBroadcastShared.addPendingMessage(abMessage);
                    CausalBroadcastShared.checkPendingMessages();

                    ABBitcakeManager abBitcakeManager = (ABBitcakeManager) bitcakeManager;
                    ABSnapshotResult abSnapshotResult = new ABSnapshotResult(AppConfig.myServentInfo.getId(), abBitcakeManager.getCurrentBitcakeAmount(), abBitcakeManager.getSent(), abBitcakeManager.getReceived());
                    collectedABValues.put(AppConfig.myServentInfo.getId(), abSnapshotResult);
                    break;
                case ALAGAR_VENKATESAN:
                    Map<Integer, Integer> avClock = CausalBroadcastShared.getVectorClock();
                    Message avMessage = new AVMarkerMessage(AppConfig.myServentInfo, AppConfig.myServentInfo, CausalBroadcastMessage.mapDeepCopy(avClock));

                    for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                        Message msg = avMessage.changeReceiver(neighbor);
                        MessageUtil.sendMessage(msg);
                    }

                    ((AVBitcakeManager)bitcakeManager).initializeChannelTransactions();
                    CausalBroadcastShared.addPendingMessage(avMessage);
                    CausalBroadcastShared.checkPendingMessages();
                    collectedAVValues.put(AppConfig.myServentInfo.getId(), bitcakeManager.getCurrentBitcakeAmount());
                    break;
                case NONE:
                    //Shouldn't be able to come here. See constructor.
                    break;
            }

            //2 wait for responses or finish
            boolean waiting = true;
            while (waiting) {
                switch (snapshotType) {
                    case NAIVE:
                        if (collectedNaiveValues.size() == AppConfig.getServentCount()) {
                            waiting = false;
                        }
                        break;
                    case ACHARYA_BADRINATH:
                        if (collectedABValues.size() == AppConfig.getServentCount()) {
                            waiting = false;
                        }
                        break;
                    case ALAGAR_VENKATESAN:
                        if (collectedAVValues.size() == AppConfig.getServentCount()) {
                            //send ask terminate messages
                            Map<Integer, Integer> avClock = CausalBroadcastShared.getVectorClock();
                            Message avMessage = new AVMarkerMessage(MessageType.AV_TERMINATE, AppConfig.myServentInfo, AppConfig.myServentInfo, CausalBroadcastMessage.mapDeepCopy(avClock));

                            AVBitcakeManager avBitcakeManager = (AVBitcakeManager) bitcakeManager;
                            AppConfig.timestampedStandardPrint("Channel transactions: " + avBitcakeManager.getChannelTransactions().toString());
                            for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                                Message msg = avMessage.changeReceiver(neighbor);
                                MessageUtil.sendMessage(msg);
                            }
                            waiting = false;
                        }
                        break;
                    case NONE:
                        //Shouldn't be able to come here. See constructor.
                        break;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (working == false) {
                    return;
                }
            }

            //print
            int sum;
            switch (snapshotType) {
                case NAIVE:
                    sum = 0;
                    for (Entry<String, Integer> itemAmount : collectedNaiveValues.entrySet()) {
                        sum += itemAmount.getValue();
                        AppConfig.timestampedStandardPrint(
                                "Info for " + itemAmount.getKey() + " = " + itemAmount.getValue() + " bitcake");
                    }

                    AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

                    collectedNaiveValues.clear(); //reset for next invocation
                    break;
                case ACHARYA_BADRINATH:
                    sum = 0;

                    for (Entry<Integer, ABSnapshotResult> nodeResult : collectedABValues.entrySet()) {
                        sum += nodeResult.getValue().getRecordedAmount();
                        AppConfig.timestampedStandardPrint(
                                "Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
                    }
                    for(int i = 0; i < AppConfig.getServentCount(); i++) {
                        for (int j = 0; j < AppConfig.getServentCount(); j++) {
                            if (i != j) {
                                if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
                                        AppConfig.getInfoById(j).getNeighbors().contains(i)) {
                                    int ijAmount = collectedABValues.get(i).getSent().get(j);
                                    int jiAmount = collectedABValues.get(j).getReceived().get(i);

                                    if (ijAmount != jiAmount) {
                                        String outputString = String.format(
                                                "Unreceived bitcake amount: %d from servent %d to servent %d",
                                                ijAmount - jiAmount, i, j);
                                        AppConfig.timestampedStandardPrint(outputString);
                                        sum += ijAmount - jiAmount;
                                    }
                                }
                            }
                        }
                    }

                    AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

                    collectedABValues.clear(); //reset for next invocation

                    break;
                case ALAGAR_VENKATESAN:
                    sum = 0;
                    for (Entry<Integer, Integer> itemAmount : collectedAVValues.entrySet()) {
                        sum += itemAmount.getValue();
                        AppConfig.timestampedStandardPrint(
                                "Info for " + itemAmount.getKey() + " = " + itemAmount.getValue() + " bitcake");
                    }

                    AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

                    collectedAVValues.clear(); //reset for next invocation
                    break;
                case NONE:
                    //Shouldn't be able to come here. See constructor.
                    break;
            }
            collecting.set(false);
        }

    }

    @Override
    public void addNaiveSnapshotInfo(String snapshotSubject, int amount) {
        collectedNaiveValues.put(snapshotSubject, amount);
    }

    @Override
    public void addABSnapshotInfo(int id, ABSnapshotResult abSnapshotResult) {
        collectedABValues.put(id, abSnapshotResult);
    }

    @Override
    public void addAVSnapshotInfo(int id, int amount) {
        collectedAVValues.put(id, amount);
    }

    @Override
    public void startCollecting() {
        boolean oldValue = this.collecting.getAndSet(true);

        if (oldValue == true) {
            AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
        }
    }

    @Override
    public void stop() {
        working = false;
    }

}
