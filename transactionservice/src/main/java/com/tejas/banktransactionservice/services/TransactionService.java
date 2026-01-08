package com.tejas.banktransactionservice.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tejas.bankingcommon.dto.DepositRequest;
import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.dto.TransferRequest;
import com.tejas.bankingcommon.dto.WithdrawRequest;
import com.tejas.bankingcommon.enums.TransactionStatus;
import com.tejas.bankingcommon.enums.TransactionType;
import com.tejas.bankingcommon.enums.UserType;
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
import com.tejas.banktransactionservice.utils.AuthUtils;
import com.tejas.banktransactionservice.utils.PdfStatementGenerator;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepo repo;
    private final TransactionLedgerRepo ledgerRepo;
    private final TransactionAndLedgerUpdater trUpdater;
    private final TransactionProducer trProducer;
	private final AccountInterface accInterface;
	private final PdfStatementGenerator statementGenerator;
	
	@Autowired
	private CacheManager cacheManager;
	
	public void evictTransactionCache(Long l) {
		Cache transactionCache = cacheManager.getCache("transaction_details");
		if (transactionCache != null && l != null) {
			transactionCache.evict(l);
		}
	}
	
	public void evictTransactionRecordsCache(String accountNumber) {
		Cache transactionRecordsCache = cacheManager.getCache("transaction_records");
		if (transactionRecordsCache != null) {
			transactionRecordsCache.clear();
		}
		
		Cache transactionRecordsDatedCache = cacheManager.getCache("transaction_records_dated");
		if (transactionRecordsDatedCache != null) {
			transactionRecordsDatedCache.clear();
		}
	}
	
	public void evictStatementCache(String accountNumber) {
		Cache statementCache = cacheManager.getCache("statement_pdf");
		if (statementCache != null) {
			statementCache.clear();
		}
	}
	
	private void evictAllTransfersCache() {
		Cache allTransfersCache = cacheManager.getCache("all_transfers");
		if (allTransfersCache != null) {
			allTransfersCache.clear();
		}
	}
	
	private void evictAllLedgerTransactionsCache() {
		Cache allLedgerCache = cacheManager.getCache("all_ledger_transactions");
		if (allLedgerCache != null) {
			allLedgerCache.clear();
		}
	}

    @Transactional
    public Transaction transfer(TransferRequest request) {
    	if (!isOwner(request.getFromAccount())) {throw new ForbiddenException("You do not have permission to use this account.");}
    	
        Transaction txn = new Transaction();
        txn.setFromAccount(request.getFromAccount());
        txn.setToAccount(request.getToAccount());
        txn.setAmount(request.getAmount());
        txn.setStatus(TransactionStatus.PENDING);
        txn.setCreatedAt(LocalDateTime.now());
        txn.setUpdatedAt(LocalDateTime.now());
        
        if (request.getAmount() <= 0.0) {
            throw new BadRequestException("Amount must be greater than zero");
        } else if (request.getFromAccount().equals(request.getToAccount())) {
        	throw new BadRequestException("Sender and receiver must be different");
        }
        
        Transaction savedTxn = repo.save(txn);
        
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        Long paymentId = AuthUtils.getRole().equals(UserType.INTERNAL_SERVICE.toString()) ? request.getPaymentId() : null;
        
        TransactionEvent event = new TransactionEvent(); 
    	event.setTransactionId(savedTxn.getId());
    	event.setUserId(userId);
    	event.setPaymentId(paymentId);
    	event.setFromAccountNumber(savedTxn.getFromAccount());
    	event.setToAccountNumber(savedTxn.getToAccount());
    	event.setAmount(savedTxn.getAmount());
    	event.setType(TransactionType.DEBIT);
    	event.setStatus(TransactionStatus.PENDING);  
    	
    	trUpdater.saveTransaction(event);
    	evictTransactionCache(savedTxn.getId());
    	evictTransactionRecordsCache(savedTxn.getFromAccount());
    	evictTransactionRecordsCache(savedTxn.getToAccount());
    	evictStatementCache(savedTxn.getFromAccount());
    	evictStatementCache(savedTxn.getToAccount());
    	evictAllTransfersCache();
    	evictAllLedgerTransactionsCache();
        
        trProducer.dispatchDebitWithRetry(event);
        
        return savedTxn;
    }

    @Transactional
    public Transaction deposit(DepositRequest request) {
    	if (!AuthUtils.isAdmin()) {
    		throw new ForbiddenException("You do not have permission to access this resource.");
    	}
    	
    	if (request.getAmount() <= 0.0) {
    		throw new BadRequestException("Amount must be greater than zero");
    	}
    	
    	Transaction txn = new Transaction();
    	txn.setFromAccount("CASH_DEPOSIT");
    	txn.setToAccount(request.getAccountNumber());
    	txn.setAmount(request.getAmount());
    	txn.setStatus(TransactionStatus.PENDING);
    	txn.setCreatedAt(LocalDateTime.now());
    	txn.setUpdatedAt(LocalDateTime.now());
    	
    	Transaction savedTxn = repo.save(txn);
    	
    	String userId = SecurityContextHolder.getContext().getAuthentication().getName();
    	
    	TransactionEvent event = new TransactionEvent();
    	event.setTransactionId(savedTxn.getId());
    	event.setUserId(userId);
    	event.setPaymentId(null);
    	event.setFromAccountNumber("CASH_DEPOSIT");
    	event.setToAccountNumber(request.getAccountNumber());
    	event.setAmount(request.getAmount());
    	event.setType(TransactionType.CREDIT);
    	event.setStatus(TransactionStatus.PENDING);
    	
    	trUpdater.saveTransaction(event);
    	evictTransactionCache(savedTxn.getId());
    	evictTransactionRecordsCache(request.getAccountNumber());
    	evictStatementCache(request.getAccountNumber());
    	evictAllTransfersCache();
    	evictAllLedgerTransactionsCache();
    	
    	trProducer.dispatchCreditWithRetry(event);
    	
    	return savedTxn;
    }

    @Transactional
    public Transaction withdraw(WithdrawRequest request) {
    	if (!AuthUtils.isAdmin()) {
    		throw new ForbiddenException("You do not have permission to access this resource.");
    	}
    	
    	if (request.getAmount() <= 0.0) {
    		throw new BadRequestException("Amount must be greater than zero");
    	}
    	
    	Transaction txn = new Transaction();
    	txn.setFromAccount(request.getAccountNumber());
    	txn.setToAccount("CASH_WITHDRAWAL");
    	txn.setAmount(request.getAmount());
    	txn.setStatus(TransactionStatus.PENDING);
    	txn.setCreatedAt(LocalDateTime.now());
    	txn.setUpdatedAt(LocalDateTime.now());
    	
    	Transaction savedTxn = repo.save(txn);
    	
    	String userId = SecurityContextHolder.getContext().getAuthentication().getName();
    	
    	TransactionEvent event = new TransactionEvent();
    	event.setTransactionId(savedTxn.getId());
    	event.setUserId(userId);
    	event.setPaymentId(null);
    	event.setFromAccountNumber(request.getAccountNumber());
    	event.setToAccountNumber("CASH_WITHDRAWAL");
    	event.setAmount(request.getAmount());
    	event.setType(TransactionType.DEBIT);
    	event.setStatus(TransactionStatus.PENDING);
    	
    	trUpdater.saveTransaction(event);
    	evictTransactionCache(savedTxn.getId());
    	evictTransactionRecordsCache(request.getAccountNumber());
    	evictStatementCache(request.getAccountNumber());
    	evictAllTransfersCache();
    	evictAllLedgerTransactionsCache();
    	
    	trProducer.dispatchDebitWithRetry(event);
    	
    	return savedTxn;
    }

    @Transactional
	public void saveTransaction(TransactionEvent trEvent) {
		Transaction tx = repo.findById(trEvent.getTransactionId());
		tx.setStatus(trEvent.getStatus());
		tx.setUpdatedAt(LocalDateTime.now());
		Transaction savedTx = repo.save(tx);
		evictTransactionCache(savedTx.getId());
		evictTransactionRecordsCache(savedTx.getFromAccount());
		evictTransactionRecordsCache(savedTx.getToAccount());
		evictStatementCache(savedTx.getFromAccount());
		evictStatementCache(savedTx.getToAccount());
		evictAllTransfersCache();
		evictAllLedgerTransactionsCache();
	}

	public Transaction getTransaction(int txnId) {
		return cachedGetTransaction(txnId);
	}
	
	@Cacheable(value="transaction_details", key="#txnId", unless="#result == null")
	protected Transaction cachedGetTransaction(int txnId) {
		Transaction tx = repo.findById(txnId);
		
		if (tx != null) {
			return tx;
		}
		
		throw new NotFoundException("Transaction not found");
	}
	
	public Page<TransactionLedgerRecord> getDebitTransaction(String accountNum, LocalDateTime from, LocalDateTime to, Pageable pageable) {
		if (!isOwner(accountNum)) {throw new ForbiddenException("You do not have permission to use this account.");}
		if (from != null && to != null) {
			return cachedGetGeneralDatedTransactionRecords(accountNum, TransactionType.DEBIT.toString(), from, to, pageable);
		} else {
			return cachedGetGeneralTransactionRecords(accountNum, TransactionType.DEBIT.toString(), pageable);
		}
	}
	
	public Page<TransactionLedgerRecord> getCreditTransaction(String accountNum, LocalDateTime from, LocalDateTime to, Pageable pageable) {
		if (!isOwner(accountNum)) {throw new ForbiddenException("You do not have permission to use this account.");}
		
		if (from != null && to != null) {
			return cachedGetGeneralDatedTransactionRecords(accountNum, TransactionType.CREDIT.toString(), from, to, pageable);
		} else {
			return cachedGetGeneralTransactionRecords(accountNum, TransactionType.CREDIT.toString(), pageable);
		}		
	}

	public Page<TransactionLedgerRecord> getAllTransaction(String accountNum, LocalDateTime from, LocalDateTime to, Pageable pageable) {
		if (!isOwner(accountNum)) {throw new ForbiddenException("You do not have permission to use this account.");}
		
		if (from != null && to != null) {
			return cachedGetGeneralDatedTransactionRecords(accountNum, null, from, to, pageable);
		} else {
			return cachedGetGeneralTransactionRecords(accountNum, null, pageable);
		}		
	}
	
	private boolean isOwner(String pathAccNo) {
		String role = AuthUtils.getRole();
		
		if (role.equals(UserType.INTERNAL_SERVICE.toString()) || role.equals(UserType.ADMIN.toString())) {
			return true;
		}
		
		if (pathAccNo.equals("") || pathAccNo == null) {
			return false;
		}
		
		try {
			 boolean isOwner = accInterface.isOwnerOfAccountNumber(pathAccNo).getBody();
			 if (isOwner) {
				 return true;     				
			 } else {
				 return false;
			 }
	    } catch (FeignException e) {
	    	return false;
	    }
	}
	
	@Cacheable(value="transaction_records", key="#accountNum + '_' + #type + '_' + #pageable.pageNumber + '_' + #pageable.pageSize", unless="#result == null || !#result.hasContent()")
	protected Page<TransactionLedgerRecord> cachedGetGeneralTransactionRecords(String accountNum, String type, Pageable pageable) {
		Page<TransactionLedgerRecord> tx; 
		if (TransactionType.CREDIT.toString().equals(type) || TransactionType.DEBIT.toString().equals(type)) {
			TransactionType transactionType = TransactionType.valueOf(type);
			tx = ledgerRepo.getByAccountNumberAndType(accountNum, transactionType, pageable);
		} else {
			tx = ledgerRepo.getByAccountNumber(accountNum, pageable);
		}
		
		if (tx.hasContent()) {
			return tx;
		}
		
		throw new NoContentException("No transactions found.");
	}
	
	@Cacheable(value="transaction_records_dated", key="#accountNum + '_' + #type + '_' + #from + '_' + #to + '_' + #pageable.pageNumber + '_' + #pageable.pageSize", unless="#result == null || !#result.hasContent()")
	protected Page<TransactionLedgerRecord> cachedGetGeneralDatedTransactionRecords(String accountNum,
			String type, LocalDateTime from, LocalDateTime to, Pageable pageable) {
		Page<TransactionLedgerRecord> tx; 
		if (TransactionType.CREDIT.toString().equals(type) || TransactionType.DEBIT.toString().equals(type)) {
			TransactionType transactionType = TransactionType.valueOf(type);
			tx = ledgerRepo.getByAccountNumberAndTypeAndCreatedAtBetween(accountNum, transactionType, from, to, pageable);
		} else {
			tx = ledgerRepo.getByAccountNumberAndCreatedAtBetween(accountNum, from, to, pageable);
		}
		
		if (tx.hasContent()) {
			return tx;
		}
		
		throw new NoContentException("No transactions found.");
	}

	public byte[] getStatement(String accountNumber, LocalDateTime from, LocalDateTime to) {
		if (!isOwner(accountNumber)) {throw new ForbiddenException("You do not have permission to use this account.");}
		
		return cachedGetStatement(accountNumber, from, to);
	}
	
	@Cacheable(value="statement_pdf", key="#accountNumber + '_' + #from + '_' + #to", unless="#result == null")
	protected byte[] cachedGetStatement(String accountNumber, LocalDateTime from, LocalDateTime to) {
		List<TransactionLedgerRecord> tx;
		
		if (from == null && to == null) {
			to = LocalDateTime.now();
			from = to.minusDays(10);
		} 
		
		
		tx = ledgerRepo.getByAccountNumberAndCreatedAtBetween(accountNumber, from, to);
		
		if (tx.isEmpty()) {
			throw new NoContentException("No transactions found.");
		}
		
		try {
			byte[] data = statementGenerator.generatePdfStatement(accountNumber, from, to, tx);
			return data;
		} catch (Exception e) {
			throw new GeneralServerException();
		}
	}

	@Cacheable(value="all_transfers", unless="#result == null || #result.isEmpty()")
	public List<Transaction> getAllTransfers() {
		if (!isOwner("")) {throw new ForbiddenException("You do not have permission to access this resource.");}
		List<Transaction> tx = repo.findAll();
		
		if (!tx.isEmpty()) {
			return tx;
		}
		
		throw new NoContentException("No transactions found.");
	}
	
	@Cacheable(value="all_ledger_transactions", unless="#result == null || #result.isEmpty()")
	public List<TransactionLedgerRecord> getAllLedgerTransaction() {
		if (!isOwner("")) {throw new ForbiddenException("You do not have permission to access this resource.");}
		List<TransactionLedgerRecord> tx = ledgerRepo.findAll();
		
		if (!tx.isEmpty()) {
			return tx;
		}
		throw new NoContentException("No transactions found.");
	}
}

