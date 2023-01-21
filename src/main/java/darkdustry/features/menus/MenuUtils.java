package darkdustry.features.menus;

import arc.func.Intc;
import mindustry.gen.Player;
import useful.Bundle;
import useful.menu.DynamicMenus;

public class MenuUtils {

    public static void showMenu(Player player, String title, String content, String[][] buttons, Intc listener, Object... values) {
        for (int i = 0; i < buttons.length; i++)
            for (int j = 0; j < buttons[i].length; j++)
                buttons[i][j] = Bundle.get(buttons[i][j], player);

        DynamicMenus.menu(player, Bundle.get(title, player), Bundle.format(content, player, values), buttons, listener);
    }

    public static void showMenuConfirm(Player player, String content, Runnable confirmed, Object... values) {
        showMenuConfirm(player, content, confirmed, () -> {}, values);
    }

    public static void showMenuConfirm(Player player, String content, Runnable confirmed, Runnable denied, Object... values) {
        showMenu(player, "ui.title.confirm", content, new String[][] {{"ui.button.yes", "ui.button.no"}}, option -> {
            if (option == 0) confirmed.run();
            else denied.run();
        }, values);
    }
}