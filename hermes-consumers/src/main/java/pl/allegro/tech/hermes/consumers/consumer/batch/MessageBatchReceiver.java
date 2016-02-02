package pl.allegro.tech.hermes.consumers.consumer.batch;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.MessageBatchWrapper;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceivingTimeoutException;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.ArrayDeque;
import java.util.Queue;

public class MessageBatchReceiver {
    private final MessageReceiver receiver;
    private final MessageBatchFactory batchFactory;
    private final MessageBatchWrapper messageBatchWrapper;
    private final HermesMetrics hermesMetrics;
    private final Trackers trackers;
    private final Queue<Message> inflight;
    private boolean receiving = true;

    public MessageBatchReceiver(MessageReceiver receiver,
                                MessageBatchFactory batchFactory,
                                MessageBatchWrapper messageBatchWrapper,
                                HermesMetrics hermesMetrics,
                                Trackers trackers) {
        this.receiver = receiver;
        this.batchFactory = batchFactory;
        this.messageBatchWrapper = messageBatchWrapper;
        this.hermesMetrics = hermesMetrics;
        this.trackers = trackers;
        this.inflight = new ArrayDeque<>(1);
    }

    public MessageBatch next(Subscription subscription) {
        MessageBatch batch = batchFactory.createBatch(subscription);
        while (isReceiving() && !batch.isReadyForDelivery()) {
            try {
                Message message = inflight.isEmpty() ? receive(subscription, batch.getId()) : inflight.poll();
                byte[] data = messageBatchWrapper.wrap(message);
                if (batch.canFit(data)) {
                    batch.append(data, messageMetadata(subscription, batch.getId(), message));
                } else {
                    inflight.offer(message);
                    break;
                }
            } catch (MessageReceivingTimeoutException ex) {
                // ignore
            }
        }
        return batch.close();
    }

    private Message receive(Subscription subscription, String batchId) {
        Message next = receiver.next();
        hermesMetrics.incrementInflightCounter(subscription);
        trackers.get(subscription).logInflight(messageMetadata(subscription, batchId, next));
        return next;
    }

    private MessageMetadata messageMetadata(Subscription subscription, String batchId, Message next) {
        return new MessageMetadata(next.getId(), batchId, next.getOffset(), next.getPartition(), subscription.getQualifiedTopicName(), subscription.getName(), next.getPublishingTimestamp(), next.getReadingTimestamp());
    }

    private boolean isReceiving() {
        return receiving;
    }

    public void stop() {
        receiving = false;
    }
}
