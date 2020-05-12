package com.eirsteir.coffeewithme.web.api.friendship;

import com.eirsteir.coffeewithme.domain.friendship.FriendshipId;
import com.eirsteir.coffeewithme.domain.friendship.FriendshipStatus;
import com.eirsteir.coffeewithme.domain.user.User;
import com.eirsteir.coffeewithme.dto.FriendshipDto;
import com.eirsteir.coffeewithme.dto.UserDto;
import com.eirsteir.coffeewithme.exception.CWMException;
import com.eirsteir.coffeewithme.security.AuthenticationSuccessHandlerImpl;
import com.eirsteir.coffeewithme.security.SecurityConfig;
import com.eirsteir.coffeewithme.service.UserDetailsServiceImpl;
import com.eirsteir.coffeewithme.service.UserPrincipalImpl;
import com.eirsteir.coffeewithme.service.friendship.FriendshipService;
import com.eirsteir.coffeewithme.service.user.UserService;
import com.eirsteir.coffeewithme.util.JSONUtils;
import com.eirsteir.coffeewithme.web.request.FriendRequest;
import config.CWMExceptionTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;

import static com.eirsteir.coffeewithme.domain.friendship.FriendshipStatus.ACCEPTED;
import static com.eirsteir.coffeewithme.domain.friendship.FriendshipStatus.REQUESTED;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, CWMExceptionTestConfig.class})
@TestPropertySource("classpath:exception.properties")
@WebMvcTest(FriendshipController.class)
@ExtendWith(SpringExtension.class)
class FriendshipControllerTest {

    private final Long REQUESTER_ID = 1L;
    private final Long ADDRESSEE_ID = 2L;
    private static final String REQUESTER_EMAIL = "requester@test.com";
    private static final String ADDRESSEE_EMAIL = "addressee@test.com";

    private UserPrincipalImpl userPrincipal;
    private User requester;
    private FriendshipDto friendshipDto;
    private FriendshipId friendshipId;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FriendshipService friendshipService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationSuccessHandlerImpl authenticationSuccessHandler;


    @TestConfiguration
    static class FriendshipControllerTestContextConfig {

        @Bean
        public UserDetailsService userDetailsService() {
            return new UserDetailsServiceImpl();
        }

        @Bean
        public ModelMapper modelMapper() {
            return new ModelMapper();
        }

    }

    @BeforeEach
    void setUp() {
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        requester = User.builder()
                .id(REQUESTER_ID)
                .email(REQUESTER_EMAIL)
                .password("password")
                .roles(new ArrayList<>())
                .build();

        userPrincipal = new UserPrincipalImpl(requester);
        Mockito.when(authentication.getPrincipal())
                .thenReturn(userPrincipal);

        friendshipId = FriendshipId.builder()
                .requesterId(REQUESTER_ID)
                .addresseeId(ADDRESSEE_ID)
                .build();
        friendshipDto = FriendshipDto.builder()
                .id(friendshipId)
                .status(REQUESTED)
                .build();
    }

    @Test
    void testAddFriendshipWhenAddresseeExists_thenReturnHttp200() throws Exception {
        when(friendshipService.registerFriendship(Mockito.any(FriendRequest.class)))
                .thenReturn(friendshipDto);

        mockMvc.perform(post("/user/friends")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("to_friend", String.valueOf(ADDRESSEE_ID))
                                .with(user(userPrincipal)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id.requesterId", equalTo(requester.getId().intValue())))
                .andExpect(jsonPath("$.id.addresseeId", equalTo(ADDRESSEE_ID.intValue())))
                .andExpect(jsonPath("$.status", equalTo(FriendshipStatus.REQUESTED.getStatus())));
    }

    @Test
    void testAddFriendshipWhenAddresseeDoesNotExists_thenReturnHttp404() throws Exception {
        when(friendshipService.registerFriendship(Mockito.any(FriendRequest.class)))
                .thenThrow(CWMException.EntityNotFoundException.class);

        mockMvc.perform(post("/user/friends")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("to_friend", String.valueOf(ADDRESSEE_ID))
                                .with(user(userPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAddFriendshipWhenFriendshipAlreadyExists_thenReturnHttp404() throws Exception {
        when(friendshipService.registerFriendship(Mockito.any(FriendRequest.class)))
                .thenThrow(CWMException.DuplicateEntityException.class);

        mockMvc.perform(post("/user/friends")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("to_friend", String.valueOf(ADDRESSEE_ID))
                                .with(user(userPrincipal)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddFriendshipWhenUnauthorized_thenReturnHttp401() throws Exception {

        mockMvc.perform(post("/user/friends")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("to_friend", String.valueOf(ADDRESSEE_ID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAllFriendshipsOfWhenUserHasNoFriendships_thenReturnHttp204() throws Exception {
        when(friendshipService.findFriendsOf(Mockito.any(UserDto.class)))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(get("/user/friends")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(user(userPrincipal)))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.message", equalTo("User has no friendships")));
    }

    @Test
    void testAddFriendshipWhenUserNotFound_thenReturnHttp404() throws Exception {
        when(friendshipService.findFriendsOf(Mockito.any(UserDto.class)))
                .thenThrow(CWMException.EntityNotFoundException.class);

        mockMvc.perform(get("/user/friends")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(user(userPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAllFriendshipsOfWhenUnauthorized_thenReturnHttp401() throws Exception {

        mockMvc.perform(get("/user/friends")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAllFriendshipsOfWhenUserHasFriendships_thenReturnHttp200WithFriends() throws Exception {
        when(friendshipService.findFriendsOf(Mockito.any(UserDto.class)))
                .thenReturn(Arrays.asList(
                        UserDto.builder()
                        .email(REQUESTER_EMAIL)
                        .build(),
                        UserDto.builder()
                        .email(ADDRESSEE_EMAIL)
                        .build()
                ));

        mockMvc.perform(get("/user/friends")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email", equalTo(REQUESTER_EMAIL)))
                .andExpect(jsonPath("$[1].email", equalTo(ADDRESSEE_EMAIL)));
    }

    @Test
    void testAcceptFriendshipWhenFriendshipExistsAndStatusIsRequested_thenReturnHttp200() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        userPrincipal.getUser().setId(ADDRESSEE_ID);

        Mockito.when(authentication.getPrincipal())
                .thenReturn(userPrincipal);
        when(friendshipService.updateFriendship(Mockito.any(FriendshipDto.class)))
                .thenReturn(friendshipDto.setStatus(ACCEPTED));

        mockMvc.perform(put("/user/friends")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSONUtils.asJsonString(friendshipDto))
                                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo(ACCEPTED.getStatus())));
    }

    @Test
    void testAcceptFriendshipWhenFriendshipDoesNotBelongToUser_thenReturnHttp400() throws Exception {
        friendshipId = FriendshipId.builder()
                .requesterId(100L)
                .addresseeId(101L)
                .build();

        friendshipDto.setId(friendshipId);

        mockMvc.perform(put("/user/friends")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSONUtils.asJsonString(friendshipDto))
                                .with(user(userPrincipal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                                    equalTo("Friendship does not belong to current user")));
    }

    @Test
    void testAcceptFriendshipSentBySelf_thenReturnHttp400() throws Exception {

        mockMvc.perform(put("/user/friends")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSONUtils.asJsonString(friendshipDto))
                                .with(user(userPrincipal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                                    equalTo("Cannot accept friend request sent by yourself")));
    }

    @Test
    void testAcceptFriendshipWhenFriendshipNotFound_thenReturnHttp404() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        userPrincipal.getUser().setId(ADDRESSEE_ID);

        Mockito.when(authentication.getPrincipal())
                .thenReturn(userPrincipal);
        when(friendshipService.updateFriendship(Mockito.any(FriendshipDto.class)))
                .thenThrow(CWMException.EntityNotFoundException.class);

        mockMvc.perform(put("/user/friends")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSONUtils.asJsonString(friendshipDto))
                                .with(user(userPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAcceptFriendshipWhenUnauthorized_thenReturnHttp401() throws Exception {

        mockMvc.perform(put("/user/friends")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSONUtils.asJsonString(friendshipDto)))
                .andExpect(status().isUnauthorized());
    }
}