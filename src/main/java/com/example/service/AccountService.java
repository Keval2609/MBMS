package com.example.service;

import com.example.dao.AccountDAO;
import com.example.dao.CustomerDAO;
import com.example.dao.TransactionDAO;
import com.example.dao.TransactionManager;
import com.example.domain.*;
import com.example.exception.AccountClosedException;
import com.example.exception.AccountFrozenException;
import com.example.exception.AccountNotFoundException;
import com.example.exception.CustomerNotFoundException;
import com.example.exception.InsufficientFundsException;
import com.example.exception.InvalidTransferException;
import com.example.factory.AccountFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.UUID;

public class AccountService {

    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final TransactionManager transactionManager;
    private final CustomerDAO customerDAO;

    public AccountService(AccountDAO accountDAO, TransactionDAO transactionDAO, TransactionManager transactionManager) {
        this(accountDAO, transactionDAO, transactionManager, null);
    }

    public AccountService(AccountDAO accountDAO, TransactionDAO transactionDAO, TransactionManager transactionManager, CustomerDAO customerDAO) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.transactionManager = transactionManager;
        this.customerDAO = customerDAO;
    }

    public Transaction transferFunds(SessionContext session, Integer fromAccountId, Integer toAccountId, BigDecimal amount) {
        session.requireRole(Role.TELLER, Role.MANAGER, Role.BRANCH_MANAGER, Role.ADMIN);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be strictly greater than zero");
        }
        if (fromAccountId == null || toAccountId == null) {
            throw new IllegalArgumentException("Account IDs cannot be null");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidTransferException("Cannot transfer funds to the same account (ID: " + fromAccountId + ")");
        }

        BigDecimal transferAmount = amount.setScale(2, RoundingMode.HALF_EVEN);

        return transactionManager.inTransaction(conn -> {
            // Deadlock Prevention: Always acquire pessimistic row locks in deterministic ascending ID order
            Integer lowerId = Math.min(fromAccountId, toAccountId);
            Integer higherId = Math.max(fromAccountId, toAccountId);

            Account lowerAccount = accountDAO.findByIdForUpdate(conn, lowerId)
                    .orElseThrow(() -> new AccountNotFoundException(lowerId));
            Account higherAccount = accountDAO.findByIdForUpdate(conn, higherId)
                    .orElseThrow(() -> new AccountNotFoundException(higherId));

            Account fromAccount = lowerId.equals(fromAccountId) ? lowerAccount : higherAccount;
            Account toAccount = lowerId.equals(toAccountId) ? lowerAccount : higherAccount;

            // Status checks
            if (fromAccount.getStatus() == AccountStatus.FROZEN) {
                throw new AccountFrozenException("Source account is frozen: " + fromAccountId);
            }
            if (fromAccount.getStatus() == AccountStatus.CLOSED) {
                throw new AccountClosedException("Source account is closed: " + fromAccountId);
            }
            if (toAccount.getStatus() == AccountStatus.FROZEN) {
                throw new AccountFrozenException("Destination account is frozen: " + toAccountId);
            }
            if (toAccount.getStatus() == AccountStatus.CLOSED) {
                throw new AccountClosedException("Destination account is closed: " + toAccountId);
            }

            // Invariant check: balance sufficiency
            if (fromAccount.getBalance().compareTo(transferAmount) < 0) {
                throw new InsufficientFundsException(
                        "Insufficient funds in account " + fromAccountId + ". Current balance: $" + fromAccount.getBalance());
            }

            BigDecimal newFromBalance = fromAccount.getBalance().subtract(transferAmount);
            BigDecimal newToBalance = toAccount.getBalance().add(transferAmount);

            accountDAO.updateBalance(conn, fromAccountId, newFromBalance);
            accountDAO.updateBalance(conn, toAccountId, newToBalance);

            Transaction txn = new Transaction(
                    null,
                    UUID.randomUUID().toString(),
                    fromAccountId,
                    toAccountId,
                    transferAmount,
                    TransactionType.TRANSFER,
                    TransactionStatus.COMPLETED,
                    session.employeeId(),
                    new Timestamp(System.currentTimeMillis())
            );
            transactionDAO.insert(conn, txn);

            return txn;
        });
    }

    public Transaction deposit(SessionContext session, Integer accountId, BigDecimal amount) {
        session.requireRole(Role.TELLER, Role.MANAGER, Role.BRANCH_MANAGER, Role.ADMIN);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be strictly greater than zero");
        }
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }

        BigDecimal depositAmount = amount.setScale(2, RoundingMode.HALF_EVEN);

        return transactionManager.inTransaction(conn -> {
            Account account = accountDAO.findByIdForUpdate(conn, accountId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));

            if (account.getStatus() == AccountStatus.FROZEN) {
                throw new AccountFrozenException("Account is frozen: " + accountId);
            }
            if (account.getStatus() == AccountStatus.CLOSED) {
                throw new AccountClosedException("Account is closed: " + accountId);
            }

            BigDecimal newBalance = account.getBalance().add(depositAmount);
            accountDAO.updateBalance(conn, accountId, newBalance);

            Transaction txn = new Transaction(
                    null,
                    UUID.randomUUID().toString(),
                    null, // Deposits have no source account
                    accountId,
                    depositAmount,
                    TransactionType.DEPOSIT,
                    TransactionStatus.COMPLETED,
                    session.employeeId(),
                    new Timestamp(System.currentTimeMillis())
            );
            transactionDAO.insert(conn, txn);

            return txn;
        });
    }

    public Transaction withdraw(SessionContext session, Integer accountId, BigDecimal amount) {
        session.requireRole(Role.TELLER, Role.MANAGER, Role.BRANCH_MANAGER, Role.ADMIN);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be strictly greater than zero");
        }
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }

        BigDecimal withdrawAmount = amount.setScale(2, RoundingMode.HALF_EVEN);

        return transactionManager.inTransaction(conn -> {
            Account account = accountDAO.findByIdForUpdate(conn, accountId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));

            if (account.getStatus() == AccountStatus.FROZEN) {
                throw new AccountFrozenException("Account is frozen: " + accountId);
            }
            if (account.getStatus() == AccountStatus.CLOSED) {
                throw new AccountClosedException("Account is closed: " + accountId);
            }

            if (account.getBalance().compareTo(withdrawAmount) < 0) {
                throw new InsufficientFundsException(
                        "Insufficient funds in account " + accountId + ". Current balance: $" + account.getBalance());
            }

            BigDecimal newBalance = account.getBalance().subtract(withdrawAmount);
            accountDAO.updateBalance(conn, accountId, newBalance);

            Transaction txn = new Transaction(
                    null,
                    UUID.randomUUID().toString(),
                    accountId,
                    null, // Withdrawals have no destination account
                    withdrawAmount,
                    TransactionType.WITHDRAWAL,
                    TransactionStatus.COMPLETED,
                    session.employeeId(),
                    new Timestamp(System.currentTimeMillis())
            );
            transactionDAO.insert(conn, txn);

            return txn;
        });
    }

    public Account openAccount(SessionContext session, Integer customerId, Integer branchId, 
                               AccountType accountType, BigDecimal initialDeposit) {
        session.requireRole(Role.TELLER, Role.MANAGER, Role.BRANCH_MANAGER, Role.ADMIN);

        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (customerDAO == null) {
            throw new IllegalStateException("CustomerDAO is not configured in AccountService");
        }

        Integer effectiveBranchId = branchId != null ? branchId : session.branchId();
        if (effectiveBranchId == null) {
            effectiveBranchId = 1;
        }

        AccountType effectiveType = accountType != null ? accountType : AccountType.SAVINGS;
        BigDecimal deposit = initialDeposit != null 
                ? initialDeposit.setScale(2, RoundingMode.HALF_EVEN) 
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

        if (deposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative");
        }

        final Integer finalBranchId = effectiveBranchId;
        return transactionManager.inTransaction(conn -> {
            // 1. Verify customer exists
            Customer customer = customerDAO.findById(conn, customerId)
                    .orElseThrow(() -> new CustomerNotFoundException(customerId));

            // 2. Generate sequential account number
            String accountNumber = accountDAO.generateNextAccountNumber(conn);

            // 3. Create account through AccountFactory
            Account newAccount = AccountFactory.create(effectiveType, customer.getId(), finalBranchId, accountNumber, deposit);

            // 4. Persist account
            Account created = accountDAO.insert(conn, newAccount);

            // 5. If initial deposit > 0, record initial deposit transaction
            if (deposit.compareTo(BigDecimal.ZERO) > 0) {
                Transaction initialTxn = new Transaction(
                        null,
                        UUID.randomUUID().toString(),
                        null,
                        created.getId(),
                        deposit,
                        TransactionType.DEPOSIT,
                        TransactionStatus.COMPLETED,
                        session.employeeId() != null ? session.employeeId() : 1,
                        new Timestamp(System.currentTimeMillis())
                );
                transactionDAO.insert(conn, initialTxn);
            }

            return created;
        });
    }
}