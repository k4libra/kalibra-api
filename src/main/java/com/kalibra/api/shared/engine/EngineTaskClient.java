package com.kalibra.api.shared.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamReadRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// Request/reply over the engine's Redis streams: a task is published on the tasks stream and the
// caller waits for the outcome with the same taskId on the results stream. Delivery is at least
// once, so an outcome nobody waits for any more (the caller timed out) is acknowledged and dropped;
// callers must be safe to repeat.
@Component
public class EngineTaskClient implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(EngineTaskClient.class);
    // Shorter than the Redis command timeout, so an idle stream is an empty read and not a timeout.
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(1);

    // The engine acknowledges what it consumed; old entries are only history.
    private static final long TASKS_MAX_LENGTH = 10_000;

    private final StringRedisTemplate redis;
    private final RedisConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;
    private final EngineProperties properties;
    private final Map<String, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final String consumerName = consumerName();

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private volatile boolean subscribed;
    private volatile boolean running;

    public EngineTaskClient(StringRedisTemplate redis,
                            RedisConnectionFactory connectionFactory,
                            ObjectMapper objectMapper,
                            EngineProperties properties) {
        this.redis = redis;
        this.connectionFactory = connectionFactory;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    // Returns the result body of the task (the same body as the engine's REST response).
    public JsonNode submit(String type, Object payload) {
        var taskId = UUID.randomUUID().toString();
        var outcome = new CompletableFuture<JsonNode>();
        pending.put(taskId, outcome);
        try {
            subscribe();
            redis.opsForStream().add(StreamRecords.string(Map.of(
                    "taskId", taskId,
                    "type", type,
                    "payload", objectMapper.writeValueAsString(payload)
            )).withStreamKey(properties.tasksStream()),
                    XAddOptions.maxlen(TASKS_MAX_LENGTH).approximateTrimming(true));
            return outcome.get(properties.taskTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException failed) {
            if (failed.getCause() instanceof EngineTaskFailedException taskFailed) {
                throw taskFailed;
            }
            throw new EngineUnavailableException("The adaptive engine task " + type + " failed", failed.getCause());
        } catch (TimeoutException timeout) {
            throw new EngineUnavailableException("The adaptive engine did not answer the task " + type + " in time");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new EngineUnavailableException("Interrupted while waiting for the adaptive engine", interrupted);
        } catch (JsonProcessingException | RuntimeException unreachable) {
            throw new EngineUnavailableException("The adaptive engine task queue is not reachable", unreachable);
        } finally {
            pending.remove(taskId);
        }
    }

    void onResult(MapRecord<String, String, String> record) {
        var fields = record.getValue();
        var outcome = pending.get(fields.getOrDefault("taskId", ""));
        if (outcome == null) {
            log.debug("Engine result {} has no waiting caller", fields.get("taskId"));
            return;
        }
        try {
            if ("SUCCEEDED".equals(fields.get("status"))) {
                outcome.complete(objectMapper.readTree(fields.getOrDefault("result", "null")));
            } else {
                var error = objectMapper.readTree(fields.getOrDefault("error", "{}"));
                outcome.completeExceptionally(new EngineTaskFailedException(
                        error.path("status").asInt(500),
                        error.path("detail").asText("The adaptive engine task failed")));
            }
        } catch (JsonProcessingException malformed) {
            outcome.completeExceptionally(malformed);
        }
    }

    // Lazy and repeatable: the API still starts (and retries here) when Redis is down at boot.
    private synchronized void subscribe() {
        if (subscribed) {
            return;
        }
        try {
            redis.opsForStream().createGroup(properties.resultsStream(), ReadOffset.latest(), properties.resultsGroup());
        } catch (RuntimeException alreadyThere) {
            if (!String.valueOf(rootMessage(alreadyThere)).contains("BUSYGROUP")) {
                throw alreadyThere;
            }
        }
        var options = StreamMessageListenerContainerOptions.builder().pollTimeout(POLL_TIMEOUT).build();
        container = StreamMessageListenerContainer.create(connectionFactory, options);
        container.register(StreamReadRequest
                        .builder(StreamOffset.create(properties.resultsStream(), ReadOffset.lastConsumed()))
                        .consumer(Consumer.from(properties.resultsGroup(), consumerName))
                        .autoAcknowledge(true)
                        .cancelOnError(failure -> false)
                        .errorHandler(failure -> log.warn("Engine results stream read failed: {}", failure.getMessage()))
                        .build(),
                this::onResult);
        container.start();
        subscribed = true;
    }

    @Override
    public void start() {
        running = true;
        try {
            subscribe();
        } catch (RuntimeException redisDown) {
            log.warn("Engine task queue is not reachable yet; it is retried on the next task: {}", redisDown.getMessage());
        }
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (container != null) {
            container.stop();
        }
        subscribed = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private static String rootMessage(Throwable failure) {
        var cause = failure;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    private static String consumerName() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + ProcessHandle.current().pid();
        } catch (Exception unknownHost) {
            return "kalibra-api-" + ProcessHandle.current().pid();
        }
    }
}
