package pwn.noobs.trouserstreak.utils;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public class PermissionUtils {

    public static int getPermissionLevel(LocalPlayer player) {
        if (player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.OWNERS))) {
            return 4;
        }
        if (player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.ADMINS))) {
            return 3;
        }
        if (player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) {
            return 2;
        }
        if (player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.MODERATORS))) {
            return 1;
        }
        if (player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.ALL))) {
            return 0;
        }
        return -1;
    }
}