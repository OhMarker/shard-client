package gg.shard.client.dev;

import gg.shard.client.ShardClient;
import gg.shard.client.modules.visual.CosmeticsModule;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/**
 * Second cosmetics drop pass (-PsmokeOnly=drop2, with -PapiBase and tools/smoke-drop2-seed.mjs run
 * first): ShardSmoke wears each new cape in turn (F5 from behind, then on an elytra), then the Halloween shield in first person (holding, blocking) and third person,
 * and ShardFriend, who wears the Halloween set, from the front and back with close-ups of the
 * bandana (front, side, back, both quarters, from above).
 */
final class DropSmoke {
    private DropSmoke() {}

    static final int TICKS = 600;
    private static final UUID FRIEND = UUID.fromString("5f3c1a2e-0000-4000-8000-0000000000aa");
    private static final String PATTERNED = "minecraft:shield[banner_patterns=[{pattern:\"minecraft:stripe_center\",color:\"red\"},"
            + "{pattern:\"minecraft:border\",color:\"black\"}],base_color=\"blue\"]";
    private static final String[] CAPES = {"cape-shard", "cape-ocean", "cape-halloween"};
    private static final int CAPE_START = 100, CAPE_STEP = 80;
    private static RemotePlayer friend;

    static void block(Minecraft mc, int local) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        CosmeticsModule cosmetics = ShardClient.modules().get(CosmeticsModule.class);
        if (local % 20 == 3) fly(p);
        if (local >= CAPE_START && local < CAPE_START + CAPES.length * CAPE_STEP) {
            capeStep(mc, p, cosmetics, (local - CAPE_START) / CAPE_STEP, (local - CAPE_START) % CAPE_STEP);
            return;
        }
        switch (local) {
            case 0 -> {
                mc.setScreen(null);
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                cmd(mc, "gamemode creative");
                cmd(mc, "clear @s");
                cmd(mc, "time set noon");
                cmd(mc, "weather clear");
                cmd(mc, "tp @s -5 60 2 180 8");
            }
            case 2 -> spawnFriend(mc);
            case 80 -> {
                if (cosmetics.account() == null) ShardClient.LOGGER.error("Smoke drop2: not signed in to the Shard API ({})", cosmetics.status());
                for (String[] e : new String[][]{{"shield", "shield-halloween"}, {"bandana", "bandana-halloween"}}) {
                    cosmetics.equip(e[0], e[1]).whenComplete((me, error) -> ShardClient.LOGGER.info("Smoke drop2: equip {} -> {}", e[1],
                            error != null ? error.getMessage() : me.equipped()));
                }
            }
            // ---- First person: a banner shield in the main hand, a plain one in the offhand.
            case 340 -> {
                mc.options.fov().set(70);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                cmd(mc, "tp @s -5 60 2 180 8");
                mc.options.hideGui = false;
                cmd(mc, "item replace entity @s hotbar.0 with " + PATTERNED);
                cmd(mc, "item replace entity @s weapon.offhand with minecraft:shield");
                p.getInventory().setSelectedSlot(0);
                friendAhead(p, -6, 0, 0);
            }
            case 360 -> shot(mc, "drop2-shield-fp-hold.png");
            case 362 -> mc.options.keyUse.setDown(true);
            case 380 -> shot(mc, "drop2-shield-fp-block.png");
            case 382 -> {
                mc.options.keyUse.setDown(false);
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.options.fov().set(45);
            }
            case 400 -> shot(mc, "drop2-shield-f5-front.png");
            case 402 -> mc.options.keyUse.setDown(true);
            case 420 -> shot(mc, "drop2-shield-f5-front-block.png");
            case 422 -> {
                mc.options.keyUse.setDown(false);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.fov().set(70);
                cmd(mc, "clear @s");
                p.setXRot(8);
                friendAhead(p, 3, 0, 0);
            }
            // ---- ShardFriend in the Halloween set, three blocks ahead.
            case 440 -> shot(mc, "drop2-friend-front.png");
            case 442 -> friendYaw(180);
            case 455 -> shot(mc, "drop2-friend-back.png");
            case 457 -> {
                friendYaw(25);
                friendBlocking(true);
            }
            case 472 -> shot(mc, "drop2-friend-block.png");
            // ---- Close-ups of the bandana, from a little above.
            case 474 -> {
                friendBlocking(false);
                cmd(mc, "tp @s -5 60 2 180 14"); // the elytra flights can leave the player drifting down
                mc.options.fov().set(30);
            }
            case 478 -> {
                p.setDeltaMovement(0, 0, 0);
                friendAhead(p, 2, -0.6, 0);
            }
            case 495 -> shot(mc, "drop2-head-front.png");
            case 497 -> friendYaw(90);
            case 510 -> shot(mc, "drop2-head-side.png");
            case 512 -> friendYaw(180);
            case 525 -> shot(mc, "drop2-head-back.png");
            case 527 -> friendYaw(140);
            case 540 -> shot(mc, "drop2-head-back-quarter.png");
            case 542 -> friendYaw(35);
            case 555 -> shot(mc, "drop2-head-front-quarter.png");
            // ---- From straight above: the top's emblem.
            case 557 -> cmd(mc, "tp @s -5 60 2 180 90");
            case 564 -> {
                fly(p);
                p.setXRot(90);
                if (friend != null) {
                    baseYaw = p.getYRot() + 180;
                    friend.setPos(p.getX(), p.getY() - 2.6, p.getZ());
                    friend.setOldPosAndRot();
                    friendYaw(0);
                }
            }
            case 580 -> shot(mc, "drop2-head-top.png");
            case 582 -> {
                SmokeTest.SUMMARY.addProperty("drop2Status", cosmetics.status());
                SmokeTest.SUMMARY.addProperty("drop2Me", cosmetics.account() == null ? "not signed in" : String.valueOf(cosmetics.account().equipped()));
                SmokeTest.SUMMARY.addProperty("drop2Friend", String.valueOf(cosmetics.wornSnapshot().get(FRIEND)));
                SmokeTest.SUMMARY.addProperty("drop2MyShieldTexture", String.valueOf(cosmetics.shieldTexture(p.getUUID(), true)));
                SmokeTest.SUMMARY.addProperty("drop2MyBandanaTexture", String.valueOf(cosmetics.bandanaTexture(p.getUUID(), true)));
                mc.options.fov().set(70);
                p.setXRot(8);
                if (friend != null) {
                    friend.discard();
                    friend = null;
                }
                cmd(mc, "clear @s");
                mc.options.hideGui = false;
            }
            default -> {
            }
        }
    }

    /** One cape: equip it, F5 from behind (zoomed in), then on an elytra. */
    private static void capeStep(Minecraft mc, LocalPlayer p, CosmeticsModule cosmetics, int index, int local) {
        String id = CAPES[index];
        // Square the body to the camera so the cape hangs straight towards it.
        p.yBodyRot = p.getYRot();
        p.yBodyRotO = p.getYRot();
        switch (local) {
            case 0 -> {
                cosmetics.equip("cape", id).whenComplete((me, error) -> ShardClient.LOGGER.info("Smoke drop2: equip {} -> {}", id,
                        error != null ? error.getMessage() : me.equipped()));
                cmd(mc, "item replace entity @s armor.chest with air");
                cmd(mc, "tp @s -5 60 2 180 8");
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.options.fov().set(26);
                friendAhead(p, 3, 40, 0); // out of the way, far overhead
            }
            case 34 -> shot(mc, "drop2-" + id + "-back.png");
            case 36 -> cmd(mc, "item replace entity @s armor.chest with elytra");
            case 56 -> shot(mc, "drop2-" + id + "-elytra.png");
            case 58 -> {
                cmd(mc, "item replace entity @s armor.chest with air");
                cmd(mc, "tp @s -5 60 2 180 8");
                p.setDeltaMovement(0, 0, 0);
            }
            default -> {
            }
        }
    }

    private static void spawnFriend(Minecraft mc) {
        if (mc.level == null) return;
        RemotePlayer player = new RemotePlayer(mc.level, new com.mojang.authlib.GameProfile(FRIEND, "ShardFriend"));
        try {
            // Real clients send their skin customisation; turn every part on (cape layer included).
            var field = net.minecraft.world.entity.Avatar.class.getDeclaredField("DATA_PLAYER_MODE_CUSTOMISATION");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var accessor = (EntityDataAccessor<Byte>) field.get(null);
            player.getEntityData().set(accessor, (byte) 0x7F);
        } catch (ReflectiveOperationException e) {
            ShardClient.LOGGER.error("Smoke drop2: could not turn on the friend's skin parts", e);
        }
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        ItemStack patterned = new ItemStack(Items.SHIELD);
        patterned.set(net.minecraft.core.component.DataComponents.BASE_COLOR, net.minecraft.world.item.DyeColor.BLUE);
        player.setItemSlot(EquipmentSlot.MAINHAND, patterned);
        // Client-side stand-in: 26.2 no longer numbers entities on construction.
        player.setId(-3_000_002);
        mc.level.addEntity(player);
        friend = player;
        if (mc.player != null) friendAhead(mc.player, 3, 0, 0);
    }

    private static float baseYaw;

    /** Puts the friend {@code dist} blocks ahead of the camera ({@code dy} lower or higher), facing it. */
    private static void friendAhead(LocalPlayer p, double dist, double dy, float rel) {
        if (friend == null) return;
        double yawRad = Math.toRadians(p.getYRot());
        double x = p.getX() - Math.sin(yawRad) * dist;
        double z = p.getZ() + Math.cos(yawRad) * dist;
        double y = p.getY() + dy;
        baseYaw = p.getYRot() + 180;
        friend.setPos(x, y, z);
        friend.xo = x;
        friend.yo = y;
        friend.zo = z;
        friend.setOldPosAndRot();
        friendYaw(rel);
    }

    /** Turns the friend: 0 faces the camera, 90 shows its side, 180 its back. */
    private static void friendYaw(float rel) {
        if (friend == null) return;
        float yaw = baseYaw + rel;
        friend.setYRot(yaw);
        friend.yRotO = yaw;
        friend.setYHeadRot(yaw);
        friend.yHeadRotO = yaw;
        friend.yBodyRot = yaw;
        friend.yBodyRotO = yaw;
        friend.setXRot(0);
    }

    /** Raises the friend's offhand shield the way a server's "using item" flag does. */
    private static void friendBlocking(boolean on) {
        if (friend == null) return;
        try {
            var field = LivingEntity.class.getDeclaredField("DATA_LIVING_ENTITY_FLAGS");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var accessor = (EntityDataAccessor<Byte>) field.get(null);
            friend.getEntityData().set(accessor, (byte) (on ? 1 | 2 : 0));
        } catch (ReflectiveOperationException e) {
            ShardClient.LOGGER.error("Smoke drop2: could not make the friend block", e);
        }
    }

    private static void fly(LocalPlayer p) {
        if (p.isCreative() && !p.getAbilities().flying) {
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }
    }

    private static void shot(Minecraft mc, String name) {
        mc.gui.getChat().clearMessages(false);
        SmokeTest.shot(mc, name, null);
    }

    private static void cmd(Minecraft mc, String command) {
        if (mc.player != null) mc.player.connection.sendCommand(command);
    }
}
