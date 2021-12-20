package pandorum.commands.client;

import arc.util.Strings;
import mindustry.content.UnitTypes;
import mindustry.game.Team;
import mindustry.gen.Player;
import mindustry.type.UnitType;
import pandorum.comp.Icons;

import static mindustry.Vars.content;
import static pandorum.Misc.*;

public class SpawnCommand {

    private static final int maxAmount = 25;

    public static void run(final String[] args, final Player player) {
        if (args.length > 1 && !Strings.canParseInt(args[1])) {
            bundled(player, "commands.non-int");
            return;
        }

        int count = args.length > 1 ? Strings.parseInt(args[1]) : 1;
        if (count > maxAmount || count < 1) {
            bundled(player, "commands.admin.spawn.limit", maxAmount);
            return;
        }

        Team team = args.length > 2 ? findTeam(args[2]) : player.team();
        if (team == null) {
            StringBuilder teams = new StringBuilder();
            for (Team t : Team.baseTeams) teams.append("\n[gold] - [white]").append(colorizedTeam(t));
            bundled(player, "commands.team-not-found", teams.toString());
            return;
        }

        UnitType type = findUnit(args[0]);
        if (type == null) {
            StringBuilder units = new StringBuilder();
            content.units().each(u -> u != UnitTypes.block, u -> units.append(" ").append(Icons.get(u.name)).append(u.name));
            bundled(player, "commands.unit-not-found", units.toString());
            return;
        }

        for (int i = 0; i < count; i++) type.spawn(team, player.x, player.y);
        bundled(player, "commands.admin.spawn.success", count, Icons.get(type.name), colorizedTeam(team));
    }
}
