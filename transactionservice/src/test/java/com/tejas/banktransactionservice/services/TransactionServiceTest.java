package com.tejas.banktransactionservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tejas.bankingcommon.dto.DepositRequest;
import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.dto.TransferRequest;
import com.tejas.bankingcommon.dto.WithdrawRequest;
import com.tejas.bankingcommon.enums.TransactionStatus;
import com.tejas.bankingcommon.enums.TransactionType;
import com.tejas.bankingcommon.exceptions.BadRequestException;
import com.tejas.bankingcommon.exceptions.ForbiddenException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NoContentException;
import com.tejas.bankingcommon.exceptions.NotFoundException;
import com.tejas.banktransactionservice.feign.AccountInterface;
import com.tejas.banktransactionservice.models.Transaction;
import com.tejas.banktransactionservice.models.TransactionLedgerRecord;
import com.tejas.banktransactionservice.repositories.TransactionLedgerRepo;
import com.tejas.banktransactionservice.repositories.TransactionRepo;
import com.tejas.banktransactionservice.utils.PdfStatementGenerator;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

    @Mock
    private TransactionRepo repo;

    @Mock
    private TransactionLedgerRepo ledgerRepo;

    @Mock
    private TransactionAndLedgerUpdater trUpdater;

    @Mock
    private TransactionProducer trProducer;

    @Mock
    private AccountInterface accInterface;

    @Mock
    private PdfStatementGenerator statementGenerator;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache transactionCache;

    @Mock
    private Cache transactionRecordsCache;

    @Mock
    private Cache transactionRecordsDatedCache;

    @Mock
    private Cache statementCache;

    @Mock
    private Cache allTransfersCache;

    @Mock
    private Cache allLedgerCache;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction testTransaction;
    private TransactionLedgerRecord testLedgerRecord;
    private String testFromAccount;
    private String testToAccount;
    private double testAmount;
    private Pageable testPageable;

    @BeforeEach
    void setUp() {
        testFromAccount = "AC112345678901234567890";
        testToAccount = "AC198765432109876543210";
        testAmount = 1000.0;
        testPageable = PageRequest.of(0, 10);

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setFromAccount(testFromAccount);
        testTransaction.setToAccount(testToAccount);
        testTransaction.setAmount(testAmount);
        testTransaction.setStatus(TransactionStatus.PENDING);
        testTransaction.setCreatedAt(LocalDateTime.now());
        testTransaction.setUpdatedAt(LocalDateTime.now());

        testLedgerRecord = new TransactionLedgerRecord();
        testLedgerRecord.setId(1L);
        testLedgerRecord.setParentTransactionId(1L);
        testLedgerRecord.setAccountNumber(testFromAccount);
        testLedgerRecord.setCounterparty(testToAccount);
        testLedgerRecord.setType(TransactionType.DEBIT);
        testLedgerRecord.setAmount(testAmount);
        testLedgerRecord.setBalanceAfter(9000.0);
        testLedgerRecord.setStatus(TransactionStatus.CREDIT_SUCCESS);
        testLedgerRecord.setCreatedAt(LocalDateTime.now());

        ReflectionTestUtils.setField(transactionService, "cacheManager", cacheManager);

        lenient().when(cacheManager.getCache("transaction_details")).thenReturn(transactionCache);
        lenient().when(cacheManager.getCache("transaction_records")).thenReturn(transactionRecordsCache);
        lenient().when(cacheManager.getCache("transaction_records_dated")).thenReturn(transactionRecordsDatedCache);
        lenient().when(cacheManager.getCache("statement_pdf")).thenReturn(statementCache);
        lenient().when(cacheManager.getCache("all_transfers")).thenReturn(allTransfersCache);
        lenient().when(cacheManager.getCache("all_ledger_transactions")).thenReturn(allLedgerCache);
        lenient().doNothing().when(transactionCache).evict(anyLong());
        lenient().doNothing().when(transactionRecordsCache).clear();
        lenient().doNothing().when(transactionRecordsDatedCache).clear();
        lenient().doNothing().when(statementCache).clear();
        lenient().doNothing().when(allTransfersCache).clear();
        lenient().doNothing().when(allLedgerCache).clear();

        RequestContextHolder.setRequestAttributes(null);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testTransfer_Success() {
        TransferRequest request = TransferRequest.builder()
                .fromAccount(testFromAccount)
                .toAccount(testToAccount)
                .amount(testAmount)
                .build();

        mockRequestContext(null);
        mockSecurityContext("1");
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(true));
        when(repo.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction txn = invocation.getArgument(0);
            txn.setId(1L);
            return txn;
        });
        doNothing().when(trUpdater).saveTransaction(any(TransactionEvent.class));
        lenient().when(trProducer.debitRequest(any(TransactionEvent.class))).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mock(SendResult.class)));

        Transaction result = transactionService.transfer(request);

        assertNotNull(result);
        assertEquals(testFromAccount, result.getFromAccount());
        assertEquals(testToAccount, result.getToAccount());
        assertEquals(testAmount, result.getAmount());
        assertEquals(TransactionStatus.PENDING, result.getStatus());
        verify(repo).save(any(Transaction.class));
        verify(trUpdater).saveTransaction(any(TransactionEvent.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testTransfer_Forbidden() {
        TransferRequest request = TransferRequest.builder()
                .fromAccount(testFromAccount)
                .toAccount(testToAccount)
                .amount(testAmount)
                .build();

        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(false));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> transactionService.transfer(request));
        assertEquals("You do not have permission to use this account.", exception.getMessage());
    }

    @Test
    void testTransfer_InvalidAmount() {
        TransferRequest request = TransferRequest.builder()
                .fromAccount(testFromAccount)
                .toAccount(testToAccount)
                .amount(0.0)
                .build();

        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(true));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> transactionService.transfer(request));
        assertEquals("Amount must be greater than zero", exception.getMessage());
    }

    @Test
    void testTransfer_SameAccount() {
        TransferRequest request = TransferRequest.builder()
                .fromAccount(testFromAccount)
                .toAccount(testFromAccount)
                .amount(testAmount)
                .build();

        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(true));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> transactionService.transfer(request));
        assertEquals("Sender and receiver must be different", exception.getMessage());
    }

    @Test
    void testTransfer_WithPaymentId_AsInternalService() {
        TransferRequest request = TransferRequest.builder()
                .fromAccount(testFromAccount)
                .toAccount(testToAccount)
                .amount(testAmount)
                .paymentId(123L)
                .build();

        mockRequestContext("INTERNAL_SERVICE");
        mockSecurityContext("1");
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(true));
        when(repo.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction txn = invocation.getArgument(0);
            txn.setId(1L);
            return txn;
        });
        doNothing().when(trUpdater).saveTransaction(any(TransactionEvent.class));
        lenient().when(trProducer.debitRequest(any(TransactionEvent.class))).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mock(SendResult.class)));

        Transaction result = transactionService.transfer(request);

        assertNotNull(result);
        verify(trUpdater).saveTransaction(any(TransactionEvent.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeposit_Success() {
        DepositRequest request = DepositRequest.builder()
                .accountNumber(testToAccount)
                .amount(testAmount)
                .build();

        mockSecurityContextAsAdmin("1");
        when(repo.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction txn = invocation.getArgument(0);
            txn.setId(1L);
            return txn;
        });
        doNothing().when(trUpdater).saveTransaction(any(TransactionEvent.class));
        lenient().when(trProducer.creditRequest(any(TransactionEvent.class))).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mock(SendResult.class)));

        Transaction result = transactionService.deposit(request);

        assertNotNull(result);
        assertEquals("CASH_DEPOSIT", result.getFromAccount());
        assertEquals(testToAccount, result.getToAccount());
        assertEquals(testAmount, result.getAmount());
        assertEquals(TransactionStatus.PENDING, result.getStatus());
        verify(repo).save(any(Transaction.class));
        verify(trUpdater).saveTransaction(any(TransactionEvent.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeposit_Forbidden_NotAdmin() {
        DepositRequest request = DepositRequest.builder()
                .accountNumber(testToAccount)
                .amount(testAmount)
                .build();

        mockSecurityContext("1");

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> transactionService.deposit(request));
        assertEquals("You do not have permission to access this resource.", exception.getMessage());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeposit_InvalidAmount() {
        DepositRequest request = DepositRequest.builder()
                .accountNumber(testToAccount)
                .amount(0.0)
                .build();

        mockSecurityContextAsAdmin("1");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> transactionService.deposit(request));
        assertEquals("Amount must be greater than zero", exception.getMessage());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testWithdraw_Success() {
        WithdrawRequest request = WithdrawRequest.builder()
                .accountNumber(testFromAccount)
                .amount(testAmount)
                .build();

        mockSecurityContextAsAdmin("1");
        when(repo.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction txn = invocation.getArgument(0);
            txn.setId(1L);
            return txn;
        });
        doNothing().when(trUpdater).saveTransaction(any(TransactionEvent.class));
        lenient().when(trProducer.debitRequest(any(TransactionEvent.class))).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mock(SendResult.class)));

        Transaction result = transactionService.withdraw(request);

        assertNotNull(result);
        assertEquals(testFromAccount, result.getFromAccount());
        assertEquals("CASH_WITHDRAWAL", result.getToAccount());
        assertEquals(testAmount, result.getAmount());
        assertEquals(TransactionStatus.PENDING, result.getStatus());
        verify(repo).save(any(Transaction.class));
        verify(trUpdater).saveTransaction(any(TransactionEvent.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testWithdraw_Forbidden_NotAdmin() {
        WithdrawRequest request = WithdrawRequest.builder()
                .accountNumber(testFromAccount)
                .amount(testAmount)
                .build();

        mockSecurityContext("1");

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> transactionService.withdraw(request));
        assertEquals("You do not have permission to access this resource.", exception.getMessage());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testWithdraw_InvalidAmount() {
        WithdrawRequest request = WithdrawRequest.builder()
                .accountNumber(testFromAccount)
                .amount(0.0)
                .build();

        mockSecurityContextAsAdmin("1");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> transactionService.withdraw(request));
        assertEquals("Amount must be greater than zero", exception.getMessage());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetTransaction_Success() {
        when(repo.findById(1L)).thenReturn(Optional.of(testTransaction));

        Transaction result = transactionService.getTransaction(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(testFromAccount, result.getFromAccount());
    }

    @Test
    void testGetTransaction_NotFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> transactionService.getTransaction(1L));
        assertEquals("Transaction not found", exception.getMessage());
    }

    @Test
    void testGetDebitTransaction_Success() {
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(true));
        Page<TransactionLedgerRecord> page = new PageImpl<>(Collections.singletonList(testLedgerRecord));
        when(ledgerRepo.getByAccountNumberAndType(testFromAccount, TransactionType.DEBIT, testPageable)).thenReturn(page);

        Page<TransactionLedgerRecord> result = transactionService.getDebitTransaction(testFromAccount, null, null, testPageable);

        assertNotNull(result);
        assertTrue(result.hasContent());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testGetDebitTransaction_WithDateRange() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(true));
        Page<TransactionLedgerRecord> page = new PageImpl<>(Collections.singletonList(testLedgerRecord));
        when(ledgerRepo.getByAccountNumberAndTypeAndCreatedAtBetween(testFromAccount, TransactionType.DEBIT, from, to, testPageable)).thenReturn(page);

        Page<TransactionLedgerRecord> result = transactionService.getDebitTransaction(testFromAccount, from, to, testPageable);

        assertNotNull(result);
        assertTrue(result.hasContent());
    }

    @Test
    void testGetDebitTransaction_Forbidden() {
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(false));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> transactionService.getDebitTransaction(testFromAccount, null, null, testPageable));
        assertEquals("You do not have permission to use this account.", exception.getMessage());
    }

    @Test
    void testGetDebitTransaction_NoContent() {
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(true));
        Page<TransactionLedgerRecord> emptyPage = new PageImpl<>(new ArrayList<>());
        when(ledgerRepo.getByAccountNumberAndType(testFromAccount, TransactionType.DEBIT, testPageable)).thenReturn(emptyPage);

        NoContentException exception = assertThrows(NoContentException.class, () -> transactionService.getDebitTransaction(testFromAccount, null, null, testPageable));
        assertEquals("No transactions found.", exception.getMessage());
    }

    @Test
    void testGetCreditTransaction_Success() {
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testToAccount)).thenReturn(ResponseEntity.ok(true));
        testLedgerRecord.setType(TransactionType.CREDIT);
        Page<TransactionLedgerRecord> page = new PageImpl<>(Collections.singletonList(testLedgerRecord));
        when(ledgerRepo.getByAccountNumberAndType(testToAccount, TransactionType.CREDIT, testPageable)).thenReturn(page);

        Page<TransactionLedgerRecord> result = transactionService.getCreditTransaction(testToAccount, null, null, testPageable);

        assertNotNull(result);
        assertTrue(result.hasContent());
    }

    @Test
    void testGetAllTransaction_Success() {
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(true));
        Page<TransactionLedgerRecord> page = new PageImpl<>(Collections.singletonList(testLedgerRecord));
        when(ledgerRepo.getByAccountNumber(testFromAccount, testPageable)).thenReturn(page);

        Page<TransactionLedgerRecord> result = transactionService.getAllTransaction(testFromAccount, null, null, testPageable);

        assertNotNull(result);
        assertTrue(result.hasContent());
    }

    @Test
    void testGetStatement_Success() {
        LocalDateTime from = LocalDateTime.now().minusDays(10);
        LocalDateTime to = LocalDateTime.now();
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(true));
        List<TransactionLedgerRecord> records = Collections.singletonList(testLedgerRecord);
        when(ledgerRepo.getByAccountNumberAndCreatedAtBetween(testFromAccount, from, to)).thenReturn(records);
        byte[] pdfData = new byte[]{1, 2, 3};
        when(statementGenerator.generatePdfStatement(testFromAccount, from, to, records)).thenReturn(pdfData);

        byte[] result = transactionService.getStatement(testFromAccount, from, to);

        assertNotNull(result);
        assertEquals(pdfData, result);
    }

    @Test
    void testGetStatement_Forbidden() {
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(false));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> transactionService.getStatement(testFromAccount, null, null));
        assertEquals("You do not have permission to use this account.", exception.getMessage());
    }

    @Test
    void testGetStatement_NoContent() {
        LocalDateTime from = LocalDateTime.now().minusDays(10);
        LocalDateTime to = LocalDateTime.now();
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(true));
        when(ledgerRepo.getByAccountNumberAndCreatedAtBetween(testFromAccount, from, to)).thenReturn(new ArrayList<>());

        NoContentException exception = assertThrows(NoContentException.class, () -> transactionService.getStatement(testFromAccount, from, to));
        assertEquals("No transactions found.", exception.getMessage());
    }

    @Test
    void testGetStatement_DefaultDateRange() {
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(true));
        List<TransactionLedgerRecord> records = Collections.singletonList(testLedgerRecord);
        when(ledgerRepo.getByAccountNumberAndCreatedAtBetween(anyString(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(records);
        byte[] pdfData = new byte[]{1, 2, 3};
        when(statementGenerator.generatePdfStatement(anyString(), any(LocalDateTime.class), any(LocalDateTime.class), anyList())).thenReturn(pdfData);

        byte[] result = transactionService.getStatement(testFromAccount, null, null);

        assertNotNull(result);
    }

    @Test
    void testGetStatement_GeneratorException() {
        LocalDateTime from = LocalDateTime.now().minusDays(10);
        LocalDateTime to = LocalDateTime.now();
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountNumber(testFromAccount)).thenReturn(ResponseEntity.ok(true));
        List<TransactionLedgerRecord> records = Collections.singletonList(testLedgerRecord);
        when(ledgerRepo.getByAccountNumberAndCreatedAtBetween(testFromAccount, from, to)).thenReturn(records);
        when(statementGenerator.generatePdfStatement(testFromAccount, from, to, records)).thenThrow(new RuntimeException("PDF generation failed"));

        assertThrows(GeneralServerException.class, () -> transactionService.getStatement(testFromAccount, from, to));
    }

    @Test
    void testGetAllTransfers_Success_AsAdmin() {
        mockRequestContext("ADMIN");
        List<Transaction> transactions = Collections.singletonList(testTransaction);
        when(repo.findAll()).thenReturn(transactions);

        List<Transaction> result = transactionService.getAllTransfers();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testGetAllTransfers_Forbidden_NotAdmin() {
        mockRequestContext("USER");

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> transactionService.getAllTransfers());
        assertEquals("You do not have permission to access this resource.", exception.getMessage());
    }

    @Test
    void testGetAllTransfers_NoContent() {
        mockRequestContext("ADMIN");
        when(repo.findAll()).thenReturn(new ArrayList<>());

        NoContentException exception = assertThrows(NoContentException.class, () -> transactionService.getAllTransfers());
        assertEquals("No transactions found.", exception.getMessage());
    }

    @Test
    void testGetAllLedgerTransaction_Success_AsAdmin() {
        mockRequestContext("ADMIN");
        List<TransactionLedgerRecord> records = Collections.singletonList(testLedgerRecord);
        when(ledgerRepo.findAll()).thenReturn(records);

        List<TransactionLedgerRecord> result = transactionService.getAllLedgerTransaction();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testGetAllLedgerTransaction_Forbidden_NotAdmin() {
        mockRequestContext("USER");

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> transactionService.getAllLedgerTransaction());
        assertEquals("You do not have permission to access this resource.", exception.getMessage());
    }

    @Test
    void testGetAllLedgerTransaction_NoContent() {
        mockRequestContext("ADMIN");
        when(ledgerRepo.findAll()).thenReturn(new ArrayList<>());

        NoContentException exception = assertThrows(NoContentException.class, () -> transactionService.getAllLedgerTransaction());
        assertEquals("No transactions found.", exception.getMessage());
    }

    @Test
    void testEvictTransactionCache() {
        when(cacheManager.getCache("transaction_details")).thenReturn(transactionCache);
        doNothing().when(transactionCache).evict(1L);

        transactionService.evictTransactionCache(1L);

        verify(transactionCache).evict(1L);
    }

    @Test
    void testEvictTransactionRecordsCache() {
        when(cacheManager.getCache("transaction_records")).thenReturn(transactionRecordsCache);
        when(cacheManager.getCache("transaction_records_dated")).thenReturn(transactionRecordsDatedCache);
        doNothing().when(transactionRecordsCache).clear();
        doNothing().when(transactionRecordsDatedCache).clear();

        transactionService.evictTransactionRecordsCache(testFromAccount);

        verify(transactionRecordsCache).clear();
        verify(transactionRecordsDatedCache).clear();
    }

    @Test
    void testEvictStatementCache() {
        when(cacheManager.getCache("statement_pdf")).thenReturn(statementCache);
        doNothing().when(statementCache).clear();

        transactionService.evictStatementCache(testFromAccount);

        verify(statementCache).clear();
    }

    private void mockRequestContext(String role) {
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("X-User-Role")).thenReturn(role);
        RequestContextHolder.setRequestAttributes(attributes);
    }

    private void mockSecurityContext(String userId) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(userId);
    }

    private void mockSecurityContextAsAdmin(String userId) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn(userId);
        
        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        lenient().when(adminAuthority.getAuthority()).thenReturn("ROLE_ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        lenient().doReturn(authorities).when(authentication).getAuthorities();
    }
}
