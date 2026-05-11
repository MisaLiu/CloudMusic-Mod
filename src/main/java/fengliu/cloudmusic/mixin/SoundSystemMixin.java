package fengliu.cloudmusic.mixin;

import fengliu.cloudmusic.command.MusicCommand;
import fengliu.cloudmusic.config.Configs;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在播放音乐的时候如果MC需要播放背景音乐的话
 * 就取消播放背景音乐的事件
 */
@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    public void play(SoundInstance soundInstance, CallbackInfo ci) {
        if (!Configs.PLAY.NOT_PLAY_GAME_MUSIC.getBooleanValue()){
            return;
        }

        if (!MusicCommand.getPlayer().isPlaying()) {
            return;
        }

        if (soundInstance.getCategory() == SoundCategory.MUSIC) {
            ci.cancel();
        }
    }
}
