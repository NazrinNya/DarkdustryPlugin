package pandorum.entry;

import arc.util.*;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.gen.*;
import mindustry.world.Block;

import pandorum.comp.*;
import static pandorum.Misc.*;

import java.util.TimeZone;
import java.time.ZoneId;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BlockEntry implements HistoryEntry {
    @Nullable
    public final boolean isPlayer;
    public final String name;
    public final Unit unit;
    public final Block block;
    public final boolean breaking;
    public final int rotation;
    public Date time;

    public BlockEntry(BlockBuildEndEvent event) {
        this.breaking = event.breaking;
        this.unit = event.unit;
        this.isPlayer = unit.isPlayer();
        this.name = isPlayer ? colorizedName(unit.getPlayer()) : null;
        this.block = this.breaking ? null : event.tile.build.block;
        this.rotation = this.breaking ? -1 : event.tile.build.rotation;
        this.time = new Date();
    }

    @Override
    public String getMessage(Player player) {
        final SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Europe/Moscow")));
        final String ftime = df.format(this.time);

        if (breaking) {
            return isPlayer ? Bundle.format("history.block.destroy.player", findLocale(player.locale), name, ftime) :
            Bundle.format("history.block.destroy.unit", findLocale(player.locale), Icons.get(unit.type.name), unit.type.name, ftime);
        }

        StringBuilder base = new StringBuilder(isPlayer ? Bundle.format("history.block.construct.player", findLocale(player.locale), name, block, ftime) : Bundle.format("history.block.construct.unit", findLocale(player.locale), Icons.get(unit.type.name), unit.type.name, block, ftime));
        if (block.rotate) {
            base.append(Bundle.format("history.block.construct.rotate", findLocale(player.locale), RotateEntry.sides[rotation]));
        }
        return base.toString();
    }
}
