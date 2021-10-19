package pandorum.commands.client;

import mindustry.gen.Player;
import pandorum.Misc;

import static mindustry.Vars.netServer;
import static pandorum.Misc.bundled;

public class UnbanCommand {
    public static void run(final String[] args, final Player player) {
        if (netServer.admins.unbanPlayerIP(args[0]) || netServer.admins.unbanPlayerID(args[0])) {
            bundled(player, "commands.admin.unban.success", netServer.admins.getInfo(args[0]).lastName);
            return;
        }
        bundled(player, "commands.admin.unban.not-banned");
    }
}
