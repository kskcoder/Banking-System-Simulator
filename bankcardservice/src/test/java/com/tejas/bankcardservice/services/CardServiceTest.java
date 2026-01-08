package com.tejas.bankcardservice.services;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tejas.bankcardservice.dtos.FirstCardResponse;
import com.tejas.bankcardservice.dtos.GeneralCardResponse;
import com.tejas.bankcardservice.exceptions.AlreadyExistsException;
import com.tejas.bankcardservice.feign.AccountInterface;
import com.tejas.bankcardservice.model.Card;
import com.tejas.bankcardservice.repositories.CardRepo;
import com.tejas.bankcardservice.utils.CardGenerals;
import com.tejas.bankingcommon.dto.CardVerificationRequest;
import com.tejas.bankingcommon.dto.CardVerificationResponse;
import com.tejas.bankingcommon.enums.AccountCardStatus;
import com.tejas.bankingcommon.enums.AccountType;
import com.tejas.bankingcommon.enums.UserType;
import com.tejas.bankingcommon.exceptions.BadRequestException;
import com.tejas.bankingcommon.exceptions.ForbiddenException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NoContentException;
import com.tejas.bankingcommon.exceptions.NotFoundException;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CardServiceTest {

    @Mock
    private CardRepo repo;

    @Mock
    private CardGenerals generals;

    @Mock
    private AccountInterface accInterface;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cardCache;

    @Mock
    private Cache userCardsCache;

    @Mock
    private Cache allCardsCache;

    @InjectMocks
    private CardService cardService;

    private Card testCard;
    private String testCardNumber;
    private String testCvv;
    private String testExpiryDate;
    private long testAccountId;
    private long testUserId;

    @BeforeEach
    void setUp() {
        testAccountId = 1L;
        testUserId = 1L;
        testCardNumber = "1111123456789012";
        testCvv = "123";
        testExpiryDate = LocalDate.now().plusYears(5).format(DateTimeFormatter.ofPattern("MM/yyyy"));

        testCard = Card.builder()
                .id(1L)
                .cardNumber("hashedCardNumber")
                .cvv("hashedCvv")
                .lastDigits("9012")
                .expiryDate(testExpiryDate)
                .cardLimit(50000.0)
                .status(AccountCardStatus.INACTIVE)
                .accountId(testAccountId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(cardService, "cacheManager", cacheManager);

        lenient().when(cacheManager.getCache("card_of_accountid")).thenReturn(cardCache);
        lenient().when(cacheManager.getCache("all_cards_of_userid")).thenReturn(userCardsCache);
        lenient().when(cacheManager.getCache("all_cards")).thenReturn(allCardsCache);
        lenient().doNothing().when(cardCache).evict(anyLong());
        lenient().doNothing().when(userCardsCache).evict(anyLong());
        lenient().doNothing().when(allCardsCache).clear();

        RequestContextHolder.setRequestAttributes(null);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateCard_Success() {
        mockRequestContext(null);
        when(accInterface.accountExists(testAccountId)).thenReturn(ResponseEntity.ok(true));
        when(accInterface.isOwnerOfAccountId(testAccountId)).thenReturn(ResponseEntity.ok(true));
        when(repo.existsByAccountId(testAccountId)).thenReturn(false);
        when(generals.cardNumberGenerator()).thenReturn(testCardNumber);
        when(generals.hash(testCardNumber)).thenReturn("hashedCardNumber");
        when(repo.existsByCardNumber("hashedCardNumber")).thenReturn(false);
        when(accInterface.getAccountTypeByAccountId(testAccountId)).thenReturn(ResponseEntity.ok(AccountType.SAVINGS));
        when(generals.cvvGenerator()).thenReturn(testCvv);
        when(generals.hash(testCvv)).thenReturn("hashedCvv");
        when(generals.expiryGenerator()).thenReturn(testExpiryDate);
        when(generals.getDailyLimit(AccountType.SAVINGS)).thenReturn(50000.0);
        when(repo.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            card.setId(1L);
            return card;
        });
        mockSecurityContext("1");

        FirstCardResponse result = cardService.createCard(testAccountId);

        assertNotNull(result);
        assertEquals(testExpiryDate, result.getExpiry());
        assertEquals(AccountCardStatus.INACTIVE, result.getStatus());
        assertEquals(50000.0, result.getLimit());
        verify(repo).save(any(Card.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateCard_AccountDoesNotExist() {
        mockRequestContext(null);
        when(accInterface.accountExists(testAccountId)).thenReturn(ResponseEntity.ok(false));

        assertThrows(GeneralServerException.class, () -> cardService.createCard(testAccountId));
    }

    @Test
    void testCreateCard_FeignException() {
        mockRequestContext(null);
        when(accInterface.accountExists(testAccountId)).thenThrow(mock(FeignException.class));

        assertThrows(GeneralServerException.class, () -> cardService.createCard(testAccountId));
    }

    @Test
    void testCreateCard_Forbidden() {
        mockRequestContext(null);
        when(accInterface.accountExists(testAccountId)).thenReturn(ResponseEntity.ok(true));
        when(accInterface.isOwnerOfAccountId(testAccountId)).thenReturn(ResponseEntity.ok(false));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> cardService.createCard(testAccountId));
        assertEquals("You do not have permission to use this card.", exception.getMessage());
    }

    @Test
    void testCreateCard_AlreadyExists() {
        mockRequestContext(null);
        when(accInterface.accountExists(testAccountId)).thenReturn(ResponseEntity.ok(true));
        when(accInterface.isOwnerOfAccountId(testAccountId)).thenReturn(ResponseEntity.ok(true));
        when(repo.existsByAccountId(testAccountId)).thenReturn(true);

        AlreadyExistsException exception = assertThrows(AlreadyExistsException.class, () -> cardService.createCard(testAccountId));
        assertEquals(String.valueOf(testAccountId), exception.getMessage());
    }

    @Test
    void testGetCardByAccountId_Success() {
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountId(testAccountId)).thenReturn(ResponseEntity.ok(true));
        when(repo.getByAccountId(testAccountId)).thenReturn(Optional.of(testCard));
        when(generals.createGeneralCardResponse(testCard)).thenReturn(GeneralCardResponse.builder()
                .maskedCardNumber("****9012")
                .expiry(testExpiryDate)
                .status(AccountCardStatus.INACTIVE)
                .limit(50000.0)
                .build());

        GeneralCardResponse result = cardService.getCardByAccountId(testAccountId);

        assertNotNull(result);
        assertEquals("****9012", result.getMaskedCardNumber());
        assertEquals(testExpiryDate, result.getExpiry());
    }

    @Test
    void testGetCardByAccountId_Forbidden() {
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountId(testAccountId)).thenReturn(ResponseEntity.ok(false));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> cardService.getCardByAccountId(testAccountId));
        assertEquals("You do not have permission to use this card.", exception.getMessage());
    }

    @Test
    void testGetCardByAccountId_NotFound() {
        mockRequestContext(null);
        when(accInterface.isOwnerOfAccountId(testAccountId)).thenReturn(ResponseEntity.ok(true));
        when(repo.getByAccountId(testAccountId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> cardService.getCardByAccountId(testAccountId));
        assertEquals("No card associated with Account ID: " + testAccountId, exception.getMessage());
    }

    @Test
    void testGetUsersAllCards_Success() {
        mockSecurityContext("1");
        List<Long> accountIds = new ArrayList<>();
        accountIds.add(testAccountId);
        when(accInterface.getAccountIdsByUserId()).thenReturn(ResponseEntity.ok(accountIds));
        when(repo.getByAccountId(testAccountId)).thenReturn(Optional.of(testCard));
        when(generals.createGeneralCardResponse(testCard)).thenReturn(GeneralCardResponse.builder()
                .maskedCardNumber("****9012")
                .expiry(testExpiryDate)
                .status(AccountCardStatus.INACTIVE)
                .limit(50000.0)
                .build());

        List<GeneralCardResponse> result = cardService.getUsersAllCards();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetUsersAllCards_NoAccounts() {
        mockSecurityContext("1");
        when(accInterface.getAccountIdsByUserId()).thenReturn(ResponseEntity.ok(new ArrayList<>()));

        NotFoundException exception = assertThrows(NotFoundException.class, () -> cardService.getUsersAllCards());
        assertEquals("No accounts associated with User ID: 1", exception.getMessage());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetUsersAllCards_NoCards() {
        mockSecurityContext("1");
        List<Long> accountIds = new ArrayList<>();
        accountIds.add(testAccountId);
        when(accInterface.getAccountIdsByUserId()).thenReturn(ResponseEntity.ok(accountIds));
        when(repo.getByAccountId(testAccountId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> cardService.getUsersAllCards());
        assertEquals("No card associated with User ID: 1", exception.getMessage());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetUsersAllCards_FeignException() {
        mockSecurityContext("1");
        when(accInterface.getAccountIdsByUserId()).thenThrow(mock(FeignException.class));

        NotFoundException exception = assertThrows(NotFoundException.class, () -> cardService.getUsersAllCards());
        assertEquals("Unauthorised or no accounts associated with User ID: 1", exception.getMessage());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testBlockCard_Success() {
        mockRequestContext(null);
        when(repo.getByAccountId(testAccountId)).thenReturn(Optional.of(testCard));
        when(accInterface.isOwnerOfAccountId(testAccountId)).thenReturn(ResponseEntity.ok(true));
        when(repo.save(any(Card.class))).thenReturn(testCard);
        mockSecurityContext("1");
        when(generals.createGeneralCardResponse(any(Card.class))).thenReturn(GeneralCardResponse.builder()
                .maskedCardNumber("****9012")
                .expiry(testExpiryDate)
                .status(AccountCardStatus.BLOCKED)
                .limit(50000.0)
                .build());

        GeneralCardResponse result = cardService.blockCard(testAccountId);

        assertNotNull(result);
        assertEquals(AccountCardStatus.BLOCKED, result.getStatus());
        verify(repo).save(any(Card.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testBlockCard_NotFound() {
        mockRequestContext(null);
        when(repo.getByAccountId(testAccountId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> cardService.blockCard(testAccountId));
        assertEquals("No card associated with Account ID: " + testAccountId, exception.getMessage());
    }

    @Test
    void testBlockCard_Forbidden() {
        mockRequestContext(null);
        when(repo.getByAccountId(testAccountId)).thenReturn(Optional.of(testCard));
        when(accInterface.isOwnerOfAccountId(testAccountId)).thenReturn(ResponseEntity.ok(false));

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> cardService.blockCard(testAccountId));
        assertEquals("You do not have permission to use this card.", exception.getMessage());
    }

    @Test
    void testUnblockCard_Success() {
        mockRequestContext(null);
        testCard.setStatus(AccountCardStatus.BLOCKED);
        when(repo.getByAccountId(testAccountId)).thenReturn(Optional.of(testCard));
        when(accInterface.isOwnerOfAccountId(testAccountId)).thenReturn(ResponseEntity.ok(true));
        when(repo.save(any(Card.class))).thenReturn(testCard);
        mockSecurityContext("1");
        when(generals.createGeneralCardResponse(any(Card.class))).thenReturn(GeneralCardResponse.builder()
                .maskedCardNumber("****9012")
                .expiry(testExpiryDate)
                .status(AccountCardStatus.INACTIVE)
                .limit(50000.0)
                .build());

        GeneralCardResponse result = cardService.unblockCard(testAccountId);

        assertNotNull(result);
        assertEquals(AccountCardStatus.INACTIVE, result.getStatus());
        verify(repo).save(any(Card.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteCard_Success() {
        mockRequestContext(null);
        when(repo.getByAccountId(testAccountId)).thenReturn(Optional.of(testCard));
        when(accInterface.isOwnerOfAccountId(testAccountId)).thenReturn(ResponseEntity.ok(true));
        doNothing().when(repo).delete(any(Card.class));
        mockSecurityContext("1");
        when(generals.createGeneralCardResponse(testCard)).thenReturn(GeneralCardResponse.builder()
                .maskedCardNumber("****9012")
                .expiry(testExpiryDate)
                .status(AccountCardStatus.INACTIVE)
                .limit(50000.0)
                .build());

        GeneralCardResponse result = cardService.deleteCard(testAccountId);

        assertNotNull(result);
        verify(repo).delete(testCard);
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testVerifyCard_Success() {
        CardVerificationRequest request = CardVerificationRequest.builder()
                .cardNumber(testCardNumber)
                .cvv(testCvv)
                .expiryDate(testExpiryDate)
                .amount(1000.0)
                .build();

        when(generals.hash(testCardNumber)).thenReturn("hashedCardNumber");
        when(repo.getByCardNumber("hashedCardNumber")).thenReturn(Optional.of(testCard));
        when(generals.hash(testCvv)).thenReturn("hashedCvv");
        when(generals.isExpired(testExpiryDate)).thenReturn(false);

        CardVerificationResponse result = cardService.verifyCard(request);

        assertNotNull(result);
        assertTrue(result.isValidated());
        assertEquals(testAccountId, result.getAccountId());
    }

    @Test
    void testVerifyCard_CardNotFound() {
        CardVerificationRequest request = CardVerificationRequest.builder()
                .cardNumber(testCardNumber)
                .cvv(testCvv)
                .expiryDate(testExpiryDate)
                .amount(1000.0)
                .build();

        when(generals.hash(testCardNumber)).thenReturn("hashedCardNumber");
        when(repo.getByCardNumber("hashedCardNumber")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> cardService.verifyCard(request));
        assertEquals("Card not found", exception.getMessage());
    }

    @Test
    void testVerifyCard_IncorrectExpiryDate() {
        CardVerificationRequest request = CardVerificationRequest.builder()
                .cardNumber(testCardNumber)
                .cvv(testCvv)
                .expiryDate("12/2025")
                .amount(1000.0)
                .build();

        when(generals.hash(testCardNumber)).thenReturn("hashedCardNumber");
        when(repo.getByCardNumber("hashedCardNumber")).thenReturn(Optional.of(testCard));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> cardService.verifyCard(request));
        assertEquals("Incorrect expiry date", exception.getMessage());
    }

    @Test
    void testVerifyCard_CardExpired() {
        CardVerificationRequest request = CardVerificationRequest.builder()
                .cardNumber(testCardNumber)
                .cvv(testCvv)
                .expiryDate(testExpiryDate)
                .amount(1000.0)
                .build();

        when(generals.hash(testCardNumber)).thenReturn("hashedCardNumber");
        when(repo.getByCardNumber("hashedCardNumber")).thenReturn(Optional.of(testCard));
        when(generals.hash(testCvv)).thenReturn("hashedCvv");
        when(generals.isExpired(testExpiryDate)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> cardService.verifyCard(request));
        assertEquals("Card has expired.", exception.getMessage());
    }

    @Test
    void testVerifyCard_IncorrectCvv_NotExpired() {
        CardVerificationRequest request = CardVerificationRequest.builder()
                .cardNumber(testCardNumber)
                .cvv("999")
                .expiryDate(testExpiryDate)
                .amount(1000.0)
                .build();

        when(generals.hash(testCardNumber)).thenReturn("hashedCardNumber");
        when(repo.getByCardNumber("hashedCardNumber")).thenReturn(Optional.of(testCard));
        when(generals.hash("999")).thenReturn("wrongCvv");
        when(generals.isExpired(testExpiryDate)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> cardService.verifyCard(request));
        assertEquals("Incorrect CVV number and Card has expired.", exception.getMessage());
    }

    @Test
    void testVerifyCard_IncorrectCvv_Expired() {
        CardVerificationRequest request = CardVerificationRequest.builder()
                .cardNumber(testCardNumber)
                .cvv("999")
                .expiryDate(testExpiryDate)
                .amount(1000.0)
                .build();

        when(generals.hash(testCardNumber)).thenReturn("hashedCardNumber");
        when(repo.getByCardNumber("hashedCardNumber")).thenReturn(Optional.of(testCard));
        when(generals.hash("999")).thenReturn("wrongCvv");
        when(generals.isExpired(testExpiryDate)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> cardService.verifyCard(request));
        assertEquals("Incorrect CVV number.", exception.getMessage());
    }

    @Test
    void testVerifyCard_ExceededLimit() {
        CardVerificationRequest request = CardVerificationRequest.builder()
                .cardNumber(testCardNumber)
                .cvv(testCvv)
                .expiryDate(testExpiryDate)
                .amount(60000.0)
                .build();

        when(generals.hash(testCardNumber)).thenReturn("hashedCardNumber");
        when(repo.getByCardNumber("hashedCardNumber")).thenReturn(Optional.of(testCard));
        when(generals.hash(testCvv)).thenReturn("hashedCvv");
        when(generals.isExpired(testExpiryDate)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> cardService.verifyCard(request));
        assertEquals("Card has exceeded daily limit.", exception.getMessage());
    }

    @Test
    void testVerifyCard_Blocked() {
        testCard.setStatus(AccountCardStatus.BLOCKED);
        CardVerificationRequest request = CardVerificationRequest.builder()
                .cardNumber(testCardNumber)
                .cvv(testCvv)
                .expiryDate(testExpiryDate)
                .amount(1000.0)
                .build();

        when(generals.hash(testCardNumber)).thenReturn("hashedCardNumber");
        when(repo.getByCardNumber("hashedCardNumber")).thenReturn(Optional.of(testCard));
        when(generals.hash(testCvv)).thenReturn("hashedCvv");
        when(generals.isExpired(testExpiryDate)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> cardService.verifyCard(request));
        assertEquals("Card is blocked.", exception.getMessage());
    }

    @Test
    void testVerifyCard_ActivatesInactiveCard() {
        CardVerificationRequest request = CardVerificationRequest.builder()
                .cardNumber(testCardNumber)
                .cvv(testCvv)
                .expiryDate(testExpiryDate)
                .amount(1000.0)
                .build();

        when(generals.hash(testCardNumber)).thenReturn("hashedCardNumber");
        when(repo.getByCardNumber("hashedCardNumber")).thenReturn(Optional.of(testCard));
        when(generals.hash(testCvv)).thenReturn("hashedCvv");
        when(generals.isExpired(testExpiryDate)).thenReturn(false);
        when(repo.save(any(Card.class))).thenReturn(testCard);

        CardVerificationResponse result = cardService.verifyCard(request);

        assertNotNull(result);
        assertTrue(result.isValidated());
        verify(repo).save(any(Card.class));
    }

    @Test
    void testGetAllCards_Success_AsAdmin() {
        mockRequestContext("ADMIN");
        List<Card> cards = new ArrayList<>();
        cards.add(testCard);
        when(repo.findAll()).thenReturn(cards);
        when(generals.createGeneralCardResponse(testCard)).thenReturn(GeneralCardResponse.builder()
                .maskedCardNumber("****9012")
                .expiry(testExpiryDate)
                .status(AccountCardStatus.INACTIVE)
                .limit(50000.0)
                .build());

        List<GeneralCardResponse> result = cardService.getAllCards();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testGetAllCards_Forbidden_NotAdmin() {
        mockRequestContext("USER");

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> cardService.getAllCards());
        assertEquals("You do not have permission to access this resource.", exception.getMessage());
    }

    @Test
    void testGetAllCards_NoContent() {
        mockRequestContext("ADMIN");
        when(repo.findAll()).thenReturn(new ArrayList<>());

        NoContentException exception = assertThrows(NoContentException.class, () -> cardService.getAllCards());
        assertEquals("No cards found.", exception.getMessage());
    }

    @Test
    void testGetAllCards_NoRequestContext() {
        RequestContextHolder.setRequestAttributes(null);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> cardService.getAllCards());
        assertEquals("You do not have permission to access this resource.", exception.getMessage());
    }

    @Test
    void testEvictCardCache() {
        when(cacheManager.getCache("card_of_accountid")).thenReturn(cardCache);
        doNothing().when(cardCache).evict(testAccountId);

        cardService.evictCardCache(testAccountId);

        verify(cardCache).evict(testAccountId);
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
}
