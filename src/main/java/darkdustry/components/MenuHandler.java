package darkdustry.components;

import arc.Events;
import arc.util.Strings;
import darkdustry.features.Ranks.Rank;
import darkdustry.utils.Find;
import mindustry.game.EventType.GameOverEvent;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.ui.Menus;

import static darkdustry.PluginVars.discordServerUrl;
import static darkdustry.PluginVars.linkWaiting;
import static darkdustry.components.Bundle.*;
import static darkdustry.components.MongoDB.getPlayerData;
import static darkdustry.components.MongoDB.setPlayerData;
import static darkdustry.discord.Bot.botChannel;
import static darkdustry.discord.Bot.botGuild;
import static darkdustry.utils.Utils.coloredTeam;
import static java.util.Objects.requireNonNull;
import static mindustry.Vars.state;

public class MenuHandler {

    public static int welcomeMenu, despawnMenu, artvMenu, statsMenu, rankInfoMenu, ranksRequirementsMenu, rankIncreaseMenu, linkMenu;

    public static void load() {
        welcomeMenu = Menus.registerMenu((player, option) -> {

            if (option == 1) {
                Call.openURI(player.con, discordServerUrl);
            } else if (option == 2) {
                getPlayerData(player.uuid()).subscribe(data -> {
                    data.welcomeMessage = false;
                    setPlayerData(data);
                });
                bundled(player, "welcome.disabled");
            }
        });

        despawnMenu = Menus.registerMenu((player, option) -> {
            if (!player.admin) return;

            switch (option) {
                case 0 -> {
                    Groups.unit.each(Call::unitEnvDeath);
                    bundled(player, "commands.despawn.success.all");
                }
                case 2 -> {
                    Groups.unit.each(Unit::isPlayer, Call::unitEnvDeath);
                    bundled(player, "commands.despawn.success.players");
                }
                case 3 -> {
                    state.rules.defaultTeam.data().units.each(Call::unitEnvDeath);
                    bundled(player, "commands.despawn.success.team", coloredTeam(state.rules.defaultTeam));
                }
                case 4 -> {
                    state.rules.waveTeam.data().units.each(Call::unitEnvDeath);
                    bundled(player, "commands.despawn.success.team", coloredTeam(state.rules.waveTeam));
                }
                case 5 -> {
                    Call.unitEnvDeath(player.unit());
                    bundled(player, "commands.despawn.success.suicide");
                }
            }
        });

        artvMenu = Menus.registerMenu((player, option) -> {
            if (!player.admin || option != 0) return;

            Events.fire(new GameOverEvent(state.rules.waveTeam));
            sendToChat("commands.artv.info", player.coloredName());
        });

        statsMenu = -1;

        rankInfoMenu = Menus.registerMenu((player, option) -> {
            if (option != 1) return;

            var builder = new StringBuilder();
            Rank.ranks.each(rank -> rank.req != null, rank -> builder.append(rank.localisedReq(Find.locale(player.locale))).append("\n"));
            showMenu(player, ranksRequirementsMenu, "commands.rank.menu.requirements.header", builder.toString(), new String[][]{{"ui.menus.close"}});
        });

        ranksRequirementsMenu = -1;

        rankIncreaseMenu = -1;

        linkMenu = Menus.registerMenu((player, options) -> {
            if (options == 1) {
                getPlayerData(player.uuid()).subscribe(data -> {
                    var id = linkWaiting.get(player.uuid());

                    if (id.isEmpty()) {
                        player.sendMessage("[scarlet]Произошла ошибка во время привязки аккаунта.[]");
                        return;
                    }

                    data.discord = id;
                    setPlayerData(data);

                    var member = botGuild.getMemberById(id);
                    botChannel.sendMessage(Strings.format("@ вы были привязаны к аккаунту @.", member.getAsMention(), player.name)).queue();
                });
            }
        });
    }

    public static void showMenu(Player player, int menu, String title, String content, String[][] buttons) {
        showMenu(player, menu, title, content, buttons, null);
    }

    public static void showMenu(Player player, int menu, String title, String content, String[][] buttons, Object titleObject, Object... contentObjects) {
        var locale = Find.locale(player.locale);
        for (int i = 0; i < buttons.length; i++)
            for (int j = 0; j < buttons[i].length; j++)
                buttons[i][j] = get(buttons[i][j], locale);
        Call.menu(player.con, menu, format(title, locale, titleObject), format(content, locale, contentObjects), buttons);
    }
}
