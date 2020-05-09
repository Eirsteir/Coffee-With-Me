package com.eirsteir.coffeewithme.service;

import com.eirsteir.coffeewithme.domain.FriendshipId;
import com.eirsteir.coffeewithme.domain.friendship.Friendship;
import com.eirsteir.coffeewithme.domain.friendship.FriendshipStatus;
import com.eirsteir.coffeewithme.domain.user.User;
import com.eirsteir.coffeewithme.dto.FriendshipDto;
import com.eirsteir.coffeewithme.dto.UserDto;
import com.eirsteir.coffeewithme.exception.CWMException;
import com.eirsteir.coffeewithme.repository.FriendshipRepository;
import com.eirsteir.coffeewithme.web.request.FriendshipRequest;
import config.CWMExceptionTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.eirsteir.coffeewithme.domain.friendship.FriendshipStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import(CWMExceptionTestConfig.class)
@TestPropertySource("classpath:exception.properties")
@RunWith(SpringRunner.class)
class FriendshipServiceImplTest {

    private static final String REQUESTER_USERNAME = "requester";
    private static final String ADDRESSEE_USERNAME = "addressee";

    private Friendship friendship;
    private FriendshipId friendshipId;
    private User requester;
    private User addressee;
    private FriendshipRequest friendshipRequest;

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

        friendship = Friendship.builder()
               .id(friendshipId)
               .status(REQUESTED)
               .build();

        friendshipRequest = FriendshipRequest.builder()
                .requesterId(requester.getId())
                .addresseeId(addressee.getId())
                .build();

        when(friendshipRepository.save(Mockito.any(Friendship.class)))
                .thenReturn(friendship);
        when(userService.findUserById(requester.getId()))
                .thenReturn(modelMapper.map(requester, UserDto.class));
        when(userService.findUserById(addressee.getId()))
                .thenReturn(modelMapper.map(addressee, UserDto.class));
    }

    @Test
    void testRegisterFriendship() {
        FriendshipDto savedFriendshipDto = friendshipService.registerFriendship(friendshipRequest);

        assertThat(savedFriendshipDto.getId().getRequester().getId()).isEqualTo(requester.getId());
        assertThat(savedFriendshipDto.getId().getAddressee().getId()).isEqualTo(addressee.getId());
        assertThat(savedFriendshipDto.getStatus()).isEqualTo(REQUESTED);
    }

    @Test
    void testRegisterFriendshipWhenDuplicate() {
        when(friendshipRepository.existsById(Mockito.any(FriendshipId.class)))
                .thenReturn(true);

        assertThatExceptionOfType(CWMException.DuplicateEntityException.class)
                .isThrownBy(() -> friendshipService.registerFriendship(friendshipRequest))
                .withMessage("Requested friendship with id=" + friendshipId +" already exists");
    }

    @Test
    void testRegisterFriendshipWhenFriendshipIdReversedIsDuplicate() {
        when(friendshipRepository.existsById(Mockito.any(FriendshipId.class)))
                .thenReturn(true);

        friendshipId = FriendshipId.builder()
                .requester(addressee)
                .addressee(requester)
                .build();

        assertThatExceptionOfType(CWMException.DuplicateEntityException.class)
                .isThrownBy(() -> friendshipService.registerFriendship(friendshipRequest))
                .withMessage("Requested friendship with id=" + friendshipId +" already exists");
    }

    @Test
    void testRemoveFriendshipWhenNotFound() {
        when(friendshipRepository.existsById(Mockito.any(FriendshipId.class)))
                .thenReturn(true);

        FriendshipDto friendshipDto = modelMapper.map(friendship, FriendshipDto.class);

        assertThatExceptionOfType(CWMException.EntityNotFoundException.class)
                .isThrownBy(() -> friendshipService.removeFriendship(friendshipDto))
                .withMessage("Requested friendship with id=" + friendshipId + " does not exist");
    }

    @Test
    void testAcceptFriendship() {
        when(friendshipRepository.findById(Mockito.any(FriendshipId.class)))
                .thenReturn(Optional.ofNullable(friendship));

        FriendshipDto friendshipDto = modelMapper.map(friendship, FriendshipDto.class);
        FriendshipDto acceptedFriendship = friendshipService.acceptFriendship(friendshipDto);

        assertThat(acceptedFriendship.getStatus()).isEqualTo(ACCEPTED);
    }

    @Test
    void testAcceptFriendshipWhenStatusIsNotRequested() {
        when(friendshipRepository.existsById(Mockito.any(FriendshipId.class)))
                .thenReturn(true);

        friendship.setStatus(BLOCKED);
        FriendshipDto friendshipDto = modelMapper.map(friendship, FriendshipDto.class);

        assertThatExceptionOfType(CWMException.InvalidStatusChangeException.class)
                .isThrownBy(() -> friendshipService.acceptFriendship(friendshipDto))
                .withMessage("Invalid status change attempted for friendship with id=" + friendshipId);
    }

    @Test
    void testAcceptFriendshipWhenNotFound() {
        when(friendshipRepository.existsById(Mockito.any(FriendshipId.class)))
                .thenReturn(true);

        friendship.setStatus(REQUESTED);
        FriendshipDto friendshipDto = modelMapper.map(friendship, FriendshipDto.class);

        assertThatExceptionOfType(CWMException.EntityNotFoundException.class)
                .isThrownBy(() -> friendshipService.acceptFriendship(friendshipDto))
                .withMessage("Requested friendship with id=" + friendshipId + " does not exist");
    }

    @Test
    void testFindAllFriendsOfWhenUserWithFriendsFound() {
        when(friendshipRepository.findByIdRequesterOrIdAddresseeAndStatus(Mockito.any(User.class),
                                                                          Mockito.any(User.class),
                                                                          Mockito.any(FriendshipStatus.class)))
                .thenReturn(Arrays.asList(Friendship.builder().build(),
                                          Friendship.builder().build()));
        List<UserDto> friendsOf = friendshipService.findFriendsOf(modelMapper.map(requester, UserDto.class));

        assertThat(friendsOf).hasSize(2);
    }

    @Test
    void testFindAllFriendsOfWhenUserNotFound() {
        when(userService.findUserById(requester.getId()))
                .thenThrow(CWMException.EntityNotFoundException.class);

        assertThatExceptionOfType(CWMException.EntityNotFoundException.class)
                .isThrownBy(() -> friendshipService.findFriendsOf(modelMapper.map(requester, UserDto.class)));
    }

}