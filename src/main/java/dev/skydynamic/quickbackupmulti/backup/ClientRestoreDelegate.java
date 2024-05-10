package dev.skydynamic.quickbackupmulti.backup;

import dev.skydynamic.quickbackupmulti.config.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static dev.skydynamic.quickbackupmulti.QuickBackupMulti.getDataBase;
import static dev.skydynamic.quickbackupmulti.i18n.Translate.tr;
import static dev.skydynamic.quickbackupmulti.utils.QbmManager.restoreClient;

@Environment(EnvType.CLIENT)
public class ClientRestoreDelegate {

    private final List<ServerPlayerEntity> playerList;
    private final String slot;

    public ClientRestoreDelegate(List<ServerPlayerEntity> playerList, String slot) {
        this.playerList = playerList;
        this.slot = slot;
    }

    public void run() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        minecraftClient.execute(() -> {
            minecraftClient.world.disconnect();
            minecraftClient.disconnect(new MessageScreen(Text.of("Restore backup")));
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                minecraftClient.execute(() -> minecraftClient.setScreen(null));
                restoreClient(slot);
                Config.TEMP_CONFIG.setIsBackupValue(false);
                if (minecraftClient.currentScreen instanceof TitleScreen) {
                    minecraftClient.currentScreen.children()
                        .stream()
                        .filter(it -> it instanceof ClickableWidget)
                        //#if MC>=11900
                        .filter(it -> ((ClickableWidget) it).getMessage().getContent() instanceof TranslatableTextContent)
                        //#else
                        //$$ .filter(it -> ((ClickableWidget) it).getMessage() instanceof TranslatableText)
                        //#endif
                        .filter(it -> ((TranslatableTextContent) ((ClickableWidget) it).getMessage())
                                .getKey().contentEquals("menu.singleplayer"))
                        .forEach(it -> ((ClickableWidget) it).active = true);
                }
                getDataBase().stopInternalMongoServer();
                minecraftClient.execute(() -> {
                    Text title = Text.of(tr("quickbackupmulti.toast.end_title"));
                    Text content = Text.of(tr("quickbackupmulti.toast.end_content"));
                    //#if MC>=11800
                    SystemToast.show(minecraftClient.toastManager, SystemToast.Type.PERIODIC_NOTIFICATION, title, content);
                    //#else
                    //$$ SystemToast.show(minecraftClient.toastManager, SystemToast.Type.WORLD_BACKUP, title, content);
                    //#endif
                });
            });
        });
    }
}
