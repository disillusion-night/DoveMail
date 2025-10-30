package top.atdove.dovemail.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;

import java.util.*;

public final class AttachmentManager {
    private static final Map<UUID, SimpleContainer> STORE = new java.util.concurrent.ConcurrentHashMap<>();
    public static final int SLOT_COUNT = 9;

    private AttachmentManager() {}

    public static SimpleContainer get(ServerPlayer player) {
        return STORE.computeIfAbsent(player.getUUID(), id -> new SimpleContainer(SLOT_COUNT));
    }

    public static java.util.List<net.minecraft.world.item.ItemStack> consume(ServerPlayer player) {
        SimpleContainer c = get(player);
        java.util.List<net.minecraft.world.item.ItemStack> list = new java.util.ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < c.getContainerSize(); i++) {
            var stack = c.getItem(i);
            if (!stack.isEmpty()) {
                list.add(stack.copy());
                c.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
        c.setChanged();
        return list;
    }
}
