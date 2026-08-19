package larrytllama.pvcmappermod;

import java.util.function.Consumer;
import larrytllama.pvcmappermod.utils.CompatUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class DirectionsProvider {
    DirectionsNode[] currentPath;
    int currentStep = 0;
    PlayerFetchUtils pfu;
    Boolean routeActive = false;
    Minimap minimap;
    Consumer<MutableComponent> chatRunner;
    boolean waitingForDimensionChange = false;

    DirectionsProvider(PlayerFetchUtils pfu, Minimap minimap) {
        this.pfu = pfu;
        this.minimap = minimap;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            updateRoute();
            spawnRouteParticle();
        });
    }

    DirectionsNode getNextNode() {
        if(currentPath.length == 0) return null;
        return currentPath[currentPath.length - 1];
    }

    void setupRoute(String fromID, String toID, Consumer<MutableComponent> chatRunner, Consumer<String> onError) {
        clearRoute();
        this.chatRunner = chatRunner;
        pfu.fetchDirectionsAsync(fromID, toID).thenAccept((DirectionsResponse res) -> {
            if(res.status.equals("NotOK")) {
                onError.accept(res.error);
                return;
            }
            if(res.path.length == 0) {
                onError.accept("Couldn't find a way there!\nYour nearest network may not be connected to another network\n(Help connect everything together! Add Network Connections in the mapper editor!)");
            }
            this.currentPath = res.path;
            boolean isok = false;
            this.currentStep = 0;
            while (isok == false) {
                // Skip over routes starting at places
                if(res.path[this.currentStep].id.startsWith("P")) {
                    this.currentStep++;
                } else {
                    isok = true;
                }
            }
            this.routeActive = true;
            this.getAngleChangeInPath(0);

            this.chatRunner.accept(
                Component.empty()
                .append(Component.literal(directionArrow))
                .append(Component.literal(" " + directionInstruction + " ").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)))
                .append(Component.literal(directionArrow))
                .append(Component.literal(" - Now"))
            );
        });
    }

    void clearRoute() {
        this.currentStep = 0;
        this.routeActive = false;
        if(chatRunner != null && this.currentPath != null && this.currentPath.length > 0) this.chatRunner.accept(Component.literal("Stopped directions to: " + this.currentPath[this.currentPath.length - 1].data.network));
        this.currentPath = null;
    }

    // Find the best block to spawn the particle on
    double getParticleY(Level level, double x, double z) {
        Minecraft mc = Minecraft.getInstance();
        if(level == null) return mc.player.getY();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, mc.player.getY(), z);
        BlockState state = level.getBlockState(pos);

        if(state.isAir()) {
            // Go down until not air
            while(state.isAir() && level.isInsideBuildHeight(pos.getY())) {
                pos.setY(pos.getY() - 1);
                state = level.getBlockState(pos);
            }
            return pos.getY() + 1.2;
        } else {
            while(!state.isAir() && level.isInsideBuildHeight(pos.getY())) {
                pos.setY(pos.getY() + 1);
                state = level.getBlockState(pos);
            }
            return pos.getY() + 0.8;
        }
    }

    void spawnRouteParticle() {
        if(currentPath == null || currentStep == 0 || routeActive == false) return;
        if(currentStep == currentPath.length-1) return;
        if(currentPath[currentStep-1].id.startsWith("P")) return;
        int step = currentStep;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!currentPath[step].data.dimension.equals(currentPath[step+1].data.dimension)) return;
        // Also do distance to node calcs in here because I'm lazy
        this.directionDistance = (int)Math.floor(Math.hypot(currentPath[step+1].data.x - mc.player.getBlockX(), currentPath[step+1].data.z - mc.player.getBlockZ()));

        int x0 = currentPath[step-1].data.x, 
            z0 = currentPath[step-1].data.z,
            x1 = currentPath[step].data.x, 
            z1 = currentPath[step].data.z;
        double dx = x1 - x0;
        double dz = z1 - z0;
        double distance = Math.hypot(dx, dz);

        for (double steps = 0; steps <= distance; steps += 2.0) {
            double t = steps / distance;
            double x = x0 + dx * t;
            double z = z0 + dz * t;
            double y = getParticleY(mc.level, x, z);
            mc.level.addParticle(ParticleTypes.WHITE_SMOKE, x, y, z, 0.0, 0.0, 0.0);
        }
    }

    String directionArrow = "";
    String directionInstruction = "";
    int directionDistance = 0;

    String getCurrentDimension() {
        String dimension = "minecraft_" + CompatUtils.getIdentifier(Minecraft.getInstance().level.dimension()).getPath();
        if(this.minimap != null && this.minimap.isInTerra2 && dimension.equals("minecraft_overworld")) return "minecraft_terra2";
        return dimension;
    }

    String dimensionToPrettyName(String dim) {
        switch (dim) {
            case "minecraft_overworld":
                return "Mondo Overworld";
            case "minecraft_terra2":
                return "Terra2 Overworld";
            case "minecraft_the_nether":
                return "Mondo Nether";
            default:
                return "Unknown World";
        }
    }

    void getAngleChangeInPath(int index) {
        if(index == currentPath.length) return; // We have arrived!

        if(!currentPath[index+1].data.dimension.equals(getCurrentDimension())) { // Switch dimension
            // Go to <new dimension>
            directionArrow = "🌌";
            directionInstruction = "Switch to " + dimensionToPrettyName(currentPath[index+1].data.dimension);
            waitingForDimensionChange = true;
            return;
        } else {
            waitingForDimensionChange = false;
        }

        if( // Arriving at destination
            (index+2 == currentPath.length && currentPath[index+1].id.startsWith("P")) || 
            (index+1 == currentPath.length)
        ) { // Next node is last node
            // Arrival imminent
            directionArrow = "⏹";
            directionInstruction = "Arrive at " + currentPath[index+1].data.x + ", " + currentPath[index+1].data.z;
            return;
        }

        if (!currentPath[index+1].data.network.equals(currentPath[index].data.network)) { // Switch Network
            directionArrow = "⏹";
            directionInstruction = "Stop at " + currentPath[index+1].data.x + ", " + currentPath[index+1].data.z;
            return;
        }
        
        
        int dx = currentPath[index+1].data.x - currentPath[index].data.x;
        int dz = currentPath[index+1].data.z - currentPath[index].data.z;
        double radians = Math.atan2(dx, -dz);
        double degrees = radians * (180/Math.PI);
        double bearing = (degrees + 360) % 360;

        //Minecraft.getInstance().gui.getChat().addMessage(Component.literal(bearing + ""));

        if (
            index == 0 || 
            currentPath[index-1].id.startsWith("P") || 
            !currentPath[index].data.network.equals(currentPath[index-1].data.network)
        ) {
            if (index == 0 || !currentPath[index].data.network.equals(currentPath[index-1].data.network)) {
                directionInstruction = "Depart on " + currentPath[index].data.network + " and head ";
            } else if (currentPath[index-1].id.startsWith("P")) {
                if (index == 1) {
                    directionInstruction = "Depart from " + currentPath[index-1].data.network + ", join " + currentPath[index].data.network + " and head ";
                } else {
                    directionInstruction = "Transfer to " + currentPath[index].data.network + " at " + currentPath[index-1].data.network + " and head ";
                }
            }

            // Head <compass direction>
            if(bearing > 22.5) {
                directionInstruction += "North-East";
                directionArrow = "↗";
            } else if(bearing > 67.5) {
                directionInstruction += "East";
                directionArrow = "→";
            } else if(bearing > 112.5) {
                directionInstruction += "South-East";
                directionArrow = "↘";
            } else if(bearing > 157.5) {
                directionInstruction += "South";
                directionArrow = "↓";
            } else if(bearing > 202.5) {
                directionInstruction += "South-West";
                directionArrow = "↙";
            } else if(bearing > 247.5) {
                directionInstruction += "West";
                directionArrow = "←";
            } else if(bearing > 292.5) {
                directionInstruction += "North-West";
                directionArrow = "↖";
            } else {
                directionInstruction += "North"; 
                directionArrow = "↑";
            }
            directionInstruction += " from " + currentPath[index].data.x + ", " + currentPath[index].data.z;
            return;
        }

        // Otherwise, find what we need to do here:

        // Do all the calcs for the next one
        int dx2 = currentPath[index].data.x - currentPath[index-1].data.x;
        int dz2 = currentPath[index].data.z - currentPath[index-1].data.z;
        double radians2 = Math.atan2(dx2, -dz2);
        double degrees2 = radians2 * (180/Math.PI);
        double bearing2 = (degrees2 + 360) % 360;

        double diff = ((bearing - bearing2) + 180) % 360;
        if(diff < 0) diff += 360;
        diff -= 180; 
        double direction = Math.abs(diff);
        //Minecraft.getInstance().gui.getChat().addMessage(Component.literal(direction + ""));
        if(direction < 5) {
            directionArrow = "↑";
            directionInstruction = "Keep Ahead";
            return;
        } else if(direction > 175) {
            directionArrow = "↓";
            directionInstruction = "Make a U-Turn (WTF?)";
        } else if(direction < 45) {
            if(diff > 0) {
                directionArrow = "↗";
                directionInstruction = "Bear Right";
            } else {
                directionArrow = "↖";
                directionInstruction = "Bear Left";
            }
        } else if(direction < 120) {
            if(diff > 0) {
                directionInstruction = "Turn Right";
                directionArrow = "→";
            } else {
                directionInstruction = "Turn Left";
                directionArrow = "←";
            }
        } else {
            if(diff > 0) {
                directionInstruction = "Sharp Right";
                directionArrow = "↘";
            } else {
                directionInstruction = "Sharp Left";
                directionArrow = "↙";
            }   
        }

    }

    private void updateRoute() {
        if(currentPath == null || currentPath.length == 0 || currentStep == -1) return;
        Minecraft mc = Minecraft.getInstance();
        DirectionsNodeData node = currentPath[currentStep].data;
        if(waitingForDimensionChange) {
            getAngleChangeInPath(currentStep);
        }
        if(
            mc.player.getX() > node.x-5 && 
            mc.player.getX() < node.x+5 &&
            mc.player.getZ() > node.z-5 &&
            mc.player.getZ() < node.z+5
        ) {
            // Move to the next step
            currentStep++;


            // If next step does not exist
            if(currentStep == currentPath.length - 1) {
                // Exit directions mode
                currentPath = null;
                currentStep = 0;
                routeActive = false;
                this.chatRunner.accept(
                    Component.empty()
                    .append(Component.literal("You have arrived!").withStyle(ChatFormatting.GREEN))
                );
            } else if (currentPath[currentStep].id.startsWith("P")) {
                currentStep++;
            } else {
                this.chatRunner.accept(
                    Component.empty()
                    .append(Component.literal(directionArrow))
                    .append(Component.literal(" " + directionInstruction + " ").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)))
                    .append(Component.literal(directionArrow))
                    .append(Component.literal(" - Now"))
                );
                // Otherwise, move along to next step...
                getAngleChangeInPath(currentStep);
            }
        }
    }
    
}


