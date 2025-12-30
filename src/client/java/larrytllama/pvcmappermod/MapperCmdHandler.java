package larrytllama.pvcmappermod;

import java.util.Scanner;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
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
