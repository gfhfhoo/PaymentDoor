package me.endnether.listener;

import me.endnether.PaymentDoor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class ConsumeListener implements Listener {
    private PaymentDoor plugin;
    private Economy econ;
    private static final BlockFace[] faces = {
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.SOUTH,
            BlockFace.NORTH,
    };


    public ConsumeListener() {
        this.econ = PaymentDoor.getEcon();
        this.plugin = PaymentDoor.getPlugin();
    }

    @EventHandler
    public void onConsume(PlayerInteractEvent event) {
        // 获取玩家
        Player player = event.getPlayer();
        // 确保右键
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getItem() == null) {
            // 获取被触碰的方块
            Block clickedBlock = event.getClickedBlock();
            Sign sign = null;
            TileState tileState = null;
            PersistentDataContainer container = null;

            // 确保被右键的是铁门
            if (clickedBlock != null && clickedBlock.getType() == Material.IRON_DOOR) {
                Door door = (Door) clickedBlock.getBlockData();
                boolean isTopHalf = door.getHalf() == Bisected.Half.TOP;
                clickedBlock = isTopHalf ? clickedBlock : clickedBlock.getRelative(BlockFace.UP);
                Block block = clickedBlock.getRelative(BlockFace.UP);

                for (BlockFace face : faces) {
                    BlockState blockState = block.getRelative(face).getState();
                    if (!(blockState instanceof TileState)) continue;
                    tileState = (TileState) blockState;
                    container = tileState.getPersistentDataContainer();
                    if (container.has(new NamespacedKey(plugin, "PD_isActive"), PersistentDataType.INTEGER)) {
                        sign = (Sign) block.getRelative(face).getState();
                        break;
                    }
                }
            }
            // 确保告示牌有效
            if (sign == null) return;

            // 确保有权限
            if (player.hasPermission("netherPD.enter")) {


                double pd_amount = container.get(new NamespacedKey(plugin, "PD_amount"), PersistentDataType.INTEGER).doubleValue();
                boolean pd_isAllowed = container.get(new NamespacedKey(plugin, "PD_isAllowed"), PersistentDataType.INTEGER) == 1;
                UUID pd_owner = UUID.fromString(container.get(new NamespacedKey(plugin, "PD_owner"), PersistentDataType.STRING));

                if (checkInDoor(player, clickedBlock)) {
                    leave(player, clickedBlock);
                    return;
                }

                if (player.getUniqueId().equals(pd_owner)) {
                    enter(player, clickedBlock);
                    return;
                }
                if (!pd_isAllowed) {
                    player.sendMessage(ChatColor.RED + "不允许进入！");
                    return;
                }
                if (this.econ.has(player, pd_amount)) {
                    boolean a = this.econ.withdrawPlayer(player, pd_amount).transactionSuccess();
                    boolean b = this.econ.depositPlayer(Bukkit.getOfflinePlayer(pd_owner), pd_amount).transactionSuccess();
                    if (a && b) {
                        enter(player, clickedBlock);
                        player.sendMessage(ChatColor.GREEN + "你已经花费了 " + pd_amount + this.econ.currencyNamePlural() + "," +
                                "现在的余额还有 " + this.econ.getBalance(player) + this.econ.currencyNamePlural());
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + "你没有进入的权限！");
            }
        }
    }

    public void enter(Player player, Block block) {
        Door door = (Door) block.getBlockData();
        BlockFace teleFacing = door.getFacing();

        Location teleLocation = block.getRelative(teleFacing).getLocation().subtract(0, 1, 0);

        // set yaw and location
        float yaw = 0;
        switch (teleFacing) {
            case SOUTH:
                yaw = 0;
                teleLocation = teleLocation.add(0.5, 0, 0.5);
                break;
            case NORTH:
                yaw = 180;
                teleLocation = teleLocation.add(0.5, 0, 0);
                break;
            case WEST:
                yaw = 90;
                teleLocation = teleLocation.add(0, 0, 0.5);
                break;
            case EAST:
                yaw = 270;
                teleLocation = teleLocation.add(0.5, 0, 0.5);
                break;
        }

        teleLocation.setYaw(yaw);
        player.teleport(teleLocation);
    }

    public void leave(Player player, Block block) {
        Door door = (Door) block.getBlockData();
        BlockFace teleFacing = door.getFacing();

        Location teleLocation = block.getLocation().subtract(0, 1, 0);

        // set yaw and location
        float yaw = 0;

        switch (teleFacing) {
            case SOUTH:
                yaw = 180;
                teleLocation = teleLocation.add(0.5, 0, -1);
                break;
            case NORTH:
                yaw = 0;
                teleLocation = teleLocation.add(0.5, 0, 2);
                break;
            case WEST:
                yaw = 270;
                teleLocation = teleLocation.add(2, 0, 0.5);
                break;
            case EAST:
                yaw = 90;
                teleLocation = teleLocation.add(-1, 0, 0.5);
                break;
        }
        teleLocation.setYaw(yaw);
        player.teleport(teleLocation);
    }

    public boolean checkInDoor(Player player, Block block) {
        BlockFace playerFacing = player.getFacing();
        Door door = (Door) block.getBlockData();
        BlockFace doorFacing = door.getFacing();
        // todo: not sure if south-east is effective?
        boolean var1 = doorFacing == BlockFace.NORTH
                && (playerFacing == BlockFace.SOUTH || playerFacing == BlockFace.SOUTH_EAST || playerFacing == BlockFace.SOUTH_WEST);
        boolean var2 = doorFacing == BlockFace.SOUTH
                && (playerFacing == BlockFace.NORTH || playerFacing == BlockFace.NORTH_EAST || playerFacing == BlockFace.NORTH_WEST);
        boolean var3 = doorFacing == BlockFace.EAST
                && (playerFacing == BlockFace.WEST || playerFacing == BlockFace.SOUTH_WEST || playerFacing == BlockFace.NORTH_WEST);
        boolean var4 = doorFacing == BlockFace.WEST
                && (playerFacing == BlockFace.EAST || playerFacing == BlockFace.NORTH_EAST || playerFacing == BlockFace.SOUTH_EAST);
        return var1 || var2 || var3 || var4;
    }
}
