/*
 * Trident - A Multithreaded Server Alternative
 * Copyright 2017 The TridentSDK Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tridentsdk.server.packet.play;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.inventory.Item;
import net.tridentsdk.server.inventory.TridentPlayerInventory;
import net.tridentsdk.server.net.NetClient;
import net.tridentsdk.server.net.Slot;
import net.tridentsdk.server.packet.PacketIn;

import javax.annotation.concurrent.Immutable;

/**
 * Sent by the client whenever an inventory action occurs
 * in creative mode.
 */
@Immutable
public class PlayInCreativeInventoryAction extends PacketIn {
    public PlayInCreativeInventoryAction() {
        super(PlayInCreativeInventoryAction.class);
    }

    @Override
    public void read(ByteBuf buf, NetClient client) {
        int slot = buf.readShort();
        Slot item = Slot.read(buf);

        TridentPlayerInventory inventory = client.getPlayer().getInventory();
        if (slot == -1) {
            Item drop = item.toItem();
            // TODO drop it
            return;
        }

        if (item.getId() == -1) {
            inventory.remove(slot, Integer.MAX_VALUE);
        } else {
            inventory.add(slot, item.toItem(), item.getCount());
        }
    }
}