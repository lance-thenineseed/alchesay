package com.nineseed.alchesay.tracker;

import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.fail;

public class MultiThreadHelper {
    private final List<Integer> threadCounts;
    private final List<NamedRunnable> runnables;
    private final Duration testDuration;
    @Getter
    private final List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

    public MultiThreadHelper(List<NamedRunnable> runnables, List<Integer> threadCounts, Duration testDuration) {
        this.threadCounts = List.copyOf(threadCounts);
        this.runnables = List.copyOf(runnables);
        this.testDuration = testDuration;
    }

    public void test() throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        List<Integer> effectiveThreadCounts = new ArrayList<>(runnables.size());
        failures.clear();

        for(int i = 0; i < runnables.size(); i++) {
            if(i < threadCounts.size()) {
                effectiveThreadCounts.add(threadCounts.get(i));
            } else {
                effectiveThreadCounts.add(20);
            }
        }

        int maxThreadCount = Collections.max(effectiveThreadCounts);
        LocalDateTime endTime = LocalDateTime.now().plus(testDuration);

        for(int threadCount = 0; threadCount < maxThreadCount; threadCount++) {
            for (int i = 0; i < runnables.size(); i++) {
                if(threadCount < effectiveThreadCounts.get(i)) {
                    NamedRunnable namedRunnable = runnables.get(i);
                    Thread thread = new Thread(() -> {
                        while (LocalDateTime.now().isBefore(endTime) && failures.isEmpty()) {
                            try {
                                namedRunnable.run();
                            } catch (Throwable e) {
                                failures.add(new AssertionError(namedRunnable.name, e));
                            }
                        }
                    }, namedRunnable.name + "-" + threadCount);
                    threads.add(thread);
                    thread.start();
                }
            }
        }
        for (Thread thread : threads) {
            thread.join();
        }

        if (!failures.isEmpty()) {
            fail(failures.size() + " failure(s); first: " + failures.getFirst().getMessage(), failures.getFirst());
        }
    }

    public static class NamedRunnable implements Runnable {
        private final String name;
        private final Runnable runnable;
        public NamedRunnable(String name, Runnable runnable) {
            this.name = name;
            this.runnable = runnable;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }
}
