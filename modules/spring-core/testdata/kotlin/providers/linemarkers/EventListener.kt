import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationEventPublisherAware
import org.springframework.context.ApplicationListener
import org.springframework.context.event.*
import org.springframework.scheduling.annotation.Async

internal open class BlockedListNotifier : ApplicationListener<BlockedListStartedEvent?> {
    private var notificationAddress: String? = null

    fun setNotificationAddress(notificationAddress: String?) {
        this.notificationAddress = notificationAddress
    }

    override fun onApplicationEvent(event: BlockedListStartedEvent?) {
    }
}

internal class BlockedListNotifierExt : BlockedListNotifier() {
    override fun onApplicationEvent(event: BlockedListStartedEvent?) {
    }
}

internal class WithoutEvent {
    fun onApplicationEvent(event: BlockedListStartedEvent?) {
    }
}

internal class BlockedListStartedEvent(source: Any?) : ContextStartedEvent(source as ApplicationContext?)

open internal class BlockedListNotifierOther {
    @EventListener
    fun processBlockedListEvent(event: BlockedListStartedEvent?) {
    }

    @EventListener(
        value = [ContextStartedEvent::class, ContextRefreshedEvent::class, ContextStoppedEvent::class, ContextClosedEvent::class]
    )
    fun handleContextStart() {
    }

    @EventListener(condition = "#blEvent.content == 'my-event'")
    fun processConditionalBlockedListEvent(blEvent: BlockedListStartedEvent?) {
    }

    @EventListener
    @Async
    open fun processAsyncBlockedListEvent(event: BlockedListStartedEvent?) {
    }
}

internal class EmailService(private val events: ApplicationEventPublisher) : ApplicationEventPublisherAware {
    private val blockedList: List<String> = emptyList()
    private var publisher: ApplicationEventPublisher = events

    fun startedEvent(address: String, content: String) {
        if (blockedList.contains(address)) {
            publisher.publishEvent(BlockedListStartedEvent(this))
        }
    }

    fun refreshedEvent(content: String) {
        if (blockedList.contains(content)) {
            publisher.publishEvent(BlockedListRefreshedEvent(this))
        }
    }

    fun closedEvent() {
        publisher.publishEvent(ContextClosedEvent(this as ApplicationContext))
    }

    fun stoppedEvent() {
        publisher.publishEvent(ContextStoppedEvent(this as ApplicationContext))
    }

    override fun setApplicationEventPublisher(applicationEventPublisher: ApplicationEventPublisher) {
    }
}

internal class BlockedListRefreshedEvent(source: Any?) : ContextRefreshedEvent(source as ApplicationContext?)