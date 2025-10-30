package top.atdove.dovemail.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.atdove.dovemail.Dovemail;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Dovemail.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<top.atdove.dovemail.menu.AttachmentMenu>> ATTACHMENTS = MENUS.register(
            "attachments",
            () -> IMenuTypeExtension.create((containerId, inv, buf) -> new top.atdove.dovemail.menu.AttachmentMenu(containerId, inv, null))
    );

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
