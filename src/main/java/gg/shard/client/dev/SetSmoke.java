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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.UUID;

/**
 * OhMarker set pass (-PsmokeOnly=set, with -PapiBase and tools/smoke-set-seed.mjs run first):
 * equips the shield and bandana (and cape) for ShardSmoke through the API, then shoots the
 * first-person shield (holding, blocking, cosmetic off, with the Shield module's tint), the player
 * in F5 from behind and in front, a second Shard player (ShardFriend) from every side, close-ups
 * of the bandana (front, side, back, sneaking, under a helmet) and the inventory doll.
 */
final class SetSmoke {
    private SetSmoke() {}

    static final int TICKS = 560;
    private static final UUID FRIEND = UUID.fromString("5f3c1a2e-0000-4000-8000-0000000000aa");
    private static final String PATTERNED = "minecraft:shield[banner_patterns=[{pattern:\"minecraft:stripe_center\",color:\"red\"},"
            + "{pattern:\"minecraft:border\",color:\"black\"}],base_color=\"blue\"]";
    private static RemotePlayer friend;
    private static boolean lowShieldWasOn;

    static void block(Minecraft mc, int local) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        CosmeticsModule cosmetics = ShardClient.modules().get(CosmeticsModule.class);
        if (local % 20 == 3) fly(p);
        // Vanilla works the pose out again every tick; hold the crouch for the sneak shot.
        if (local > 472 && local <= 485 && friend != null) friend.setPose(Pose.CROUCHING);
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
                if (cosmetics.account() == null) ShardClient.LOGGER.error("Smoke set: not signed in to the Shard API ({})", cosmetics.status());
                for (String[] e : new String[][]{{"cape", "cape-ohmarker"}, {"shield", "shield-ohmarker"}, {"bandana", "bandana-ohmarker"}}) {
                    cosmetics.equip(e[0], e[1]).whenComplete((me, error) -> ShardClient.LOGGER.info("Smoke set: equip {} -> {}", e[1],
                            error != null ? error.getMessage() : me.equipped()));
                }
            }
            // ---- First person: a banner shield in the main hand, a plain one in the offhand.
            case 200 -> {
                mc.options.hideGui = false;
                cmd(mc, "item replace entity @s hotbar.0 with " + PATTERNED);
                cmd(mc, "item replace entity @s weapon.offhand with minecraft:shield");
                p.getInventory().setSelectedSlot(0);
                friendAhead(p, -6, 0, 0);
            }
            case 220 -> shot(mc, "set-fp-hold.png");
            case 222 -> mc.options.keyUse.setDown(true);
            case 240 -> shot(mc, "set-fp-block.png");
            case 242 -> {
                mc.options.keyUse.setDown(false);
                setSetting("cosmetics", "show-my-shield", "false");
            }
            case 256 -> shot(mc, "set-fp-cosmetic-off.png");
            case 258 -> {
                setSetting("cosmetics", "show-my-shield", "true");
                lowShieldWasOn = module("low-shield").isEnabled();
                module("low-shield").setEnabled(true);
                setSetting("low-shield", "opacity", "50");
                setSetting("low-shield", "tint-preset", "ICE");
            }
            case 272 -> shot(mc, "set-fp-lowshield-ice50.png");
            case 274 -> {
                setSetting("low-shield", "opacity", "100");
                setSetting("low-shield", "tint-preset", "CUSTOM");
                setSetting("low-shield", "tint", "#FFFFFF");
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            // ---- F5: yourself with bandana, shields and cape.
            case 295 -> shot(mc, "set-f5-back.png");
            case 297 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 315 -> shot(mc, "set-f5-front.png");
            case 317 -> mc.options.keyUse.setDown(true);
            case 335 -> shot(mc, "set-f5-front-block.png");
            case 337 -> {
                mc.options.keyUse.setDown(false);
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                p.setXRot(8);
                friendAhead(p, 3, 0, 0);
            }
            // ---- ShardFriend (another Shard player) three blocks ahead, every side.
            case 355 -> shot(mc, "set-friend-front.png");
            case 357 -> friendYaw(90);
            case 370 -> shot(mc, "set-friend-side.png");
            case 372 -> friendYaw(180);
            case 385 -> shot(mc, "set-friend-back.png");
            case 387 -> {
                friendYaw(25);
                friendBlocking(true);
            }
            case 402 -> shot(mc, "set-friend-block.png");
            // ---- Close-ups of the bandana, from a little above.
            case 404 -> {
                friendBlocking(false);
                p.setXRot(14);
                mc.options.fov().set(30);
                friendAhead(p, 2, -0.6, 0);
            }
            case 425 -> shot(mc, "set-head-front.png");
            case 427 -> friendYaw(90);
            case 440 -> shot(mc, "set-head-side.png");
            case 442 -> friendYaw(180);
            case 455 -> shot(mc, "set-head-back.png");
            case 457 -> friendYaw(140);
            case 470 -> shot(mc, "set-head-back-quarter.png");
            case 472 -> {
                friendYaw(90);
                if (friend != null) {
                    friend.setShiftKeyDown(true);
                    friend.setPose(Pose.CROUCHING);
                }
            }
            case 485 -> shot(mc, "set-head-sneak.png");
            case 487 -> {
                if (friend != null) {
                    friend.setShiftKeyDown(false);
                    friend.setPose(Pose.STANDING);
                    friend.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                }
                friendYaw(30);
            }
            case 500 -> shot(mc, "set-head-helmet.png");
            // ---- Inventory doll (survival inventory).
            case 502 -> {
                if (friend != null) friend.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                mc.options.fov().set(70);
                cmd(mc, "tp @s -5 60 2 180 8");
                cmd(mc, "gamemode survival");
            }
            case 506 -> {
                mc.options.guiScale().set(4);
                mc.resizeDisplay();
            }
            case 508 -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(p));
            case 510 -> SmokeTest.parkCursor(mc);
            case 525 -> shot(mc, "set-inventory-doll.png");
            case 527 -> {
                mc.setScreen(null);
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                cmd(mc, "gamemode creative");
                SmokeTest.SUMMARY.addProperty("setStatus", cosmetics.status());
                SmokeTest.SUMMARY.addProperty("setMe", cosmetics.account() == null ? "not signed in" : String.valueOf(cosmetics.account().equipped()));
                SmokeTest.SUMMARY.addProperty("setFriend", String.valueOf(cosmetics.wornSnapshot().get(FRIEND)));
                SmokeTest.SUMMARY.addProperty("setMyShieldTexture", String.valueOf(cosmetics.shieldTexture(p.getUUID(), true)));
                SmokeTest.SUMMARY.addProperty("setMyBandanaTexture", String.valueOf(cosmetics.bandanaTexture(p.getUUID(), true)));
                SmokeTest.SUMMARY.addProperty("setFriendShieldTexture", String.valueOf(cosmetics.shieldTexture(FRIEND, false)));
                module("low-shield").setEnabled(lowShieldWasOn);
            }
            // ---- From straight above: the top's emblem must read upright and not mirrored.
            case 529 -> cmd(mc, "tp @s -5 60 2 180 90");
            case 536 -> {
                fly(p);
                p.setXRot(90);
                mc.options.fov().set(30);
                mc.options.hideGui = true;
                if (friend != null) {
                    baseYaw = p.getYRot() + 180;
                    friend.setPos(p.getX(), p.getY() - 2.6, p.getZ());
                    friend.setOldPosAndRot();
                    friendYaw(0);
                }
            }
            case 552 -> shot(mc, "set-head-top.png");
            case 554 -> {
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
            ShardClient.LOGGER.error("Smoke set: could not turn on the friend's skin parts", e);
        }
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        ItemStack patterned = new ItemStack(Items.SHIELD);
        patterned.set(net.minecraft.core.component.DataComponents.BASE_COLOR, net.minecraft.world.item.DyeColor.BLUE);
        player.setItemSlot(EquipmentSlot.MAINHAND, patterned);
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
            ShardClient.LOGGER.error("Smoke set: could not make the friend block", e);
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

    private static gg.shard.client.module.Module module(String key) {
        for (var m : ShardClient.modules().all()) if (m.key().equals(key)) return m;
        throw new IllegalStateException("no module " + key);
    }

    private static void setSetting(String moduleKey, String settingKey, String value) {
        boolean found = false;
        for (var m : ShardClient.modules().all()) {
            if (!m.key().equals(moduleKey)) continue;
            for (var st : m.settings()) {
                if (st.key().equals(settingKey)) {
                    st.parse(value);
                    found = true;
                }
            }
        }
        if (!found) ShardClient.LOGGER.error("Smoke set: no setting {}.{}", moduleKey, settingKey.toLowerCase(Locale.ROOT));
    }
}
