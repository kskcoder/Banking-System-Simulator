package com.tejas.bankaccountservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.tejas.bankaccountservice.feign.AuthInterface;
import com.tejas.bankaccountservice.models.Account;
import com.tejas.bankaccountservice.models.CreateAccountDTO;
import com.tejas.bankaccountservice.repositories.AccountRepo;
import com.tejas.bankingcommon.enums.AccountType;
import com.tejas.bankingcommon.enums.UserType;
import com.tejas.bankingcommon.exceptions.ForbiddenException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NoContentException;
import com.tejas.bankingcommon.exceptions.NotFoundException;

import feign.FeignException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceTest {

    @Mock
    private AccountRepo repo;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private AuthInterface authInterface;

    @Mock
    private Cache accountDetailsCache;

    @Mock
    private Cache balanceCache;

    @Mock
    private Cache accountTypeCache;

    @Mock
    private Cache userIdCache;

    @Mock
    private Cache userAccountsCache;

    @Mock
    private Cache allAccountIdsCache;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private CreateAccountDTO createAccountDTO;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUserid(1L);
        testAccount.setAccountnumber("AC112345678901234567890");
        testAccount.setAccountType(AccountType.SAVINGS);
        testAccount.setBalance(1000.0);

        createAccountDTO = new CreateAccountDTO();
        createAccountDTO.setUserId(1);
        createAccountDTO.setInitialAmount(1000.0);
        createAccountDTO.setAccountType(AccountType.SAVINGS);

        lenient().when(cacheManager.getCache("account_details")).thenReturn(accountDetailsCache);
        lenient().when(cacheManager.getCache("balance")).thenReturn(balanceCache);
        lenient().when(cacheManager.getCache("account_type")).thenReturn(accountTypeCache);
        lenient().when(cacheManager.getCache("userId")).thenReturn(userIdCache);
        lenient().when(cacheManager.getCache("all_accounts_of_userid")).thenReturn(userAccountsCache);
        lenient().when(cacheManager.getCache("all_account_ids")).thenReturn(allAccountIdsCache);

        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateAccount_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("1");

        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        lenient().when(adminAuthority.getAuthority()).thenReturn("ROLE_ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        lenient().doReturn(authorities).when(authentication).getAuthorities();

        when(authInterface.userExists(1L)).thenReturn(ResponseEntity.ok(true));
        when(repo.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(1L);
            return account;
        });
        doNothing().when(userAccountsCache).evict(anyLong());
        doNothing().when(allAccountIdsCache).clear();

        Account result = accountService.createAccount(createAccountDTO);

        assertNotNull(result);
        assertEquals(createAccountDTO.getUserId(), result.getUserid());
        assertEquals(createAccountDTO.getAccountType(), result.getAccountType());
        assertEquals(createAccountDTO.getInitialAmount(), result.getBalance());
        assertNotNull(result.getAccountnumber());
        verify(repo).save(any(Account.class));
        verify(userAccountsCache).evict(1L);
        verify(allAccountIdsCache).clear();

        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateAccount_UserDoesNotExist() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("1");

        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        lenient().when(adminAuthority.getAuthority()).thenReturn("ROLE_ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        lenient().doReturn(authorities).when(authentication).getAuthorities();

        when(authInterface.userExists(1L)).thenReturn(ResponseEntity.ok(false));

        assertThrows(GeneralServerException.class, () -> accountService.createAccount(createAccountDTO));
        verify(repo, never()).save(any(Account.class));

        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateAccount_FeignException() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("1");

        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        lenient().when(adminAuthority.getAuthority()).thenReturn("ROLE_ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        lenient().doReturn(authorities).when(authentication).getAuthorities();

        when(authInterface.userExists(1L)).thenThrow(mock(FeignException.class));

        assertThrows(GeneralServerException.class, () -> accountService.createAccount(createAccountDTO));
        verify(repo, never()).save(any(Account.class));

        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateAccount_Forbidden() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("2");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        when(authInterface.userExists(1L)).thenReturn(ResponseEntity.ok(true));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> accountService.createAccount(createAccountDTO));
        assertEquals("You do not have permission to access this resource.", exception.getMessage());
        verify(repo, never()).save(any(Account.class));

        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateAccount_DatabaseException() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("1");

        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        lenient().when(adminAuthority.getAuthority()).thenReturn("ROLE_ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        lenient().doReturn(authorities).when(authentication).getAuthorities();

        when(authInterface.userExists(1L)).thenReturn(ResponseEntity.ok(true));
        when(repo.save(any(Account.class))).thenThrow(new RuntimeException("Database error"));

        assertThrows(GeneralServerException.class, () -> accountService.createAccount(createAccountDTO));

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetAccountByAccountNumber_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("1");

        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        lenient().when(adminAuthority.getAuthority()).thenReturn("ROLE_ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        lenient().doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getByAccountnumber("AC112345678901234567890")).thenReturn(Optional.of(testAccount));

        Account result = accountService.getAccountByAccountNumber("AC112345678901234567890");

        assertNotNull(result);
        assertEquals(testAccount.getId(), result.getId());
        assertEquals(testAccount.getAccountnumber(), result.getAccountnumber());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetAccountByAccountNumber_NotFound() {
        when(repo.getByAccountnumber("INVALID")).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> accountService.getAccountByAccountNumber("INVALID"));
    }

    @Test
    void testGetAccountByAccountNumber_Forbidden() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("2");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getByAccountnumber("AC112345678901234567890")).thenReturn(Optional.of(testAccount));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> accountService.getAccountByAccountNumber("AC112345678901234567890"));
        assertEquals("You do not have permission to access this resource.", exception.getMessage());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetAccountsByUserId_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        List<Account> accounts = new ArrayList<>();
        accounts.add(testAccount);
        when(repo.getByUserid(1L)).thenReturn(Optional.of(accounts));
        when(repo.getById(1L)).thenReturn(Optional.of(testAccount));

        List<Account> result = accountService.getAccountsByUserId(1);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testAccount.getId(), result.get(0).getId());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetAccountsByUserId_NotFound() {
        when(repo.getByUserid(1L)).thenReturn(Optional.of(new ArrayList<>()));

        assertThrows(NotFoundException.class, () -> accountService.getAccountsByUserId(1));
    }

    @Test
    void testGetAccountsByUserId_Forbidden() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        Account otherAccount = new Account();
        otherAccount.setId(2L);
        otherAccount.setUserid(3L);

        List<Account> accounts = new ArrayList<>();
        accounts.add(otherAccount);
        when(repo.getByUserid(2L)).thenReturn(Optional.of(accounts));
        when(repo.getById(2L)).thenReturn(Optional.of(otherAccount));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> accountService.getAccountsByUserId(2));
        assertEquals("You do not have permission to access this resource.", exception.getMessage());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetBalanceByAccountId_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("1");

        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        lenient().when(adminAuthority.getAuthority()).thenReturn("ROLE_ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        lenient().doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getById(1L)).thenReturn(Optional.of(testAccount));

        Double result = accountService.getBalanceByAccountId(1L);

        assertNotNull(result);
        assertEquals(1000.0, result);

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetBalanceByAccountId_NotFound() {
        when(repo.getById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> accountService.getBalanceByAccountId(1L));
    }

    @Test
    void testGetBalanceByAccountId_Forbidden() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("2");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getById(1L)).thenReturn(Optional.of(testAccount));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> accountService.getBalanceByAccountId(1L));
        assertEquals("You do not have permission to access this resource.", exception.getMessage());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testCloseAccount_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("1");

        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        lenient().when(adminAuthority.getAuthority()).thenReturn("ROLE_ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        lenient().doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getByAccountnumber("AC112345678901234567890")).thenReturn(Optional.of(testAccount));
        doNothing().when(repo).delete(testAccount);
        doNothing().when(accountDetailsCache).evict(1L);
        doNothing().when(userAccountsCache).evict(1L);
        doNothing().when(allAccountIdsCache).clear();

        String result = accountService.closeAccount("AC112345678901234567890");

        assertEquals("Account Closed Successfully!", result);
        verify(repo).delete(testAccount);
        verify(accountDetailsCache).evict(1L);
        verify(userAccountsCache).evict(1L);
        verify(allAccountIdsCache).clear();

        SecurityContextHolder.clearContext();
    }

    @Test
    void testCloseAccount_NotFound() {
        when(repo.getByAccountnumber("INVALID")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> accountService.closeAccount("INVALID"));
    }

    @Test
    void testCloseAccount_Forbidden() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("2");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getByAccountnumber("AC112345678901234567890")).thenReturn(Optional.of(testAccount));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> accountService.closeAccount("AC112345678901234567890"));
        assertEquals("You do not have permission to access this resource.", exception.getMessage());
        verify(repo, never()).delete(any(Account.class));

        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsOwnerOfAccountNumber_Success_AsOwner() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getByAccountnumber("AC112345678901234567890")).thenReturn(Optional.of(testAccount));

        Boolean result = accountService.isOwnerOfAccountNumber("AC112345678901234567890");

        assertTrue(result);

        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsOwnerOfAccountNumber_Success_AsAdmin() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);

        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        when(adminAuthority.getAuthority()).thenReturn("ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        Boolean result = accountService.isOwnerOfAccountNumber("AC112345678901234567890");

        assertTrue(result);
        verify(repo, never()).getByAccountnumber(anyString());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsOwnerOfAccountNumber_Success_AsInternalService() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);

        GrantedAuthority internalAuthority = mock(GrantedAuthority.class);
        when(internalAuthority.getAuthority()).thenReturn("INTERNAL_SERVICE");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(internalAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        Boolean result = accountService.isOwnerOfAccountNumber("AC112345678901234567890");

        assertTrue(result);
        verify(repo, never()).getByAccountnumber(anyString());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsOwnerOfAccountNumber_NotFound() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getByAccountnumber("INVALID")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> accountService.isOwnerOfAccountNumber("INVALID"));

        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsOwnerOfAccountId_Success_AsOwner() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getById(1L)).thenReturn(Optional.of(testAccount));

        Boolean result = accountService.isOwnerOfAccountId(1L);

        assertTrue(result);

        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsOwnerOfAccountId_Success_AsAdmin() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);

        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        when(adminAuthority.getAuthority()).thenReturn("ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        Boolean result = accountService.isOwnerOfAccountId(1L);

        assertTrue(result);
        verify(repo, never()).getById(anyLong());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsOwnerOfAccountId_Success_AsInternalService() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);

        GrantedAuthority internalAuthority = mock(GrantedAuthority.class);
        when(internalAuthority.getAuthority()).thenReturn("INTERNAL_SERVICE");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(internalAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        Boolean result = accountService.isOwnerOfAccountId(1L);

        assertTrue(result);
        verify(repo, never()).getById(anyLong());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsOwnerOfAccountId_NotFound() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> accountService.isOwnerOfAccountId(1L));

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetAccountTypeByAccountId_Success() {
        when(repo.getById(1L)).thenReturn(Optional.of(testAccount));

        AccountType result = accountService.getAccountTypeByAccountId(1L);

        assertNotNull(result);
        assertEquals(AccountType.SAVINGS, result);
    }

    @Test
    void testGetAccountTypeByAccountId_NotFound() {
        when(repo.getById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> accountService.getAccountTypeByAccountId(1L));
    }

    @Test
    void testGetAccountIdsByUserId_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("1");

        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        lenient().when(adminAuthority.getAuthority()).thenReturn("ROLE_ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        lenient().doReturn(authorities).when(authentication).getAuthorities();

        List<Account> accounts = new ArrayList<>();
        accounts.add(testAccount);
        when(repo.getByUserid(1L)).thenReturn(Optional.of(accounts));

        List<Long> result = accountService.getAccountIdsByUserId();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0));

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetAccountIdsByUserId_NoContent() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("1");

        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        lenient().when(adminAuthority.getAuthority()).thenReturn("ROLE_ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        lenient().doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getByUserid(1L)).thenReturn(Optional.of(new ArrayList<>()));

        assertThrows(NoContentException.class, () -> accountService.getAccountIdsByUserId());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetAccountIdsByUserId_Forbidden() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        Account otherAccount = new Account();
        otherAccount.setId(2L);
        otherAccount.setUserid(3L);

        List<Account> accounts = new ArrayList<>();
        accounts.add(otherAccount);
        when(repo.getByUserid(1L)).thenReturn(Optional.of(accounts));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> accountService.getAccountIdsByUserId());
        assertEquals("You do not have permission to access this resource.", exception.getMessage());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetMyAccounts_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        List<Account> accounts = new ArrayList<>();
        accounts.add(testAccount);
        when(repo.getByUserid(1L)).thenReturn(Optional.of(accounts));

        List<Account> result = accountService.getMyAccounts();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testAccount.getId(), result.get(0).getId());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetMyAccounts_NoContent() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getByUserid(1L)).thenReturn(Optional.of(new ArrayList<>()));

        assertThrows(NoContentException.class, () -> accountService.getMyAccounts());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetUserIdByAccountId_Success() {
        when(repo.getById(1L)).thenReturn(Optional.of(testAccount));

        Long result = accountService.getuserIdByAccountId(1L);

        assertNotNull(result);
        assertEquals(1L, result);
    }

    @Test
    void testGetUserIdByAccountId_NotFound() {
        when(repo.getById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> accountService.getuserIdByAccountId(1L));
    }

    @Test
    void testGetAccountNumberByAccountId_Success() {
        when(repo.getById(1L)).thenReturn(Optional.of(testAccount));

        String result = accountService.getAccountNumberByAccountId(1L);

        assertNotNull(result);
        assertEquals("AC112345678901234567890", result);
    }

    @Test
    void testGetAccountNumberByAccountId_NotFound() {
        when(repo.getById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> accountService.getAccountNumberByAccountId(1L));
    }

    @Test
    void testAccountExists_Success_AsOwner() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getById(1L)).thenReturn(Optional.of(testAccount));

        Boolean result = accountService.accountExists(1L);

        assertTrue(result);

        SecurityContextHolder.clearContext();
    }

    @Test
    void testAccountExists_Success_AsAdmin() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("1");

        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        lenient().when(adminAuthority.getAuthority()).thenReturn("ROLE_ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        lenient().doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getById(1L)).thenReturn(Optional.of(testAccount));

        Boolean result = accountService.accountExists(1L);

        assertTrue(result);

        SecurityContextHolder.clearContext();
    }

    @Test
    void testAccountExists_Forbidden() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("2");

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        when(repo.getById(1L)).thenReturn(Optional.of(testAccount));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> accountService.accountExists(1L));
        assertEquals("You do not have permission to access this resource.", exception.getMessage());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetAllAccounts_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);

        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        lenient().when(adminAuthority.getAuthority()).thenReturn("ROLE_ADMIN");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        lenient().doReturn(authorities).when(authentication).getAuthorities();

        List<Account> allAccounts = new ArrayList<>();
        allAccounts.add(testAccount);
        when(repo.findAll()).thenReturn(allAccounts);
        when(repo.getById(1L)).thenReturn(Optional.of(testAccount));

        List<Account> result = accountService.getAllAccounts();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetAllAccounts_Forbidden() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);

        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> accountService.getAllAccounts());
        assertEquals("You do not have permission to access this resource.", exception.getMessage());
        verify(repo, never()).findAll();

        SecurityContextHolder.clearContext();
    }
}
