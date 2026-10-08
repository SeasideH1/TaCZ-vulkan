/*
 * TaCZ-vulkan port: added or modified for Fabric / Minecraft 26.4 and native rendering.
 * Modified version published by SeasideH1, 2026-10-09. See NOTICE.md at repository root.
 * Existing upstream copyright and license notices remain applicable.
 */
package com.tacz.guns.client.model;

import com.mojang.blaze3d.vertex.*;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.bedrock.ModelRendererWrapper;
import com.tacz.guns.client.model.functional.BeamRenderer;
import com.tacz.guns.client.model.functional.TextShowRender;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import com.tacz.guns.client.renderer.nativeapi.NativeRenderQueue;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BedrockAttachmentModel extends BedrockAnimatedModel {
    private static final String SCOPE_VIEW_NODE = "scope_view";
    private static final String SCOPE_BODY_NODE = "scope_body";
    private static final String OCULAR_RING_NODE = "ocular_ring";
    private static final String DIVISION_NODE = "division";
    private static final String OCULAR_NODE = "ocular";
    private static final String OCULAR_SIGHT_NODE = "ocular_sight";
    private static final String OCULAR_SCOPE_NODE = "ocular_scope";
    private static final Pattern LASER_BEAM_PATTERN = Pattern.compile("^laser_beam(_(\\d+))?$");

    protected List<List<BedrockPart>> scopeViewPaths;
    protected @Nullable List<BedrockPart> scopeBodyPath;
    protected @Nullable List<BedrockPart> ocularRingPath;
    protected List<List<BedrockPart>> ocularNodePaths;
    protected List<Boolean> isScopeOcular;
    protected List<List<BedrockPart>> divisionNodePaths;
    protected @Nullable List<List<BedrockPart>> laserBeamPaths;

    private @Nullable ItemStack currentGunItem;
    private @Nullable ItemStack attachmentItem;

    private boolean isScope = false;
    private boolean isSight = false;
    private float scopeViewRadiusModifier = 1;

    public BedrockAttachmentModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
        scopeViewPaths = new ArrayList<>();
        ocularNodePaths = new ArrayList<>();
        isScopeOcular = new ArrayList<>();
        divisionNodePaths = new ArrayList<>();
        laserBeamPaths = new ArrayList<>();
        // 初始化 view 的 node path
        List<BedrockPart> path = getPath(modelMap.get(SCOPE_VIEW_NODE));
        int i = 2;
        while (path != null) {
            scopeViewPaths.add(path);
            path = getPath(modelMap.get(SCOPE_VIEW_NODE + '_' + i++));
        }
        // 初始化 ocular 的 node path
        String ocularRegex = "^(" + OCULAR_NODE + "|" + OCULAR_SIGHT_NODE + "|" + OCULAR_SCOPE_NODE + ")(_(\\d+))?$";
        Pattern ocularPattern = Pattern.compile(ocularRegex);
        TreeMap<Integer, OcularWrapper> map = new TreeMap<>();
        for (Map.Entry<String, ModelRendererWrapper> entry : modelMap.entrySet()) {
            Matcher matcher = ocularPattern.matcher(entry.getKey());
            if (matcher.matches()) {
                int num = 1;
                String numStr = matcher.group(3);
                if (numStr != null) {
                    num = Integer.parseInt(numStr);
                }
                String type = matcher.group(1);
                boolean isScope = OCULAR_SCOPE_NODE.equals(type);
                map.put(num, new OcularWrapper(entry.getValue(), isScope));
            }
            if (LASER_BEAM_PATTERN.matcher(entry.getKey()).find()) {
                laserBeamPaths.add(getPath(entry.getValue()));
            }
        }
        for (OcularWrapper wrapper : map.values()) {
            ocularNodePaths.add(getPath(wrapper.renderer));
            isScopeOcular.add(wrapper.isScope);
        }
        // 初始化 division 的 node path
        ModelRendererWrapper divisionModel = modelMap.get(DIVISION_NODE);
        path = getPath(modelMap.get(DIVISION_NODE));
        i = 2;
        while (path != null) {
            divisionNodePaths.add(path);
            divisionModel.setHidden(true);
            divisionModel = modelMap.get(DIVISION_NODE + '_' + i++);
            path = getPath(divisionModel);
        }

        scopeBodyPath = getPath(modelMap.get(SCOPE_BODY_NODE));
        ocularRingPath = getPath(modelMap.get(OCULAR_RING_NODE));
    }

    @Nullable
    public List<BedrockPart> getScopeViewPath(int viewSwitchCount) {
        if (scopeViewPaths.isEmpty()) {
            return null;
        }
        if (viewSwitchCount >= scopeViewPaths.size()) {
            return scopeViewPaths.get(0);
        }
        return scopeViewPaths.get(viewSwitchCount);
    }

    public void setIsScope(boolean isScope) {
        this.isScope = isScope;
    }

    public void setIsSight(boolean isSight) {
        this.isSight = isSight;
    }

    public boolean isScope() {
        return isScope;
    }

    public boolean isSight() {
        return isSight;
    }

    public void setScopeViewRadiusModifier(float scopeViewRadiusModifier) {
        this.scopeViewRadiusModifier = scopeViewRadiusModifier;
    }

    /**
     * 添加枪械自定义的文本显示
     */
    public void setTextShowList(Map<String, TextShow> textShowList) {
        textShowList.forEach((name, textShow) -> this.setFunctionalRenderer(name,
                bedrockPart -> new TextShowRender(this, textShow, currentGunItem)));
    }

    public void render(@Nullable ItemStack attachmentItem, ItemStack currentGunItem, PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        this.currentGunItem = currentGunItem;
        this.attachmentItem = attachmentItem;
        if (transformType.firstPerson()) {
            if (isScope && isSight) {
                renderBoth(matrixStack, transformType, renderType, light, overlay);
            } else if (isScope) {
                renderScope(matrixStack, transformType, renderType, light, overlay);
            } else if (isSight) {
                renderSight(matrixStack, transformType, renderType, light, overlay);
            }
        } else {
            if (scopeBodyPath != null) {
                renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
            }
            if (ocularRingPath != null) {
                renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
            }
        }
        if (!isScope && !isSight && laserBeamPaths != null) {
            for (var entry : laserBeamPaths) {
                BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry);
            }
        }
        super.render(matrixStack, transformType, renderType, light, overlay);
        if ((isScope || isSight) && laserBeamPaths != null) {
            for (var entry : laserBeamPaths) {
                BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry);
            }
        }
    }

    private Vector3f getBedrockPartCenter(PoseStack poseStack, @Nonnull List<BedrockPart> path) {
        poseStack.pushPose();
        for (BedrockPart part : path) part.translateAndRotateAndScale(poseStack);
        Vector3f center = new Vector3f(poseStack.last().pose().m30(), poseStack.last().pose().m31(), poseStack.last().pose().m32());
        poseStack.popPose();
        return center;
    }

    private com.tacz.guns.client.renderer.nativeapi.GeometrySnapshot capturePart(
            PoseStack pose, ItemDisplayContext context, int light, int overlay, List<BedrockPart> path) {
        pose.pushPose();
        try {
            for (int i = 0; i < path.size() - 1; i++) path.get(i).translateAndRotateAndScale(pose);
            BedrockPart part = path.get(path.size() - 1);
            var vertices = new com.tacz.guns.client.renderer.nativeapi.GeometrySnapshot.Builder();
            part.visible = true;
            try { part.render(pose, context, vertices, light, overlay); }
            finally { part.visible = false; }
            return vertices.build();
        } finally { pose.popPose(); }
    }

    private void renderTempPart(PoseStack pose, ItemDisplayContext context, RenderType material,
                                int light, int overlay, @Nonnull List<BedrockPart> path) {
        var queue = NativeRenderQueue.current();
        capturePart(pose, context, light, overlay, path).render(new PoseStack().last(), queue.getBuffer(material));
        queue.endBatch(material);
    }

    private List<NativeRenderQueue.Eye> captureEyes(PoseStack pose, ItemDisplayContext context, int light, int overlay) {
        float ads = 1;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) ads = IClientPlayerGunOperator.fromLocalPlayer(player)
                .getClientAimingProgress(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        List<NativeRenderQueue.Eye> eyes = new ArrayList<>();
        for (int i = 0; i < ocularNodePaths.size(); i++) {
            if (i > Byte.MAX_VALUE) throw new IllegalArgumentException("Index of ocular exceeds the official eight-bit range");
            var path = ocularNodePaths.get(i);
            Vector3f center = getBedrockPartCenter(pose, path);
            eyes.add(new NativeRenderQueue.Eye(i + 1, isScopeOcular.get(i), capturePart(pose, context, light, overlay, path),
                    center.x(), center.y(), center.z(), scopeViewRadiusModifier, ads));
        }
        return List.copyOf(eyes);
    }

    private void renderBoth(PoseStack pose, ItemDisplayContext context, RenderType material, int light, int overlay) {
        renderNativeScope(pose, context, material, light, overlay, true);
    }

    private void renderScope(PoseStack pose, ItemDisplayContext context, RenderType material, int light, int overlay) {
        renderNativeScope(pose, context, material, light, overlay, false);
    }

    /** Ordered scope-body/sight writes matter: a scope body changes the depth seen by later sight IDs. */
    private void renderNativeScope(PoseStack pose, ItemDisplayContext context, RenderType material, int light, int overlay, boolean combined) {
        var queue = NativeRenderQueue.current();
        queue.setMask(NativeRenderQueue.MaskRule.ALWAYS);
        if (ocularRingPath != null) renderTempPart(pose, context, material, light, overlay, ocularRingPath);
        List<NativeRenderQueue.Eye> eyes = captureEyes(pose, context, light, overlay);
        List<NativeRenderQueue.Eye> initial = eyes.stream().filter(eye -> eye.scope() == combined).toList();
        queue.writeEyes(initial, material, false, true);
        queue.setMask(NativeRenderQueue.MaskRule.equal(0));
        if (scopeBodyPath != null) renderTempPart(pose, context, material, light, overlay, scopeBodyPath);
        if (combined) queue.writeEyes(eyes.stream().filter(eye -> !eye.scope()).toList(), material, false, false);
        queue.resolveEyes(eyes, combined);
        for (int i = 0; i < eyes.size() && i < divisionNodePaths.size(); i++) {
            NativeRenderQueue.Eye eye = eyes.get(i);
            if (combined && !eye.scope()) {
                queue.setMask(NativeRenderQueue.MaskRule.equal(eye.id()));
                renderTempPart(pose, context, material, light, overlay, divisionNodePaths.get(i));
            } else {
                queue.setMask(NativeRenderQueue.MaskRule.equal(eye.id()));
                renderTempPart(pose, context, material, light, overlay, ocularNodePaths.get(i));
                queue.setMask(NativeRenderQueue.MaskRule.equal((~eye.id()) & 255));
                renderTempPart(pose, context, material, light, overlay, divisionNodePaths.get(i));
            }
        }
        queue.setMask(NativeRenderQueue.MaskRule.ALWAYS);
        super.render(pose, context, material, light, overlay);
    }

    private void renderSight(PoseStack pose, ItemDisplayContext context, RenderType material, int light, int overlay) {
        var queue = NativeRenderQueue.current();
        queue.setMask(NativeRenderQueue.MaskRule.ALWAYS);
        List<NativeRenderQueue.Eye> eyes = captureEyes(pose, context, light, overlay);
        queue.writeEyes(eyes.stream().filter(eye -> !eye.scope()).toList(), material, false, true);
        queue.setDepthTest(false);
        for (int i = 0; i < divisionNodePaths.size(); i++) {
            queue.setMask(NativeRenderQueue.MaskRule.equal(i + 1));
            renderTempPart(pose, context, material, light, overlay, divisionNodePaths.get(i));
        }
        queue.setDepthTest(true);
        queue.setMask(NativeRenderQueue.MaskRule.ALWAYS);
        if (scopeBodyPath != null) renderTempPart(pose, context, material, light, overlay, scopeBodyPath);
        super.render(pose, context, material, light, overlay);
    }

    private static class OcularWrapper{
        public ModelRendererWrapper renderer;
        public boolean isScope;

        public OcularWrapper (ModelRendererWrapper renderer, boolean isScope){
            this.renderer = renderer;
            this.isScope = isScope;
        }
    }
}
