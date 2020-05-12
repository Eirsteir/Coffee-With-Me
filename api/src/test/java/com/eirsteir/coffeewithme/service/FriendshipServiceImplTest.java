package com.eirsteir.coffeewithme.service;

import com.eirsteir.coffeewithme.domain.friendship.Friendship;
import com.eirsteir.coffeewithme.domain.friendship.FriendshipId;
import com.eirsteir.coffeewithme.domain.friendship.FriendshipStatus;
import com.eirsteir.coffeewithme.domain.user.User;
import com.eirsteir.coffeewithme.dto.FriendshipDto;
import com.eirsteir.coffeewithme.dto.UserDto;
import com.eirsteir.coffeewithme.exception.CWMException;
import com.eirsteir.coffeewithme.repository.FriendshipRepository;
import com.eirsteir.coffeewithme.service.friendship.FriendshipService;
import com.eirsteir.coffeewithme.service.friendship.FriendshipServiceImpl;
import com.eirsteir.coffeewithme.service.user.UserService;
import com.eirsteir.coffeewithme.web.request.FriendRequest;
import config.CWMExceptionTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.eirsteir.coffeewithme.domain.friendship.FriendshipStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@Import(CWMExceptionTestConfig.class)
@TestPropertySource("classpath:exception.properties")
@ExtendWith(SpringExtension.class)
class FriendshipServiceImplTest {

    private static final String REQUESTER_USERNAME = "requester";
    private static final String ADDRESSEE_USERNAME = "addressee";

    private Friendship friendshipRequested;
    private FriendshipId friendshipId;
    private User requester;
    private User addressee;
    private FriendRequest friendRequest;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FriendshipService friendshipService;

    @MockBean
    private FriendshipRepository friendshipRepository;

    @MockBean
    private UserService userService;

    @TestConfiguration
    static class FriendshipServiceImplTestContextConfiguration {

        @Bean
        public FriendshipService friendshipService() {
            return new FriendshipServiceImpl();
        }

        @Bean
        public ModelMapper modelMapper() {
            return new ModelMapper();
        }
    }

    @BeforeEach
    void setUp() {
        requester = User.builder()
                .id(1L)
               .username(REQUESTER_USERNAME)
               .build();

        addressee = User.builder()
                .id(2L)
               .username(ADDRESSEE_USERNAME)
               .build();

        friendshipId = FriendshipId.builder()
                .requester(requester)
                .addressee(addressee)
                .build();

        friendshipRequested = Friendship.builder()
                .id(friendshipId)
                .requester(requester)
                .addressee(addressee)
                .status(REQUESTED)
                .build();

        friendRequest = FriendRequest.builder()
                .requesterId(requester.getId())
                .addresseeId(addressee.getId())
                .build();

        when(friendshipRepository.save(Mockito.any(Friendship.class)))
                .thenReturn(friendshipRequested);
        when(userService.findUserById(requester.getId()))
                .thenReturn(requester);
        when(userService.findUserById(addressee.getId()))
                .thenReturn(addressee);
    }

    @Test
    void testRegisterFriendship() {
        FriendshipDto savedFriendshipDto = friendshipService.registerFriendship(friendRequest);

        assertThat(savedFriendshipDto.getId().getRequester()).isEqualTo(requester);
        assertThat(savedFriendshipDto.getId().getAddressee()).isEqualTo(addressee);
        assertThat(savedFriendshipDto.getStatus()).isEqualTo(REQUESTED);
    }

    @Test
    void testRegisterFriendshipWhenDuplicate() {
        when(friendshipRepository.existsById(Mockito.any(FriendshipId.class)))
                .thenReturn(true);

        assertThatExceptionOfType(CWMException.DuplicateEntityException.class)
                .isThrownBy(() -> friendshipService.registerFriendship(friendRequest))
                .withMessage("Requested friendship with id=" +
                                     friendshipId +
                                     " already exists");
    }

    @Test
    void testRegisterFriendshipWhenFriendshipIdReversedIsDuplicate() {
        when(friendshipRepository.existsById(Mockito.any(FriendshipId.class)))
                .thenReturn(true);

        friendshipId = FriendshipId.builder()
                .requester(addressee)
                .addressee(requester)
                .build();

        FriendshipId expectedId = FriendshipId.builder()
                .requester(requester)
                .addressee(addressee)
                .build();

        assertThatExceptionOfType(CWMException.DuplicateEntityException.class)
                .isThrownBy(() -> friendshipService.registerFriendship(friendRequest))
                .withMessage("Requested friendship with id=" +
                                     expectedId +
                                     " already exists");
    }

    @Test
    void testRemoveFriendshipWhenNotFound() {
        when(friendshipRepository.existsById(Mockito.any(FriendshipId.class)))
                .thenReturn(true);

        FriendshipDto friendshipDto = modelMapper.map(friendshipRequested, FriendshipDto.class);

        assertThatExceptionOfType(CWMException.EntityNotFoundException.class)
                .isThrownBy(() -> friendshipService.removeFriendship(friendshipDto))
                .withMessage("Requested friendship with id=" +
                                     friendshipId +
                                     " does not exist");
    }

    static Stream<Arguments> statusChangeFromRequestedProvider() {
        return Stream.of(
                Arguments.of(REQUESTED),
                Arguments.of(ACCEPTED),
                Arguments.of(DECLINED),
                Arguments.of(BLOCKED)
        );
    }

    @ParameterizedTest
    @MethodSource("statusChangeFromRequestedProvider")
    void testUpdateFriendshipFromRequestedStatus(FriendshipStatus newStatus) {
        when(friendshipRepository.findByIdRequesterIdAndIdAddresseeId(requester.getId(), addressee.getId()))
                .thenReturn(Optional.ofNullable(friendshipRequested));

        FriendshipDto friendshipDto = modelMapper.map(friendshipRequested, FriendshipDto.class);
        friendshipDto.setStatus(newStatus);
        FriendshipDto acceptedFriendship = friendshipService.updateFriendship(friendshipDto);

        assertThat(acceptedFriendship.getStatus()).isEqualTo(newStatus);
    }

    @Test
    void testUpdateFriendshipFromAcceptedToBlocked() {
        Friendship friendshipAccepted = friendshipRequested.setStatus(ACCEPTED);
        when(friendshipRepository.findByIdRequesterIdAndIdAddresseeId(requester.getId(), addressee.getId()))
                .thenReturn(Optional.ofNullable(friendshipAccepted));

        FriendshipDto friendshipDto = modelMapper.map(friendshipAccepted, FriendshipDto.class);
        friendshipDto.setStatus(BLOCKED);
        FriendshipDto acceptedFriendship = friendshipService.updateFriendship(friendshipDto);

        assertThat(acceptedFriendship.getStatus()).isEqualTo(BLOCKED);
    }

    @Test
    void testUpdateFriendshipFromAcceptedToAccepted() {
        Friendship friendshipAccepted = friendshipRequested.setStatus(ACCEPTED);
        when(friendshipRepository.findByIdRequesterIdAndIdAddresseeId(requester.getId(), addressee.getId()))
                .thenReturn(Optional.ofNullable(friendshipAccepted));

        FriendshipDto friendshipDto = modelMapper.map(friendshipAccepted, FriendshipDto.class);
        friendshipDto.setStatus(ACCEPTED);

        assertThatExceptionOfType(CWMException.InvalidStatusChangeException.class)
                .isThrownBy(() -> friendshipService.updateFriendship(friendshipDto))
                .withMessage("Invalid status change attempted for friendship with id=" + friendshipId);
    }

    static Stream<Arguments> invalidStatusChangeFromAcceptedProvider() {
        return Stream.of(
                Arguments.of(REQUESTED),
                Arguments.of(DECLINED)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidStatusChangeFromAcceptedProvider")
    void testUpdateFriendshipFromAcceptedStatusToInvalidStatus(FriendshipStatus newStatus) {
        Friendship friendshipAccepted = friendshipRequested.setStatus(ACCEPTED);
        when(friendshipRepository.findByIdRequesterIdAndIdAddresseeId(requester.getId(), addressee.getId()))
                .thenReturn(Optional.ofNullable(friendshipAccepted));

        FriendshipDto friendshipDto = modelMapper.map(friendshipAccepted, FriendshipDto.class);
        friendshipDto.setStatus(newStatus);

        assertThatExceptionOfType(CWMException.InvalidStatusChangeException.class)
                .isThrownBy(() -> friendshipService.updateFriendship(friendshipDto))
                .withMessage("Invalid status change attempted for friendship with id=" + friendshipId);
    }

    @Test
    void testUpdateFriendshipWhenNotFound() {
        when(friendshipRepository.findByIdRequesterIdAndIdAddresseeId(requester.getId(), addressee.getId()))
                .thenReturn(Optional.empty());

        FriendshipDto friendshipDto = modelMapper.map(friendshipRequested, FriendshipDto.class);

        assertThatExceptionOfType(CWMException.EntityNotFoundException.class)
                .isThrownBy(() -> friendshipService.updateFriendship(friendshipDto))
                .withMessage("Requested friendship with id=" +
                                     friendshipId +
                                     " does not exist");
    }

    @Test
    void testFindAllFriendsOfWhenUserNotFound() {
        when(userService.findUserById(requester.getId()))
                .thenThrow(CWMException.EntityNotFoundException.class);

        assertThatExceptionOfType(CWMException.EntityNotFoundException.class)
                .isThrownBy(() -> friendshipService.findFriendsOf(modelMapper.map(requester, UserDto.class)));
    }

    @Test
    void testFindAllFriendsOfWhenUserHasFriendships() {
        when(userService.findUserById(requester.getId()))
                .thenReturn(requester);

        User newFriendAccepted = User.builder()
                .id(100L)
                .build();
        User newFriendRequested = User.builder()
                .id(101L)
                .build();

        List<Friendship> friends = List.of(
                friendshipRequested.setStatus(ACCEPTED),
                Friendship.builder()
                        .id(FriendshipId.builder()
                                    .requester(newFriendAccepted)
                                    .addressee(requester)
                                    .build()
                        )
                        .requester(newFriendAccepted)
                        .addressee(requester)
                        .status(ACCEPTED)
                        .build(),
                Friendship.builder()
                        .id(FriendshipId.builder()
                                    .requester(newFriendRequested)
                                    .addressee(requester)
                                    .build()
                        )
                        .requester(newFriendRequested)
                        .addressee(requester)
                        .status(REQUESTED)
                        .build()
        );

        requester.setFriends(friends);

        List<UserDto> friendsFound = friendshipService.findFriendsOf(modelMapper.map(requester, UserDto.class));
        UserDto firstFriendDto = modelMapper.map(friendshipRequested.getAddressee(), UserDto.class);

        assertThat(friendsFound).hasSize(2);
        assertThat(friendsFound).contains(firstFriendDto);
    }

}