package gg.shard.client.mixin;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.chat.ChatModule;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Chat: hide, stack and decorate incoming lines before vanilla stores them. */
@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {
    @Shadow @Final private List<GuiMessage> allMessages;

    @Shadow
    protected abstract void refreshTrimmedMessages();

    @Unique
    private boolean shard$inner;

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void shard$hide(Component message, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
        if (ShardClient.isReady() && ShardClient.modules().get(ChatModule.class).hide(message)) ci.cancel();
    }

    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private Component shard$decorate(Component message) {
        if (!ShardClient.isReady() || shard$inner) return message;
        ChatModule chat = ShardClient.modules().get(ChatModule.class);
        int count = chat.repeatCount(message);
        if (count > 1 && !allMessages.isEmpty()) {
            // Replace the previous copy with this one plus its count.
            allMessages.remove(0);
            refreshTrimmedMessages();
        }
        return chat.decorate(message, count);
    }
}
