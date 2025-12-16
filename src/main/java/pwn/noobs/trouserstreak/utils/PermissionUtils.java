package pwn.noobs.trouserstreak.utils;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;

public class PermissionUtils {

    public static int getPermissionLevel(ClientPlayerEntity player) {
        if (player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.OWNERS))) {
            return 4;
        }
        if (player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.ADMINS))) {
            return 3;
        }
        if (player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS))) {
            return 2;
        }
        if (player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.MODERATORS))) {
            return 1;
        }
        if (player.getPermissions().hasPermission(new Permission.Level(PermissionLevel.ALL))) {
            return 0;
        }
        return -1;
    }
}