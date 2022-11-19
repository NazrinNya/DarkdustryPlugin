package darkdustry.components;

import arc.func.Cons;
import arc.struct.StringMap;
import arc.util.*;
import arc.util.serialization.Jval;
import darkdustry.DarkdustryPlugin;
import darkdustry.utils.Find;
import mindustry.gen.*;

import static darkdustry.PluginVars.*;
import static darkdustry.components.Database.getPlayersData;
import static mindustry.Vars.netServer;

public class Translator {

    public static void load() {
        translatorLanguages.putAll(
                "ca", "Català",
                "id", "Indonesian",
                "da", "Dansk",
                "de", "Deutsch",
                "et", "Eesti",
                "en", "English",
                "es", "Español",
                "eu", "Euskara",
                "fil", "Filipino",
                "fr", "Français",
                "it", "Italiano",
                "lt", "Lietuvių",
                "hu", "Magyar",
                "nl", "Nederlands",
                "pl", "Polski",
                "pt", "Português",
                "ro", "Română",
                "fi", "Suomi",
                "sv", "Svenska",
                "vi", "Tiếng Việt",
                "tk", "Türkmen dili",
                "tr", "Türkçe",
                "cs", "Čeština",
                "be", "Беларуская",
                "bg", "Български",
                "ru", "Русский",
                "sr", "Српски",
                "uk_UA", "Українська",
                "th", "ไทย",
                "zh", "简体中文",
                "ja", "日本語",
                "ko", "한국어"
        );

        DarkdustryPlugin.info("Loaded @ translator languages.", translatorLanguages.size);
    }

    public static void translate(String text, String from, String to, Cons<String> result, Runnable error) {
        Http.post(translatorApiUrl, "tl=" + to + "&sl=" + from + "&q=" + Strings.encode(text))
                .error(throwable -> error.run())
                .submit(response -> result.get(Jval.read(response.getResultAsString()).asArray().get(0).asArray().get(0).asString()));
    }

    public static void translate(Player author, String text) {
        var cache = new StringMap();
        var message = netServer.chatFormatter.format(author, text);

        getPlayersData(Groups.player).doOnNext(data -> {
            var player = Find.playerByUuid(data.uuid);
            if (player == null || player == author) return;

            if (data.language.equals("off")) {
                player.sendMessage(message, author, text);
                return;
            }

            if (cache.containsKey(data.language)) {
                player.sendMessage(cache.get(data.language), author, text);
            } else translate(text, "auto", data.language, result -> {
                cache.put(data.language, message + " [white]([lightgray]" + result + "[])");
                player.sendMessage(cache.get(data.language), author, text);
            }, () -> player.sendMessage(message, author, text));
        }).subscribe();
    }
}