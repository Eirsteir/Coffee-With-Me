package com.eirsteir.coffeewithme.domain.friendship;

import com.eirsteir.coffeewithme.domain.user.User;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Entity
@AssociationOverrides({
        @AssociationOverride(name ="id.requester", joinColumns = @JoinColumn(name ="requester_id")),
        @AssociationOverride(name ="id.addressee", joinColumns = @JoinColumn(name ="addressee_id"))
})
public class Friendship {

    @EmbeddedId
    private FriendshipId id = new FriendshipId();

    @Transient
    private User requester;

    @Transient
    private User addressee;

    private FriendshipStatus status;

    @CreationTimestamp
    private Date createdDateTime;

    @UpdateTimestamp
    private Date updatedDateTime;

}


