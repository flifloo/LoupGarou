package fr.leomelki.loupgarou.events;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGVote;
import lombok.Getter;

import java.util.ArrayList;

public class LGVoteLeaderChange extends LGEvent {

    @Getter
    final ArrayList<LGPlayer> latest;
    @Getter
    final ArrayList<LGPlayer> now;
    @Getter
    final LGVote vote;
    public LGVoteLeaderChange(LGGame game, LGVote vote, ArrayList<LGPlayer> latest, ArrayList<LGPlayer> now) {
        super(game);
        this.latest = latest;
        this.now = now;
        this.vote = vote;
    }

}
