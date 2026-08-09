package com.example;

import com.example.service.AccountService;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyTest {

    public static void runStressTest(AccountService accountService) {
        System.out.println("\n=========================================");
        System.out.println("INITIATING CONCURRENCY STRESS TEST");
        System.out.println("=========================================");
        
        int totalThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch latch = new CountDownLatch(totalThreads);
        
        AtomicInteger successfulTransfers = new AtomicInteger(0);
        AtomicInteger failedTransfers = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    boolean success;
                    // Half the threads move money A -> B, the other half B -> A
                    if (threadNum % 2 == 0) {
                        success = accountService.transferFunds(3, 4, new BigDecimal("1.00"), 1);
                    } else {
                        success = accountService.transferFunds(4, 3, new BigDecimal("1.00"), 1);
                    }

                    if (success) {
                        successfulTransfers.incrementAndGet();
                    } else {
                        failedTransfers.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedTransfers.incrementAndGet();
                } finally {
                    latch.countDown(); // Tell the main thread we are done
                }
            });
        }

        try {
            latch.await(); // Wait for all 100 threads to finish
            executor.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();

        System.out.println("\nTEST COMPLETE in " + (endTime - startTime) + "ms");
        System.out.println("Successful Transfers: " + successfulTransfers.get());
        System.out.println("Failed Transfers:     " + failedTransfers.get());
        System.out.println("=========================================\n");
    }
}