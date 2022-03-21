package pandorum.commands.discord;

import arc.struct.Seq;
import arc.util.CommandHandler.Command;
import arc.util.CommandHandler.CommandRunner;
import pandorum.discord.Context;
import pandorum.util.Utils;

import static pandorum.PluginVars.discordCommands;
import static pandorum.util.Utils.adminCheck;

public class HelpCommand implements CommandRunner<Context> {
    public void accept(String[] args, Context context) {
        Seq<Command> commandsList = Utils.getAvailableDiscordCommands(adminCheck(context.member));
        StringBuilder commands = new StringBuilder();

        for (Command command : commandsList) {
            commands.append(discordCommands.getPrefix()).append("**").append(command.text).append("**");
            if (!command.paramText.isEmpty()) {
                commands.append(" *").append(command.paramText).append("*");
            }
            commands.append(" - ").append(command.description).append("\n");
        }

        context.info(":newspaper: Доступные команды:", commands.toString());
    }
}
