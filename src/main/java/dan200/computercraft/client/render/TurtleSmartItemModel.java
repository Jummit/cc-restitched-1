/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.google.common.base.Objects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@Environment( EnvType.CLIENT )
public class TurtleSmartItemModel implements BakedModel
{
    private static final Transformation identity, flip;

    static
    {
        PoseStack stack = new PoseStack();
        stack.scale( 0, -1, 0 );
        stack.translate( 0, 0, 1 );

        identity = Transformation.identity();
        flip = new Transformation( stack.last()
            .pose() );
    }

    private static class TurtleModelCombination
    {
        final boolean colour;
        final ITurtleUpgrade leftUpgrade;
        final ITurtleUpgrade rightUpgrade;
        final ResourceLocation overlay;
        final boolean christmas;
        final boolean flip;

        TurtleModelCombination( boolean colour, ITurtleUpgrade leftUpgrade, ITurtleUpgrade rightUpgrade, ResourceLocation overlay, boolean christmas,
                                boolean flip )
        {
            this.colour = colour;
            this.leftUpgrade = leftUpgrade;
            this.rightUpgrade = rightUpgrade;
            this.overlay = overlay;
            this.christmas = christmas;
            this.flip = flip;
        }

        @Override
        public boolean equals( Object other )
        {
            if( other == this )
            {
                return true;
            }
            if( !(other instanceof TurtleModelCombination otherCombo) )
            {
                return false;
            }

            return otherCombo.colour == colour && otherCombo.leftUpgrade == leftUpgrade && otherCombo.rightUpgrade == rightUpgrade && Objects.equal(
                otherCombo.overlay, overlay ) && otherCombo.christmas == christmas && otherCombo.flip == flip;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 0;
            result = prime * result + (colour ? 1 : 0);
            result = prime * result + (leftUpgrade != null ? leftUpgrade.hashCode() : 0);
            result = prime * result + (rightUpgrade != null ? rightUpgrade.hashCode() : 0);
            result = prime * result + (overlay != null ? overlay.hashCode() : 0);
            result = prime * result + (christmas ? 1 : 0);
            result = prime * result + (flip ? 1 : 0);
            return result;
        }
    }

    private final BakedModel familyModel;
    private final BakedModel colourModel;

    private final HashMap<TurtleModelCombination, BakedModel> cachedModels = new HashMap<>();
    private final ItemOverrides overrides;

    public TurtleSmartItemModel( BakedModel familyModel, BakedModel colourModel )
    {
        this.familyModel = familyModel;
        this.colourModel = colourModel;

        // this actually works I think, trust me
        overrides = new ItemOverrides( null, null, null, Collections.emptyList() )
        {
            @Nonnull
            @Override
            public BakedModel resolve( BakedModel originalModel, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed )
            {
                ItemTurtle turtle = (ItemTurtle) stack.getItem();
                int colour = turtle.getColour( stack );
                ITurtleUpgrade leftUpgrade = turtle.getUpgrade( stack, TurtleSide.LEFT );
                ITurtleUpgrade rightUpgrade = turtle.getUpgrade( stack, TurtleSide.RIGHT );
                ResourceLocation overlay = turtle.getOverlay( stack );
                boolean christmas = HolidayUtil.getCurrentHoliday() == Holiday.CHRISTMAS;
                String label = turtle.getLabel( stack );
                boolean flip = label != null && (label.equals( "Dinnerbone" ) || label.equals( "Grumm" ));
                TurtleModelCombination combo = new TurtleModelCombination( colour != -1, leftUpgrade, rightUpgrade, overlay, christmas, flip );

                BakedModel model = cachedModels.get( combo );
                if( model == null )
                {
                    cachedModels.put( combo, model = buildModel( combo ) );
                }
                return model;
            }
        };
    }

    private BakedModel buildModel( TurtleModelCombination combo )
    {
        Minecraft mc = Minecraft.getInstance();
        ModelManager modelManager = mc.getItemRenderer()
            .getItemModelShaper()
            .getModelManager();
        ModelResourceLocation overlayModelLocation = TileEntityTurtleRenderer.getTurtleOverlayModel( combo.overlay, combo.christmas );

        BakedModel baseModel = combo.colour ? colourModel : familyModel;
        BakedModel overlayModel = overlayModelLocation != null ? modelManager.getModel( overlayModelLocation ) : null;
        Transformation transform = combo.flip ? flip : identity;
        TransformedModel leftModel = combo.leftUpgrade != null ? combo.leftUpgrade.getModel( null, TurtleSide.LEFT ) : null;
        TransformedModel rightModel = combo.rightUpgrade != null ? combo.rightUpgrade.getModel( null, TurtleSide.RIGHT ) : null;
        return new TurtleMultiModel( baseModel, overlayModel, transform, leftModel, rightModel );
    }

    @Nonnull
    @Override
    @Deprecated
    public List<BakedQuad> getQuads( BlockState state, Direction facing, @Nonnull Random rand )
    {
        return familyModel.getQuads( state, facing, rand );
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return familyModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d()
    {
        return familyModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight()
    {
        return familyModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer()
    {
        return familyModel.isCustomRenderer();
    }

    @Override
    @Deprecated
    public TextureAtlasSprite getParticleIcon()
    {
        return familyModel.getParticleIcon();
    }

    @Nonnull
    @Override
    public ItemOverrides getOverrides()
    {
        return overrides;
    }

    @Nonnull
    @Override
    @Deprecated
    public ItemTransforms getTransforms()
    {
        return familyModel.getTransforms();
    }

}
