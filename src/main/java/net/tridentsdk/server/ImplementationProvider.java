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
package net.tridentsdk.server;

import net.tridentsdk.Impl;
import net.tridentsdk.Server;
import net.tridentsdk.base.Substance;
import net.tridentsdk.config.Config;
import net.tridentsdk.entity.living.Player;
import net.tridentsdk.inventory.Inventory;
import net.tridentsdk.inventory.InventoryType;
import net.tridentsdk.inventory.Item;
import net.tridentsdk.logger.LogHandler;
import net.tridentsdk.logger.Logger;
import net.tridentsdk.meta.ItemMeta;
import net.tridentsdk.server.config.TridentConfig;
import net.tridentsdk.server.inventory.TridentInventory;
import net.tridentsdk.server.inventory.TridentItem;
import net.tridentsdk.server.logger.InfoLogger;
import net.tridentsdk.server.logger.LoggerHandlers;
import net.tridentsdk.server.logger.PipelinedLogger;
import net.tridentsdk.server.player.TridentPlayer;
import net.tridentsdk.server.ui.bossbar.CustomBossBar;
import net.tridentsdk.server.ui.tablist.TridentCustomTabList;
import net.tridentsdk.server.ui.tablist.TridentGlobalTabList;
import net.tridentsdk.server.ui.title.CustomTitle;
import net.tridentsdk.ui.bossbar.BossBar;
import net.tridentsdk.ui.tablist.TabList;
import net.tridentsdk.ui.title.Title;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * This class is the bridge between the server and the API,
 * and provides the implementation classes for the API via
 * {@link Impl}.
 */
@Immutable
public class ImplementationProvider implements Impl.ImplementationProvider {
    // head of the logger pipeline
    private final PipelinedLogger head;
    // instance of the handlers class
    private final LoggerHandlers handlers;

    public ImplementationProvider(PipelinedLogger head) {
        this.head = head;

        for (PipelinedLogger logger = head; logger.next() != null; logger = logger.next()) {
            if (logger.getClass().equals(LoggerHandlers.class)) {
                this.handlers = (LoggerHandlers) logger;
                return;
            }
        }
        throw new IllegalStateException("No handler found");
    }

    @Override
    public Server getServer() {
        return TridentServer.getInstance();
    }

    @Override
    public Config newCfg(Path p) {
        return TridentConfig.load(p);
    }

    @Override
    public Logger newLogger(String s) {
        return InfoLogger.get(this.head, s);
    }

    @Override
    public void attachHandler(Logger logger, LogHandler handler) {
        if (logger == null) {
            this.handlers.handlers().add(handler);
        } else {
            InfoLogger info = (InfoLogger) logger;
            info.handlers().add(handler);
        }
    }

    @Override
    public boolean removeHandler(Logger logger, LogHandler handler) {
        if (logger == null) {
            return this.handlers.handlers().remove(handler);
        } else {
            InfoLogger info = (InfoLogger) logger;
            return info.handlers().remove(handler);
        }
    }

    @Override
    public TabList getGlobalTabList() {
        return TridentGlobalTabList.getInstance();
    }

    @Override
    public TabList newTabList() {
        return new TridentCustomTabList();
    }

    @Override
    public BossBar newBossBar() {
        return new CustomBossBar();
    }

    @Override
    public Title newTitle() {
        return new CustomTitle();
    }

    @Override
    public Inventory newInventory(InventoryType type, int slots) {
        return new TridentInventory(type, slots);
    }

    @Override
    public Item newItem(Substance substance, int count, byte damage, ItemMeta meta) {
        return new TridentItem(substance, count, damage, meta);
    }

    @Override
    @Nonnull
    // TODO improve
    public Map<String, Player> findByName(String name) {
        Map.Entry<String, Player> top = TridentPlayer.getPlayerNames().floorEntry(name);
        Map.Entry<String, Player> bot = TridentPlayer.getPlayerNames().ceilingEntry(name);
        if (top == null) {
            return Collections.unmodifiableMap(TridentPlayer.getPlayerNames().headMap(bot.getKey(), true));
        } else if (bot == null) {
            return Collections.unmodifiableMap(TridentPlayer.getPlayerNames().tailMap(top.getKey(), true));
        }

        return Collections.unmodifiableMap(TridentPlayer.getPlayerNames().subMap(top.getKey(), true, bot.getKey(), true));
    }

    @Override
    @Nullable
    public Player getByName(String name) {
        return TridentPlayer.getPlayerNames().get(name);
    }
}
