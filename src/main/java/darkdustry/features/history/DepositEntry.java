package darkdustry.features.history;

import arc.util.Time;
import mindustry.game.EventType.DepositEvent;
import mindustry.gen.Player;
import useful.Bundle;

import static darkdustry.utils.Utils.*;
import static mindustry.Vars.*;

public class DepositEntry implements HistoryEntry {

    public final String uuid;
    public final short blockID;
    public final short itemID;
    public final int amount;
    public final long timestamp;

    public DepositEntry(DepositEvent event) {
        this.uuid = event.player.uuid();
        this.blockID = event.tile.block.id;
        this.itemID = event.item.id;
        this.amount = event.amount;
        this.timestamp = Time.millis();
    }

    public String getMessage(Player player) {
        var info = netServer.admins.getInfo(uuid);
        return Bundle.format("history.deposit", player, info.lastName, amount, content.item(itemID).emoji(), content.block(blockID).emoji(), formatTime(timestamp));
    }
}