package mchorse.emoticons.client.gui;

import java.io.IOException;

import mchorse.emoticons.Emoticons;
import mchorse.emoticons.api.animation.model.AnimatorEmoticonsController;
import mchorse.emoticons.capabilities.cosmetic.Cosmetic;
import mchorse.emoticons.client.WheelMenuController;
import mchorse.emoticons.common.emotes.Emote;
import mchorse.emoticons.common.emotes.Emotes;
import mchorse.emoticons.skin_n_bones.api.animation.model.ActionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class GuiEmoteWheel extends GuiScreen
{
    private static final ResourceLocation CENTER_TEXTURE = new ResourceLocation(
        Emoticons.MOD_ID,
        "textures/gui/wheel/center.png"
    );
    private static final ResourceLocation[] SEGMENT_ICONS_GRAY = new ResourceLocation[] {
        icon("yes_gray"),
        icon("no_gray"),
        icon("clap_gray"),
        icon("laugh_gray"),
        icon("think_gray"),
        icon("threat_gray"),
        icon("cry_gray"),
        icon("greet_gray")
    };
    private static final ResourceLocation[] SEGMENT_ICONS_WHITE = new ResourceLocation[] {
        icon("yes_white"),
        icon("no_white"),
        icon("clap_white"),
        icon("laugh_white"),
        icon("think_white"),
        icon("threat_white"),
        icon("cry_white"),
        icon("greet_white")
    };
    private static final ResourceLocation SCROLL_SOUND = new ResourceLocation(Emoticons.MOD_ID, "wheel_scroll");
    private static final int SEGMENT_COUNT = 8;
    private static final long CLICK_ANIMATION_MS = 140L;
    private static final float CENTER_TEXTURE_SIZE = 1224.0F;
    private static final float CENTER_LABEL_SCALE = 3.60F;
    private static final float CURRENT_EMOTE_SCALE = 4.60F;
    private static final float COOLDOWN_LABEL_SCALE = 2.70F;
    private static final float ICON_SIZE_RATIO = 0.38F;
    private static final float ICON_RADIUS_RATIO = 0.60F;
    private static final float PREVIEW_SCALE_RATIO = 0.56F;
    private static final float PREVIEW_Y_RATIO = 0.39F;
    private static final float PREVIEW_PITCH = 10.0F;
    private static final float PREVIEW_YAW = 0.0F;
    private static final float REFERENCE_DISPLAY_WIDTH = 2560.0F;
    private static final float REFERENCE_DISPLAY_HEIGHT = 1440.0F;
    private static final float SEGMENT_ANGLE_STEP = 45.0F;
    private static final float SEGMENT_GAP_DEGREES = 4.0F;
    private static final float SEGMENT_SWEEP_DEGREES = SEGMENT_ANGLE_STEP - SEGMENT_GAP_DEGREES;
    private static final float FIRST_SEGMENT_CENTER_ANGLE = -112.5F;
    private static final float OUTER_RADIUS_RATIO = 0.377F;
    private static final float INNER_RADIUS_RATIO = 0.565F;

    private static final int SEGMENT_INNER_COLOR = 0xFF02050E;
    private static final int SEGMENT_OUTER_COLOR = 0xFF172547;
    private static final int HOVER_INNER_COLOR = 0xFF071326;
    private static final int HOVER_OUTER_COLOR = 0xFF274579;
    private static final int OUTLINE_COLOR = 0xFF7A8091;

    private long openedAt;
    private int hoveredSegment = -1;
    private int selectedSegment = -1;
    private int previewSegment = -1;
    private long selectedAt;
    private AnimatorEmoticonsController previewController;

    @Override
    public void initGui()
    {
        this.openedAt = Minecraft.getSystemTime();
        this.hoveredSegment = -1;
        this.selectedSegment = -1;
        this.previewSegment = -1;
        this.selectedAt = 0L;
        this.setupPreviewController();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        long currentTime = Minecraft.getSystemTime();

        this.updateSelection(currentTime);

        if (this.mc == null || this.mc.currentScreen != this)
        {
            return;
        }

        float animationProgress = Math.min(
            1.0F,
            (currentTime - this.openedAt + partialTicks * 16.0F) / 180.0F
        );
        float easedProgress = this.easeOutCubic(animationProgress);
        float openScale = 0.84F + 0.16F * easedProgress;
        float outerRadius = Math.min(this.width, this.height) * OUTER_RADIUS_RATIO * openScale;
        float innerRadius = outerRadius * INNER_RADIUS_RATIO;
        float centerX = this.width / 2.0F;
        float centerY = this.height / 2.0F;
        float selectionProgress = this.getSelectionProgress(currentTime, partialTicks);

        if (this.selectedSegment >= 0)
        {
            this.hoveredSegment = this.selectedSegment;
        }
        else
        {
            int previousHoveredSegment = this.hoveredSegment;

            this.hoveredSegment = this.getSegmentAt(mouseX, mouseY, centerX, centerY, innerRadius, outerRadius);

            if (this.hoveredSegment != previousHoveredSegment && this.hoveredSegment >= 0)
            {
                this.playScrollSound();
            }
        }

        int previewSegment = this.selectedSegment >= 0 ? this.selectedSegment : this.hoveredSegment;

        this.syncPreview(previewSegment);

        this.drawWheel(centerX, centerY, innerRadius, outerRadius, easedProgress, selectionProgress);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();

        if (this.previewSegment >= 0 && this.previewController != null && this.mc != null && this.mc.player != null)
        {
            this.previewController.update(this.mc.player);
            this.clearPreviewHeldItem();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (mouseButton == 0 && this.hoveredSegment >= 0 && this.selectedSegment < 0)
        {
            this.beginSelection(this.hoveredSegment);

            return;
        }

        if (mouseButton == 1 && this.selectedSegment < 0)
        {
            this.mc.displayGuiScreen(null);

            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            this.mc.displayGuiScreen(null);

            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }

    private void drawWheel(
        float centerX,
        float centerY,
        float innerRadius,
        float outerRadius,
        float animationProgress,
        float selectionProgress
    )
    {
        float renderAlpha = 0.50F + 0.50F * animationProgress;
        boolean selectionActive = this.selectedSegment >= 0;
        int activeSegment = selectionActive ? this.selectedSegment : this.hoveredSegment;
        boolean dimInactiveSegments = activeSegment >= 0;
        float inactiveAlpha = renderAlpha * (dimInactiveSegments ? 0.30F : 1.0F);

        this.drawCenterTexture(centerX, centerY, outerRadius * 1.10F, 0.45F + 0.55F * animationProgress);
        this.drawCenterPreview(centerX, centerY, innerRadius, activeSegment);
        this.beginShapeRendering();

        for (int index = 0; index < SEGMENT_COUNT; index++)
        {
            if (index == activeSegment)
            {
                continue;
            }

            this.drawSegment(centerX, centerY, innerRadius, outerRadius, index, inactiveAlpha, false);
        }

        if (activeSegment >= 0)
        {
            float highlightInnerRadius = selectionActive
                ? innerRadius * (0.982F - 0.02F * selectionProgress)
                : innerRadius * 0.985F;
            float highlightOuterRadius = selectionActive
                ? outerRadius * (1.02F + 0.055F * selectionProgress)
                : outerRadius * 1.03F;
            float highlightAlpha = selectionActive
                ? 0.90F + 0.10F * selectionProgress
                : 0.85F + 0.15F * animationProgress;

            this.drawSegment(
                centerX,
                centerY,
                highlightInnerRadius,
                highlightOuterRadius,
                activeSegment,
                highlightAlpha,
                true
            );
        }

        this.endShapeRendering();
        this.drawIcons(centerX, centerY, innerRadius, outerRadius, renderAlpha, activeSegment);
        this.drawLabels(centerX, centerY, innerRadius, outerRadius, renderAlpha);
    }

    private void drawCenterTexture(float centerX, float centerY, float outerRadius, float alpha)
    {
        float scale = outerRadius * 2.0F / CENTER_TEXTURE_SIZE;

        this.mc.getTextureManager().bindTexture(CENTER_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        GlStateManager.translate(centerX, centerY, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        this.drawModalRectWithCustomSizedTexture(
            Math.round(-CENTER_TEXTURE_SIZE / 2.0F),
            Math.round(-CENTER_TEXTURE_SIZE / 2.0F),
            0.0F,
            0.0F,
            Math.round(CENTER_TEXTURE_SIZE),
            Math.round(CENTER_TEXTURE_SIZE),
            CENTER_TEXTURE_SIZE,
            CENTER_TEXTURE_SIZE
        );
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void drawCenterPreview(float centerX, float centerY, float innerRadius, int activeSegment)
    {
        if (activeSegment < 0 || this.previewController == null || this.mc == null || this.mc.player == null)
        {
            return;
        }

        int previewX = Math.round(centerX);
        int previewY = Math.round(centerY + innerRadius * PREVIEW_Y_RATIO);
        float previewScale = innerRadius * PREVIEW_SCALE_RATIO;

        this.previewController.renderOnScreen(
            this.mc.player,
            previewX,
            previewY,
            previewScale,
            1.0F,
            PREVIEW_PITCH,
            PREVIEW_YAW
        );
    }

    private void drawSegment(
        float centerX,
        float centerY,
        float innerRadius,
        float outerRadius,
        int index,
        float alphaScale,
        boolean hovered
    )
    {
        float centerAngle = FIRST_SEGMENT_CENTER_ANGLE + index * SEGMENT_ANGLE_STEP;
        float startAngle = centerAngle - SEGMENT_SWEEP_DEGREES / 2.0F;
        float endAngle = centerAngle + SEGMENT_SWEEP_DEGREES / 2.0F;
        int innerColor = this.scaleAlpha(hovered ? HOVER_INNER_COLOR : SEGMENT_INNER_COLOR, alphaScale);
        int outerColor = this.scaleAlpha(hovered ? HOVER_OUTER_COLOR : SEGMENT_OUTER_COLOR, alphaScale);
        int outlineColor = this.scaleAlpha(OUTLINE_COLOR, hovered ? 0.72F : 0.58F);

        this.drawAnnularSector(centerX, centerY, innerRadius, outerRadius, startAngle, endAngle, innerColor, outerColor);
        this.drawSectorOutline(centerX, centerY, innerRadius, outerRadius, startAngle, endAngle, outlineColor);
    }

    private void drawIcons(
        float centerX,
        float centerY,
        float innerRadius,
        float outerRadius,
        float alphaScale,
        int activeSegment
    )
    {
        float iconRadius = innerRadius + (outerRadius - innerRadius) * ICON_RADIUS_RATIO;
        float iconSize = (outerRadius - innerRadius) * ICON_SIZE_RATIO;

        for (int index = 0; index < SEGMENT_COUNT; index++)
        {
            float angle = FIRST_SEGMENT_CENTER_ANGLE + index * SEGMENT_ANGLE_STEP;
            double radians = Math.toRadians(angle);
            float iconX = centerX + (float) Math.cos(radians) * iconRadius;
            float iconY = centerY + (float) Math.sin(radians) * iconRadius;
            boolean highlighted = index == activeSegment;
            ResourceLocation texture = highlighted ? SEGMENT_ICONS_WHITE[index] : SEGMENT_ICONS_GRAY[index];
            float alpha = alphaScale * (highlighted ? 1.0F : 0.86F);

            this.drawCenteredIcon(texture, iconX, iconY, iconSize, alpha);
        }
    }

    private void drawLabels(float centerX, float centerY, float innerRadius, float outerRadius, float alphaScale)
    {
        int activeSegment = this.selectedSegment >= 0 ? this.selectedSegment : this.hoveredSegment;
        boolean cooldownActive = this.hasCooldown();
        String currentEmoteLabel = this.getCurrentEmoteLabel(activeSegment, false);
        int textColor = this.scaleAlpha(0xFFFFFFFF, alphaScale);
        float centerTextWidth = innerRadius * 4.40F;
        float smallTextWidth = innerRadius * 5.00F;

        if (currentEmoteLabel == null)
        {
            this.drawCenteredScaledText(
                WheelMenuController.CENTER_LABEL,
                centerX,
                centerY,
                CENTER_LABEL_SCALE,
                textColor,
                centerTextWidth
            );
        }

        if (currentEmoteLabel != null)
        {
            this.drawCenteredScaledText(
                currentEmoteLabel,
                centerX,
                centerY + this.getUiIndependentOffset(this.fontRenderer.FONT_HEIGHT * 17.3F),
                CURRENT_EMOTE_SCALE,
                this.scaleAlpha(0xFFE4ECFF, alphaScale),
                smallTextWidth
            );
        }

        if (cooldownActive)
        {
            this.drawCooldownLabel(
                centerX,
                centerY + this.getUiIndependentOffset(this.fontRenderer.FONT_HEIGHT * 25.0F),
                COOLDOWN_LABEL_SCALE,
                alphaScale,
                smallTextWidth
            );
        }
    }

    private void drawCenteredIcon(ResourceLocation texture, float centerX, float centerY, float size, float alpha)
    {
        this.mc.getTextureManager().bindTexture(texture);
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        GlStateManager.translate(centerX, centerY, 0.0F);
        GlStateManager.scale(size / 100.0F, size / 100.0F, 1.0F);

        this.drawModalRectWithCustomSizedTexture(-50, -50, 0.0F, 0.0F, 100, 100, 100.0F, 100.0F);

        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void drawCenteredScaledText(String text, float centerX, float centerY, float scale, int color, float maxWidth)
    {
        float adjustedScale = this.getAutoTextScale(scale, text, maxWidth);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.translate(centerX, centerY, 0.0F);
        GlStateManager.scale(adjustedScale, adjustedScale, 1.0F);

        int drawX = Math.round(-this.fontRenderer.getStringWidth(text) / 2.0F);
        int drawY = Math.round(-this.fontRenderer.FONT_HEIGHT / 2.0F);

        this.fontRenderer.drawStringWithShadow(text, drawX, drawY, color);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawCooldownLabel(float centerX, float centerY, float scale, float alphaScale, float maxWidth)
    {
        long remainingSeconds = WheelMenuController.getRemainingCooldownSeconds();

        if (remainingSeconds <= 0L)
        {
            return;
        }

        String prefix = "\u0421\u043b\u0435\u0434\u0443\u044e\u0449\u0430\u044f \u044d\u043c\u043e\u0446\u0438\u044f: ";
        String suffix = remainingSeconds + " \u0441\u0435\u043a.";
        int prefixColor = this.scaleAlpha(0xFFD5DDF0, alphaScale);
        int suffixColor = this.scaleAlpha(0xFFFF4D4D, alphaScale);
        int prefixWidth = this.fontRenderer.getStringWidth(prefix);
        int suffixWidth = this.fontRenderer.getStringWidth(suffix);
        float adjustedScale = this.getAutoTextScale(scale, prefix + suffix, maxWidth);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.translate(centerX, centerY, 0.0F);
        GlStateManager.scale(adjustedScale, adjustedScale, 1.0F);

        int drawX = Math.round(-(prefixWidth + suffixWidth) / 2.0F);
        int drawY = Math.round(-this.fontRenderer.FONT_HEIGHT / 2.0F);

        this.fontRenderer.drawStringWithShadow(prefix, drawX, drawY, prefixColor);
        this.fontRenderer.drawStringWithShadow(suffix, drawX + prefixWidth, drawY, suffixColor);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private String getCurrentEmoteLabel(int activeSegment, boolean showLastActivated)
    {
        if (activeSegment >= 0 && activeSegment < WheelMenuController.DISPLAY_LABELS.length)
        {
            return WheelMenuController.DISPLAY_LABELS[activeSegment];
        }

        if (!showLastActivated)
        {
            return null;
        }

        int lastActivatedSegment = WheelMenuController.getLastActivatedSegment();

        if (lastActivatedSegment < 0 || lastActivatedSegment >= WheelMenuController.DISPLAY_LABELS.length)
        {
            return null;
        }

        return WheelMenuController.DISPLAY_LABELS[lastActivatedSegment];
    }

    private String getCooldownLabel()
    {
        return this.hasCooldown() ? "cooldown" : null;
    }

    private boolean hasCooldown()
    {
        return WheelMenuController.getRemainingCooldownSeconds() > 0L;
    }

    private float getAutoTextScale(float baseScale, String text, float maxWidth)
    {
        float scale = this.getUiIndependentScale(baseScale) * this.getResolutionLayoutFactor();
        int width = this.fontRenderer.getStringWidth(text);

        if (width <= 0 || maxWidth <= 0.0F)
        {
            return scale;
        }

        return Math.min(scale, maxWidth / width);
    }

    private float getUiIndependentScale(float baseScale)
    {
        return baseScale / this.getGuiScaleFactor();
    }

    private float getUiIndependentOffset(float offset)
    {
        return offset / this.getGuiScaleFactor() * this.getResolutionLayoutFactor();
    }

    private float getGuiScaleFactor()
    {
        if (this.mc == null)
        {
            return 1.0F;
        }

        return new ScaledResolution(this.mc).getScaleFactor();
    }

    private float getResolutionLayoutFactor()
    {
        if (this.mc == null || this.mc.displayWidth <= 0 || this.mc.displayHeight <= 0)
        {
            return 1.0F;
        }

        float widthFactor = this.mc.displayWidth / REFERENCE_DISPLAY_WIDTH;
        float heightFactor = this.mc.displayHeight / REFERENCE_DISPLAY_HEIGHT;

        return Math.min(1.0F, Math.min(widthFactor, heightFactor));
    }

    private static ResourceLocation icon(String name)
    {
        return new ResourceLocation(Emoticons.MOD_ID, "textures/gui/wheel/icons/" + name + ".png");
    }

    private int getSegmentAt(
        int mouseX,
        int mouseY,
        float centerX,
        float centerY,
        float innerRadius,
        float outerRadius
    )
    {
        float dx = mouseX - centerX;
        float dy = mouseY - centerY;
        double distance = Math.hypot(dx, dy);

        if (distance < innerRadius || distance > outerRadius)
        {
            return -1;
        }

        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

        for (int index = 0; index < SEGMENT_COUNT; index++)
        {
            float centerAngle = FIRST_SEGMENT_CENTER_ANGLE + index * SEGMENT_ANGLE_STEP;
            float delta = this.wrapDegrees(angle - centerAngle);

            if (Math.abs(delta) <= SEGMENT_SWEEP_DEGREES / 2.0F)
            {
                return index;
            }
        }

        return -1;
    }

    private void beginSelection(int segmentIndex)
    {
        this.selectedSegment = segmentIndex;
        this.selectedAt = Minecraft.getSystemTime();
        this.hoveredSegment = segmentIndex;
        this.playClickSound();
    }

    private void updateSelection(long currentTime)
    {
        if (this.selectedSegment < 0 || currentTime - this.selectedAt < CLICK_ANIMATION_MS)
        {
            return;
        }

        int segmentIndex = this.selectedSegment;

        this.selectedSegment = -1;
        this.selectedAt = 0L;

        if (WheelMenuController.activateSegment(segmentIndex))
        {
            this.mc.displayGuiScreen(null);
        }
    }

    private void setupPreviewController()
    {
        this.previewController = null;

        if (this.mc == null || this.mc.player == null)
        {
            return;
        }

        Cosmetic cap = (Cosmetic) Cosmetic.get(this.mc.player);

        if (cap == null)
        {
            return;
        }

        if (cap.animator == null)
        {
            cap.setupAnimator(this.mc.player);
        }

        if (cap.animator == null)
        {
            return;
        }

        AnimatorEmoticonsController controller = new AnimatorEmoticonsController(
            cap.animator.animationName,
            cap.animator.userData.copy()
        );

        controller.fetchAnimation();
        controller.userConfig.copy(cap.animator.userConfig);
        controller.userConfig.renderHeldItems = false;
        controller.itemSlot = ItemStack.EMPTY;
        controller.itemSlotScale = 0.0F;

        this.previewController = controller;
    }

    private void syncPreview(int segmentIndex)
    {
        if (this.previewController == null || segmentIndex == this.previewSegment)
        {
            return;
        }

        this.previewSegment = segmentIndex;

        if (segmentIndex < 0 || segmentIndex >= WheelMenuController.EMOTE_KEYS.length)
        {
            this.previewController.setEmote(null);

            return;
        }

        String key = WheelMenuController.EMOTE_KEYS[segmentIndex];
        Emote emote = Emotes.get(key);
        ActionConfig config = this.previewController.userConfig.actions.getConfig("emote_" + key);

        if (emote == null || this.previewController.animation == null || config == null)
        {
            this.previewController.setEmote(null);

            return;
        }

        this.previewController.setEmote(
            this.previewController.animation.createAction(
                null,
                config,
                true
            )
        );
        this.clearPreviewHeldItem();
    }

    private void clearPreviewHeldItem()
    {
        if (this.previewController == null)
        {
            return;
        }

        this.previewController.userConfig.renderHeldItems = false;
        this.previewController.itemSlot = ItemStack.EMPTY;
        this.previewController.itemSlotScale = 0.0F;
    }

    private void playScrollSound()
    {
        this.playUiSound(0.55F, 1.0F);
    }

    private void playClickSound()
    {
        this.playUiSound(0.75F, 0.85F);
    }

    private void playUiSound(float volume, float pitch)
    {
        if (this.mc == null || this.mc.getSoundHandler() == null)
        {
            return;
        }

        this.mc.getSoundHandler().playSound(
            new PositionedSoundRecord(
                SCROLL_SOUND,
                SoundCategory.MASTER,
                volume,
                pitch,
                false,
                0,
                ISound.AttenuationType.NONE,
                0.0F,
                0.0F,
                0.0F
            )
        );
    }

    private void beginShapeRendering()
    {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
    }

    private void endShapeRendering()
    {
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void drawAnnularSector(
        float centerX,
        float centerY,
        float innerRadius,
        float outerRadius,
        float startAngle,
        float endAngle,
        int innerColor,
        int outerColor
    )
    {
        int steps = Math.max(18, (int) Math.ceil((endAngle - startAngle) / 2.0F));
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (int step = 0; step <= steps; step++)
        {
            float progress = step / (float) steps;
            float angle = startAngle + (endAngle - startAngle) * progress;
            double radians = Math.toRadians(angle);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);

            this.addColoredVertex(buffer, centerX + outerRadius * cos, centerY + outerRadius * sin, outerColor);
            this.addColoredVertex(buffer, centerX + innerRadius * cos, centerY + innerRadius * sin, innerColor);
        }

        tessellator.draw();
    }

    private void drawSectorOutline(
        float centerX,
        float centerY,
        float innerRadius,
        float outerRadius,
        float startAngle,
        float endAngle,
        int color
    )
    {
        int steps = Math.max(20, (int) Math.ceil((endAngle - startAngle) / 2.0F));
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.glLineWidth(1.0F);
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (int step = 0; step <= steps; step++)
        {
            float progress = step / (float) steps;
            float angle = startAngle + (endAngle - startAngle) * progress;
            double radians = Math.toRadians(angle);

            this.addColoredVertex(
                buffer,
                centerX + outerRadius * Math.cos(radians),
                centerY + outerRadius * Math.sin(radians),
                color
            );
        }

        this.addColoredVertex(
            buffer,
            centerX + innerRadius * Math.cos(Math.toRadians(endAngle)),
            centerY + innerRadius * Math.sin(Math.toRadians(endAngle)),
            color
        );

        for (int step = steps; step >= 0; step--)
        {
            float progress = step / (float) steps;
            float angle = startAngle + (endAngle - startAngle) * progress;
            double radians = Math.toRadians(angle);

            this.addColoredVertex(
                buffer,
                centerX + innerRadius * Math.cos(radians),
                centerY + innerRadius * Math.sin(radians),
                color
            );
        }

        this.addColoredVertex(
            buffer,
            centerX + outerRadius * Math.cos(Math.toRadians(startAngle)),
            centerY + outerRadius * Math.sin(Math.toRadians(startAngle)),
            color
        );
        tessellator.draw();
    }

    private void addColoredVertex(BufferBuilder buffer, double x, double y, int argb)
    {
        int alpha = argb >>> 24 & 255;
        int red = argb >>> 16 & 255;
        int green = argb >>> 8 & 255;
        int blue = argb & 255;

        buffer.pos(x, y, 0.0D).color(red, green, blue, alpha).endVertex();
    }

    private int scaleAlpha(int argb, float alphaScale)
    {
        int alpha = Math.min(255, Math.max(0, Math.round((argb >>> 24 & 255) * alphaScale)));

        return alpha << 24 | argb & 0xFFFFFF;
    }

    private float wrapDegrees(float degrees)
    {
        while (degrees <= -180.0F)
        {
            degrees += 360.0F;
        }

        while (degrees > 180.0F)
        {
            degrees -= 360.0F;
        }

        return degrees;
    }

    private float getSelectionProgress(long currentTime, float partialTicks)
    {
        if (this.selectedSegment < 0)
        {
            return 0.0F;
        }

        float progress = (currentTime - this.selectedAt + partialTicks * 16.0F) / (float) CLICK_ANIMATION_MS;

        return this.easeOutCubic(Math.min(1.0F, progress));
    }

    private float easeOutCubic(float progress)
    {
        float inverted = 1.0F - progress;

        return 1.0F - inverted * inverted * inverted;
    }
}
