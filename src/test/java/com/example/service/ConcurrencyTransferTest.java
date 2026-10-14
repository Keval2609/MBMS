package com.example.service;

import com.example.dao.AccountDAO;
import com.example.dao.TransactionDAO;
import com.example.dao.TransactionManager;
import com.example.domain.*;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrencyTransferTest {

    private final SessionContext tellerSession = new SessionContext(1, 1, "teller", Role.TELLER);

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    @Test
    public void testConcurrentAsymmetricTransfersConserveTotalBalance() throws Exception {
        // Setup Account 1 and Account 2 with known starting balances ($10,000 each)
        final Map<Integer, BigDecimal> balances = new ConcurrentHashMap<>();
        balances.put(1, new BigDecimal("10000.00"));
        balances.put(2, new BigDecimal("10000.00"));

        final Map<Integer, ReentrantLock> rowLocks = new ConcurrentHashMap<>();
        rowLocks.put(1, new ReentrantLock());
        rowLocks.put(2, new ReentrantLock());

        final List<Transaction> recordedTransactions = Collections.synchronizedList(new ArrayList<>());

        // Thread-local to track which row locks a connection holds so they can be released on commit/rollback
        final ThreadLocal<List<ReentrantLock>> heldLocks = ThreadLocal.withInitial(ArrayList::new);

        Connection connectionProxy = createProxy(Connection.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("commit".equals(name) || "rollback".equals(name) || "close".equals(name)) {
                List<ReentrantLock> locks = heldLocks.get();
                for (ReentrantLock lock : locks) {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
                locks.clear();
                return null;
            }
            if ("setAutoCommit".equals(name)) return null;
            if ("getAutoCommit".equals(name)) return true;
            return null;
        });

        DataSource dataSource = createProxy(DataSource.class, (proxy, method, args) -> {
            if ("getConnection".equals(method.getName())) {
                return connectionProxy;
            }
            return null;
        });

        TransactionManager transactionManager = new TransactionManager(dataSource);

        AccountDAO accountDAO = createProxy(AccountDAO.class, (proxy, method, args) -> {
            String name = method.getName();
            if ("findByIdForUpdate".equals(name)) {
                Integer id = (Integer) args[1];
                ReentrantLock lock = rowLocks.get(id);
                lock.lock();
                heldLocks.get().add(lock);

                BigDecimal currentBal = balances.get(id);
                Account account = new Account(id, 10, "ACC-" + id, 1, AccountType.SAVINGS, currentBal, AccountStatus.ACTIVE, 0);
                return Optional.of(account);
            }
            if ("updateBalance".equals(name)) {
                Integer id = (Integer) args[1];
                BigDecimal newBal = (BigDecimal) args[2];
                balances.put(id, newBal.setScale(2, RoundingMode.HALF_EVEN));
                return null;
            }
            return null;
        });

        TransactionDAO transactionDAO = createProxy(TransactionDAO.class, (proxy, method, args) -> {
            if ("insert".equals(method.getName())) {
                Transaction txn = (Transaction) args[1];
                recordedTransactions.add(txn);
                return null;
            }
            return null;
        });

        AccountService accountService = new AccountService(accountDAO, transactionDAO, transactionManager);

        int threadsAtoB = 30; // 30 transfers of $10.00 from Acc 1 -> Acc 2
        int threadsBtoA = 20; // 20 transfers of $25.00 from Acc 2 -> Acc 1
        int totalThreads = threadsAtoB + threadsBtoA;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CyclicBarrier startBarrier = new CyclicBarrier(totalThreads);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadsAtoB; i++) {
            executor.submit(() -> {
                try {
                    startBarrier.await();
                    accountService.transferFunds(tellerSession, 1, 2, new BigDecimal("10.00"));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        for (int i = 0; i < threadsBtoA; i++) {
            executor.submit(() -> {
                try {
                    startBarrier.await();
                    accountService.transferFunds(tellerSession, 2, 1, new BigDecimal("25.00"));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All concurrent transfers must complete within timeout");
        executor.shutdown();

        // Verification: All 50 transfers must succeed without lost updates or deadlocks
        assertEquals(totalThreads, successCount.get(), "All concurrent transfers must succeed");
        assertEquals(0, failureCount.get(), "Zero failures expected under serialized row locks");

        // Mathematical verification of final balances:
        // Acc 1 net change: -(30 * 10) + (20 * 25) = -300 + 500 = +200 -> 10,200.00
        // Acc 2 net change: +(30 * 10) - (20 * 25) = +300 - 500 = -200 -> 9,800.00
        BigDecimal finalBal1 = balances.get(1);
        BigDecimal finalBal2 = balances.get(2);

        assertEquals(new BigDecimal("10200.00"), finalBal1, "Account 1 must reflect exact net change (+200.00)");
        assertEquals(new BigDecimal("9800.00"), finalBal2, "Account 2 must reflect exact net change (-200.00)");

        // Balance Conservation Law: Total initial balance must strictly equal total final balance
        BigDecimal totalFinal = finalBal1.add(finalBal2);
        assertEquals(new BigDecimal("20000.00"), totalFinal, "Total money across accounts must be strictly conserved");

        // Transaction Audit Conservation: Every successful transfer must insert exactly 1 transaction record
        assertEquals(totalThreads, recordedTransactions.size(), "Transaction audit count must equal total successful transfers");
    }
}
