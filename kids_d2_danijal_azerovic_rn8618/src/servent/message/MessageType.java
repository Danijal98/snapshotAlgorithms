package servent.message;

public enum MessageType {
	CAUSAL_BROADCAST,
	TRANSACTION,
	NAIVE_TOKEN_AMOUNT,
	NAIVE_ASK_AMOUNT, NAIVE_TELL_AMOUNT,
	AB_MARKER, AB_TELL,
	AV_MARKER, AV_TERMINATE, AV_DONE
}
