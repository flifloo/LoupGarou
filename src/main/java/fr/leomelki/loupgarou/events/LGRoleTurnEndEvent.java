package fr.leomelki.loupgarou.events;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.roles.Role;
import lombok.Getter;

public class LGRoleTurnEndEvent extends LGEvent {
    @Getter
    private final Role newRole, previousRole;

    public LGRoleTurnEndEvent(LGGame game, Role newRole, Role previousRole) {
        super(game);
        this.newRole = newRole;
        this.previousRole = previousRole;
    }
}