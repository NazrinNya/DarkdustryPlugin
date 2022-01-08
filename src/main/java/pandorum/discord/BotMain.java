package pandorum.discord;

import arc.util.CommandHandler.CommandResponse;
import arc.util.CommandHandler.ResponseType;
import arc.util.Log;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.rest.request.RouteMatcher;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.route.Routes;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Color;
import pandorum.comp.Authme;

import static pandorum.Misc.sendToChat;
import static pandorum.PluginVars.config;
import static pandorum.PluginVars.loginWaiting;

public class BotMain {

    public static final Color normalColor = Color.ORANGE;
    public static final Color successColor = Color.GREEN;
    public static final Color errorColor = Color.RED;

    public static DiscordClient bot;
    public static GatewayDiscordClient client;

    public static void start() {
        try {
            bot = DiscordClientBuilder.create(config.discordBotToken).onClientResponse(ResponseFunction.emptyIfNotFound()).onClientResponse(ResponseFunction.emptyOnErrorStatus(RouteMatcher.route(Routes.REACTION_CREATE), 400)).setDefaultAllowedMentions(AllowedMentions.suppressAll()).build();
            client = bot.login().block();

            client.on(MessageCreateEvent.class).subscribe(event -> {
                Message msg = event.getMessage();
                Member member = msg.getAuthorAsMember().block();
                CommandResponse response = BotHandler.handler.handleMessage(msg.getContent(), msg);
                if (response.type == ResponseType.fewArguments || response.type == ResponseType.manyArguments) {
                    BotHandler.err(msg.getChannel().block(), "Неверное количество аргументов.", "Использование : **@@** @", BotHandler.handler.getPrefix(), response.command.text, response.command.paramText);
                    return;
                }

                if (msg.getChannel().block() == BotHandler.botChannel && member != null && !member.isBot() && msg.getContent().length() > 0) {
                    sendToChat("events.discord.chat", member.getDisplayName(), msg.getContent());
                    Log.info("[Discord] @: @", member.getDisplayName(), msg.getContent());
                }
            }, e -> {});

            client.on(ButtonInteractionEvent.class).subscribe(event -> {
                Message message = event.getMessage().get();
                if (loginWaiting.containsKey(message)) {
                    switch (event.getCustomId()) {
                        case "confirm" -> Authme.confirm(message, event);
                        case "deny" -> Authme.deny(message, event);
                        case "check" -> Authme.check(message, event);
                    }
                }
            }, e -> {});

            BotHandler.init();
            Log.info("[Darkdustry] Бот успешно запущен...");
        } catch (Exception e) {
            Log.err("[Darkdustry] Ошибка запуска бота...");
            throw new RuntimeException(e);
        }
    }
}
