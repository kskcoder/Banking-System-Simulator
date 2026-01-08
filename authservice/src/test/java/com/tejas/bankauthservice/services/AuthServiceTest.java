package com.tejas.bankauthservice.services;

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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.tejas.bankauthservice.exceptions.AlreadyUsedException;
import com.tejas.bankauthservice.models.ChangePasswordRequest;
import com.tejas.bankauthservice.models.LoginRequest;
import com.tejas.bankauthservice.models.Otp;
import com.tejas.bankauthservice.models.SignupRequest;
import com.tejas.bankauthservice.models.User;
import com.tejas.bankauthservice.repositories.AuthRepo;
import com.tejas.bankauthservice.repositories.OtpRepo;
import com.tejas.bankingcommon.dto.ContactDetails;
import com.tejas.bankingcommon.dto.MessageType;
import com.tejas.bankingcommon.dto.OtpRequestDTO;
import com.tejas.bankingcommon.dto.OtpStatus;
import com.tejas.bankingcommon.dto.OtpValidateRequest;
import com.tejas.bankingcommon.dto.OtpValidateResponse;
import com.tejas.bankingcommon.enums.InternalServiceType;
import com.tejas.bankingcommon.enums.UserType;
import com.tejas.bankingcommon.exceptions.BadRequestException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NotFoundException;
import com.tejas.bankingcommon.exceptions.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthRepo repo;

    @Mock
    private OtpRepo otpRepo;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JWTService jwtService;

    @Mock
    private AuthUserDetailsService userDetailsService;

    @Mock
    private AuthOtpProducer otpProducer;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setPhone("1234567890");
        testUser.setRole(UserType.USER);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        signupRequest = new SignupRequest();
        signupRequest.setUsername("newuser");
        signupRequest.setEmail("newuser@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setPhone("9876543210");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setOldPassword("oldPassword");
        changePasswordRequest.setNewPassword("newPassword");

        lenient().when(cacheManager.getCache("user_details")).thenReturn(cache);
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSignupUser_Success() {
        when(repo.getByUsername(signupRequest.getUsername())).thenReturn(Optional.empty());
        when(repo.getByEmail(signupRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(signupRequest.getPassword())).thenReturn("encodedPassword");
        when(repo.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        User result = authService.signupUser(signupRequest);

        assertNotNull(result);
        assertEquals(signupRequest.getUsername(), result.getUsername());
        assertEquals(signupRequest.getEmail(), result.getEmail());
        assertEquals(UserType.USER, result.getRole());
        verify(repo).save(any(User.class));
        verify(cache).evict(signupRequest.getUsername());
    }

    @Test
    void testSignupUser_UsernameAlreadyExists() {
        when(repo.getByUsername(signupRequest.getUsername())).thenReturn(Optional.of(testUser));

        assertThrows(AlreadyUsedException.class, () -> authService.signupUser(signupRequest));
        verify(repo, never()).save(any(User.class));
    }

    @Test
    void testSignupUser_EmailAlreadyExists() {
        when(repo.getByUsername(signupRequest.getUsername())).thenReturn(Optional.empty());
        when(repo.getByEmail(signupRequest.getEmail())).thenReturn(Optional.of(testUser));

        assertThrows(AlreadyUsedException.class, () -> authService.signupUser(signupRequest));
        verify(repo, never()).save(any(User.class));
    }

    @Test
    void testSignupUser_DatabaseException() {
        when(repo.getByUsername(signupRequest.getUsername())).thenReturn(Optional.empty());
        when(repo.getByEmail(signupRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(signupRequest.getPassword())).thenReturn("encodedPassword");
        when(repo.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        assertThrows(GeneralServerException.class, () -> authService.signupUser(signupRequest));
    }

    @Test
    void testVerifyUser_Success() {
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(repo.getByUsername(loginRequest.getUsername())).thenReturn(Optional.of(testUser));
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userDetailsService.loadUserByUsername(loginRequest.getUsername())).thenReturn(userDetails);
        when(jwtService.generateToken(testUser.getId(), userDetails)).thenReturn("jwtToken");

        String result = authService.verifyUser(loginRequest);

        assertNotNull(result);
        assertEquals("jwtToken", result);
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(testUser.getId(), userDetails);
    }

    @Test
    void testVerifyUser_UserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password");
        
        when(repo.getByUsername("nonexistent")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> authService.verifyUser(request));
        assertEquals("User not found", exception.getMessage());
        verify(repo).getByUsername("nonexistent");
        verify(authManager, never()).authenticate(any());
    }

    @Test
    void testVerifyUser_BadCredentials() {
        when(repo.getByUsername(loginRequest.getUsername())).thenReturn(Optional.of(testUser));
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> authService.verifyUser(loginRequest));
        assertEquals("Incorrect Password", exception.getMessage());
    }

    @Test
    void testVerifyUser_NotAuthenticated() {
        Authentication authentication = mock(Authentication.class);
        when(repo.getByUsername(loginRequest.getUsername())).thenReturn(Optional.of(testUser));
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> authService.verifyUser(loginRequest));
        assertEquals("Authentication failed", exception.getMessage());
    }

    @Test
    void testVerifyUser_IllegalArgumentException() {
        when(repo.getByUsername(loginRequest.getUsername())).thenReturn(Optional.of(testUser));
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new IllegalArgumentException("Invalid argument"));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> authService.verifyUser(loginRequest));
        assertEquals("Authentication configuration error", exception.getMessage());
    }

    @Test
    void testVerifyUser_InternalAuthenticationServiceException() {
        when(repo.getByUsername(loginRequest.getUsername())).thenReturn(Optional.of(testUser));
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new InternalAuthenticationServiceException("Internal error"));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> authService.verifyUser(loginRequest));
        assertEquals("Authentication failed", exception.getMessage());
    }

    @Test
    void testChangePassword_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");
        
        when(repo.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(changePasswordRequest.getOldPassword(), testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(changePasswordRequest.getNewPassword(), testUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(changePasswordRequest.getNewPassword())).thenReturn("newEncodedPassword");
        when(repo.save(testUser)).thenReturn(testUser);

        String result = authService.changePassword(1L, changePasswordRequest);

        assertEquals("Password changed successfully.", result);
        verify(repo).save(testUser);
        verify(cache).evict(testUser.getUsername());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testChangePassword_UserNotFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.changePassword(1L, changePasswordRequest));
        verify(repo, never()).save(any(User.class));
    }

    @Test
    void testChangePassword_IncorrectOldPassword() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");
        
        when(repo.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(changePasswordRequest.getOldPassword(), testUser.getPassword())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.changePassword(1L, changePasswordRequest));
        verify(repo, never()).save(any(User.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testChangePassword_SameOldAndNewPassword() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");
        
        when(repo.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(changePasswordRequest.getOldPassword(), testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(changePasswordRequest.getNewPassword(), testUser.getPassword())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.changePassword(1L, changePasswordRequest));
        verify(repo, never()).save(any(User.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testChangePassword_Unauthorized() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("2");
        
        when(repo.findById(1L)).thenReturn(Optional.of(testUser));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> authService.changePassword(1L, changePasswordRequest));
        assertEquals("You do not have access to this account.", exception.getMessage());
        verify(repo, never()).save(any(User.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testMakeAdmin_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        when(adminAuthority.getAuthority()).thenReturn("ADMIN");
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        doReturn(authorities).when(authentication).getAuthorities();
        
        when(repo.getById(1L)).thenReturn(Optional.of(testUser));
        when(repo.save(testUser)).thenReturn(testUser);

        String result = authService.makeAdmin(1L);

        assertEquals("Changed role to admin successfully.", result);
        assertEquals(UserType.ADMIN, testUser.getRole());
        verify(repo).save(testUser);
        verify(cache).evict(testUser.getUsername());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testMakeAdmin_UserNotFound() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        when(adminAuthority.getAuthority()).thenReturn("ADMIN");
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        doReturn(authorities).when(authentication).getAuthorities();
        
        when(repo.getById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.makeAdmin(1L));
        verify(repo, never()).save(any(User.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testMakeAdmin_AlreadyAdmin() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        when(adminAuthority.getAuthority()).thenReturn("ADMIN");
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        doReturn(authorities).when(authentication).getAuthorities();
        
        testUser.setRole(UserType.ADMIN);
        when(repo.getById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> authService.makeAdmin(1L));
        verify(repo, never()).save(any(User.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testMakeAdmin_Unauthorized() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authorities).when(authentication).getAuthorities();

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> authService.makeAdmin(1L));
        assertEquals("You do not have access to this account.", exception.getMessage());
        verify(repo, never()).getById(anyLong());
        verify(repo, never()).save(any(User.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteUser_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");
        
        when(repo.getById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(repo).delete(testUser);

        String result = authService.deleteUser(1L);

        assertEquals("User deleted successfully.", result);
        verify(repo).delete(testUser);
        verify(cache).evict(testUser.getUsername());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteUser_UserNotFound() {
        when(repo.getById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> authService.deleteUser(1L));
        assertEquals("User not found", exception.getMessage());
        verify(repo, never()).delete(any(User.class));
    }

    @Test
    void testDeleteUser_Unauthorized() {
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
        
        when(repo.getById(1L)).thenReturn(Optional.of(testUser));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> authService.deleteUser(1L));
        assertEquals("You do not have access to this account.", exception.getMessage());
        verify(repo, never()).delete(any(User.class));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetContact_Success_AsAdmin() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        when(adminAuthority.getAuthority()).thenReturn("ADMIN");
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(adminAuthority);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authoritiesCast = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        doReturn(authoritiesCast).when(authentication).getAuthorities();
        
        when(repo.getById(1L)).thenReturn(Optional.of(testUser));

        ContactDetails result = authService.getContact(1L);

        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getPhone(), result.getPhone());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetContact_Success_AsInternalService() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        GrantedAuthority internalAuthority = mock(GrantedAuthority.class);
        when(internalAuthority.getAuthority()).thenReturn("ROLE_INTERNAL_SERVICE");
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(internalAuthority);
        doReturn(authorities).when(authentication).getAuthorities();
        
        when(repo.getById(1L)).thenReturn(Optional.of(testUser));

        ContactDetails result = authService.getContact(1L);

        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getPhone(), result.getPhone());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetContact_UserNotFound() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        when(adminAuthority.getAuthority()).thenReturn("ADMIN");
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        doReturn(authorities).when(authentication).getAuthorities();
        
        when(repo.getById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.getContact(1L));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetContact_Unauthorized() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        GrantedAuthority userAuthority = mock(GrantedAuthority.class);
        when(userAuthority.getAuthority()).thenReturn("USER");
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authoritiesCast = (Collection<GrantedAuthority>) Collections.singletonList(userAuthority);
        doReturn(authoritiesCast).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("2");

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> authService.getContact(1L));
        assertEquals("You do not have access to this account.", exception.getMessage());
        verify(repo, never()).getById(anyLong());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUserExists_True() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");
        
        when(repo.findById(1L)).thenReturn(Optional.of(testUser));

        Boolean result = authService.userExists(1L);

        assertTrue(result);
        verify(repo).findById(1L);
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUserExists_False() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("1");
        
        when(repo.findById(1L)).thenReturn(Optional.empty());

        Boolean result = authService.userExists(1L);

        assertFalse(result);
        verify(repo).findById(1L);
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUserExists_AsAdmin() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        
        GrantedAuthority adminAuthority = mock(GrantedAuthority.class);
        when(adminAuthority.getAuthority()).thenReturn("ADMIN");
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(adminAuthority);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authoritiesCast = (Collection<GrantedAuthority>) Collections.singletonList(adminAuthority);
        doReturn(authoritiesCast).when(authentication).getAuthorities();
        
        when(repo.findById(1L)).thenReturn(Optional.of(testUser));

        Boolean result = authService.userExists(1L);

        assertTrue(result);
        verify(repo).findById(1L);
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUserExists_Unauthorized() {
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

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> authService.userExists(1L));
        assertEquals("You do not have access to this account.", exception.getMessage());
        verify(repo, never()).findById(anyLong());
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSendOtp_Success() {
        OtpRequestDTO request = new OtpRequestDTO();
        request.setUserId(1L);
        request.setReferenceId(12345L);
        request.setType(MessageType.PAYMENT_OTP);

        when(repo.getById(1L)).thenReturn(Optional.of(testUser));
        when(otpRepo.save(any(Otp.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(otpProducer).dispatchResponseWithRetry(any());

        Boolean result = authService.sendOtp(request);

        assertTrue(result);
        verify(otpRepo).save(any(Otp.class));
        verify(otpProducer).dispatchResponseWithRetry(any());
    }

    @Test
    void testSendOtp_UserNotFound() {
        OtpRequestDTO request = new OtpRequestDTO();
        request.setUserId(1L);
        request.setReferenceId(12345L);
        request.setType(MessageType.PAYMENT_OTP);

        when(repo.getById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.sendOtp(request));
        verify(otpRepo, never()).save(any(Otp.class));
    }

    @Test
    void testValidateOtp_Success() {
        OtpValidateRequest request = new OtpValidateRequest();
        request.setReferenceId("12345");
        request.setType(MessageType.PAYMENT_OTP);
        request.setOtpValue("123456");

        Otp otp = Otp.builder()
                .otpHash(com.tejas.bankauthservice.utils.OtpUtils.hash("123456"))
                .type(MessageType.PAYMENT_OTP)
                .referenceId("12345")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .maxAttempts(3)
                .status(OtpStatus.PENDING)
                .build();

        when(otpRepo.findByReferenceIdAndType("12345", MessageType.PAYMENT_OTP)).thenReturn(Optional.of(otp));
        when(otpRepo.save(otp)).thenReturn(otp);

        OtpValidateResponse result = authService.validateOtp(request);

        assertNotNull(result);
        assertTrue(result.isValidated());
        assertEquals("OTP verified successfully.", result.getMessage());
        assertEquals(OtpStatus.VERIFIED, otp.getStatus());
        verify(otpRepo).save(otp);
    }

    @Test
    void testValidateOtp_AlreadyVerified() {
        OtpValidateRequest request = new OtpValidateRequest();
        request.setReferenceId("12345");
        request.setType(MessageType.PAYMENT_OTP);
        request.setOtpValue("123456");

        Otp otp = Otp.builder()
                .otpHash(com.tejas.bankauthservice.utils.OtpUtils.hash("123456"))
                .type(MessageType.PAYMENT_OTP)
                .referenceId("12345")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .maxAttempts(3)
                .status(OtpStatus.VERIFIED)
                .build();

        when(otpRepo.findByReferenceIdAndType("12345", MessageType.PAYMENT_OTP)).thenReturn(Optional.of(otp));

        OtpValidateResponse result = authService.validateOtp(request);

        assertNotNull(result);
        assertTrue(result.isValidated());
        assertTrue(result.isRetried());
        assertEquals("OTP already verified.", result.getMessage());
    }

    @Test
    void testValidateOtp_Expired() {
        OtpValidateRequest request = new OtpValidateRequest();
        request.setReferenceId("12345");
        request.setType(MessageType.PAYMENT_OTP);
        request.setOtpValue("123456");

        Otp otp = Otp.builder()
                .otpHash(com.tejas.bankauthservice.utils.OtpUtils.hash("123456"))
                .type(MessageType.PAYMENT_OTP)
                .referenceId("12345")
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .attempts(0)
                .maxAttempts(3)
                .status(OtpStatus.PENDING)
                .build();

        when(otpRepo.findByReferenceIdAndType("12345", MessageType.PAYMENT_OTP)).thenReturn(Optional.of(otp));
        when(otpRepo.save(otp)).thenReturn(otp);

        OtpValidateResponse result = authService.validateOtp(request);

        assertNotNull(result);
        assertFalse(result.isValidated());
        assertEquals("OTP has expired.", result.getMessage());
        assertEquals(OtpStatus.EXPIRED, otp.getStatus());
    }

    @Test
    void testValidateOtp_MaxAttempts() {
        OtpValidateRequest request = new OtpValidateRequest();
        request.setReferenceId("12345");
        request.setType(MessageType.PAYMENT_OTP);
        request.setOtpValue("wrongotp");

        Otp otp = Otp.builder()
                .otpHash(com.tejas.bankauthservice.utils.OtpUtils.hash("123456"))
                .type(MessageType.PAYMENT_OTP)
                .referenceId("12345")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(3)
                .maxAttempts(3)
                .status(OtpStatus.PENDING)
                .build();

        when(otpRepo.findByReferenceIdAndType("12345", MessageType.PAYMENT_OTP)).thenReturn(Optional.of(otp));
        when(otpRepo.save(otp)).thenReturn(otp);

        OtpValidateResponse result = authService.validateOtp(request);

        assertNotNull(result);
        assertFalse(result.isValidated());
        assertEquals("Max attempts crossed for this OTP.", result.getMessage());
        assertEquals(OtpStatus.MAX_ATTEMPTS, otp.getStatus());
    }

    @Test
    void testValidateOtp_InvalidOtp() {
        OtpValidateRequest request = new OtpValidateRequest();
        request.setReferenceId("12345");
        request.setType(MessageType.PAYMENT_OTP);
        request.setOtpValue("wrongotp");

        Otp otp = Otp.builder()
                .otpHash(com.tejas.bankauthservice.utils.OtpUtils.hash("123456"))
                .type(MessageType.PAYMENT_OTP)
                .referenceId("12345")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(1)
                .maxAttempts(3)
                .status(OtpStatus.PENDING)
                .build();

        when(otpRepo.findByReferenceIdAndType("12345", MessageType.PAYMENT_OTP)).thenReturn(Optional.of(otp));
        when(otpRepo.save(otp)).thenReturn(otp);

        OtpValidateResponse result = authService.validateOtp(request);

        assertNotNull(result);
        assertFalse(result.isValidated());
        assertEquals(2, otp.getAttempts());
    }

    @Test
    void testValidateOtp_NotFound() {
        OtpValidateRequest request = new OtpValidateRequest();
        request.setReferenceId("12345");
        request.setType(MessageType.PAYMENT_OTP);
        request.setOtpValue("123456");

        when(otpRepo.findByReferenceIdAndType("12345", MessageType.PAYMENT_OTP)).thenReturn(Optional.empty());

        OtpValidateResponse result = authService.validateOtp(request);

        assertNotNull(result);
        assertFalse(result.isValidated());
        assertEquals("12345", result.getReferenceId());
    }
}
