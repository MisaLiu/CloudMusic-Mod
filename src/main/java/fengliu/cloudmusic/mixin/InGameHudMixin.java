package fengliu.cloudmusic.mixin;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.systems.RenderSystem;
import fengliu.cloudmusic.command.MusicCommand;
import fengliu.cloudmusic.config.Anchor;
import fengliu.cloudmusic.config.Configs;
import fengliu.cloudmusic.music163.IMusic;
import fengliu.cloudmusic.music163.data.DjMusic;
import fengliu.cloudmusic.music163.data.Music;
import fengliu.cloudmusic.render.MusicIconTexture;
import fengliu.cloudmusic.util.MusicPlayer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Unique
    private final MinecraftClient client = MinecraftClient.getInstance();

    @Inject(method = "<init>", at = @At("TAIL"))
    public void initLyricRender(MinecraftClient client, CallbackInfo ci) {
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            MusicPlayer player = MusicCommand.getPlayer();
            IMusic playingMusic = player.getPlayingMusic();
            if (playingMusic == null){
                return;
            }

            if (Configs.GUI.STOP_PLAY_SHOW_UI.getBooleanValue() && !player.isPlaying() && !player.isPaused()){
                return;
            }

            float lyricScale = (float) Configs.GUI.LYRIC_SCALE.getDoubleValue();
            int offsetY = Configs.GUI.LYRIC_Y.getIntegerValue();
            int offsetX = Configs.GUI.LYRIC_X.getIntegerValue();
            int lyricColor = Configs.GUI.LYRIC_COLOR.getIntegerValue();
            String[] lines = MusicCommand.getPlayer().getLyric();

            float lyricWidth = 0;
            for (String line : lines) {
                float w = client.textRenderer.getWidth(line);
                if (w > lyricWidth) lyricWidth = w;
            }
            float lyricHeight = lines.length * 10f;
            int screenW = client.getWindow().getScaledWidth();
            int screenH = client.getWindow().getScaledHeight();
            Anchor anchor = (Anchor) Configs.GUI.LYRIC_ANCHOR.getOptionListValue();
            float px = Anchor.positionX(anchor, screenW, lyricWidth, offsetX);
            float py = Anchor.positionY(anchor, screenH, lyricHeight, offsetY);
            float ax = Anchor.anchorX(anchor, lyricWidth, px);
            float ay = Anchor.anchorY(anchor, lyricHeight, py);

            MatrixStack matrices = drawContext.getMatrices();
            matrices.push();
            matrices.translate(ax, ay, 0);
            matrices.scale(lyricScale, lyricScale, 1.0f);
            matrices.translate(-ax, -ay, 0);
            float ly = py;
            for (String lyric : lines) {
                drawContext.drawText(client.textRenderer, lyric, (int) px, (int) ly, lyricColor, true);
                ly += 10;
            }
            matrices.pop();
        });
    }

    @Unique
    public void renderLoginQrCode(DrawContext context) {
        if (!MusicCommand.loadQRCode) {
            return;
        }
        Function<Identifier, RenderLayer> renderLayer = RenderLayer::getGuiTexturedOverlay;
        context.drawTexture(renderLayer, MusicIconTexture.QR_CODE_ID, 5, 10, 1f, 1f, 64, 64, 64, 64);
    }

    @Unique
    public int[] getMusicInfoPos() {
        int y = Configs.GUI.MUSIC_INFO_Y.getIntegerValue();
        int x = Configs.GUI.MUSIC_INFO_X.getIntegerValue();
        if (this.client.player == null || !Configs.GUI.MUSIC_INFO_EFFECT_OFFSET.getBooleanValue()) {
            return new int[]{y, x};
        }

        int offset = 0;
        for (StatusEffectInstance statusEffect : this.client.player.getStatusEffects()) {
            if (statusEffect.getEffectType().value().isBeneficial()) {
                offset = 1;
            } else {
                offset = 2;
                break;
            }
        }

        if (offset == 0) {
            return new int[]{y, x};
        }
        return new int[]{y + Configs.GUI.MUSIC_INFO_EFFECT_OFFSET_Y.getIntegerValue() * offset, x + Configs.GUI.MUSIC_INFO_EFFECT_OFFSET_X.getIntegerValue()};
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        this.renderLoginQrCode(context);

        MusicPlayer player = MusicCommand.getPlayer();
        IMusic playingMusic = player.getPlayingMusic();

        if (playingMusic == null) {
            return;
        }

        if (Configs.GUI.STOP_PLAY_SHOW_UI.getBooleanValue() && !player.isPlaying() && !player.isPaused()){
            return;
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (!Configs.GUI.MUSIC_INFO.getBooleanValue()) {
            return;
        }

        int screenW = this.client.getWindow().getScaledWidth();
        int screenH = this.client.getWindow().getScaledHeight();

        int[] pos = this.getMusicInfoPos();
        int y = pos[0];
        int x = pos[1];

        float infoScale = (float) Configs.GUI.MUSIC_INFO_SCALE.getDoubleValue();
        Anchor infoAnchor = (Anchor) Configs.GUI.MUSIC_INFO_ANCHOR.getOptionListValue();
        int panelLeft = (int) Anchor.positionX(infoAnchor, screenW, 175, x);
        int panelTop = (int) Anchor.positionY(infoAnchor, screenH, 48, y);
        float ax = Anchor.anchorX(infoAnchor, 175f, panelLeft);
        float ay = Anchor.anchorY(infoAnchor, 48f, panelTop);

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(ax, ay, 0);
        matrices.scale(infoScale, infoScale, 1.0f);
        matrices.translate(-ax, -ay, 0);

        context.fill(panelLeft, panelTop, panelLeft + 175, panelTop + 48, Configs.GUI.MUSIC_INFO_COLOR.getIntegerValue());
        context.fill(panelLeft + 30, panelTop + 40, panelLeft + 145, panelTop + 43, Configs.GUI.MUSIC_PROGRESS_BAR_COLOR.getIntegerValue());
        int progress = Math.round((115 / (float) playingMusic.getDurationSecond()) * player.getPlayingProgressSecond());
        if (progress > 115) {
            progress = 115;
        }
        Function<Identifier, RenderLayer> renderLayer = RenderLayer::getGuiTexturedOverlay;
        context.fill(panelLeft + 30, panelTop + 40, panelLeft + 30 + progress, panelTop + 43, Configs.GUI.MUSIC_PLAYED_PROGRESS_BAR_COLOR.getIntegerValue());
        context.drawTexture(renderLayer, MusicIconTexture.MUSIC_ICON_ID, panelLeft + 3, panelTop + 2, 32f, 32f, 32, 32, 32, 32);
        context.drawText(this.client.textRenderer, playingMusic.getName().length() > 16 ? playingMusic.getName().substring(0, 16) + "..." : playingMusic.getName(), panelLeft + 40, panelTop + 4, Configs.GUI.MUSIC_INFO_TITLE_FONT_COLOR.getIntegerValue(), true);

        int progressFontColor = Configs.GUI.MUSIC_PROGRESS_FONT_COLOR.getIntegerValue();
        context.drawText(this.client.textRenderer, player.getPlayingProgressToString(), panelLeft + 3, panelTop + 38, progressFontColor, true);
        context.drawText(this.client.textRenderer, playingMusic.getDurationToString(), panelLeft + 147, panelTop + 38, progressFontColor, true);
        int musicFontColor = Configs.GUI.MUSIC_INFO_FONT_COLOR.getIntegerValue();
        if (playingMusic instanceof DjMusic music) {
            context.drawText(this.client.textRenderer, Text.translatable("cloudmusic.info.dj.creator", music.dj.get("nickname").getAsString()), panelLeft + 40, panelTop + 14, musicFontColor, true);
            context.drawText(this.client.textRenderer, Text.translatable("cloudmusic.info.dj.music.count", music.listenerCount, music.likedCount), panelLeft + 40, panelTop + 24, musicFontColor, true);
            matrices.pop();
            return;
        }

        Music music = (Music) playingMusic;
        if (!music.aliasName.isEmpty()) {
            context.drawText(this.client.textRenderer, music.aliasName.length() > 16 ? music.aliasName.substring(0, 16) + "..." : music.aliasName, panelLeft + 40, panelTop + 14, musicFontColor, true);
        } else {
            String album = music.album.get("name").getAsString();
            context.drawText(this.client.textRenderer, album.length() > 16 ? album.substring(0, 16) + "..." : album, panelLeft + 40, panelTop + 14, musicFontColor, true);
        }

        StringBuilder artist = new StringBuilder();
        for (JsonElement artistData : music.artists.asList()) {
            artist.append(artistData.getAsJsonObject().get("name").getAsString()).append("/");
        }
        artist = new StringBuilder(artist.substring(0, artist.length() - 1));
        context.drawText(this.client.textRenderer, artist.length() > 16 ? artist.substring(0, 16) + "..." : artist.toString(), panelLeft + 40, panelTop + 24, musicFontColor, true);

        if (music.freeTrialInfo != null) {
            int freeTrialEndProgress = Math.round((115 / (float) playingMusic.getDurationSecond()) * music.freeTrialInfo.get("end").getAsInt());
            context.fill(panelLeft + 30 + freeTrialEndProgress - 1, panelTop + 40, panelLeft + 30 + freeTrialEndProgress + 1, panelTop + 43, Configs.GUI.MUSIC_PLAYED_PROGRESS_BAR_COLOR.getIntegerValue());
        }

        matrices.pop();
    }

}
