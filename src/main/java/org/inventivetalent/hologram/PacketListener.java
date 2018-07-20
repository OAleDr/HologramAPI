package org.inventivetalent.hologram;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.plugin.Plugin;
import org.inventivetalent.hologram.reflection.NMSClass;
import org.inventivetalent.hologram.touch.TouchAction;
import org.inventivetalent.hologram.touch.TouchHandler;
import org.inventivetalent.hologram.view.ViewHandler;
import org.inventivetalent.packetlistener.handler.PacketHandler;
import org.inventivetalent.packetlistener.handler.PacketOptions;
import org.inventivetalent.packetlistener.handler.ReceivedPacket;
import org.inventivetalent.packetlistener.handler.SentPacket;
import org.inventivetalent.reflection.minecraft.DataWatcher;
import org.inventivetalent.reflection.minecraft.Minecraft;
import org.inventivetalent.reflection.minecraft.Minecraft.Version;
import org.inventivetalent.reflection.resolver.FieldResolver;
import org.inventivetalent.reflection.resolver.MethodResolver;
import org.inventivetalent.reflection.util.AccessUtil;

public class PacketListener extends PacketHandler {
	
	static FieldResolver DataWatcherFieldResolver = new FieldResolver(NMSClass.DataWatcher);
	static MethodResolver DataWatcherMethodResolver = new MethodResolver(NMSClass.DataWatcher);
	protected static PacketListener instance;

	public PacketListener(Plugin pl) {
		super(pl);
		if (instance != null) {
			throw new IllegalStateException("Cannot instantiate PacketListener twice");
		}
		instance = this;
		addHandler(instance);
	}

	public static void disable() {
		if (instance != null) {
			removeHandler(instance);
			instance = null;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@PacketOptions(forcePlayer = true)
	public void onSend(SentPacket packet) {
		if (!packet.hasPlayer()) {
			return;
		}
		int listIndex;
		if (packet.getPacketName().equals("PacketPlayOutSpawnEntityLiving")) {
			int a = ((Integer) packet.getPacketValue("a")).intValue();
			Object dataWatcher = Minecraft.VERSION.olderThan(Version.v1_9_R1) ? packet.getPacketValue("l") : packet.getPacketValue("m");
			if (dataWatcher != null) {
				try {
					dataWatcher = cloneDataWatcher(dataWatcher);
					AccessUtil.setAccessible(Minecraft.VERSION.olderThan(Version.v1_9_R1) ? NMSClass.DataWatcher.getDeclaredField("a") : NMSClass.DataWatcher.getDeclaredField("b")).set(dataWatcher, null);
					if (Minecraft.VERSION.olderThan(Version.v1_9_R1)) {
						packet.setPacketValue("l", dataWatcher);
					} else {
						packet.setPacketValue("m", dataWatcher);
					}
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
			listIndex = -1;

			String text = null;
			try {
				if (Minecraft.VERSION.olderThan(Version.v1_9_R1)) {
					text = (String) DataWatcher.V1_8.getWatchableObjectValue(DataWatcher.V1_8.getValue(dataWatcher, 2));
				} else {
					Field dField = AccessUtil.setAccessible(NMSClass.DataWatcher.getDeclaredField("d"));
					Object dValue = dField.get(dataWatcher);
					if (dValue == null) {
						return;
					}
					if ((Map.class.isAssignableFrom(dValue.getClass())) && (((Map) dValue).isEmpty())) {
						return;
					}
					text = (String) DataWatcher.V1_9.getValue(dataWatcher, DataWatcher.V1_9.ValueType.ENTITY_NAME);
				}
			} catch (Exception e) {
				if (!HologramAPI.useProtocolSupport) {
					e.printStackTrace();
				}
			}
			if (text == null)
				return;
			for(Hologram h : HologramAPI.getHolograms()) {
				if (((CraftHologram) h).matchesHologramID(a)) {
					for (ViewHandler v : h.getViewHandlers()) {
						text = v.onView(h, packet.getPlayer(), text);
					}
				}
			}
			if (text == null) {
				packet.setCancelled(true);
				return;
			}
			try {
				DataWatcher.setValue(dataWatcher, 2, DataWatcher.V1_9.ValueType.ENTITY_NAME, text);
			} catch (Exception e) {
				e.printStackTrace();
			}
		
			
		}
		if (packet.getPacketName().equals("PacketPlayOutEntityMetadata")) {
			int a = ((Integer) packet.getPacketValue("a")).intValue();
			List list = (List) packet.getPacketValue("b");
			listIndex = -1;

			String text = null;
			try {
				if (list != null) {
					if (Minecraft.VERSION.olderThan(Version.v1_9_R1)) {
						for (int i = 0; i < list.size(); i++) {
							int index = DataWatcher.V1_8.getWatchableObjectIndex(list.get(i));
							if (index == 2) {
								if (DataWatcher.V1_8.getWatchableObjectType(list.get(i)) == 4) {
									text = (String) DataWatcher.V1_8.getWatchableObjectValue(list.get(i));
									listIndex = i;
									break;
								}
							}
						}
					} else if ((list.size() > 2) && (DataWatcher.V1_9.getItemType(list.get(2)) == String.class)) {
						text = (String) DataWatcher.V1_9.getItemValue(list.get(2));
						listIndex = 2;
					}
				}
			} catch (Exception e) {
				if (!HologramAPI.useProtocolSupport) {
					e.printStackTrace();
				}
			}
			if (text == null) {
				return;
			}
			for(Hologram h : HologramAPI.getHolograms()) {
				if (((CraftHologram) h).matchesHologramID(a)) {
					for (ViewHandler v : h.getViewHandlers()) {
						text = v.onView(h, packet.getPlayer(), text);
					}
				}
			}
			if (text == null) {
				packet.setCancelled(true);
				return;
			}
			try {
				if ((list == null) || (listIndex == -1)) {
					return;
				}
				Object object = Minecraft.VERSION.olderThan(Version.v1_9_R1) ? DataWatcher.V1_8.newWatchableObject(2, text) : DataWatcher.V1_9.newDataWatcherItem(DataWatcher.V1_9.ValueType.ENTITY_NAME.getType(), text);
				list.set(listIndex, object);
				packet.setPacketValue("b", list);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (packet.getPacketName().equals("PacketPlayOutMapChunkBulk")) {
			int[] a = (int[]) packet.getPacketValue("a");
			int[] b = (int[]) packet.getPacketValue("b");
			for (int i = 0; i < (a.length + b.length) / 2; i++) {
				for (Hologram hologram : HologramAPI.getHolograms()) {
					if (hologram.isSpawned()) {
						int chunkX = hologram.getLocation().getBlockX() >> 4;
						int chunkZ = hologram.getLocation().getBlockZ() >> 4;
						if ((chunkX == a[i]) && (chunkZ == b[i])) {
							try {
								HologramAPI.spawn(hologram, Collections.singletonList(packet.getPlayer()));
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}
					}
				}
			}
		}
	}

	@PacketOptions(forcePlayer = true)
	public void onReceive(ReceivedPacket packet) {
		if (packet.hasPlayer() && packet.getPacketName().equals("PacketPlayInUseEntity")) {
			int id = (int)packet.getPacketValue("a");
			Object useAction = packet.getPacketValue("action");
			TouchAction action = TouchAction.fromUseAction(useAction);
			if (action == TouchAction.UNKNOWN) {
				return;
			}
			for (Hologram h : HologramAPI.getHolograms()) {
				if (((DefaultHologram) h).matchesTouchID(id)) {
					for (TouchHandler t : h.getTouchHandlers()) {
						t.onTouch(h, packet.getPlayer(), action);
					}
				}
			}
		}
	}

	public Object cloneDataWatcher(Object original) throws Exception {
		if (original == null) {
			return null;
		}
		Object clone = DataWatcher.newDataWatcher(null);
		int index = 0;
		Object current = null;
		if (Minecraft.VERSION.olderThan(Version.v1_9_R1)) {
			while ((current = DataWatcher.V1_8.getValue(original, index++)) != null) {
				DataWatcher.V1_8.setValue(clone, DataWatcher.V1_8.getWatchableObjectIndex(current), DataWatcher.V1_8.getWatchableObjectValue(current));
			}
		}
		Field mapField = DataWatcherFieldResolver.resolve(new String[] { "c" });
		mapField.set(clone, mapField.get(original));

		return clone;
	}
}
