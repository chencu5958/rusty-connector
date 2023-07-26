package group.aelysium.rustyconnector.plugin.velocity.lib.friends;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.proxy.Player;
import group.aelysium.rustyconnector.core.lib.serviceable.ServiceableService;
import group.aelysium.rustyconnector.plugin.velocity.central.VelocityAPI;
import group.aelysium.rustyconnector.plugin.velocity.lib.friends.commands.CommandFM;
import group.aelysium.rustyconnector.plugin.velocity.lib.friends.commands.CommandFriends;
import group.aelysium.rustyconnector.plugin.velocity.lib.friends.commands.CommandUnFriend;
import group.aelysium.rustyconnector.plugin.velocity.lib.lang_messaging.VelocityLang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

public class FriendsService extends ServiceableService<FriendsServiceHandler> {
    private final Vector<FriendRequest> friendRequests = new Vector<>();
    private final FriendsSettings settings;

    public FriendsService(FriendsSettings settings, FriendsMySQLService friendsMySQLService) {
        super(new FriendsServiceHandler());
        this.services().add(new FriendsDataEnclaveService(friendsMySQLService));
        this.settings = settings;
    }

    public void initCommand() {
        CommandManager commandManager = VelocityAPI.get().getServer().getCommandManager();
        if(!commandManager.hasCommand("friends"))
            try {
                commandManager.register(
                        commandManager.metaBuilder("friends").aliases("/friends").build(),
                        CommandFriends.create()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        if(!commandManager.hasCommand("unfriend"))
            try {
                commandManager.register(
                        commandManager.metaBuilder("unfriend").aliases("/unfriend").build(),
                        CommandUnFriend.create()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        if(!commandManager.hasCommand("fm"))
            try {
                commandManager.register(
                        commandManager.metaBuilder("fm").aliases("/fm").build(),
                        CommandFM.create()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public FriendsSettings getSettings() {
        return this.settings;
    }

    public List<FriendRequest> findRequestsToTarget(Player target) {
        return this.friendRequests.stream().filter(request -> request.getTarget() == target).findAny().stream().toList();
    }
    public Optional<FriendRequest> findRequest(Player target, Player sender) {
        return this.friendRequests.stream().filter(invite -> invite.getTarget().equals(target) && invite.getSender().equals(sender)).findFirst();
    }

    public Optional<List<Player>> findFriends(Player player, boolean forcePull) {
        List<Player> friends = new ArrayList<>();
        List<FriendMapping> friendMappings = this.services.dataEnclave().findFriends(player, forcePull).orElse(null);
        if(friendMappings == null) return Optional.empty();

        friendMappings.forEach(mapping -> {
            try {
                friends.add(mapping.getFriendOf(player));
            } catch (NullPointerException ignore) {}
        });

        return Optional.of(friends);
    }

    public boolean areFriends(Player player1, Player player2) {
        return this.services.dataEnclave().areFriends(player1, player2);
    }

    public FriendMapping sendRequest(Player sender, Player target) {
        if(this.getFriendCount(sender).orElseThrow() > this.getSettings().maxFriends())
            sender.sendMessage(Component.text("You have reached the max number of friends!", NamedTextColor.RED));

        FriendRequest friendRequest = new FriendRequest(sender, target);
        this.friendRequests.add(friendRequest);

        sender.sendMessage(Component.text("Friend request sent to " + target.getUsername(), NamedTextColor.GREEN));

        target.sendMessage(VelocityLang.FRIEND_REQUEST.build(sender));
        return new FriendMapping(sender, target);
    }

    public boolean removeFriend(Player sender, Player target) {
        try {
            this.services().dataEnclave().removeFriend(sender, target);
            return true;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception ignore) {}
        return false;
    }

    public void closeInvite(FriendRequest request) {
        this.friendRequests.remove(request);
        request.decompose();
    }

    public Optional<Integer> getFriendCount(Player player) {
        return this.services().dataEnclave().getFriendCount(player);
    }

    @Override
    public void kill() {
        this.friendRequests.clear();
        super.kill();
    }

    public record FriendsSettings(
            int maxFriends,
            boolean sendNotifications,
            boolean showFamilies,
            boolean allowMessaging
    ) {}
}
