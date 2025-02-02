package darkdustry.features.menus;

import arc.func.*;
import arc.graphics.Color;
import arc.struct.Seq;
import darkdustry.components.Cache;
import darkdustry.utils.Admins;
import mindustry.content.Fx;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import useful.*;
import useful.State.StateKey;
import useful.menu.Menu;
import useful.menu.Menu.MenuView;
import useful.menu.Menu.MenuView.OptionData;
import useful.menu.impl.*;
import useful.text.TextInput;

import java.time.Duration;

import static darkdustry.PluginVars.*;
import static darkdustry.components.Database.*;
import static darkdustry.features.Ranks.*;
import static darkdustry.utils.Utils.*;
import static mindustry.net.Administration.Config.*;

@SuppressWarnings("unchecked")
public class MenuHandler {

    // region menu

    public static final ListMenu listMenu = new ListMenu(maxPerPage);
    public static final ConfirmMenu confirmMenu = new ConfirmMenu();

    public static final Menu
            statsMenu = new Menu(),
            promotionMenu = new Menu(),
            requirementsMenu = new Menu(),
            welcomeMenu = new Menu(),
            despawnMenu = new Menu(),
            settingsMenu = new Menu(),
            languagesMenu = new Menu(),
            effectsMenu = new Menu(),
            invalidDurationMenu = new Menu();

    // endregion
    // region input

    public static final TextInput
            kickDurationInput = new TextInput(),
            banDurationInput = new TextInput(),
            kickReasonInput = new TextInput(),
            banReasonInput = new TextInput();

    // endregion
    // region keys

    public static final StateKey<Long> DURATION = new StateKey<>("duration");
    public static final StateKey<Player> TARGET = new StateKey<>("target");
    public static final StateKey<PlayerData> DATA = new StateKey<>("data");

    // endregion
    // region transforms

    public static void load() {
        // region menu

        statsMenu.transform(TARGET, DATA, (menu, target, data) -> {
            menu.title("stats.title");
            menu.content("stats.content", target.coloredName(), data.id, data.rank.name(menu.player), data.rank.description(menu.player), data.blocksPlaced, data.blocksBroken, data.gamesPlayed, data.wavesSurvived, data.attackWins, data.pvpWins, data.hexedWins, Bundle.formatDuration(menu.player, Duration.ofMinutes(data.playTime)));

            menu.option("stats.requirements.show", Action.open(requirementsMenu)).row();
            menu.option("ui.button.close");
        });

        promotionMenu.transform(DATA, (menu, data) -> {
            menu.title("stats.promotion.title");
            menu.content("stats.promotion.content", data.rank.name(menu.player), data.rank.description(menu.player));

            menu.option("stats.requirements.show", Action.open(requirementsMenu)).row();
            menu.option("ui.button.close");
        });

        requirementsMenu.transform(menu -> {
            var builder = new StringBuilder();
            ranks.each(rank -> rank.requirements != null, rank -> builder.append(rank.requirements(menu.player)).append("\n"));

            menu.title("stats.requirements.title");
            menu.content(builder.toString());

            menu.option("ui.button.back", Action.back());
            menu.option("ui.button.close");
        });

        welcomeMenu.transform(menu -> {
            var builder = new StringBuilder();
            welcomeMessageCommands.each(command -> builder.append("[cyan]/").append(command).append("[gray] - [lightgray]").append(Bundle.get("commands." + command + ".description", menu.player)).append("\n"));

            menu.title("welcome.title");
            menu.content("welcome.content", serverName.string(), builder.toString());

            menu.option("ui.button.close").row();
            menu.option("welcome.discord", Action.uri(discordServerUrl)).row();
            menu.option("welcome.disable", view -> {
                Cache.get(view.player).welcomeMessage = false;
                Bundle.send(view.player, "welcome.disabled");
            });
        });

        despawnMenu.transform(menu -> {
            menu.title("despawn.title");
            menu.content("despawn.content");

            menu.options(1, DespawnType.values()).row();
            menu.option("despawn.suicide", view -> {
                Call.unitEnvDeath(view.player.unit());
                Bundle.send(view.player, "despawn.success.suicide");
            }).row();

            menu.option("ui.button.close");
        });

        settingsMenu.transform(menu -> {
            var data = Cache.get(menu.player);

            menu.title("settings.title");
            menu.content("settings.content");

            menu.options(1, Setting.values()).row();
            menu.option("setting.translator", Action.open(languagesMenu), data.language.name(menu)).row();
            menu.option("setting.effects", Action.open(effectsMenu), data.effects.name(menu)).row();

            menu.option("ui.button.close");
        }).followUp(true);

        languagesMenu.transform(menu -> {
            var data = Cache.get(menu.player);

            menu.title("language.title");
            menu.content("language.content", data.language.name(menu));

            menu.options(3, Language.values()).row();

            menu.option("ui.button.back", Action.back());
            menu.option("ui.button.close");
        }).followUp(true);

        effectsMenu.transform(menu -> {
            var data = Cache.get(menu.player);

            menu.title("effects.title");
            menu.content("effects.content", data.effects.name(menu));

            menu.options(2, EffectsPack.values()).row();

            menu.option("ui.button.back", Action.back());
            menu.option("ui.button.close");
        }).followUp(true);

        invalidDurationMenu.transform(menu -> {
            menu.title("duration.invalid.title");
            menu.content("duration.invalid.content");

            menu.option("ui.button.back", Action.back());
        });

        // endregion
        // region input

        kickDurationInput.transform(TARGET, (input, target) -> {
            input.title("kick.duration.title");
            input.content("kick.duration.content", target.coloredName());

            input.defaultText("kick.duration.default");
            input.textLength(32);

            input.result(text -> {
                var duration = parseDuration(text);
                if (duration.isZero()) {
                    invalidDurationMenu.open(input);
                    return;
                }

                kickReasonInput.open(input, DURATION, duration.toMillis());
            });
        });

        banDurationInput.transform(TARGET, (input, target) -> {
            input.title("ban.duration.title");
            input.content("ban.duration.content", target.coloredName());

            input.defaultText("ban.duration.default");
            input.textLength(32);

            input.result(text -> {
                var duration = parseDuration(text);
                if (duration.isZero()) {
                    invalidDurationMenu.open(input);
                    return;
                }

                banReasonInput.open(input, DURATION, duration.toMillis());
            });
        });

        kickReasonInput.transform(TARGET, (input, target) -> {
            input.title("kick.reason.title");
            input.content("kick.reason.content", target.coloredName(), Bundle.formatDuration(input.player, input.state.get(DURATION)));

            input.defaultText("kick.reason.default");
            input.textLength(64);

            input.closed(Action.back());
            input.result(text -> Admins.kick(target, input.player, input.state.get(DURATION), text));
        });

        banReasonInput.transform(TARGET, (input, target) -> {
            input.title("ban.reason.title");
            input.content("ban.reason.content", target.coloredName(), Bundle.formatDuration(input.player, input.state.get(DURATION)));

            input.defaultText("ban.reason.default");
            input.textLength(64);

            input.closed(Action.back());
            input.result(text -> Admins.ban(target, input.player, input.state.get(DURATION), text));
        });

        // endregion
    }

    // endregion
    // region show

    public static <T> void showListMenu(Player player, int page, String title, Prov<Seq<T>> content, Cons3<StringBuilder, Integer, T> formatter) {
        listMenu.show(player, page, title, content, formatter);
    }

    public static void showConfirmMenu(Player player, String content, Runnable confirmed, Object... values) {
        confirmMenu.show(player, "ui.title.confirm", content, confirmed, values);
    }

    public static void showStatsMenu(Player player, Player target, PlayerData data) {
        statsMenu.show(player, TARGET, target, DATA, data);
    }

    public static void showPromotionMenu(Player player, PlayerData data) {
        promotionMenu.show(player, DATA, data);
    }

    public static void showWelcomeMenu(Player player) {
        welcomeMenu.show(player);
    }

    public static void showDespawnMenu(Player player) {
        despawnMenu.show(player);
    }

    public static void showSettingsMenu(Player player) {
        settingsMenu.show(player);
    }

    public static void showKickInput(Player player, Player target) {
        kickDurationInput.show(player, TARGET, target);
    }

    public static void showBanInput(Player player, Player target) {
        banDurationInput.show(player, TARGET, target);
    }

    // endregion
    // region enums

    public enum DespawnType implements OptionData {
        all(unit -> true),
        players(Unit::isPlayer),
        ai(Unit::isAI),
        ally((player, unit) -> player.team() == unit.team),
        enemy((player, unit) -> player.team() != unit.team);

        public final Func2<Player, Unit, Boolean> filter;

        DespawnType(Boolf<Unit> filter) {
            this((player, unit) -> filter.get(unit));
        }

        DespawnType(Func2<Player, Unit, Boolean> filter) {
            this.filter = filter;
        }

        @Override
        public void option(MenuView menu) {
            menu.option("despawn." + name(), view -> {
                Groups.unit.each(unit -> filter.get(view.player, unit), Call::unitEnvDeath);
                Bundle.send(view.player, "despawn.success");
            }, Groups.unit.count(unit -> filter.get(menu.player, unit)));
        }
    }

    public enum Setting implements OptionData {
        alerts(data -> data.alerts = !data.alerts, data -> data.alerts),
        history(data -> data.history = !data.history, data -> data.history),
        welcomeMessage(data -> data.welcomeMessage = !data.welcomeMessage, data -> data.welcomeMessage),
        discordLink(data -> data.discordLink = !data.discordLink, data -> data.discordLink);

        public final Cons<PlayerData> setter;
        public final Func<PlayerData, Boolean> getter;

        Setting(Cons<PlayerData> setter, Func<PlayerData, Boolean> getter) {
            this.setter = setter;
            this.getter = getter;
        }

        @Override
        public void option(MenuView menu) {
            menu.option("setting." + name(), view -> {
                var data = Cache.get(view.player);
                setter.get(data);

                view.getInterface().show(view);
            }, Bundle.get(getter.get(Cache.get(menu.player)) ? "setting.on" : "setting.off", menu.player));
        }
    }

    public enum Language implements OptionData {
        english("en", "English"),
        french("fr", "Français"),
        german("de", "Deutsch"),

        italian("it", "Italiano"),
        spanish("es", "Español"),
        portuguese("pt", "Portuga"),

        russian("ru", "Русский"),
        polish("pl", "Polski"),
        turkish("tr", "Türkçe"),

        chinese("zh", "简体中文"),
        korean("ko", "한국어"),
        japanese("ja", "日本語"),

        off("off", "language.disabled", "language.disable");

        public final String code, name, button;

        Language(String code, String name) {
            this(code, name, name);
        }

        Language(String code, String name, String button) {
            this.code = code;
            this.name = name;
            this.button = button;
        }

        public String name(MenuView menu) {
            return Bundle.get(name, menu.player);
        }

        @Override
        public void option(MenuView menu) {
            menu.option(button, view -> {
                var data = Cache.get(view.player);
                data.language = this;

                view.getInterface().show(view);
            });
        }
    }

    public enum EffectsPack implements OptionData {
        scathe("Scathe",
                player -> Effects.stack(player, Fx.scatheExplosion, Fx.scatheLight),
                player -> Effects.stack(player, Fx.scatheExplosion, Fx.scatheLight),
                player -> Effects.at(Fx.neoplasiaSmoke, player)
        ),

        titan("Titan",
                player -> Effects.stack(player, Fx.titanExplosion, Fx.titanSmoke),
                player -> Effects.stack(player, Fx.titanExplosion, Fx.titanSmoke),
                player -> Effects.at(Fx.incendTrail, player, 3f)
        ),

        lancer("Lancer",
                player -> Effects.stack(player, Fx.railShoot, Fx.railShoot, Fx.railShoot),
                player -> Effects.stack(player, Fx.railShoot, Fx.railShoot, Fx.railShoot),
                player -> Effects.at(Fx.lightningCharge, player)
        ),

        foreshadow("Foreshadow",
                player -> Effects.stack(player, Fx.instHit, Fx.instHit, Fx.instHit),
                player -> Effects.stack(player, Fx.instHit, Fx.instHit, Fx.instHit),
                player -> Effects.at(Fx.mineWallSmall, player, Color.yellow)
        ),

        neoplasm("Neoplasm",
                player -> Effects.stack(player, Pal.neoplasm1, Fx.neoplasmSplat, Fx.titanSmoke),
                player -> Effects.stack(player, Pal.neoplasm1, Fx.neoplasmSplat, Fx.titanSmoke),
                player -> Effects.at(Fx.neoplasmHeal, player)
        ),

        teleport("Teleport",
                player -> Effects.stack(player, Fx.teleport, Fx.teleportActivate, Fx.teleportOut),
                player -> Effects.stack(player, Fx.teleport, Fx.teleportActivate, Fx.teleportOut),
                player -> Effects.at(Fx.smeltsmoke, player, Color.red)
        ),

        coreDust("Core Dust",
                player -> Effects.rotatedPoly(Fx.coreLandDust, player, 6, 12f, -180f, 90f, Color.royal),
                player -> Effects.rotatedPoly(Fx.coreLandDust, player, 6, 4f, -90f, 30f, Color.royal),
                player -> Effects.at(Fx.shootLiquid, player, player.unit().rotation - 180f, Color.royal)
        ),

        impactDrill("Impact Drill",
                player -> Effects.stack(player, 120f, Fx.mineImpactWave, Fx.mineImpactWave, Fx.mineImpactWave, Fx.mineImpact),
                player -> Effects.stack(player, 120f, Fx.mineImpactWave, Fx.mineImpactWave, Fx.mineImpactWave, Fx.mineImpact),
                player -> Effects.at(Fx.mineSmall, player, Color.cyan)
        ),

        thoriumReactor("Thorium reactor",
                player -> Effects.at(Fx.reactorExplosion, player),
                player -> Effects.at(Fx.reactorExplosion, player),
                player -> Effects.at(Fx.shootSmokeSquareSparse, player, player.unit().rotation - 180f, Color.purple)
        ),

        impactReactor("Impact reactor",
                player -> Effects.at(Fx.impactReactorExplosion, player),
                player -> Effects.at(Fx.impactReactorExplosion, player),
                player -> Effects.at(Fx.shootSmokeSquareSparse, player, player.unit().rotation - 180f, Color.gold)
        ),

        greenLaser("Green Laser",
                player -> Effects.at(Fx.greenBomb, player),
                player -> Effects.at(Fx.greenLaserCharge, player),
                player -> Effects.at(Fx.electrified, player)
        ),

        suppressParticle("Suppress Particle",
                player -> Effects.at(Fx.dynamicSpikes, player, 60f, Pal.sapBullet),
                player -> Effects.at(Fx.dynamicSpikes, player, 60f, Pal.sapBullet),
                player -> Effects.at(Fx.regenSuppressSeek, player, player.unit())
        ),

        none("effects.disabled", "effects.disable");

        public final String name, button;
        public final Cons<Player> join, leave, move;

        EffectsPack(String name, Cons<Player> join, Cons<Player> leave, Cons<Player> move) {
            this(name, name, join, leave, move);
        }

        EffectsPack(String name, String button) {
            this(name, button, player -> {}, player -> {}, player -> {});
        }

        EffectsPack(String name, String button, Cons<Player> join, Cons<Player> leave, Cons<Player> move) {
            this.name = name;
            this.button = button;

            this.join = join;
            this.leave = leave;
            this.move = move;
        }

        public String name(MenuView menu) {
            return Bundle.get(name, menu.player);
        }

        @Override
        public void option(MenuView menu) {
            menu.option(button, view -> {
                var data = Cache.get(view.player);
                data.effects = this;

                view.getInterface().show(view);
            });
        }
    }

    // endregion
}