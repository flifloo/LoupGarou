package fr.leomelki.loupgarou.events;

import fr.leomelki.loupgarou.classes.LGGame;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;

public class LGPreDayStartEvent extends LGEvent implements Cancellable {
    @Getter
    @Setter
    private boolean cancelled;

    public LGPreDayStartEvent(LGGame game) {
        super(game);
    }
}