package com.eirsteir.coffeewithme.social.service.friendship;

import com.eirsteir.coffeewithme.commons.domain.user.UserDetails;
import com.eirsteir.coffeewithme.commons.dto.UserDetailsDto;
import com.eirsteir.coffeewithme.commons.exception.CWMException;
import com.eirsteir.coffeewithme.commons.exception.EntityType;
import com.eirsteir.coffeewithme.commons.exception.ExceptionType;
import com.eirsteir.coffeewithme.social.domain.friendship.Friendship;
import com.eirsteir.coffeewithme.social.domain.friendship.FriendshipId;
import com.eirsteir.coffeewithme.social.domain.friendship.FriendshipStatus;
import com.eirsteir.coffeewithme.social.domain.user.User;
import com.eirsteir.coffeewithme.social.dto.FriendshipDto;
import com.eirsteir.coffeewithme.social.repository.FriendshipRepository;
import com.eirsteir.coffeewithme.social.service.user.UserService;
import com.eirsteir.coffeewithme.social.util.UserServiceUtils;
import com.eirsteir.coffeewithme.social.web.request.FriendRequest;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.events.publisher.ResultWithEvents;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
public class FriendshipServiceImpl implements FriendshipService {

  private DomainEventPublisher domainEventPublisher;

  private UserService userService;

  private FriendshipRepository friendshipRepository;

  private ModelMapper modelMapper;

  @Autowired
  public FriendshipServiceImpl(
      DomainEventPublisher domainEventPublisher,
      FriendshipRepository friendshipRepository,
      UserService userService,
      ModelMapper modelMapper) {
    this.domainEventPublisher = domainEventPublisher;
    this.friendshipRepository = friendshipRepository;
    this.userService = userService;
    this.modelMapper = modelMapper;
  }

  @Override
  public List<FriendshipDto> findFriendshipsOf(UserDetailsDto UserDetailsDto) {
    return findAllFriendshipsWithStatus(UserDetailsDto, FriendshipStatus.ACCEPTED);
  }

  @Override
  public List<FriendshipDto> findAllFriendshipsWithStatus(
      UserDetailsDto userDetailsDto, FriendshipStatus status) {
    List<Friendship> friendships =
        friendshipRepository.findByUserAndStatus(userDetailsDto.getId(), status);

    return getFriendshipDtos(friendships);
  }

  @Override
  public List<FriendshipDto> findFriendshipsOf(Long id, FriendshipStatus status) {
    List<Friendship> friendships = friendshipRepository.findByUserAndStatus(id, status);

    return getFriendshipDtos(friendships);
  }

  private List<FriendshipDto> getFriendshipDtos(List<Friendship> friendships) {
    return friendships.stream()
        .map(friendship -> modelMapper.map(friendship, FriendshipDto.class))
        .collect(Collectors.toList());
  }

  @Override
  public Integer getFriendsCount(Long userId) {
    return friendshipRepository.countByUserId(userId);
  }

  @Override
  public List<FriendshipDto> findFriendshipsAtUniversity(User user) {
    List<Friendship> friendships =
        friendshipRepository.findByUserAndStatusAndUniversity(
            user.getId(), FriendshipStatus.ACCEPTED, user.getUniversity());
    return getFriendshipDtos(friendships);
  }

  @Override
  public FriendshipDto registerFriendship(FriendRequest friendRequest) {
    User requester = userService.findUserById(friendRequest.getRequesterId());
    User addressee = userService.findUserById(friendRequest.getAddresseeId());
    FriendshipId id = FriendshipId.builder().requester(requester).addressee(addressee).build();

    if (friendshipExists(id))
      throw CWMException.getException(
          EntityType.FRIENDSHIP,
          ExceptionType.DUPLICATE_ENTITY,
          friendRequest.getRequesterId().toString(),
          friendRequest.getAddresseeId().toString());

    Friendship friendship = requester.addFriend(addressee, FriendshipStatus.REQUESTED);
    log.info("[x] Registered friendship: {}", friendship);

    UserDetails user = UserServiceUtils.getUserDetailsFrom(friendship.getRequester());
    publish(Friendship.createFriendRequest(friendship, user));

    return modelMapper.map(friendship, FriendshipDto.class);
  }

  @Override
  public boolean friendshipExists(FriendshipId friendshipId) {
    return friendshipRepository.existsById(friendshipId);
  }

  @Override
  public void removeFriendship(FriendshipDto friendshipDto) {
    Friendship friendship =
        friendshipRepository
            .findByIdRequesterIdAndIdAddresseeId(
                friendshipDto.getRequester().getId(), friendshipDto.getAddressee().getId())
            .orElseThrow(
                () ->
                    CWMException.getException(
                        EntityType.FRIENDSHIP,
                        ExceptionType.ENTITY_NOT_FOUND,
                        friendshipDto.getRequester().getId().toString(),
                        friendshipDto.getAddressee().getId().toString()));

    friendshipRepository.delete(friendship);
    log.info("[x] Removed friendship: {}", friendshipDto);
  }

  @Override
  public FriendshipDto updateFriendship(FriendshipDto friendshipDto) {
    Friendship friendshipToUpdate = findFriendship(friendshipDto);

    if (isValidStatusChange(friendshipToUpdate.getStatus(), friendshipDto.getStatus()))
      return updateFriendship(friendshipDto, friendshipToUpdate);

    throw CWMException.getException(
        EntityType.FRIENDSHIP,
        ExceptionType.INVALID_STATUS_CHANGE,
        friendshipDto.getRequester().getId().toString(),
        friendshipDto.getAddressee().getId().toString());
  }

  private Friendship findFriendship(FriendshipDto friendshipDto) {
    Long requesterId = friendshipDto.getRequester().getId();
    Long addresseeId = friendshipDto.getAddressee().getId();

    return friendshipRepository
        .findByIdRequesterIdAndIdAddresseeId(requesterId, addresseeId)
        .orElseThrow(
            () ->
                CWMException.getException(
                    EntityType.FRIENDSHIP,
                    ExceptionType.ENTITY_NOT_FOUND,
                    friendshipDto.getRequester().getId().toString(),
                    friendshipDto.getAddressee().getId().toString()));
  }

  private boolean isValidStatusChange(FriendshipStatus oldStatus, FriendshipStatus newStatus) {
    if (oldStatus == FriendshipStatus.REQUESTED) return true;

    return oldStatus == FriendshipStatus.ACCEPTED && newStatus == FriendshipStatus.BLOCKED;
  }

  private FriendshipDto updateFriendship(
      FriendshipDto friendshipDto, Friendship friendshipToUpdate) {
    friendshipToUpdate.setStatus(friendshipDto.getStatus());
    Friendship updatedFriendship = friendshipRepository.save(friendshipToUpdate);

    if (updatedFriendship.getStatus() == FriendshipStatus.ACCEPTED) {
      UserDetails addressee = UserServiceUtils.getUserDetailsFrom(updatedFriendship.getAddressee());
      ResultWithEvents<Friendship> friendshipWithEvents =
          Friendship.createFriendRequestAccepted(friendshipToUpdate, addressee);
      publish(friendshipWithEvents);
    }

    log.info("[x] Friendship was updated to {}: {}", friendshipDto.getStatus(), friendshipToUpdate);
    return modelMapper.map(updatedFriendship, FriendshipDto.class);
  }

  private void publish(ResultWithEvents<Friendship> friendshipWithEvents) {
    log.info("[x] Publishing {} to {}", friendshipWithEvents, Friendship.class);
    domainEventPublisher.publish(
        Friendship.class, friendshipWithEvents, friendshipWithEvents.events);
  }
}
