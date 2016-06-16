package me.soringaming.moon.korra.com;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;

public class AirJumpListener implements Listener {

	@EventHandler
	public void Shift(PlayerToggleSneakEvent e) {
		BendingPlayer bp = BendingPlayer.getBendingPlayer(e.getPlayer().getName());
		if (e.getPlayer().isSneaking() && !bp.isAvatarState()) {

			return;
		}

		if (canBend(e.getPlayer())) {
			new AirJump(e.getPlayer());
		}
	}

	private boolean canBend(Player p) {
		BendingPlayer bp = BendingPlayer.getBendingPlayer(p.getName());

		if (bp.canBend(CoreAbility.getAbility("AirJump"))) {
			return true;
		}

		return false;
	}

}
