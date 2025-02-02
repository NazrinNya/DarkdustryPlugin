package darkdustry.features.history;

import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.*;
import mindustry.ai.UnitCommand;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType.ConfigEvent;
import mindustry.gen.Player;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.logic.CanvasBlock.CanvasBuild;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import mindustry.world.blocks.power.LightBlock;
import mindustry.world.blocks.units.UnitFactory.UnitFactoryBuild;
import useful.Bundle;

import static mindustry.Vars.*;
import static mindustry.gen.Iconc.*;

public class ConfigEntry implements HistoryEntry {
    public final String uuid;
    public final short blockID;
    public final Object config;
    public final long timestamp;

    public ConfigEntry(ConfigEvent event) {
        this.uuid = event.player.uuid();
        this.blockID = event.tile.block.id;
        this.config = getConfig(event);
        this.timestamp = Time.millis();
    }

    // Ифы сила, Дарк могила
    // (C) Овлер, 2021 год до н.э.
    @Override
    public String getMessage(Player player) {
        var info = netServer.admins.getInfo(uuid);
        var block = content.block(blockID);

        if (config instanceof UnlockableContent content) {
            return Bundle.format("history.config", player, info.lastName, block.emoji(), content.emoji(), Bundle.formatRelative(player, timestamp));
        }

        if (config instanceof Boolean enabled) {
            return enabled ?
                    Bundle.format("history.config.on", player, info.lastName, block.emoji(), Bundle.formatRelative(player, timestamp)) :
                    Bundle.format("history.config.off", player, info.lastName, block.emoji(), Bundle.formatRelative(player, timestamp));
        }

        if (config instanceof String text) {
            return text.isBlank() ?
                    Bundle.format("history.config.default", player, info.lastName, block.emoji(), Bundle.formatRelative(player, timestamp)) :
                    Bundle.format("history.config.text", player, info.lastName, block.emoji(), text.replaceAll("\n", " "), Bundle.formatRelative(player, timestamp));
        }

        if (config instanceof UnitCommand command) {
            return Bundle.format("history.config.command", player, info.lastName, block.emoji(), (char) codes.get(command.icon), Bundle.formatRelative(player, timestamp));
        }

        if (config instanceof Point2 point) {
            return point.pack() == -1 ?
                    Bundle.format("history.config.disconnect", player, info.lastName, block.emoji(), Bundle.formatRelative(player, timestamp)) :
                    Bundle.format("history.config.connect", player, info.lastName, block.emoji(), point, Bundle.formatRelative(player, timestamp));
        }

        if (config instanceof Point2[] points) {
            return points.length == 0 ?
                    Bundle.format("history.config.disconnect", player, info.lastName, block.emoji(), Bundle.formatRelative(player, timestamp)) :
                    Bundle.format("history.config.connect", player, info.lastName, block.emoji(), Seq.with(points).map(Point2::toString).toString(", "), Bundle.formatRelative(player, timestamp));
        }

        if (block instanceof LightBlock) {
            return Bundle.format("history.config.color", player, info.lastName, block.emoji(), Tmp.c1.set((int) config), Bundle.formatRelative(player, timestamp));
        }

        if (block instanceof LogicBlock) {
            return Bundle.format("history.config.code", player, info.lastName, block.emoji(), Bundle.formatRelative(player, timestamp));
        }

        if (block instanceof CanvasBlock) {
            return Bundle.format("history.config.image", player, info.lastName, block.emoji(), Bundle.formatRelative(player, timestamp));
        }

        return Bundle.format("history.config.default", player, info.lastName, block.emoji(), Bundle.formatRelative(player, timestamp));
    }

    public Object getConfig(ConfigEvent event) {
        if (event.tile instanceof LogicBuild || event.tile instanceof CanvasBuild)
            return null;

        if (event.tile instanceof UnitFactoryBuild factory)
            return factory.unit();

        if (event.tile.config() instanceof Point2 point)
            return point.add(event.tile.tileX(), event.tile.tileY());

        if (event.tile.config() instanceof Point2[] points) {
            Structs.each(point -> point.add(event.tile.tileX(), event.tile.tileY()), points);
            return points;
        }

        return event.tile.config();
    }
}