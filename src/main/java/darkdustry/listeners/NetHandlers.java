package darkdustry.listeners;

import arc.Events;
import arc.struct.Seq;
import arc.util.CommandHandler.CommandResponse;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.net.Administration.TraceInfo;
import mindustry.net.NetConnection;
import mindustry.net.Packets.*;
import useful.Bundle;

import static arc.util.CommandHandler.ResponseType.*;
import static darkdustry.PluginVars.*;
import static darkdustry.components.Database.getVpnData;
import static darkdustry.utils.Administration.*;
import static darkdustry.utils.Checks.notAdmin;
import static darkdustry.utils.Utils.*;
import static mindustry.Vars.*;
import static useful.Bundle.sendToChat;

public class NetHandlers {

    public static String invalidResponse(Player player, CommandResponse response) {
        if (response.type == manyArguments)
            return Bundle.format("commands.unknown.many-arguments", player, response.command.text, response.command.paramText);
        if (response.type == fewArguments)
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

        var connections = Seq.with(net.getConnections());
        if (connections.filter(connection -> connection.address.equals(ip)).size >= maxIdenticalIPs) {
            netServer.admins.blacklistDos(ip);
            connections.each(connection -> kick(connection, "kick.too-many-connections", locale));
            return;
        }

        if (netServer.admins.isIDBanned(uuid) || netServer.admins.isIPBanned(ip) || netServer.admins.isSubnetBanned(ip)) {
            kick(con, 0, true, "kick.banned", locale);
            return;
        }

        if (netServer.admins.getKickTime(uuid, ip) > Time.millis()) {
            kick(con, netServer.admins.getKickTime(uuid, ip) - Time.millis(), true, "kick.temp-banned", locale);
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

        getVpnData(ip).filter(data -> data.isVpn).subscribe(data -> kick(con, "kick.vpn", locale));

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
        Player admin = con.player, target = packet.other;
        var action = packet.action;

        if (notAdmin(admin) || target == null || (target.admin && target != admin)) return;

        Events.fire(new AdminRequestEvent(admin, target, action));

        switch (action) {
            case kick -> kick(admin, target, kickDuration);
            case ban -> ban(admin, target, 0L);
            case trace -> {
                var info = target.getInfo();
                Call.traceInfo(con, target, new TraceInfo(target.ip(), target.uuid(), target.con.modclient, target.con.mobile, info.timesJoined, info.timesKicked));
                Log.info("@ has requested trace info of @.", admin.plainName(), target.plainName());
            }
            case wave -> {
                logic.skipWave();
                Log.info("@ has skipped the wave.", admin.plainName());
                sendToChat("events.admin.wave", admin.coloredName());
            }
        }
    }
}