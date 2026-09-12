package net.elementalcrystals.item;

import net.elementalcrystals.element.Element;
import net.elementalcrystals.element.ElementAbility;
import net.elementalcrystals.util.CrystalDataHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Supplier;

/**
 * The single Elemental Crystal item. Behaviour branches entirely on the
 * NBT-stored state read via CrystalDataHelper:
 * - Deactivated (Element.NONE, Activated=false): right-click rolls a random
 *   element and permanently converts this exact ItemStack.
 * - Activated: right-click instead invokes that element's active ability,
 *   subject to its own per-stack cooldown.
 * <p>
 * All roll/ability logic only executes server-side (guarded by
 * `!world.isClient` checks) to keep RNG and gameplay effects authoritative;
 * the client copy of this item only ever renders textures/tooltips derived
 * from NBT that the server has already written and synced back down.
 * <p>
 * maxCount(1) is set in ModItems so deactivated and activated crystals -
 * and crystals of different elements - never stack together; vanilla only
 * stacks ItemStacks whose NBT is exactly equal, so differing Element/
 * Activated NBT already prevents unwanted merging even without the
 * maxCount cap, but the cap also stops two *matching* elemental crystals
 * from silently merging into a stack of 2 (each crystal should be a
 * distinct, trackable stack with its own cooldown).
 */
public class CrystalItem extends Item {

    private final Supplier<ElementAbility> fireAbility;
    private final Supplier<ElementAbility> frostAbility;
    private final Supplier<ElementAbility> lightningAbility;
    private final Supplier<ElementAbility> voidAbility;

    public CrystalItem(Settings settings,
                        Supplier<ElementAbility> fireAbility,
                        Supplier<ElementAbility> frostAbility,
                        Supplier<ElementAbility> lightningAbility,
                        Supplier<ElementAbility> voidAbility) {
        super(settings);
        this.fireAbility = fireAbility;
        this.frostAbility = frostAbility;
        this.lightningAbility = lightningAbility;
        this.voidAbility = voidAbility;
    }

    private ElementAbility abilityFor(Element element) {
        switch (element) {
            case FIRE:
                return fireAbility.get();
            case FROST:
                return frostAbility.get();
            case LIGHTNING:
                return lightningAbility.get();
            case VOID_ELEMENT:
                return voidAbility.get();
            default:
                return null;
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            // Purely visual early return on the client; the authoritative
            // logic below only runs on the server copy of this call.
            return TypedActionResult.pass(stack);
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }
        ServerWorld serverWorld = (ServerWorld) world;

        if (!CrystalDataHelper.isActivated(stack)) {
            Element rolled = CrystalDataHelper.rollRandomElement(stack);
            playActivationFeedback(serverWorld, player, rolled);
            return TypedActionResult.success(stack, false);
        }

        Element element = CrystalDataHelper.getElement(stack);
        ElementAbility ability = abilityFor(element);
        if (ability == null) {
            return TypedActionResult.pass(stack);
        }

        if (CrystalDataHelper.isOnCooldown(stack, serverWorld)) {
            long remainingTicks = CrystalDataHelper.getCooldownTicksRemaining(stack, serverWorld);
            player.sendMessage(
                    Text.translatable("message.elementalcrystals.on_cooldown", (remainingTicks / 20) + 1)
                            .formatted(Formatting.GRAY),
                    true
            );
            return TypedActionResult.fail(stack);
        }

        ability.triggerActive(player, stack);
        CrystalDataHelper.startCooldown(stack, serverWorld, ability.getCooldownTicks());
        return TypedActionResult.success(stack, false);
    }

    /**
     * Server-authoritative particle burst + sound played once, at the
     * moment of activation, distinct per rolled element. Uses
     * ServerWorld#spawnParticles which automatically networks the burst to
     * nearby clients - no custom packet needed for this purely cosmetic step.
     */
    private void playActivationFeedback(ServerWorld world, ServerPlayerEntity player, Element rolled) {
        Vec3d pos = player.getPos();
        switch (rolled) {
            case FIRE -> {
                world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + 1.0, pos.z, 50, 0.6, 0.8, 0.6, 0.05);
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 1.4f);
            }
            case FROST -> {
                world.spawnParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 1.0, pos.z, 50, 0.6, 0.8, 0.6, 0.05);
                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8f, 1.8f);
            }
            case LIGHTNING -> {
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 1.0, pos.z, 60, 0.6, 0.8, 0.6, 0.1);
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.5f, 1.6f);
            }
            case VOID_ELEMENT -> {
                world.spawnParticles(ParticleTypes.PORTAL, pos.x, pos.y + 1.0, pos.z, 60, 0.6, 0.8, 0.6, 0.2);
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.7f, 0.8f);
            }
            default -> {
            }
        }
        player.sendMessage(
                Text.translatable("message.elementalcrystals.awakened", Text.translatable(rolled.getTranslationKey()).formatted(Formatting.BOLD))
                        .formatted(Formatting.YELLOW),
                false
        );
    }

    @Override
    public Text getName(ItemStack stack) {
        Element element = CrystalDataHelper.getElement(stack);
        if (element == Element.NONE) {
            return super.getName(stack);
        }
        MutableText base = Text.translatable("item.elementalcrystals.crystal_of", Text.translatable(element.getTranslationKey()));
        return base.formatted(colorFor(element));
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        Element element = CrystalDataHelper.getElement(stack);
        if (element == Element.NONE) {
            tooltip.add(Text.translatable("tooltip.elementalcrystals.inert").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
            return;
        }
        tooltip.add(Text.translatable("tooltip.elementalcrystals.passive_header").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.elementalcrystals." + element.getId() + ".passive").formatted(colorFor(element)));
        tooltip.add(Text.translatable("tooltip.elementalcrystals.active_header").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.elementalcrystals." + element.getId() + ".active").formatted(colorFor(element)));
        tooltip.add(Text.translatable("tooltip.elementalcrystals." + element.getId() + ".tradeoff").formatted(Formatting.RED, Formatting.ITALIC));
    }

    private Formatting colorFor(Element element) {
        switch (element) {
            case FIRE:
                return Formatting.RED;
            case FROST:
                return Formatting.AQUA;
            case LIGHTNING:
                return Formatting.YELLOW;
            case VOID_ELEMENT:
                return Formatting.DARK_PURPLE;
            default:
                return Formatting.GRAY;
        }
    }
}
