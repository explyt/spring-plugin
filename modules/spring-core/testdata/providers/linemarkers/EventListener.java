import org.springframework.context.*;
import org.springframework.context.event.*;
import org.springframework.scheduling.annotation.Async;

import java.util.List;

class BlockedListNotifier implements ApplicationListener<BlockedListStartedEvent> {

    private String notificationAddress;

    public void setNotificationAddress(String notificationAddress) {
        this.notificationAddress = notificationAddress;
    }

    @Override
    public void onApplicationEvent(BlockedListStartedEvent event) {
    }
}

class BlockedListNotifierExt extends BlockedListNotifier {
    @Override
    public void onApplicationEvent(BlockedListStartedEvent event) {
    }
}

class WithoutEvent {
    public void onApplicationEvent(BlockedListStartedEvent event) {
    }
}

class BlockedListStartedEvent extends ContextStartedEvent {
    public BlockedListStartedEvent(Object source) {
        super((ApplicationContext) source);
    }
}

class BlockedListNotifierOther {
    @EventListener
    public void processBlockedListEvent(BlockedListStartedEvent event) {
    }

    @EventListener(value = {ContextStartedEvent.class, ContextRefreshedEvent.class,
            ContextStoppedEvent.class, ContextClosedEvent.class})
    public void handleContextStart() {
    }

    @EventListener(condition = "#blEvent.content == 'my-event'")
    public void processConditionalBlockedListEvent(BlockedListStartedEvent blEvent) {
    }

    @EventListener
    @Async
    public void processAsyncBlockedListEvent(BlockedListStartedEvent event) {
    }

}

class EmailService implements ApplicationEventPublisherAware {

    private List<String> blockedList;
    private ApplicationEventPublisher publisher;

    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    void startedEvent(String address, String content) {
        if (blockedList.contains(address)) {
            publisher.publishEvent(new BlockedListStartedEvent(this));
        }
    }

    public void refreshedEvent(String content) {
        if (blockedList.contains(content)) {
            publisher.publishEvent(new BlockedListRefreshedEvent(this));
        }
    }

    public void closedEvent() {
        publisher.publishEvent(new ContextClosedEvent((ApplicationContext) this));
    }

    public void stoppedEvent() {
        publisher.publishEvent(new ContextStoppedEvent((ApplicationContext) this));
    }
}

class BlockedListRefreshedEvent extends ContextRefreshedEvent {
    public BlockedListRefreshedEvent(Object source) {
        super((ApplicationContext) source);
    }

}
