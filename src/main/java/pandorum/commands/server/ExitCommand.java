package pandorum.commands.server;

import arc.func.Cons;
import arc.util.Log;

public class ExitCommand implements Cons<String[]> {
    public void get(String[] args) {
        Log.info("Выключаю сервер...");
        System.exit(2);
    }
}
