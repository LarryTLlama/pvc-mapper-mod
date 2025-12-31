package larrytllama.pvcmappermod;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class MapperCmdHandler {

    // https://stackoverflow.com/a/13632114
    public static String readStringFromURL(String requestURL) throws IOException {
        try (Scanner scanner = new Scanner(URI.create(requestURL).toURL().openStream(),
                StandardCharsets.UTF_8.toString()))
        {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    public PlayerFetchUtils pfu;

    public MapperCmdHandler(PlayerFetchUtils pfu, PVCMapperModClient modclient) {
        this.pfu = pfu;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(
                Commands.literal("search")
                .then(Commands.argument("query", StringArgumentType.greedyString())
                .executes(context -> {
                    String query = StringArgumentType.getString(context, "query");
                    if(query == null || query.length() < 2) {
                        Minecraft.getInstance().execute(() -> {
                            context.getSource().sendFailure(Component.literal("Your search query '" + query + "' needs to be 3 or more characters long!").withStyle(ChatFormatting.RED));
                        });
                        return 1;
                    }
                    CompletableFuture.runAsync(() -> {
                        SearchResult[] results = pfu.fetchSearchResults(query);
                        if(results == null) {
                            Minecraft.getInstance().execute(() -> {
                                context.getSource().sendFailure(Component.literal("Search failed. Try again later!").withStyle(ChatFormatting.RED));
                            });
                        } else if(results.length == 0) {
                            context.getSource().sendFailure(Component.literal("No search results found. Try another search.").withStyle(ChatFormatting.YELLOW));
                        } else {
                            MutableComponent chatMsg = Component.literal("PVC Mapper - Search Results\n").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withColor(ChatFormatting.BOLD));
                            chatMsg.append(Component.literal("" + results.length + " results found!\n").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withColor(ChatFormatting.ITALIC)));
                            chatMsg.append(Component.literal(""));
                            for (int i = 0; i < results.length; i++) {
                                // 1. The place name!
                                //    It's a such and such
                                //    Type: place. [View on Map]
                                chatMsg.append("" + (i+1) + ". ").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
                                chatMsg.append(results[i].name + "\n   ");
                                chatMsg.append(results[i].description + "\n   ");
                                chatMsg.append("Type: " + results[i].type);
                                if (results[i].type.equals("place") || results[i].type.equals("area")) {
                                    chatMsg.append(
                                        Component.literal(" [View on Map]").withStyle(
                                            Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("map " + results[i].x + " " + results[i].z))
                                            .withColor(ChatFormatting.GREEN)
                                        )
                                    );
                                }
                                chatMsg.append("\n");
                            }

                            Minecraft.getInstance().execute(() -> {
                                context.getSource().sendSuccess(() -> chatMsg, true);
                            });
                        }
                    });

                    return 1;
                })
                )
            );

            dispatcher.register(
                Commands.literal("map")
                .executes((context) -> {
                    Minecraft.getInstance().setScreen(modclient.fsm);
                    return 1;
                })
                .then(Commands.argument("x", IntegerArgumentType.integer())
                    .then(Commands.argument("z", IntegerArgumentType.integer())
                        .executes((context) -> {
                            Minecraft.getInstance().execute(() -> {
                                Minecraft.getInstance().setScreen(modclient.fsm);
                                modclient.fsm.navToCoords(IntegerArgumentType.getInteger(context, "x"), IntegerArgumentType.getInteger(context, "z"));
                            });
                            return 1;
                        })
                    )
                )
            );

            dispatcher.register(
                Commands.literal("mapper_fetch")
                .then(Commands.argument("type", StringArgumentType.string())
                .then(Commands.argument("id", StringArgumentType.string())
                .executes(context -> {
                    switch (StringArgumentType.getString(context, "type")) {
                        case "place":
                            
                            break;
                        case "area":
                            
                            break;
                        case "network":
                            
                            break;
                        case "collection":
                            
                            break;
                        case "user":
                            
                            break;
                        default:
                            context.getSource().sendFailure(Component.literal("Command usage: /mapper_fetch <place|area|network|collection|user> <id>").withStyle(ChatFormatting.RED));
                            break;
                    }
                    return 1;
                })
            )));
        });
    }
}
