package me.soringaming.moon.korra.com;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ParticleEffect;

public class AirJump extends AirAbility implements AddonAbility, Listener {
	
	//test

	static FileConfiguration cm = ConfigManager.defaultConfig.get();

	private Player player;
	private Permission perm;
	private double jumpPower = cm.getDouble("ExtraAbilities.Soringaming&Moon.Air.AirJump.JumpPower");
	private double avatarStateJumpPower = cm.getDouble("ExtraAbilities.Soringaming&Moon.Air.AirJump.AvatarStateJumpPower");
	private long chargetime = cm.getLong("ExtraAbilities.Soringaming&Moon.Air.AirJump.ChargeTime");
	private long cooldown = cm.getLong("ExtraAbilities.Soringaming&Moon.Air.AirJump.Cooldown");
	private boolean canJumpWithEntitys = cm.getBoolean("ExtraAbilities.Soringaming&Moon.Air.AirJump.JumpWithEntitys");
	private boolean canStopFallDamage = cm.getBoolean("ExtraAbilities.Soringaming&Moon.Air.AirJump.StopFallDamageOfEntitys");
	private boolean canExtinguish = cm.getBoolean("ExtraAbilities.Soringaming&Moon.Air.AirJump.ExtinguishPlayers");
	private boolean Charged;
	private boolean notified;
	Location loc;
	double t = 0;
	double t1 = 0.5;
	double r1 = 3;
	private boolean hasJumped;
	BendingPlayer bp;
	private double particleHeight;
	private boolean lowered;

	private static final ConcurrentHashMap<Entity, Entity> instances = new ConcurrentHashMap<Entity, Entity>();

	public AirJump(Player player) {
		super(player);
		this.player = player;
		bp = BendingPlayer.getBendingPlayer(player.getName());
		loc = player.getLocation();
		startTime = System.currentTimeMillis();
		lowered = true;
		Charged = false;
		notified = false;
		start();
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public String getName() {
		return "AirJump";
	}

	@Override
	public boolean isHarmlessAbility() {
		return true;
	}

	@Override
	public boolean isSneakAbility() {
		return false;
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			return;
		}
		if (!Charged) {
			if (bp.isAvatarState()) {
				Charged = true;
			}
			if (player.isSneaking() && System.currentTimeMillis() < startTime + chargetime) {
				if(canExtinguish) {
					player.setFireTicks(0);
				}
				doChargeParticles();
			}
			if (player.isSneaking() && System.currentTimeMillis() > startTime + chargetime) {
				if(canExtinguish) {
					player.setFireTicks(0);
				}
				doChargedParticles();
				loc = player.getLocation();
				if (notified == false) {
					player.sendMessage(ChatColor.GRAY + "You are ready to jump.");
					notified = true;
				}
			}
			if (!player.isSneaking()) {
				if (System.currentTimeMillis() > startTime + chargetime || Charged == true) {
					Charged = true;
				} else {
					remove();
					return;
				}
			}
		} else {
			doJumpParticles();
			if (hasJumped == false) {
				hasJumped = true;
				if (bp.isAvatarState()) {
					player.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(avatarStateJumpPower));
				} else {
					player.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(jumpPower));
				}
			}
			if (!bp.isAvatarState()) {
				bp.addCooldown((Ability) this);
			}
		}

	}

	public void doJumpParticles() {
		t = t + 0.2 * Math.PI;
		for (double theta = 0; theta <= 2 * Math.PI; theta = theta + Math.PI / 32) {
			double xed = t * Math.cos(theta);
			double yed = 3 * Math.exp(-0.3 * t) * Math.sin(t) + 0.2;
			double zed = t * Math.sin(theta);
			loc.add(xed, yed, zed);
			ParticleEffect.CLOUD.display(0.01F, 0.1F, 0.001F, 0.0F, 1, loc, 300);
			loc.subtract(xed, yed, zed);
			for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, 5)) {
				if (e.getEntityId() != player.getEntityId() && canJumpWithEntitys) {
					if (bp.isAvatarState()) {
						e.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(avatarStateJumpPower));
					} else {
						e.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(jumpPower));
					}
					doInAirParticles();
				}
				if(canExtinguish) {
					player.setFireTicks(0);
					e.setFireTicks(0);
				}
				if(canStopFallDamage) {
					instances.put(e, e);
				}
			}
			if (t > 10) {
				remove();
				return;
			}
		}
	}

	public void doChargeParticles() {
		Location Currentloc = player.getLocation();
		if(new Random().nextInt(4) == 0) {
			playAirbendingSound(Currentloc);
		}
		t1 = t1 + Math.PI / 8;
		double x = r1 * Math.cos(t1);
		double y = 0.3;
		double z = r1 * Math.sin(t1);
		Currentloc.add(x, y, z);
		ParticleEffect.CLOUD.display(0.1F, 0.1F, 0.1F, 0.05F, 10, Currentloc, 300);

		Currentloc.subtract(x, y, z);
		double x2 = r1 * Math.sin(t1);
		double y2 = 0.3;
		double z2 = r1 * Math.cos(t1);
		Currentloc.add(x2, y2, z2);
		ParticleEffect.CLOUD.display(0.1F, 0.1F, 0.1F, 0.05F, 10, Currentloc, 300);
		Currentloc.subtract(x2, y2, z2);

	}

	public void doInAirParticles() {
		Location Currentloc = player.getLocation();
		if(new Random().nextInt(1) == 0) {
			playAirbendingSound(Currentloc);
		}
		
		if(r1 > 1) {
			r1 -= 0.05;
		}
		
		t1 = t1 + Math.PI / 8;
		
		double x = r1 * Math.cos(t1);
		double y = 0.3;
		double z = r1 * Math.sin(t1);
		Currentloc.add(x, y, z);
		ParticleEffect.CLOUD.display(0.1F, 0.1F, 0.1F, 0.05F, 10, Currentloc, 300);

		Currentloc.subtract(x, y, z);
		double x2 = r1 * Math.sin(t1);
		double y2 = 0.3;
		double z2 = r1 * Math.cos(t1);
		Currentloc.add(x2, y2, z2);
		ParticleEffect.CLOUD.display(0.1F, 0.1F, 0.1F, 0.05F, 10, Currentloc, 300);
		Currentloc.subtract(x2, y2, z2);
		
	}

	public void doChargedParticles() {
		Location Currentloc = player.getLocation();
		if(new Random().nextInt(2) == 0) {
			playAirbendingSound(Currentloc);
		}
		t1 = t1 + Math.PI / 8;
		if (particleHeight <= 4 && lowered == true) {
			particleHeight += 0.5;
			if (particleHeight == 4) {
				lowered = false;
			}
		} else {
			particleHeight -= 0.5;
			if (particleHeight <= 0.5) {
				lowered = true;
			}
		}
		double x = r1 * Math.cos(t1);
		double y = particleHeight;
		double z = r1 * Math.sin(t1);
		Currentloc.add(x, y, z);
		ParticleEffect.CLOUD.display(0.1F, 0.1F, 0.1F, 0.05F, 3, Currentloc, 300);
		Currentloc.subtract(x, y, z);

		double x2 = r1 * Math.sin(t1);
		double y2 = particleHeight;
		double z2 = r1 * Math.cos(t1);
		Currentloc.add(x2, y2, z2);
		ParticleEffect.CLOUD.display(0.1F, 0.1F, 0.1F, 0.05F, 3, Currentloc, 300);
		Currentloc.subtract(x2, y2, z2);

	}

	@Override
	public String getDescription() {

		return getVersion() + " Developed By " + getAuthor()
				+ ":\nAirJump allows the bender to jump into the air, and if enabled via config, bring entitys with them dealing fall damage to the entity affected if eneabled in the config!";
	}

	@Override
	public String getAuthor() {
		return "Moon243 and Soringaming";
	}

	@Override
	public String getVersion() {
		return "v1.0";
	}

	@Override
	public void load() {
		ProjectKorra.plugin.getServer().getLogger().log(Level.INFO,
				getName() + " " + getVersion() + " Developed By " + getAuthor() + "Has Been Enabled ");
		ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new AirJumpListener(), ProjectKorra.plugin);
		Bukkit.getServer().getPluginManager().registerEvents(this, ProjectKorra.plugin);
		perm = new Permission("bending.ability.AirJump");
		ProjectKorra.plugin.getServer().getPluginManager().addPermission(perm);
		perm.setDefault(PermissionDefault.TRUE);

		FileConfiguration c = ConfigManager.defaultConfig.get();
		c.addDefault("ExtraAbilities.Soringaming&Moon.Air.AirJump.JumpPower", 3);
		c.addDefault("ExtraAbilities.Soringaming&Moon.Air.AirJump.AvatarStateJumpPower", 6);
		c.addDefault("ExtraAbilities.Soringaming&Moon.Air.AirJump.ChargeTime", 2000);
		c.addDefault("ExtraAbilities.Soringaming&Moon.Air.AirJump.Cooldown", 12000);
		c.addDefault("ExtraAbilities.Soringaming&Moon.Air.AirJump.JumpWithEntitys", true);
		c.addDefault("ExtraAbilities.Soringaming&Moon.Air.AirJump.StopFallDamageOfEntitys", true);
		c.addDefault("ExtraAbilities.Soringaming&Moon.Air.AirJump.ExtinguishPlayers", true);
		ConfigManager.defaultConfig.save();
	}

		//Moon was here
	@Override
	public void stop() {
		ProjectKorra.plugin.getServer().getLogger().log(Level.INFO,
				getName() + " " + getVersion() + " Developed By " + getAuthor() + "Has Been Disabled ");
		ProjectKorra.plugin.getServer().getPluginManager().removePermission(perm);
		super.remove();
	}

	@EventHandler
	public void stopFallDamage(EntityDamageEvent event) {
		if (instances.containsKey(event.getEntity())
				&& event.getCause() == DamageCause.FALL) {
					event.setCancelled(true);
		}
	}

}