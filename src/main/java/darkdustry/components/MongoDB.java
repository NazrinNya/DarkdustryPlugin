package darkdustry.components;

import arc.Events;
import arc.func.Cons;
import arc.util.Log;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.reactivestreams.client.*;
import darkdustry.DarkdustryPlugin;
import mindustry.game.EventType;
import org.bson.codecs.pojo.PojoCodecProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.mongodb.client.model.Filters.all;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.reactivestreams.client.MongoClients.getDefaultCodecRegistry;
import static darkdustry.PluginVars.config;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoDB {

    public static MongoCollection<PlayerData> collection;
    public static MongoDatabase db;

    public static void connect() {
        try {
            MongoClient client = MongoClients.create(config.mongoUrl);
            db = client.getDatabase("darkdustry").withCodecRegistry(fromRegistries(getDefaultCodecRegistry(), fromProviders(PojoCodecProvider.builder().automatic(true).build())));
            collection = db.getCollection("players", PlayerData.class);

            DarkdustryPlugin.info("Database connected.");

            Events.on(EventType.PlayerJoin.class,e->{
                MongoDB.insertPlayer(e.player.uuid()).subscribe();
            });
        } catch (Exception e) {
            DarkdustryPlugin.error("Failed to connect to the database: @", e);
        }

//        getPlayerData("dddd", (data) -> {
//            Log.info("Мы получили плейер дату!");
//            data.gamesPlayed = 9999;
//            setPlayerData(data);
//        });

        for (int i = 0; i <= 10; i++) {
            Log.info(i);
        }
    }

    public static Mono<PlayerData> getPlayerData(String uuid){
        return Mono.from(collection.find(eq("uuid", uuid))).defaultIfEmpty(new PlayerData(uuid));
    }

    public static Flux<PlayerData> getPlayersData(List<String> uuids){
        return Flux.from(collection.find(all("uuid",uuids))).defaultIfEmpty(new PlayerData());
    }
    public static void setPlayerData(PlayerData data) {
        Mono.from(collection.replaceOne(eq("uuid", data.uuid), data))
                .filter(r -> r.getModifiedCount() < 1)
                .flatMap(r -> Mono.from(collection.insertOne(data)))
                .subscribe();
    }
    public static Mono<Void> setPlayersData(List<PlayerData> data){
        return Mono.from(collection.bulkWrite(data.stream()
                        .map(p -> new ReplaceOneModel<>(eq("uuid", p.uuid), p))
                        .toList()))
                .then();
    }

    public static Mono<Void> insertPlayer(String uuid){
        return Mono.from(collection.find(eq("uuid",uuid)).first())
                .switchIfEmpty(Mono.from(collection.insertOne(new PlayerData(uuid))).then(Mono.empty()))
                .then();
    }

    public static class PlayerData {
        public String uuid;
        public String discord;
        public String language = "off";

        public boolean welcomeMessage = true;
        public boolean alertsEnabled = true;

        public int playTime = 0;
        public int buildingsBuilt = 0;
        public int gamesPlayed = 0;

        public int rank = 0;
        public PlayerData() {}
        public PlayerData(String uuid) {
            this.uuid = uuid;
        }
    }
}
