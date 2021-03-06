package fr.leomelki.loupgarou.classes;

import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.EnumWrappers.TitleAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerChat;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerPlayerInfo;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerScoreboardTeam;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerTitle;
import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.chat.LGChat;
import fr.leomelki.loupgarou.classes.chat.LGChat.LGChatCallback;
import fr.leomelki.loupgarou.classes.chat.LGNoChat;
import fr.leomelki.loupgarou.roles.Role;
import fr.leomelki.loupgarou.roles.RoleType;
import fr.leomelki.loupgarou.roles.RoleWinType;
import fr.leomelki.loupgarou.scoreboard.CustomScoreboard;
import fr.leomelki.loupgarou.utils.VariableCache;
import fr.leomelki.loupgarou.utils.VariousUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_15_R1.DimensionManager;
import net.minecraft.server.v1_15_R1.EnumGamemode;
import net.minecraft.server.v1_15_R1.PacketPlayOutRespawn;
import net.minecraft.server.v1_15_R1.WorldType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LGPlayer {
    private static final HashMap<Player, LGPlayer> cachedPlayers = new HashMap<>();
    private final MainLg plugin;
    @Getter
    private final VariableCache cache = new VariableCache();
    public boolean canSelectDead;
    @Getter
    boolean muted;
    long lastChoose;
    @Getter
    @Setter
    private int place;
    @Getter
    private Player player;
    @Getter
    @Setter
    private boolean dead;
    @Setter
    @Getter
    private Role role;
    private LGChooseCallback chooseCallback;
    private List<LGPlayer> blacklistedChoice = new ArrayList<>(0);
    @Getter
    @Setter
    private LGGame game;
    @Getter
    @Setter
    private String latestObjective;
    @Getter
    private CustomScoreboard scoreboard;
    private String name;
    @Getter
    private LGChat chat;

    public LGPlayer(MainLg mainLg, Player player) {
        this.plugin = mainLg;
        this.player = player;
    }

    public LGPlayer(MainLg mainLg, String name) {
        this.plugin = mainLg;
        this.name = name;
    }

    public static LGPlayer thePlayer(MainLg plugin, Player player) {
        LGPlayer lgp = cachedPlayers.get(player);
        if (lgp == null) {
            lgp = new LGPlayer(plugin, player);
            cachedPlayers.put(player, lgp);
        }
        return lgp;
    }

    public static LGPlayer removePlayer(Player player) {
        return cachedPlayers.remove(player);//.remove();
    }

    public void setScoreboard(CustomScoreboard scoreboard) {
        if (player != null) {
            if (this.scoreboard != null)
                this.scoreboard.hide();

            this.scoreboard = scoreboard;

            if (scoreboard != null)
                scoreboard.show();
        }
    }

    public void sendActionBarMessage(String msg) {
        if (this.player != null) {
            WrapperPlayServerChat chat = new WrapperPlayServerChat();
            chat.setPosition((byte) 2);
            chat.setMessage(WrappedChatComponent.fromText(msg));
            chat.sendPacket(getPlayer());
        }
    }

    public void sendMessage(String msg) {
        if (this.player != null)
            getPlayer().sendMessage(MainLg.getPrefix() + msg);
    }

    public void sendTitle(String title, String subTitle, int stay) {
        if (this.player != null) {
            WrapperPlayServerTitle titlePacket = new WrapperPlayServerTitle();
            titlePacket.setAction(TitleAction.TIMES);
            titlePacket.setFadeIn(10);
            titlePacket.setStay(stay);
            titlePacket.setFadeOut(10);
            titlePacket.sendPacket(player);

            titlePacket = new WrapperPlayServerTitle();
            titlePacket.setAction(TitleAction.TITLE);
            titlePacket.setTitle(WrappedChatComponent.fromText(title));
            titlePacket.sendPacket(player);

            titlePacket = new WrapperPlayServerTitle();
            titlePacket.setAction(TitleAction.SUBTITLE);
            titlePacket.setTitle(WrappedChatComponent.fromText(subTitle));
            titlePacket.sendPacket(player);
        }
    }

    public void remove() {
        this.player = null;
    }

    public String getName() {
        return player != null ? getPlayer().getName() : name;
    }

    public boolean join(LGGame game) {
        if (getPlayer().getGameMode() == GameMode.SPECTATOR) {
            sendMessage("§cÉtant en mode spectateur, vous ne rejoignez pas la partie !");
            return false;
        }
        if (game.tryToJoin(this)) {
            //To update the skin
            updateOwnSkin();
            getPlayer().setWalkSpeed(0.2f);
            //		sendMessage("§2Vous venez de rejoindre une partie de Loup-Garou. §aBon jeu!");
            return true;
        }
        return false;
    }

    public void choose(LGChooseCallback callback, LGPlayer... blacklisted) {
        this.blacklistedChoice = blacklisted == null ? new ArrayList<>(0) : Arrays.asList(blacklisted);
        this.chooseCallback = callback;
        //sendMessage("§7§oTIP: Regardez un joueur et tapez le afin de le sélectionner.");
    }

    public void stopChoosing() {
        this.blacklistedChoice = null;
        this.chooseCallback = null;
    }

    public void showView() {
        if (getGame() != null && player != null)
            for (LGPlayer lgp : getGame().getAlive())
                if (!lgp.isDead()) {
                    if (lgp != this && lgp.getPlayer() != null)
                        getPlayer().showPlayer(plugin, lgp.getPlayer());
                    else {
                        WrapperPlayServerScoreboardTeam team = new WrapperPlayServerScoreboardTeam();
                        team.setMode(2);
                        team.setName(lgp.getName());
                        team.setPrefix(WrappedChatComponent.fromText(""));
                        team.setPlayers(Collections.singletonList(lgp.getName()));
                        team.sendPacket(getPlayer());

                        WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
                        ArrayList<PlayerInfoData> infos = new ArrayList<>();
                        info.setAction(PlayerInfoAction.ADD_PLAYER);
                        infos.add(new PlayerInfoData(new WrappedGameProfile(getPlayer().getUniqueId(), getName()), 0, NativeGameMode.ADVENTURE, WrappedChatComponent.fromText(getName())));
                        info.setData(infos);
                        info.sendPacket(getPlayer());
                    }
                }

        getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
        getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 2, false, false));
    }

    //TODO Update prefix for only one guy
    public void updatePrefix() {
        if (getGame() != null && !isDead() && player != null) {
            List<String> meList = Collections.singletonList(getName());
            for (LGPlayer lgp : getGame().getInGame()) {
                WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
                ArrayList<PlayerInfoData> infos = new ArrayList<>();
                info.setAction(PlayerInfoAction.ADD_PLAYER);
                infos.add(new PlayerInfoData(new WrappedGameProfile(getPlayer().getUniqueId(), getName()), 0, NativeGameMode.ADVENTURE, WrappedChatComponent.fromText(getName())));
                info.setData(infos);
                info.sendPacket(lgp.getPlayer());

                WrapperPlayServerScoreboardTeam team = new WrapperPlayServerScoreboardTeam();
                team.setMode(2);
                team.setName(getName());
                team.setPrefix(WrappedChatComponent.fromText(""));
                team.setPlayers(meList);
                team.sendPacket(lgp.getPlayer());
            }
        }
    }

    public void hideView() {
        if (getGame() != null && player != null) {
            WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
            ArrayList<PlayerInfoData> infos = new ArrayList<>();
            info.setAction(PlayerInfoAction.ADD_PLAYER);
            for (LGPlayer lgp : getGame().getAlive())
                if (lgp != this && lgp.getPlayer() != null) {
                    if (!lgp.isDead())
                        infos.add(new PlayerInfoData(new WrappedGameProfile(lgp.getPlayer().getUniqueId(), lgp.getName()), 0, NativeGameMode.ADVENTURE, WrappedChatComponent.fromText(lgp.getName())));
                    getPlayer().hidePlayer(plugin, lgp.getPlayer());
                }
            info.setData(infos);
            info.sendPacket(getPlayer());
        }

        getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
        getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 999999, 1, false, false));
    }

    public void updateSkin() {
        if (getGame() != null && player != null) {
            for (LGPlayer lgp : getGame().getInGame()) {
                if (lgp == this) {
                    WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo();
                    ArrayList<PlayerInfoData> infos = new ArrayList<>();
                    info.setAction(PlayerInfoAction.ADD_PLAYER);
                    infos.add(new PlayerInfoData(new WrappedGameProfile(getPlayer().getUniqueId(), getName()), 0, NativeGameMode.ADVENTURE, WrappedChatComponent.fromText(getName())));
                    info.setData(infos);
                    info.sendPacket(getPlayer());
                } else if (!isDead() && lgp.getPlayer() != null) {
                    lgp.getPlayer().hidePlayer(plugin, getPlayer());
                    lgp.getPlayer().showPlayer(plugin, getPlayer());
                }
            }
        }
    }

    public void updateOwnSkin() {
        if (player != null) {
            //On change son skin avec un packet de PlayerInfo (dans le tab)
            WrapperPlayServerPlayerInfo infos = new WrapperPlayServerPlayerInfo();
            infos.setAction(PlayerInfoAction.ADD_PLAYER);
            WrappedGameProfile gameProfile = new WrappedGameProfile(getPlayer().getUniqueId(), getPlayer().getName());
            infos.setData(Collections.singletonList(new PlayerInfoData(gameProfile, 10, NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(getPlayer().getName()))));
            infos.sendPacket(getPlayer());
            //Pour qu'il voit son skin changer (sa main et en f5), on lui dit qu'il respawn (alors qu'il n'est pas mort mais ça marche quand même mdr)
            PacketPlayOutRespawn respawn = new PacketPlayOutRespawn(DimensionManager.OVERWORLD, 0, WorldType.NORMAL, EnumGamemode.ADVENTURE);
            ((CraftPlayer) getPlayer()).getHandle().playerConnection.sendPacket(respawn);
            //Enfin, on le téléporte à sa potion actuelle car sinon il se verra dans le vide
            getPlayer().teleport(getPlayer().getLocation());
            float speed = getPlayer().getWalkSpeed();
            getPlayer().setWalkSpeed(0.2f);
            new BukkitRunnable() {

                @Override
                public void run() {
                    getPlayer().updateInventory();
                    getPlayer().setWalkSpeed(speed);
                }
            }.runTaskLater(MainLg.getInstance(), 5);
            //Et c'est bon, le joueur se voit avec un nouveau skin avec quasiment aucun problème visible à l'écran :D
        }
    }

    public LGPlayer getPlayerOnCursor(List<LGPlayer> list) {
        Location loc = getPlayer().getLocation();
        if (loc.getPitch() > 60)
            if (blacklistedChoice.contains(this))
                return null;
            else
                return this;
        for (int i = 0; i < 50; i++) {
            loc.add(loc.getDirection());
            for (LGPlayer player : list) {
                if (player != this && !blacklistedChoice.contains(player) && (!player.isDead() || canSelectDead) && VariousUtils.distanceSquaredXZ(loc, player.getPlayer().getLocation()) < 0.35 && Math.abs(loc.getY() - player.getPlayer().getLocation().getY()) < 2) {
                    return player;
                }
            }
        }
        return null;
    }

    public RoleType getRoleType() {
        return this.getCache().getBoolean("vampire") ? RoleType.VAMPIRE : this.getCache().getBoolean("infected") ? RoleType.LOUP_GAROU : getRole().getType(this);
    }

    public RoleWinType getRoleWinType() {
        return this.getCache().getBoolean("vampire") ? RoleWinType.VAMPIRE : this.getCache().getBoolean("infected") ? RoleWinType.LOUP_GAROU : getRole().getWinType(this);
    }

    public boolean isRoleActive() {
        return !this.getCache().getBoolean("vampire");
    }

    public void die() {
        setMuted();
    }

    private void setMuted() {
        if (player != null)
            for (LGPlayer lgp : getGame().getInGame())
                if (lgp != this && lgp.getPlayer() != null)
                    lgp.getPlayer().hidePlayer(plugin, getPlayer());
        muted = true;
    }

    public void resetMuted() {
        muted = false;
    }

    public void joinChat(LGChat chat, LGChatCallback callback) {
        joinChat(chat, callback, false);
    }

    public void joinChat(LGChat chat) {
        joinChat(chat, null, false);
    }

    public void joinChat(LGChat chat, boolean muted) {
        joinChat(chat, null, muted);
    }

    public void joinChat(LGChat chat, LGChatCallback callback, boolean muted) {
        if (this.chat != null && !muted)
            this.chat.leave(this);

        if (!muted)
            this.chat = chat;

        if (chat != null && player != null)
            chat.join(this, callback == null ? chat.getDefaultCallback() : callback);
    }


    public void leaveChat() {
        joinChat(new LGNoChat(), null);
    }

    public void onChat(String message) {
        if (chat != null) {
            chat.sendMessage(this, message);
        }
    }


    public void playAudio(LGSound sound, double volume) {
        if (player != null)
            getPlayer().playSound(getPlayer().getLocation(), sound.getSound(), (float) volume, 1);
    }

    public void stopAudio(LGSound sound) {
        if (player != null)
            getPlayer().stopSound(sound.getSound());
    }

    public void chooseAction() {
        long now = System.currentTimeMillis();
        if (lastChoose + 200 < now) {
            if (chooseCallback != null)
                chooseCallback.callback(getPlayerOnCursor(getGame().getInGame()));
            lastChoose = now;
        }
    }

    @Override
    public String toString() {
        return super.toString() + " (" + getName() + ")";
    }

    public interface LGChooseCallback {
        void callback(LGPlayer choosen);
    }
}
