package com.eirsteir.coffeewithme.commons.domain.friendship

import com.eirsteir.coffeewithme.commons.domain.notification.AbstractEntityNotificationEvent
import com.eirsteir.coffeewithme.commons.domain.user.UserDetails

class FriendRequestEvent(subjectId: Long, user: UserDetails)
    : AbstractEntityNotificationEvent(subjectId, user), FriendshipEvent