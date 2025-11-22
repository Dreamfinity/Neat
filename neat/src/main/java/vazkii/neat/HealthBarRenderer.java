package vazkii.neat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import noppes.npcs.entity.EntityCustomNpc;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Set;

public class HealthBarRenderer {
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (NeatConfig.renderInF1 || Minecraft.isGuiEnabled()) {
            EntityLivingBase cameraEntity = mc.renderViewEntity;
            Vec3 renderingVector = cameraEntity.getPosition(event.partialTicks);
            Frustrum frustrum = new Frustrum();
            double viewX = cameraEntity.lastTickPosX + (cameraEntity.posX - cameraEntity.lastTickPosX) * (double) event.partialTicks;
            double viewY = cameraEntity.lastTickPosY + (cameraEntity.posY - cameraEntity.lastTickPosY) * (double) event.partialTicks;
            double viewZ = cameraEntity.lastTickPosZ + (cameraEntity.posZ - cameraEntity.lastTickPosZ) * (double) event.partialTicks;
            frustrum.setPosition(viewX, viewY, viewZ);
            WorldClient client = mc.theWorld;

            for (Entity entity : (Set<Entity>) client.entityList) {
                if (entity instanceof EntityLiving
                        && entity.isInRangeToRender3d(renderingVector.xCoord, renderingVector.yCoord, renderingVector.zCoord)
                        && (entity.ignoreFrustumCheck || frustrum.isBoundingBoxInFrustum(entity.boundingBox))
                        && entity.isEntityAlive()) {
                    this.renderHealthBar((EntityLiving) entity, event.partialTicks, cameraEntity);
                }
            }

        }
    }

    public void renderHealthBar(EntityLivingBase passedEntity, float partialTicks, Entity viewPoint) {
        if (passedEntity.riddenByEntity != null) {
            return;
        }

        EntityLivingBase entity = passedEntity;

        while (entity.ridingEntity instanceof EntityLivingBase) {
            entity = (EntityLivingBase) entity.ridingEntity;
        }

        if (entity instanceof EntityCustomNpc) {
            if (((EntityCustomNpc) entity).display.disableLivingAnimation) {
                return;
            }
        }

        Minecraft mc = Minecraft.getMinecraft();

        Entity riddenBy;
        for (float pastTranslate = 0.0F; entity != null; entity = (EntityLivingBase) riddenBy) {
            float distance = passedEntity.getDistanceToEntity(viewPoint);
            if (!(distance > (float) NeatConfig.maxDistance)
                    && passedEntity.canEntityBeSeen(viewPoint)
                    && !entity.isInvisible()
                    && (NeatConfig.showOnBosses || !(entity instanceof IBossDisplayData))
                    && (NeatConfig.showOnPlayers || !(entity instanceof EntityPlayer))) {
                double x = passedEntity.lastTickPosX + (passedEntity.posX - passedEntity.lastTickPosX) * (double) partialTicks;
                double y = passedEntity.lastTickPosY + (passedEntity.posY - passedEntity.lastTickPosY) * (double) partialTicks;
                double z = passedEntity.lastTickPosZ + (passedEntity.posZ - passedEntity.lastTickPosZ) * (double) partialTicks;
                float scale = 0.026666673F;
                float maxHealth = entity.getMaxHealth();
                float health = Math.min(maxHealth, entity.getHealth());
                if (!(maxHealth <= 0.0F)) {
                    float percent = (float) ((int) (health / maxHealth * 100.0F));
                    GL11.glPushMatrix();
                    GL11.glTranslatef(
                        (float) (x - RenderManager.renderPosX),
                        (float) (y - RenderManager.renderPosY + (double) passedEntity.height + NeatConfig.heightAbove),
                        (float) (z - RenderManager.renderPosZ)
                    );
                    GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
                    GL11.glScalef(-scale, -scale, scale);
                    GL11.glDisable(2896);
                    GL11.glDepthMask(false);
                    GL11.glDisable(2929);
                    GL11.glDisable(3553);
                    GL11.glEnable(3042);
                    GL11.glBlendFunc(770, 771);
                    Tessellator tessellator = Tessellator.instance;
                    float padding = (float) NeatConfig.backgroundPadding;
                    int bgHeight = NeatConfig.backgroundHeight;
                    int barHeight = NeatConfig.barHeight;
                    float size = (float) NeatConfig.plateSize;
                    int r = 0;
                    int g = 255;
                    int b = 0;
                    ItemStack stack = null;
                    if (entity instanceof IMob) {
                        r = 255;
                        g = 0;
                        EnumCreatureAttribute attr = entity.getCreatureAttribute();
                        switch (attr) {
                            case ARTHROPOD:
                                stack = new ItemStack(Items.spider_eye);
                                break;
                            case UNDEAD:
                                stack = new ItemStack(Items.rotten_flesh);
                                break;
                            default:
                                stack = new ItemStack(Items.skull, 1, 4);
                        }
                    }

                    if (entity instanceof IBossDisplayData) {
                        stack = new ItemStack(Items.skull);
                        size = (float) NeatConfig.plateSizeBoss;
                        r = 128;
                        g = 0;
                        b = 128;
                    }

                    int armor = entity.getTotalArmorValue();
                    boolean useHue = !NeatConfig.colorByType;
                    if (useHue) {
                        float hue = Math.max(0.0F, health / maxHealth / 3.0F - 0.07F);
                        Color color = Color.getHSBColor(hue, 1.0F, 1.0F);
                        r = color.getRed();
                        g = color.getGreen();
                        b = color.getBlue();
                    }

                    GL11.glTranslatef(0.0F, pastTranslate, 0.0F);
                    float s = 0.5F;
                    String name = StatCollector.translateToLocal("entity." + EntityList.getEntityString(entity) + ".name");

                    if (entity instanceof EntityCustomNpc) {
                        name = ((EntityCustomNpc) entity).display.getName();
                        String title = ((EntityCustomNpc) entity).display.title;

                        if (!((EntityCustomNpc) entity).display.title.isEmpty()) {
                            name += " – " + title;
                        }
                    } else if (entity instanceof EntityLiving && ((EntityLiving) entity).hasCustomNameTag()) {
                        name = EnumChatFormatting.ITALIC + ((EntityLiving) entity).getCustomNameTag();
                    }

                    float entityNameLength = (float) mc.fontRenderer.getStringWidth(name) * s;
                    if (entityNameLength + 20.0F > size * 2.0F) {
                        size = entityNameLength / 2.0F + 10.0F;
                    }

                    float healthSize = size * (health / maxHealth);
                    if (NeatConfig.drawBackground) {
                        tessellator.startDrawingQuads();
                        tessellator.setColorRGBA(0, 0, 0, 64);
                        tessellator.addVertex(-size - padding, -bgHeight, 0.0);
                        tessellator.addVertex(-size - padding, (float) barHeight + padding, 0.0);
                        tessellator.addVertex(size + padding, (float) barHeight + padding, 0.0);
                        tessellator.addVertex(size + padding, -bgHeight, 0.0);
                        tessellator.draw();
                    }

                    tessellator.startDrawingQuads();
                    tessellator.setColorRGBA(127, 127, 127, 127);
                    tessellator.addVertex(-size, 0.0, 0.0);
                    tessellator.addVertex(-size, barHeight, 0.0);
                    tessellator.addVertex(size, barHeight, 0.0);
                    tessellator.addVertex(size, 0.0, 0.0);
                    tessellator.draw();
                    tessellator.startDrawingQuads();
                    tessellator.setColorRGBA(r, g, b, 127);
                    tessellator.addVertex(-size, 0.0, 0.0);
                    tessellator.addVertex(-size, barHeight, 0.0);
                    tessellator.addVertex(healthSize * 2.0F - size, barHeight, 0.0);
                    tessellator.addVertex(healthSize * 2.0F - size, 0.0, 0.0);
                    tessellator.draw();
                    GL11.glEnable(3553);
                    GL11.glPushMatrix();
                    GL11.glTranslatef(-size, -4.5F, 0.0F);
                    GL11.glScalef(s, s, s);
                    mc.fontRenderer.drawString(name, 0, 0, 0xffffff);
                    GL11.glPushMatrix();
                    float s1 = 0.75F;
                    GL11.glScalef(s1, s1, s1);
                    int h = NeatConfig.hpTextHeight;
                    String maxHpStr = EnumChatFormatting.BOLD + "" + (double) Math.round((double) maxHealth * 100.0) / 100.0;
                    String hpStr = "" + (double) Math.round((double) health * 100.0) / 100.0;
                    String percStr = (int) percent + "%";
                    if (maxHpStr.endsWith(".0")) {
                        maxHpStr = maxHpStr.substring(0, maxHpStr.length() - 2);
                    }

                    if (hpStr.endsWith(".0")) {
                        hpStr = hpStr.substring(0, hpStr.length() - 2);
                    }
                    int sa = 0xffffff;
                    if (NeatConfig.showCurrentHP) {
                        mc.fontRenderer.drawString(hpStr, 2, h, 0xffffff);
                    }

                    if (NeatConfig.showMaxHP) {
                        mc.fontRenderer.drawString(maxHpStr, (int) (size / (s * s1) * 2.0F) - 2 - mc.fontRenderer.getStringWidth(maxHpStr), h, 0xffffff);
                    }

                    if (NeatConfig.showPercentage) {
                        mc.fontRenderer.drawString(percStr, (int) (size / (s * s1)) - mc.fontRenderer.getStringWidth(percStr) / 2, h, -1);
                    }

                    GL11.glPopMatrix();
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    int off = 0;
                    s1 = 0.5F;
                    GL11.glScalef(s1, s1, s1);
                    GL11.glTranslatef(size / (s * s1) * 2.0F - 16.0F, 0.0F, 0.0F);
                    mc.renderEngine.bindTexture(TextureMap.locationItemsTexture);
                    if (stack != null && NeatConfig.showAttributes) {
                        RenderItem.getInstance().renderIcon(off, 0, stack.getIconIndex(), 16, 16);
                        off -= 16;
                    }

                    if (armor > 0 && NeatConfig.showArmor) {
                        int ironArmor = armor % 5;
                        int diamondArmor = armor / 5;
                        if (!NeatConfig.groupArmor) {
                            ironArmor = armor;
                            diamondArmor = 0;
                        }

                        stack = new ItemStack(Items.iron_chestplate);

                        for (int i = 0; i < ironArmor; ++i) {
                            RenderItem.getInstance().renderIcon(off, 0, stack.getIconIndex(), 16, 16);
                            off -= 4;
                        }

                        stack = new ItemStack(Items.diamond_chestplate);

                        for (int i = 0; i < diamondArmor; ++i) {
                            RenderItem.getInstance().renderIcon(off, 0, stack.getIconIndex(), 16, 16);
                            off -= 4;
                        }
                    }

                    GL11.glPopMatrix();
                    GL11.glEnable(2929);
                    GL11.glDepthMask(true);
                    GL11.glEnable(2896);
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    GL11.glPopMatrix();
                    pastTranslate = -((float) (bgHeight + barHeight) + padding);
                }
            }

            riddenBy = entity.riddenByEntity;
            if (!(riddenBy instanceof EntityLivingBase)) {
                return;
            }
        }

    }
}
