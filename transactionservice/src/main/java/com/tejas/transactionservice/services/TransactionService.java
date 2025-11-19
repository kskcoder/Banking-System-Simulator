package com.tejas.transactionservice.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.enums.TransactionStatus;
import com.tejas.transactionservice.enums.TransactionType;
import com.tejas.transactionservice.feign.AccountInterface;
import com.tejas.transactionservice.models.Transaction;
import com.tejas.transactionservice.models.TransactionLedgerRecord;
import com.tejas.transactionservice.models.TransferRequest;
import com.tejas.transactionservice.repositories.TransactionLedgerRepo;
import com.tejas.transactionservice.repositories.TransactionRepo;
import com.tejas.transactionservice.utils.PdfStatementGenerator;

import feign.FeignException;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepo repo;
    
    @Autowired
    private TransactionLedgerRepo ledgerRepo;
    
    @Autowired
    private TransactionAndLedgerUpdater trUpdater;
    
    @Autowired
    private TransactionProducer trProducer;
    
	@Autowired
	private AccountInterface accInterface;
	
	@Autowired
	private PdfStatementGenerator statementGenerator;

    @Transactional
    public ResponseEntity<Transaction> transfer(TransferRequest request) {
        Transaction txn = new Transaction();
        txn.setFromAccount(request.getFromAccount());
        txn.setToAccount(request.getToAccount());
        txn.setAmount(request.getAmount());
        txn.setStatus("PENDING");
        txn.setCreatedAt(LocalDateTime.now());
        txn.setUpdatedAt(LocalDateTime.now());
        
        if (request.getAmount() <= 0.0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        } else if (request.getFromAccount().equals(request.getToAccount())) {
        	throw new IllegalArgumentException("Sender and receiver must be different");
        }
        
        repo.save(txn);
        
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        TransactionEvent event = new TransactionEvent(); 
    	event.setTransactionId(txn.getId());
    	event.setFromAccountNumber(txn.getFromAccount());
    	event.setToAccountNumber(txn.getToAccount());
    	event.setAmount(txn.getAmount());
    	event.setUserId(Integer.parseInt(userId));
    	event.setType(TransactionType.DEBIT.toString());
    	event.setStatus(TransactionStatus.PENDING.toString());  
    	
    	trUpdater.saveTransaction(event);
        
        trProducer.dispatchDebitWithRetry(event);
        
        return ResponseEntity.ok(txn);
    }

    @Transactional
	public void saveTransaction(TransactionEvent trEvent) {
		Transaction tx = repo.findById(trEvent.getTransactionId());
		tx.setStatus(trEvent.getStatus());
		tx.setUpdatedAt(LocalDateTime.now());
		repo.save(tx);
	}

	public ResponseEntity<Transaction> getTransaction(int txnId) {
		Transaction tx = repo.findById(txnId);
		
		if (tx != null) {
			return new ResponseEntity<>(tx, HttpStatus.OK);
		}
		
		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}
	
	//For admin to get all ledger records
	public ResponseEntity<List<Transaction>> getAllTransfers() {
		List<Transaction> tx = repo.findAll();
		
		if (!tx.isEmpty()) {
			return new ResponseEntity<>(tx, HttpStatus.OK);
		}
		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}
	
	public ResponseEntity<Page<TransactionLedgerRecord>> getDebitTransaction(String accountNum, LocalDateTime from, LocalDateTime to, Pageable pageable) {
		if (!isOwner(accountNum)) {return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);}
		if (from != null && to != null) {
			return getGeneralDatedTransactionRecords(accountNum, TransactionType.DEBIT.toString(), from, to, pageable);
		} else {
			return getGeneralTransactionRecords(accountNum, TransactionType.DEBIT.toString(), pageable);
		}
	}
	
	public ResponseEntity<Page<TransactionLedgerRecord>> getCreditTransaction(String accountNum, LocalDateTime from, LocalDateTime to, Pageable pageable) {
		if (!isOwner(accountNum)) {return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);}
		
		if (from != null && to != null) {
			return getGeneralDatedTransactionRecords(accountNum, TransactionType.CREDIT.toString(), from, to, pageable);
		} else {
			return getGeneralTransactionRecords(accountNum, TransactionType.CREDIT.toString(), pageable);
		}		
	}

	public ResponseEntity<Page<TransactionLedgerRecord>> getAllTransaction(String accountNum, LocalDateTime from, LocalDateTime to, Pageable pageable) {
		if (!isOwner(accountNum)) {return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);}
		
		if (from != null && to != null) {
			return getGeneralDatedTransactionRecords(accountNum, null, from, to, pageable);
		} else {
			return getGeneralTransactionRecords(accountNum, null, pageable);
		}		
	}
	
	private boolean isOwner(String pathAccNo) {
		String role = ((ServletRequestAttributes) RequestContextHolder
		        .getRequestAttributes())
		        .getRequest()
		        .getHeader("X-User-Role");
		try {
			 boolean isOwner = accInterface.isOwnerOfAccount(pathAccNo).getBody();
			 if (!isOwner && !role.equals("ADMIN")) {
				 return false;     				
	    	}
	    } catch (FeignException e) {
	    	return false;
	    }
		return true;
	}
	
	private ResponseEntity<Page<TransactionLedgerRecord>> getGeneralTransactionRecords(String accountNum, String type, Pageable pageable) {
		Page<TransactionLedgerRecord> tx; 
		if (TransactionType.CREDIT.toString().equals(type) || TransactionType.DEBIT.toString().equals(type)) {
			tx = ledgerRepo.getByAccountNumberAndType(accountNum, type, pageable);
		} else {
			tx = ledgerRepo.getByAccountNumber(accountNum, pageable);
		}
		
		if (tx.hasContent()) {
			return new ResponseEntity<>(tx, HttpStatus.OK);
		}
		
		return new ResponseEntity<>(tx, HttpStatus.NO_CONTENT);
	}
	
	private ResponseEntity<Page<TransactionLedgerRecord>> getGeneralDatedTransactionRecords(String accountNum,
			String type, LocalDateTime from, LocalDateTime to, Pageable pageable) {
		Page<TransactionLedgerRecord> tx; 
		if (TransactionType.CREDIT.toString().equals(type) || TransactionType.DEBIT.toString().equals(type)) {
			tx = ledgerRepo.getByAccountNumberAndTypeAndCreatedAtBetween(accountNum, type, from, to, pageable);
		} else {
			tx = ledgerRepo.getByAccountNumberAndCreatedAtBetween(accountNum, from, to, pageable);
		}
		
		if (tx.hasContent()) {
			return new ResponseEntity<>(tx, HttpStatus.OK);
		}
		
		return new ResponseEntity<>(tx, HttpStatus.NO_CONTENT);
	}

	public ResponseEntity<byte[]> getStatement(String accountNumber, LocalDateTime from, LocalDateTime to) {
		if (!isOwner(accountNumber)) {return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);}
		
		List<TransactionLedgerRecord> tx;
		
		if (from == null && to == null) {
			to = LocalDateTime.now();
			from = to.minusDays(10);
		} 
		
		
		tx = ledgerRepo.getByAccountNumberAndCreatedAtBetween(accountNumber, from, to);
		
		byte[] data = statementGenerator.generatePdfStatement(accountNumber, from, to, tx);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		
		DateTimeFormatter ftr = DateTimeFormatter.ofPattern("ddMMMyyyyHH:mm");
		
		headers.set(HttpHeaders.CONTENT_DISPOSITION,
				"attachment, filename=statement_"+accountNumber+""+LocalDateTime.now().format(ftr)+".pdf");
		
		return new ResponseEntity<>(data, headers, HttpStatus.OK);
	}
	
	//For admin to get all ledger records
	public ResponseEntity<List<TransactionLedgerRecord>> getAllLedgerTransaction() {
		List<TransactionLedgerRecord> tx = ledgerRepo.findAll();
		
		if (!tx.isEmpty()) {
			return new ResponseEntity<>(tx, HttpStatus.OK);
		}
		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}
}

