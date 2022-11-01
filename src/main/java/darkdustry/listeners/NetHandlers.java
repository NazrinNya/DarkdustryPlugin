package darkdustry.listeners;

import arc.Events;
import arc.util.CommandHandler.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.net.Administration.TraceInfo;
import mindustry.net.NetConnection;
import mindustry.net.Packets.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import useful.Bundle;

import static darkdustry.PluginVars.*;
import static darkdustry.discord.Bot.Palette.error;
import static darkdustry.discord.Bot.bansChannel;
import static darkdustry.utils.Administration.*;
import static darkdustry.utils.Checks.notAdmin;
import static darkdustry.utils.Utils.*;
import static mindustry.Vars.*;
import static useful.Bundle.sendToChat;

public class NetHandlers {

    public static String invalidResponse(Player player, CommandResponse response) {
        if (response.type == ResponseType.manyArguments)
            return Bundle.format("commands.unknown.many-arguments", player, response.command.text, response.command.paramText);
        if (response.type == ResponseType.fewArguments)
            return Bundle.format("commands.unknown.few-arguments", player, response.command.text, response.command.paramText);

        var closest = getAvailableCommands(player)
                .map(command -> command.text)
                .filter(command -> Strings.levenshtein(command, response.runCommand) < 3)
                .min(command -> Strings.levenshtein(command, response.runCommand));

        return closest != null ? Bundle.format("commands.unknown.closest", player, closest) : Bundle.format("commands.unknown", player);
    }

    public static void connect(NetConnection con, Connect packet) {
        Events.fire(new ConnectionEvent(con));
    }

    public static void connect(NetConnection con, ConnectPacket packet) {
        con.connectTime = Time.millis();

        Events.fire(new ConnectPacketEvent(con, packet));

        String uuid = con.uuid = packet.uuid,
                usid = con.usid = packet.usid,
                ip = con.address,
                locale = notNullElse(packet.locale, defaultLanguage),
                name = Reflect.invoke(netServer, "fixName", Structs.arr(packet.name), String.class);

        con.mobile = packet.mobile;
        con.modclient = packet.version == -1;

        if (con.hasBegunConnecting || Groups.player.contains(player -> player.uuid().equals(uuid) || player.usid().equals(usid))) {
            kick(con, "kick.already-connected", locale);
            return;
        }

        con.hasBegunConnecting = true;

        if (Groups.player.count(player -> player.ip().equals(ip)) >= maxIdenticalIPs) {
            kick(con, "kick.too-many-connections", locale);
            return;
        }

        if (netServer.admins.isIDBanned(uuid) || netServer.admins.isIPBanned(ip) || netServer.admins.isSubnetBanned(ip)) {
            kick(con, 0, true, "kick.banned", locale);
            return;
        }

        if (netServer.admins.getKickTime(uuid, ip) > Time.millis()) {
            kick(con, netServer.admins.getKickTime(uuid, ip) - Time.millis(), true, "kick.recent-kick", locale);
            return;
        }

        if (Strings.stripColors(name).trim().isEmpty()) {
            kick(con, "kick.name-is-empty", locale);
            return;
        }

        if (netServer.admins.getPlayerLimit() > 0 && Groups.player.size() >= netServer.admins.getPlayerLimit()) {
            kick(con, "kick.player-limit", locale, netServer.admins.getPlayerLimit());
            return;
        }

        var extraMods = packet.mods.copy();
        var missingMods = mods.getIncompatibility(extraMods);

        if (extraMods.any()) {
            kick(con, "kick.extra-mods", locale, extraMods.toString("\n> "));
            return;
        }

        if (missingMods.any()) {
            kick(con, "kick.missing-mods", locale, missingMods.toString("\n> "));
            return;
        }

        var info = netServer.admins.getInfo(uuid);
        if (!netServer.admins.isWhitelisted(uuid, usid)) {
            info.adminUsid = usid;
            info.names.addUnique(info.lastName = name);
            info.ips.addUnique(info.lastIP = ip);
            kick(con, "kick.not-whitelisted", locale);
            return;
        }

        if (packet.versionType == null || (packet.version == -1 && !netServer.admins.allowsCustomClients())) {
            kick(con, "kick.custom-client", locale);
            return;
        }

        if (packet.version != mindustryVersion && packet.version != -1 && mindustryVersion != -1 && !packet.versionType.equals("bleeding-edge")) {
            kick(con, packet.version > mindustryVersion ? "kick.server-outdated" : "kick.client-outdated", locale, packet.version, mindustryVersion);
            return;
        }

        // TODO вернуть
        //if (packet.versionType.equals(("bleeding-edge")) && !Version.type.equals("bleeding-edge")) {
        //    Call.infoMessage(con, format("events.join.bleeding-edge", Find.locale(locale), mindustryVersion));
        //}

        if (con.kicked) return;

        netServer.admins.updatePlayerJoined(uuid, ip, name);
        if (!info.admin) info.adminUsid = usid;

        var player = Player.create();
        player.con(con);
        player.name(name);
        player.locale(locale);
        player.admin(netServer.admins.isAdmin(uuid, usid));
        player.color.set(packet.color).a(1f);

        con.player = player;

        player.team(netServer.assignTeam(player));
        netServer.sendWorldData(player);

        Events.fire(new PlayerConnect(player));
    }

    public static void adminRequest(NetConnection con, AdminRequestCallPacket packet) {
        Player player = con.player, other = packet.other;
        var action = packet.action;

        if (notAdmin(player) || other == null || (other.admin && other != player)) return;

        Events.fire(new AdminRequestEvent(player, other, action));

        switch (action) {
            case kick -> kick(other, player.coloredName());
            case ban -> {
                ban(other, player.coloredName());

                var embed = new EmbedBuilder().setTitle("Бан")
                        .setColor(error)
                        .addField("Violator", other.plainName(), false)
                        .addField("Administrator", player.plainName(), false)
                        .addField("Server", config.mode.name(), false);

                bansChannel.sendMessageEmbeds(embed.build()).addActionRow(Button.danger("editban", "Edit Ban")).queue();
            }
            case trace -> {
                var info = other.getInfo();
                Call.traceInfo(con, other, new TraceInfo(other.ip(), other.uuid(), other.con.modclient, other.con.mobile, info.timesJoined, info.timesKicked));
                Log.info("@ has requested trace info of @.", player.plainName(), other.plainName());
            }
            case wave -> {
                logic.skipWave();
                Log.info("@ has skipped the wave.", player.plainName());
                sendToChat("events.admin.wave", player.coloredName());
            }
        }
    }
}