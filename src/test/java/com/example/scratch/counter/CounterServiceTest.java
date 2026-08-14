package com.example.scratch.counter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CounterServiceTest {

    private static final int CONCURRENT_INCREMENTS = 50;

    private CounterService counterService;

    @BeforeEach
    void setUp() {
        counterService = new CounterService();
    }

    @Test
    void increment_WhenCounterIsNew_ReturnsValueOne() {
        CounterResponse response = counterService.increment("jobs");

        assertThat(response.name()).isEqualTo("jobs");
        assertThat(response.value()).isEqualTo(1L);
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void increment_WhenCalledTwice_ReturnsValueTwo() {
        counterService.increment("jobs");
        CounterResponse response = counterService.increment("jobs");

        assertThat(response.value()).isEqualTo(2L);
    }

    @Test
    void get_AfterIncrement_ReturnsSameValue() {
        counterService.increment("jobs");

        CounterResponse response = counterService.get("jobs");

        assertThat(response.name()).isEqualTo("jobs");
        assertThat(response.value()).isEqualTo(1L);
    }

    @Test
    void get_WhenCounterUnknown_ThrowsCounterNotFoundException() {
        assertThatThrownBy(() -> counterService.get("missing"))
                .isInstanceOf(CounterNotFoundException.class);
    }

    @Test
    void increment_WhenCalledConcurrently_NoLostUpdates() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_INCREMENTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_INCREMENTS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENT_INCREMENTS);

        try {
            for (int i = 0; i < CONCURRENT_INCREMENTS; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        counterService.increment("concurrent");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            ready.await();
            start.countDown();
            boolean completed = done.await(10, TimeUnit.SECONDS);

            assertThat(completed).isTrue();
            assertThat(counterService.get("concurrent").value()).isEqualTo(CONCURRENT_INCREMENTS);
        } finally {
            executor.shutdownNow();
        }
    }
}
