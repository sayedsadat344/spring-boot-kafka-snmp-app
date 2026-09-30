package com.traps.RoshanNOCTraps.kafka.producer;

import com.mycompany.app.sharedClasses.BssHwTrapBody;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Service
public class HwTrapProducer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HwTrapProducer.class);

    private static final String TOPIC = "HW_TRAPS";

    private static final String FAILED_FOLDER = "FAILED-HW-TRAPS";
    private static final String RETRY_FOLDER = "RETRY-HW-TRAPS";
    private static final String SUCCESS_FOLDER = "SUCCESS-HW-TRAPS";

    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 1000;

    private final KafkaTemplate<String, BssHwTrapBody> kafkaTemplate;

    private final ScheduledExecutorService retryExecutor =
            Executors.newScheduledThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("hw-trap-retry");
                thread.setDaemon(false);
                return thread;
            });

    public HwTrapProducer(
            KafkaTemplate<String, BssHwTrapBody> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Start sending a trap.
     */
    public void sendMessage(BssHwTrapBody data) {
        if (data == null) {
            LOGGER.error("Cannot send HW trap: data is null");
            return;
        }

        sendWithRetry(data, 1);
    }

    /**
     * Send a trap to Kafka.
     */
    private void sendWithRetry(BssHwTrapBody data, int attempt) {

        LOGGER.info(
                "Sending HW trap | Alarm ID: {} | Alarm Code: {} | Attempt: {}/{}",
                data.getTrapId(),
                data.getAlarmCode(),
                attempt,
                MAX_RETRIES
        );

        try {
            Message<BssHwTrapBody> message = MessageBuilder
                    .withPayload(data)
                    .setHeader(KafkaHeaders.TOPIC, TOPIC)
                    .setHeader(
                            KafkaHeaders.KEY,
                            data.getTrapId().toString()
                    )
                    .build();

            kafkaTemplate.send(message)
                    .whenComplete((result, ex) -> {

                        if (ex != null) {
                            handleFailure(data, attempt, ex);
                            return;
                        }

                        handleSuccess(data, attempt, result);
                    });

        } catch (Exception ex) {
            handleFailure(data, attempt, ex);
        }
    }

    /**
     * Handle successful Kafka delivery.
     */
    private void handleSuccess(
            BssHwTrapBody data,
            int attempt,
            org.springframework.kafka.support.SendResult<
                    String, BssHwTrapBody> result) {

        try {
            LocalDateTime now = LocalDateTime.now();

            String log = String.format(
                    "%s | Alarm ID: %s | Alarm Code: %s | Attempt: %d"
                            + " | Topic: %s | Partition: %d"
                            + " | Offset: %d | SUCCESS",
                    now,
                    data.getTrapId(),
                    data.getAlarmCode(),
                    attempt,
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );

            LOGGER.info(log);

            appendData(
                    SUCCESS_FOLDER,
                    data.getAlarmCode(),
                    log
            );

        } catch (Exception ex) {
            LOGGER.error(
                    "Failed to process HW trap success callback"
                            + " | Alarm ID: {} | Attempt: {}",
                    data.getTrapId(),
                    attempt,
                    ex
            );
        }
    }

    /**
     * Handle Kafka send failure and schedule retries.
     */
    private void handleFailure(
            BssHwTrapBody data,
            int attempt,
            Throwable ex) {

        LocalDateTime now = LocalDateTime.now();

        String rootCause = getRootCause(ex);

        String errorLog = String.format(
                "%s | Alarm ID: %s | Alarm Code: %s"
                        + " | Attempt: %d/%d | FAILED"
                        + " | Root Cause: %s",
                now,
                data.getTrapId(),
                data.getAlarmCode(),
                attempt,
                MAX_RETRIES,
                rootCause
        );

        LOGGER.error(errorLog, ex);

        appendData(
                FAILED_FOLDER,
                data.getAlarmCode(),
                errorLog
        );

        if (attempt < MAX_RETRIES) {
            scheduleRetry(data, attempt);
        } else {
            logPermanentFailure(data, attempt);
        }
    }

    /**
     * Schedule the next retry.
     */
    private void scheduleRetry(
            BssHwTrapBody data,
            int currentAttempt) {

        long delay = RETRY_DELAY_MS * currentAttempt;
        int nextAttempt = currentAttempt + 1;

        String retryLog = String.format(
                "%s | Alarm ID: %s | Alarm Code: %s"
                        + " | Current Attempt: %d"
                        + " | Next Attempt: %d"
                        + " | Delay: %d ms | RETRY SCHEDULED",
                LocalDateTime.now(),
                data.getTrapId(),
                data.getAlarmCode(),
                currentAttempt,
                nextAttempt,
                delay
        );

        LOGGER.warn(retryLog);

        appendData(
                RETRY_FOLDER,
                data.getAlarmCode(),
                retryLog
        );

        try {
            retryExecutor.schedule(
                    () -> sendWithRetry(data, nextAttempt),
                    delay,
                    TimeUnit.MILLISECONDS
            );

        } catch (Exception ex) {
            LOGGER.error(
                    "Failed to schedule HW trap retry"
                            + " | Alarm ID: {}"
                            + " | Next Attempt: {}",
                    data.getTrapId(),
                    nextAttempt,
                    ex
            );

            appendData(
                    FAILED_FOLDER,
                    data.getAlarmCode(),
                    String.format(
                            "%s | Alarm ID: %s | Alarm Code: %s"
                                    + " | Next Attempt: %d"
                                    + " | RETRY SCHEDULING FAILED"
                                    + " | Error: %s",
                            LocalDateTime.now(),
                            data.getTrapId(),
                            data.getAlarmCode(),
                            nextAttempt,
                            getRootCause(ex)
                    )
            );
        }
    }

    /**
     * Log permanent failure after exhausting all attempts.
     */
    private void logPermanentFailure(
            BssHwTrapBody data,
            int attempt) {

        String finalLog = String.format(
                "%s | Alarm ID: %s | Alarm Code: %s"
                        + " | PERMANENT FAILURE"
                        + " | Total Attempts: %d",
                LocalDateTime.now(),
                data.getTrapId(),
                data.getAlarmCode(),
                attempt
        );

        LOGGER.error(finalLog);

        appendData(
                FAILED_FOLDER,
                data.getAlarmCode(),
                finalLog
        );
    }

    /**
     * Get the deepest available exception cause.
     */
    private String getRootCause(Throwable ex) {

        if (ex == null) {
            return "Unknown error";
        }

        Throwable cause = ex;

        while (cause.getCause() != null
                && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        return cause.getClass().getName()
                + ": "
                + cause.getMessage();
    }

    /**
     * Append a log entry to the specified folder.
     */
    private void appendData(
            String folder,
            String fileName,
            String line) {

        try {
            Path dirPath = Paths.get(folder);

            Files.createDirectories(dirPath);

            String safeFileName = fileName == null
                    ? "unknown"
                    : fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

            Path filePath = dirPath.resolve(safeFileName + ".log");

            Files.write(
                    filePath,
                    (line + System.lineSeparator())
                            .getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException ex) {
            LOGGER.error(
                    "Failed to write HW trap log"
                            + " | Folder: {} | File: {}",
                    folder,
                    fileName,
                    ex
            );
        }
    }

    /**
     * Gracefully shut down the retry executor.
     */
    @PreDestroy
    public void shutdown() {
        LOGGER.info("Shutting down HW trap retry executor");

        retryExecutor.shutdown();

        try {
            if (!retryExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                LOGGER.warn(
                        "HW trap retry executor did not terminate"
                                + " within 30 seconds"
                );

                retryExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            retryExecutor.shutdownNow();
            Thread.currentThread().interrupt();

            LOGGER.warn(
                    "Interrupted while shutting down HW trap retry executor",
                    ex
            );
        }
    }
}


//@Service
//public class HwTrapProducer {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(ZteTrapProducer.class);
//
//    private KafkaTemplate<String, BssHwTrapBody> kafkaTemplate;
//
//    public HwTrapProducer(KafkaTemplate<String, BssHwTrapBody> kafkaTemplate) {
//        this.kafkaTemplate = kafkaTemplate;
//    }
//
////    public void sendMessage(BssHwTrapBody data){
////
////        Message<BssHwTrapBody> message = MessageBuilder
////                .withPayload(data)
////                .setHeader(KafkaHeaders.TOPIC, "HW_TRAPS")
////                .build();
////
////        System.out.println("HW TRAP PRODUCED!!");
////        kafkaTemplate.send(message);
////    }
//
//    public void sendMessage(BssHwTrapBody data) {
//
//        Message<BssHwTrapBody> message = MessageBuilder
//                .withPayload(data)
//                .setHeader(KafkaHeaders.TOPIC, "HW_TRAPS")
//                .build();
//
////        Message<BssHwTrapBody> message = MessageBuilder
////                .withPayload(data)
////                .setHeader(KafkaHeaders.TOPIC, "HW_TRAPS")
////                .setHeader(KafkaHeaders.KEY, data.getTrapId().toString())
////                .build();
//
//        kafkaTemplate.send(message).whenComplete((result, ex) -> {
//
//            if (ex != null) {
//                log.error(
//                        "HW TRAP DELIVERY FAILED | Alarm ID: {} | Error: {}",
//                        data.getTrapId(),
//                        ex.getMessage(),
//                        ex
//                );
//            } else {
//                log.info(
//                        "HW TRAP DELIVERED | Alarm ID: {} | Topic: {} | Partition: {} | Offset: {}",
//                        data.getTrapId(),
//                        result.getRecordMetadata().topic(),
//                        result.getRecordMetadata().partition(),
//                        result.getRecordMetadata().offset()
//                );
//            }
//        });
//    }
//}
