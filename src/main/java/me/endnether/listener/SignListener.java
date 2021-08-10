package me.endnether.listener;

import me.endnether.PaymentDoor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.*;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class SignListener implements Listener {

    private JavaPlugin plugin;

    public SignListener() {
        this.plugin = PaymentDoor.getPlugin();
    }

    @EventHandler
    public void onChangedSign(SignChangeEvent event) {

        if (event.isCancelled()) {
            return;
        }
        // Get ready to check
        Player player = event.getPlayer();
        String[] lines = event.getLines();

        // Check if it is wallSign
        if (event.getBlock().getBlockData() instanceof WallSign) {
            // Get the sign
            Block signBlock = event.getBlock();
            BlockState signState = signBlock.getState();
            Sign sign = (Sign) signState;
            WallSign wallSignData = (WallSign) signBlock.getBlockData();

            if (lines[0].equals("[lock]") && lines[1].matches("^[0-9]+$")
                    && checkDoor(signBlock, wallSignData)
                    && player.hasPermission("netherPD.create")) {
                Integer cost = Integer.parseInt(lines[1]);
                event.setLine(0, ChatColor.GREEN + "[付费]");
                event.setLine(1, ChatColor.DARK_BLUE + "花费: " + cost);
                event.setLine(2, ChatColor.DARK_PURPLE + player.getName());

                if (signState instanceof TileState) {
                    TileState tileState = (TileState) signState;
                    PersistentDataContainer container = tileState.getPersistentDataContainer();
                    container.set(new NamespacedKey(plugin, "PD_isActive"), PersistentDataType.INTEGER, 1);
                    container.set(new NamespacedKey(plugin, "PD_isAllowed"), PersistentDataType.INTEGER, 1);
                    container.set(new NamespacedKey(plugin, "PD_amount"), PersistentDataType.INTEGER, cost);
                    container.set(new NamespacedKey(plugin, "PD_owner"), PersistentDataType.STRING, player.getUniqueId().toString());
                }
                sign.update();
            }
        }
    }

    @EventHandler
    public void onRightClickOnPD(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getItem() != null
                && event.getItem().getType() == Material.WOODEN_AXE) {

            Block clickedBlock = event.getClickedBlock();
            BlockState blockState = clickedBlock.getState();
            if (!(clickedBlock.getBlockData() instanceof WallSign)) return;
            Sign sign = (Sign) blockState;
            if (blockState instanceof TileState) {
                TileState tileState = (TileState) blockState;
                PersistentDataContainer container = tileState.getPersistentDataContainer();

                if (!container.has(new NamespacedKey(plugin, "PD_isActive"), PersistentDataType.INTEGER)) return;
                if (!player.getUniqueId().toString().equals(container.get(new NamespacedKey(plugin, "PD_owner"), PersistentDataType.STRING)))
                    return;
                if (player.hasPermission("netherPD.edit")) {
                    boolean pd_isAllowed = container.get(new NamespacedKey(plugin, "PD_isAllowed"), PersistentDataType.INTEGER) == 1;
                    pd_isAllowed = !pd_isAllowed;
                    ChatColor color = pd_isAllowed ? ChatColor.GREEN : ChatColor.RED;
                    String status = pd_isAllowed ? "[付费]" : "[禁用]";
                    sign.setLine(0, color + status);
                    container.set(new NamespacedKey(plugin, "PD_isAllowed"), PersistentDataType.INTEGER, pd_isAllowed ? 1 : 0);
                    sign.update();
                }
            }
        }
    }

    public boolean checkDoor(Block sign, WallSign data) {
        BlockFace oppositeFace = data.getFacing().getOppositeFace();
        Block relative = sign.getRelative(oppositeFace);

        Block downBlock = relative.getRelative(BlockFace.DOWN);

        return downBlock.getType() == Material.IRON_DOOR;
    }
}
