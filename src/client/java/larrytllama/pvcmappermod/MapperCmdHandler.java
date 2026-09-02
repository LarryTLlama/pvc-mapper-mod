package larrytllama.pvcmappermod;

import larrytllama.pvcmappermod.utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

/*? if <26.1 {*/
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
/*?} else {*/
/*import net.fabricmc.fabric.api.client.command.v2.ClientCommands;*/
/*?}*/
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

public class MapperCmdHandler {

    public PlayerFetchUtils pfu;

    public MapperCmdHandler(PlayerFetchUtils pfu, PVCMapperModClient modclient) {
        this.pfu = pfu;
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LogUtils.debug("[PVC Mapper Mod] ClientCommandRegistrationCallback.EVENT triggered");

            dispatcher.register(
                ClientCommandManager.literal("search")
                .executes(context -> {
                    Minecraft.getInstance().execute(() -> {
                        context.getSource().sendError(Component.literal("Please specify a search query!").withStyle(ChatFormatting.RED));
                    });
                    return 1;
                })
                .then(ClientCommandManager.argument("query", StringArgumentType.greedyString())
                .executes(context -> {
                    String query = StringArgumentType.getString(context, "query");
                    if(query == null || query.length() < 2) {
                        Minecraft.getInstance().execute(() -> {
                            context.getSource().sendError(Component.literal("Your search query '" + query + "' needs to be 3 or more characters long!").withStyle(ChatFormatting.RED));
                        });
                        return 1;
                    }
                    pfu.fetchSearchResultsAsync(query).thenAccept(results -> {
                        if(results == null) {
                            Minecraft.getInstance().execute(() -> {
                                context.getSource().sendError(Component.literal("Search failed. Try again later!").withStyle(ChatFormatting.RED));
                            });
                        } else if(results.length == 0) {
                            context.getSource().sendError(Component.literal("No search results found. Try another search.").withStyle(ChatFormatting.YELLOW));
                        } else {
                            MutableComponent chatMsg = Component.literal("");
                            chatMsg.append(Component.literal("PVC Mapper - Search Results\n").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)));
                            chatMsg.append(Component.literal("" + results.length + " results found!\n").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true)));
                            for (int i = 0; i < results.length; i++) {
                                // 1. The place name!
                                //    It's a such and such
                                //    Type: place. [View on Map]
                                chatMsg.append(Component.literal("" + (i+1) + ". ").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
                                chatMsg.append(Component.literal(results[i].name + "\n   ").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
                                chatMsg.append(Component.literal(results[i].description + "\n   ").withStyle(Style.EMPTY.withItalic(true)));
                                chatMsg.append(Component.literal("Type: " + results[i].type));
                                if (results[i].type.equals("place") || results[i].type.equals("area")) {
                                    chatMsg.append(
                                        Component.literal(" [View on Map]").withStyle(
                                            Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("map " + results[i].x + " " + results[i].z + " " + results[i].dimension))
                                            .withColor(ChatFormatting.GREEN)
                                        )
                                    );
                                    
                                    chatMsg.append(
                                        Component.literal(" [Directions Here]").withStyle(
                                            Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("mapper directions " + results[i].id))
                                            .withColor(ChatFormatting.GREEN)
                                        )
                                    );

                                    chatMsg.append(
                                        Component.literal(" [Share Coords]").withStyle(
                                            Style.EMPTY.withClickEvent(new ClickEvent.SuggestCommand(String.format("%s: %d, %d", results[i].name, results[i].x, results[i].z)))
                                            .withColor(ChatFormatting.GREEN)
                                        )
                                    );
                                }
                                chatMsg.append("\n");
                            }

                            Minecraft.getInstance().execute(() -> {
                                context.getSource().sendFeedback(chatMsg);
                            });
                        }
                    });

                    return 1;
                })
                )
            );

            dispatcher.register(
                ClientCommandManager.literal("map")
                .executes((context) -> {
                    LogUtils.debug("[PVC Mapper Mod] /map command executes called");
                    PVCMapperModClient.setScreenOnNextTick(modclient.fsm);
                    return 1;
                })
                .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                    .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
                        .then(ClientCommandManager.argument("dimension", StringArgumentType.string())
                            .executes((context) -> {
                                int x = IntegerArgumentType.getInteger(context, "x");
                                int z = IntegerArgumentType.getInteger(context, "z");
                                String dimension = StringArgumentType.getString(context, "dimensionID");
                                PVCMapperModClient.setScreenOnNextTick(modclient.fsm, () -> {
                                    modclient.fsm.currentDimension = dimension;
                                    modclient.fsm.navToCoords(x, z);
                                });
                                return 1;
                            })
                        )
                    )
                )
            );

            dispatcher.register(
                ClientCommandManager.literal("shops")
                .executes((context) -> {
                    LogUtils.debug("[PVC Mapper Mod] /shops command executes called");
                    PVCMapperModClient.setScreenOnNextTick(modclient.shopsScreen);
                    return 1;
                })
                .then(ClientCommandManager.argument("item", StringArgumentType.greedyString()).suggests(ShopsHandler.ITEM_SUGGESTIONS)
                    .executes((context) -> {
                        String item = StringArgumentType.getString(context, "item");
                        LogUtils.debug("[PVC Mapper Mod] /shops command executes called with item: " + item);
                        PVCMapperModClient.setScreenOnNextTick(modclient.shopsScreen, () -> {
                            CompletableFuture.runAsync(() -> {
                                modclient.shopsScreen.openWithItem(item);
                            });
                        });
                        return 1;
                    })
                )
            );

            MutableComponent directionsPrefix = Component.literal("[").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY)
                .append(Component.literal("DIR").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("] ").withStyle(ChatFormatting.GRAY));

            dispatcher.register(
                ClientCommandManager.literal("mapper")
                .executes((context) -> {
                    sendHelpFeedback(context);
                    return 1;
                })
                .then(ClientCommandManager.literal("help").executes((context) -> {
                    sendHelpFeedback(context);
                    return 1;
                }))
                .then(ClientCommandManager.literal("clearcache").executes((context) -> {
                    // Reset minimap image state
                    modclient.minimap.textureUrls = new String[4];
                    // Including this one which should never realistically be a value
                    // (Unless PVC's still expanding the map 15000 years later)
                    modclient.minimap.tileCoords = new int[][] {
                        { Integer.MIN_VALUE, Integer.MIN_VALUE },
                        { Integer.MIN_VALUE, Integer.MIN_VALUE },
                        { Integer.MIN_VALUE, Integer.MIN_VALUE },
                        { Integer.MIN_VALUE, Integer.MIN_VALUE }
                    };
                    // Reset the central global tile cache in TextureUtils
                    TextureUtils.clearCache();

                    context.getSource().sendFeedback(Component.literal("Successfully cleared map tile cache (yay!)"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("retryupdates").executes((context) -> {
                    if(pfu.isUpdating) {
                        context.getSource().sendFeedback(Component.literal("The player tracker is already updating!"));
                    } else {
                        context.getSource().sendFeedback(Component.literal("Retrying player updates..."));
                        pfu.startUpdates();
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("stop-directions").executes((context) -> {
                    if(modclient.dp == null) {
                        context.getSource().sendFeedback(Component.literal("The Directions Provider has not initialised yet.").withStyle(ChatFormatting.RED));
                    } else if(!modclient.dp.routeActive) {
                        context.getSource().sendFeedback(Component.empty().append(directionsPrefix).append(Component.literal("A route is not currently active").withStyle(ChatFormatting.RED)));
                    } else {
                        modclient.dp.clearRoute();
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("directions")
                    .then(ClientCommandManager.argument("destinationid", StringArgumentType.string()).executes((context) -> {
                        String destinationID = StringArgumentType.getString(context, "destinationid");

                        // Set up route
                        if(modclient.dp == null) {
                            context.getSource().sendFeedback(Component.literal("The Directions Provider has not initialised yet.").withStyle(ChatFormatting.RED));
                            return 1;
                        } else {
                            context.getSource().sendFeedback(Component.empty().append(directionsPrefix).append("Now loading directions..."));
                            modclient.dp.setupRoute(
                                String.format("X%dZ%dD%s", Minecraft.getInstance().player.getBlockX(), Minecraft.getInstance().player.getBlockZ(), modclient.dp.getCurrentDimension()), 
                                destinationID, 
                                (MutableComponent chatMsg) -> {
                                    context.getSource().sendFeedback(Component.empty().append(directionsPrefix).append(chatMsg));
                                },
                                (String error) -> {
                                    context.getSource().sendFeedback(Component.empty().append(directionsPrefix).append(Component.literal("An error occurred when trying to get directions: ").withStyle(ChatFormatting.RED).append(Component.literal(error).withStyle(ChatFormatting.YELLOW))));
                                }
                            );
                        }
                        return 1;
                    }))
                )  
            );

            dispatcher.register(
                ClientCommandManager.literal("afksince").then(ClientCommandManager.argument("player", StringArgumentType.greedyString())
                .suggests(this.PLAYER_SUGGESTIONS)
                .executes((context) -> {
                    try {
                        ArrayList<PlayerFetch> p = pfu.getPlayers();
                        for (int i = 0; i < p.size(); i++) {
                            if(p.get(i).name.toLowerCase().contains(StringArgumentType.getString(context, "player").toLowerCase())) {
                                MutableComponent response = Component.literal(p.get(i).name).withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                                Instant afkSince = Instant.parse(p.get(i).afksince);
                                Duration dur = Duration.between(afkSince, Instant.now()).abs();
                                if(dur.toMinutes() < 2) {
                                    response.append(Component.literal(" is ").withStyle(ChatFormatting.YELLOW));
                                    response.append(Component.literal("NOT AFK").withStyle(ChatFormatting.RED));
                                    response.append(Component.literal(".").withStyle(ChatFormatting.YELLOW));
                                } else {
                                    response.append(Component.literal(" has been AFK for: ").withStyle(ChatFormatting.YELLOW));
                                    String timelength = "";
                                    if (dur.toDaysPart() > 0) timelength += dur.toDaysPart() + " days, " ;
                                    if (dur.toHoursPart() > 0) timelength += dur.toHoursPart() + " hours, ";
                                    if (dur.toMinutesPart() > 0) timelength += dur.toMinutesPart() + " mins, ";
                                    if (dur.toSecondsPart() > 0) timelength += dur.toSecondsPart() + " secs";
                                    response.append(Component.literal(timelength).withStyle(ChatFormatting.RED));
                                }
                                Minecraft.getInstance().execute(() -> {
                                    context.getSource().sendFeedback(response);
                                });
                                return 1;
                            }
                        }
                        Minecraft.getInstance().execute(() -> {
                            context.getSource().sendFeedback(Component.literal("That player was not found online."));
                        });
                        return 1;
                    } catch(Exception e) {
                        System.out.println(e);
                        Minecraft.getInstance().execute(() -> {
                            context.getSource().sendFeedback(Component.literal("An unknown exception occurred when checking for player AFK-ness"));
                        });
                        return 1;
                    }
                }))
            );
        });
    }

    private void sendHelpFeedback(com.mojang.brigadier.context.CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> context) {
        context.getSource().sendFeedback(
            Component.literal("Commands List\n")
            .append(Component.literal("/afksince <player>").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                .append(Component.literal(" - Find how long a player has been AFK\n").setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)))
            ).append(Component.literal("/search [query]").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                .append(Component.literal(" - Search the entire PVC Mapper!\n").setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)))
            ).append(Component.literal("/shops [item]").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                .append(Component.literal(" - Search for shops by this item ID (or open the viewer with no arguments)\n").setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)))
            ).append(Component.literal("/map [x] [z]").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                .append(Component.literal(" - Open the map screen, optionally to the chosen x/z coords\n").setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)))
            ).append(Component.literal("/mapper clearcache").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                .append(Component.literal(" - Clears the map cache refreshing all tiles\n").setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)))
            ).append(Component.literal("/mapper help").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                .append(Component.literal(" - You can guess what this does\n").setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)))
            ).append(Component.literal("/mapper retryupdates").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                .append(Component.literal(" - Use if the mod stops refreshing players\n").setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)))
            )
        );
    }

    public SuggestionProvider<FabricClientCommandSource> PLAYER_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (Item item : BuiltInRegistries.ITEM) {
            ResIdentifier id = ResIdentifier.of(BuiltInRegistries.ITEM.getKey(item));
            if (id != null) {
                String path = id.getPath();
                if (path.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    String suggestion = path.toUpperCase(Locale.ROOT);
                    builder.suggest(suggestion);
                }
            }
        }
        
        List<PlayerFetch> players = this.pfu.getPlayers();

        for (PlayerFetch playerName : players) {
            if (playerName.name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(playerName.name);
            }
        }
        return builder.buildFuture();
    };

    /*? if >=26.1 {*/
    /*private static class ClientCommandManager {
        public static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> literal(String name) {
            return ClientCommands.literal(name);
        }
        public static <T> com.mojang.brigadier.builder.RequiredArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource, T> argument(String name, com.mojang.brigadier.arguments.ArgumentType<T> type) {
            return ClientCommands.argument(name, type);
        }
    }*/
    /*?}*/
}
