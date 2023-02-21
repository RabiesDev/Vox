package dev.rabies.vox.mixin;

import dev.rabies.vox.events.EventTiming;
import dev.rabies.vox.events.render.RenderModelEvent;
import dev.rabies.vox.events.render.RenderNameEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLivingBase.class)
public abstract class MixinRenderLivingBase<T extends EntityLivingBase> {
    @Shadow
    protected ModelBase mainModel;

    @Shadow
    protected abstract void renderLayers(T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scaleIn);

    @Inject(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V", at = @At("HEAD"), cancellable = true)
    public void renderName(T entity, double x, double y, double z, CallbackInfo ci) {
        RenderNameEvent renderNameEvent = new RenderNameEvent();
        MinecraftForge.EVENT_BUS.post(renderNameEvent);
        if (renderNameEvent.isCanceled()) ci.cancel();
    }

    @Inject(method = "renderModel", at = @At(value = "INVOKE",
    		target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V",
    		shift = At.Shift.BEFORE), cancellable = true)
    public void preRenderModel(T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float ageInTicks,
                               float netHeadYaw, float headPitch, float scaleFactor, CallbackInfo ci) {
        float partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();
        RenderModelEvent renderModelEvent = new RenderModelEvent(EventTiming.PRE, entitylivingbaseIn,
                limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor,
                event -> mainModel.render(entitylivingbaseIn, event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(),
                        event.getNetHeadYaw(), event.getHeadPitch(), event.getScaleFactor()),
                event -> renderLayers(entitylivingbaseIn, event.getLimbSwing(), event.getLimbSwingAmount(), partialTicks, event.getAgeInTicks(),
                        event.getNetHeadYaw(), event.getHeadPitch(), event.getScaleFactor()));
        MinecraftForge.EVENT_BUS.post(renderModelEvent);
        if (renderModelEvent.isCanceled())
        	ci.cancel();
    }

    @Inject(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V", shift = At.Shift.AFTER))
    public void postRenderModel(T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float ageInTicks,
                                float netHeadYaw, float headPitch, float scaleFactor, CallbackInfo ci) {
        float partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();
        RenderModelEvent renderModelEvent = new RenderModelEvent(EventTiming.POST, entitylivingbaseIn,
                limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor,
                event -> mainModel.render(entitylivingbaseIn, event.getLimbSwing(), event.getLimbSwingAmount(), event.getAgeInTicks(),
                        event.getNetHeadYaw(), event.getHeadPitch(), event.getScaleFactor()),
                event -> renderLayers(entitylivingbaseIn, event.getLimbSwing(), event.getLimbSwingAmount(), partialTicks, event.getAgeInTicks(),
                        event.getNetHeadYaw(), event.getHeadPitch(), event.getScaleFactor()));
        MinecraftForge.EVENT_BUS.post(renderModelEvent);
    }
}
