package org.mtr.mapping.holder;

import org.mtr.mapping.mapper.BlockHelper;
import org.mtr.mapping.tool.BlockStateDefinition;
import org.mtr.mapping.tool.HolderBase;
import java.util.ArrayList;
import java.util.List;

public class BlockAbstractMapping extends net.minecraft.block.Block implements BlockHelper {
    private static final ClassValue<Boolean> RANDOM_TICKS=new ClassValue<Boolean>() {
        @Override protected Boolean computeValue(Class<?> type) {
            try { return type.getMethod("hasRandomTicks2",BlockState.class).getDeclaringClass()!=BlockAbstractMapping.class; }
            catch (NoSuchMethodException impossible) { throw new IllegalStateException(impossible); }
        }
    };
    // The native client push-out hook has no position. Never evaluate an overridden shape with a null world.
    private static final ClassValue<Boolean> DEFAULT_FULL_CUBE_COLLISION=new ClassValue<Boolean>() {
        @Override protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("getCollisionShape2",BlockState.class,BlockView.class,BlockPos.class,ShapeContext.class).getDeclaringClass()==BlockAbstractMapping.class
                    && type.getMethod("getOutlineShape2",BlockState.class,BlockView.class,BlockPos.class,ShapeContext.class).getDeclaringClass()==BlockAbstractMapping.class;
            } catch (NoSuchMethodException impossible) { throw new IllegalStateException(impossible); }
        }
    };
    // Native neighbors have no state/world here; only the default culling shape guarantees a solid cube.
    private static final ClassValue<Boolean> DEFAULT_FULL_CUBE_CULLING=new ClassValue<Boolean>() {
        @Override protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("getCullingShape2",BlockState.class,BlockView.class,BlockPos.class).getDeclaringClass()==BlockAbstractMapping.class
                    && type.getMethod("getOutlineShape2",BlockState.class,BlockView.class,BlockPos.class,ShapeContext.class).getDeclaringClass()==BlockAbstractMapping.class;
            } catch (NoSuchMethodException impossible) { throw new IllegalStateException(impossible); }
        }
    };
    @Override public String getLocalizedName() {
        return net.minecraft.util.StatCollector.translateToLocal(getTranslationKey2());
    }
    public String getTranslationKey2() { return org.mtr.mapping.tool.LegacyTranslations.getTranslationKey(getUnlocalizedName()); }
    private final BlockSettings settings;
    private BlockStateDefinition definition;
    private BlockState defaultState;

    public BlockAbstractMapping(BlockSettings settings) {
        super(settings.data);
        this.settings = settings;
        if (!settings.opaque) setLightOpacity(0);
        setHardness(settings.hardness);
        setResistance(settings.hardness);
        // Native section counts are per block type; the hook checks the actual state before dispatch.
        setTickRandomly(RANDOM_TICKS.get(getClass()));
    }
    public final BlockStateDefinition getStateDefinition() {
        if (definition == null) {
            List<HolderBase<?>> properties = new ArrayList<>();
            addBlockProperties(properties);
            definition = new BlockStateDefinition(this, properties);
        }
        return definition;
    }
    public final BlockState getDefaultState2() {
        if (defaultState == null) defaultState = new BlockState(getStateDefinition().defaultState());
        return defaultState;
    }
    protected final void setDefaultState2(BlockState state) {
        if (state.data.definition.block != this) throw new IllegalArgumentException("Default state belongs to another block");
        defaultState = state;
    }
    public VoxelShape getOutlineShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) { return VoxelShapes.fullCube(); }
    public VoxelShape getCullingShape2(BlockState state, BlockView world, BlockPos pos) {
        return getOutlineShape2(state,world,pos,new ShapeContext(null));
    }
    public boolean isSideInvisible2(BlockState state,BlockState neighborState,Direction direction) { return false; }
    public float getAmbientOcclusionLightLevel2(BlockState state,BlockView world,BlockPos pos) {
        return org.mtr.mapping.tool.LegacyBlockOcclusion.isFullCube(getCollisionShape2(state,world,pos,new ShapeContext(null)))?.2F:1;
    }
    public boolean isTranslucent2(BlockState state,BlockView world,BlockPos pos) {
        return !org.mtr.mapping.tool.LegacyBlockOcclusion.isFullCube(getOutlineShape2(state,world,pos,new ShapeContext(null)))
            && state.getFluidState().isEmpty();
    }
    public FluidState getFluidState2(BlockState state) { return org.mtr.mapping.tool.Waterlogging.fluidState(state); }
    public boolean isOpaqueFullCube2(BlockState state,BlockView world,BlockPos pos) {
        return (settings==null || settings.opaque) && org.mtr.mapping.tool.LegacyBlockOcclusion.isFullCube(getCullingShape2(state,world,pos));
    }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public boolean shouldSideBeRendered(net.minecraft.world.IBlockAccess world,int x,int y,int z,int side) {
        Direction direction=Direction.values()[side];
        BlockView view=new BlockView(world); BlockPos neighborPos=new BlockPos(x,y,z),pos=neighborPos.offset(direction.getOpposite());
        BlockState state=view.getBlockState(pos),neighbor=view.getBlockState(neighborPos);
        if (state.getBlock().data!=this) return super.shouldSideBeRendered(world,x,y,z,side);
        if (isSideInvisible2(state,neighbor,direction)) return false;
        net.minecraft.block.Block other=neighbor.getBlock().data;
        boolean opaque=other instanceof BlockAbstractMapping?((BlockAbstractMapping)other).settings.opaque
            :other.isOpaqueCube() || org.mtr.mapping.tool.NativeSlabStates.supports(other);
        return !opaque || org.mtr.mapping.tool.LegacyBlockOcclusion.shouldRenderFace(getCullingShape2(state,view,pos),
            org.mtr.mapping.tool.LegacyBlockOcclusion.cullingShape(neighbor,view,neighborPos),direction);
    }
    public VoxelShape getCameraCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getCollisionShape2(state, world, pos, context);
    }
    public Item asItem2() { return new Item(net.minecraft.item.Item.getItemFromBlock(this)); }
    public BlockRenderType getRenderType2(BlockState state) { return BlockRenderType.MODEL; }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public int getRenderType() {
        if (getRenderType2(getDefaultState2())==BlockRenderType.MODEL) return org.mtr.mapping.registry.LegacyBlockRenderers.type();
        return org.mtr.mapping.registry.LegacyContainedWaterRenderer.supports(this)?org.mtr.mapping.registry.LegacyBlockRenderers.waterType():-1;
    }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public int getRenderBlockPass() { return org.mtr.mapping.registry.LegacyBlockRenderers.pass(this); }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public boolean canRenderInPass(int pass) {
        return pass==getRenderBlockPass() || pass==1 && org.mtr.mapping.registry.LegacyContainedWaterRenderer.supports(this);
    }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public void registerBlockIcons(net.minecraft.client.renderer.texture.IIconRegister register) {
        blockIcon=org.mtr.mapping.registry.LegacyBlockRenderers.icons(this,register);
    }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public net.minecraft.util.IIcon getIcon(int side,int metadata) {
        if (metadata<0 || metadata>15) throw new IllegalArgumentException("Invalid metadata-only icon metadata: "+metadata);
        // This API has no persistent state: choose the lowest full state with these metadata bits.
        return org.mtr.mapping.registry.LegacyBlockRenderers.particle(new BlockState(getStateDefinition().readRawStateIndex(metadata)),blockIcon);
    }
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public net.minecraft.util.IIcon getIcon(net.minecraft.world.IBlockAccess world,int x,int y,int z,int side) {
        return org.mtr.mapping.registry.LegacyBlockRenderers.particle(new BlockView(world).getBlockState(new BlockPos(x,y,z)),blockIcon);
    }
    @Override protected boolean canSilkHarvest() { return false; }
    @Override public java.util.ArrayList<net.minecraft.item.ItemStack> getDrops(net.minecraft.world.World world, int x, int y, int z, int metadata, int fortune) {
        org.mtr.mapping.tool.BlockLootTable table = org.mtr.mapping.tool.BlockLootTable.forBlock(new Block(this));
        return table == null ? new java.util.ArrayList<>() : table.drops(new World(world).getBlockState(new BlockPos(x, y, z)));
    }
    @Override public void dropBlockAsItemWithChance(net.minecraft.world.World world, int x, int y, int z, int metadata, float chance, int fortune) {
        if (world.isRemote || world.restoringBlockSnapshots) return;
        java.util.ArrayList<net.minecraft.item.ItemStack> drops = getDrops(world, x, y, z, metadata, fortune);
        chance = net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(drops, world, this, x, y, z, metadata, fortune, chance, false, harvesters.get());
        org.mtr.mapping.tool.BlockLootTable table = org.mtr.mapping.tool.BlockLootTable.forBlock(new Block(this));
        for (net.minecraft.item.ItemStack stack : drops) {
            if (table != null && table.hasExplosionDecay()) {
                int remaining = 0;
                for (int i = 0; i < stack.stackSize; i++) if (world.rand.nextFloat() <= chance) remaining++;
                if (remaining > 0) {
                    net.minecraft.item.ItemStack copy = stack.copy();
                    copy.stackSize = remaining;
                    dropBlockAsItem(world, x, y, z, copy);
                }
            } else if (world.rand.nextFloat() <= chance) dropBlockAsItem(world, x, y, z, stack);
        }
    }
    @Override public boolean removedByPlayer(net.minecraft.world.World world, net.minecraft.entity.player.EntityPlayer player,
            int x, int y, int z, boolean willHarvest) {
        // Retain extended state until Forge asks for drops; creative removal remains immediate.
        return willHarvest && !world.isRemote || super.removedByPlayer(world, player, x, y, z, willHarvest);
    }
    @Override public void harvestBlock(net.minecraft.world.World world, net.minecraft.entity.player.EntityPlayer player,
            int x, int y, int z, int metadata) {
        super.harvestBlock(world, player, x, y, z, metadata);
        if (world.getBlock(x, y, z) == this) world.setBlockToAir(x, y, z);
    }
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return ActionResult.PASS;
    }
    @Override public final boolean onBlockActivated(net.minecraft.world.World world, int x, int y, int z,
            net.minecraft.entity.player.EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        World mapped = new World(world);
        BlockPos pos = new BlockPos(x, y, z);
        BlockHitResult hit = new BlockHitResult(new net.minecraft.util.MovingObjectPosition(x, y, z, side,
            net.minecraft.util.Vec3.createVectorHelper(x + (double) hitX, y + (double) hitY, z + (double) hitZ)));
        return onUse2(mapped.getBlockState(pos), mapped, pos, new PlayerEntity(player), Hand.MAIN_HAND, hit).isAccepted();
    }
    public BlockState getPlacementState2(ItemPlacementContext context) { return getDefaultState2(); }
    public boolean canReplace2(BlockState state,ItemPlacementContext context) {
        BlockPos pos=context.getBlockPos();
        return isReplaceable(context.getWorld().data,pos.getX(),pos.getY(),pos.getZ());
    }
    public void scheduledTick2(BlockState state,ServerWorld world,BlockPos pos,Random random) {}
    public boolean hasRandomTicks2(BlockState state) { return false; }
    public void randomTick2(BlockState state,ServerWorld world,BlockPos pos,Random random) {}
    public void randomDisplayTick2(BlockState state,World world,BlockPos pos,Random random) {}
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override public final void randomDisplayTick(net.minecraft.world.World world,int x,int y,int z,java.util.Random random) {
        World mapped=new World(world); BlockPos pos=new BlockPos(x,y,z);
        randomDisplayTick2(mapped.getBlockState(pos),mapped,pos,new Random(random));
    }
    @Override public final void updateTick(net.minecraft.world.World world,int x,int y,int z,java.util.Random random) {
        if (world instanceof net.minecraft.world.WorldServer && !world.isRemote) {
            BlockPos pos=new BlockPos(x,y,z);
            scheduledTick2(new World(world).getBlockState(pos),new ServerWorld((net.minecraft.world.WorldServer)world),pos,new Random(random));
        }
    }
    public boolean emitsRedstonePower2(BlockState state) { return false; }
    public int getWeakRedstonePower2(BlockState state,BlockView world,BlockPos pos,Direction direction) { return 0; }
    public int getStrongRedstonePower2(BlockState state,BlockView world,BlockPos pos,Direction direction) { return 0; }
    @Override public final boolean canProvidePower() { return emitsRedstonePower2(getDefaultState2()); }
    @Override public final int isProvidingWeakPower(net.minecraft.world.IBlockAccess world,int x,int y,int z,int side) {
        BlockView view=new BlockView(world); BlockPos pos=new BlockPos(x,y,z);
        return getWeakRedstonePower2(view.getBlockState(pos),view,pos,Direction.values()[side]);
    }
    @Override public final int isProvidingStrongPower(net.minecraft.world.IBlockAccess world,int x,int y,int z,int side) {
        BlockView view=new BlockView(world); BlockPos pos=new BlockPos(x,y,z);
        return getStrongRedstonePower2(view.getBlockState(pos),view,pos,Direction.values()[side]);
    }
    public void neighborUpdate2(BlockState state,World world,BlockPos pos,Block sourceBlock,BlockPos sourcePos,boolean notify) {}
    @Override public void onNeighborBlockChange(net.minecraft.world.World world,int x,int y,int z,net.minecraft.block.Block source) {
        World mapped=new World(world); BlockPos pos=new BlockPos(x,y,z);
        neighborUpdate2(mapped.getBlockState(pos),mapped,pos,new Block(source),org.mtr.mapping.tool.NeighborUpdates.sourcePos(world,pos),false);
        org.mtr.mapping.tool.Waterlogging.changed(world,x,y,z);
    }
    public void onPlaced2(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(world.data, pos.getX(), pos.getY(), pos.getZ(), placer.data, stack.data);
    }
    @Override public final void onBlockPlacedBy(net.minecraft.world.World world, int x, int y, int z,
            net.minecraft.entity.EntityLivingBase placer, net.minecraft.item.ItemStack stack) {
        World mapped = new World(world);
        BlockPos pos = new BlockPos(x, y, z);
        onPlaced2(mapped, pos, mapped.getBlockState(pos), new LivingEntity(placer), new ItemStack(stack));
    }
    public BlockState getStateForNeighborUpdate2(BlockState state, Direction direction, BlockState neighborState,
            WorldAccess world, BlockPos pos, BlockPos neighborPos) { return state; }
    public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape2(state, world, pos, context);
    }
    public void onEntityCollision2(BlockState state, World world, BlockPos pos, Entity entity) {
        super.onEntityCollidedWithBlock(world.data, pos.getX(), pos.getY(), pos.getZ(), entity.data);
    }
    @Override public final void onEntityCollidedWithBlock(net.minecraft.world.World world, int x, int y, int z, net.minecraft.entity.Entity entity) {
        World mapped = new World(world);
        BlockPos pos = new BlockPos(x, y, z);
        onEntityCollision2(mapped.getBlockState(pos), mapped, pos, new Entity(entity));
    }
    @Override public void addCollisionBoxesToList(net.minecraft.world.World world, int x, int y, int z,
            net.minecraft.util.AxisAlignedBB mask, java.util.List boxes, net.minecraft.entity.Entity entity) {
        BlockView view = new BlockView(world);
        BlockPos pos = new BlockPos(x, y, z);
        for (Box box : getCollisionShape2(view.getBlockState(pos), view, pos, new ShapeContext(entity)).data) {
            net.minecraft.util.AxisAlignedBB translated = box.data.getOffsetBoundingBox(x, y, z);
            if (translated.intersectsWith(mask)) boxes.add(translated);
        }
    }
    @Override public net.minecraft.util.MovingObjectPosition collisionRayTrace(net.minecraft.world.World world, int x, int y, int z,
            net.minecraft.util.Vec3 start, net.minecraft.util.Vec3 end) {
        BlockView view = new BlockView(world);
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.util.MovingObjectPosition nearest = null;
        for (Box box : getOutlineShape2(view.getBlockState(pos), view, pos, new ShapeContext(null)).data) {
            net.minecraft.util.MovingObjectPosition hit = box.data.getOffsetBoundingBox(x, y, z).calculateIntercept(start, end);
            if (hit != null && (nearest == null || start.squareDistanceTo(hit.hitVec) < start.squareDistanceTo(nearest.hitVec))) {
                nearest = new net.minecraft.util.MovingObjectPosition(x, y, z, hit.sideHit, hit.hitVec);
            }
        }
        return nearest;
    }
    @Override public net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(net.minecraft.world.World world, int x, int y, int z) {
        BlockView view = new BlockView(world);
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state=view.getBlockState(pos);
        if (state.getBlock().data!=this) {
            state=getDefaultState2();
            ItemUsageContext usage=org.mtr.mapping.mapper.BlockItemExtension.currentUsage();
            if (usage!=null && usage.getWorld().data==world && usage.getStack().data!=null
                    && usage.getStack().data.getItem() instanceof net.minecraft.item.ItemBlock
                    && ((net.minecraft.item.ItemBlock)usage.getStack().data.getItem()).field_150939_a==this) {
                state=getPlacementState2(new ItemPlacementContext(usage,pos));
                if (state==null) return null;
            }
        }
        return enclosingBounds(getCollisionShape2(state, view, pos, new ShapeContext(null)), x, y, z);
    }
    @Override public net.minecraft.util.AxisAlignedBB getSelectedBoundingBoxFromPool(net.minecraft.world.World world, int x, int y, int z) {
        BlockView view = new BlockView(world);
        BlockPos pos = new BlockPos(x, y, z);
        return enclosingBounds(getOutlineShape2(view.getBlockState(pos), view, pos, new ShapeContext(null)), x, y, z);
    }
    private static net.minecraft.util.AxisAlignedBB enclosingBounds(VoxelShape shape, int x, int y, int z) {
        net.minecraft.util.AxisAlignedBB bounds = null;
        for (Box box : shape.data) {
            if (bounds == null) bounds = box.data.copy();
            else bounds = bounds.func_111270_a(box.data);
        }
        return bounds == null ? null : bounds.getOffsetBoundingBox(x, y, z);
    }
    // Native Block reads this before settings exists to initialize lightOpacity; preserve that constructor default.
    @Override public boolean isOpaqueCube() { return settings == null || settings.opaque && DEFAULT_FULL_CUBE_CULLING.get(getClass()); }
    @Override public boolean renderAsNormalBlock() { return isOpaqueCube(); }
    @Override public boolean isNormalCube() { return isOpaqueCube() && DEFAULT_FULL_CUBE_COLLISION.get(getClass()) && super.isNormalCube(); }
    @Override public boolean isNormalCube(net.minecraft.world.IBlockAccess world,int x,int y,int z) {
        if (world==null || settings!=null && !settings.opaque || !getMaterial().isOpaque() || canProvidePower()) return false;
        BlockView view=new BlockView(world); BlockPos pos=new BlockPos(x,y,z); BlockState state=view.getBlockState(pos);
        return state.getBlock().data==this && org.mtr.mapping.tool.LegacyBlockOcclusion.isFullCube(getCollisionShape2(state,view,pos,new ShapeContext(null)));
    }
    @Override public int getMobilityFlag() { return settings!=null && settings.blockPiston?2:super.getMobilityFlag(); }
    @Override public int getLightValue() {
        return settings==null || settings.luminance==null?super.getLightValue():luminance(getDefaultState2());
    }
    @Override public int getLightValue(net.minecraft.world.IBlockAccess world,int x,int y,int z) {
        net.minecraft.block.Block actual=world.getBlock(x,y,z);
        if (actual!=this) return actual.getLightValue(world,x,y,z);
        return settings==null || settings.luminance==null?super.getLightValue(world,x,y,z):luminance(new BlockView(world).getBlockState(new BlockPos(x,y,z)));
    }
    private int luminance(BlockState state) { return net.minecraft.util.MathHelper.clamp_int(settings.luminance.applyAsInt(state),0,15); }
}
