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
package net.tridentsdk.server.world;

import lombok.Getter;
import net.tridentsdk.doc.Policy;
import net.tridentsdk.logger.Logger;
import net.tridentsdk.util.Misc;
import net.tridentsdk.world.World;
import net.tridentsdk.world.WorldLoader;
import net.tridentsdk.world.opt.Dimension;
import net.tridentsdk.world.opt.WorldCreateSpec;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The implementation of the TridentSDK world registry.
 */
@Policy("singleton")
@ThreadSafe
public class TridentWorldLoader implements WorldLoader {
    /**
     * The default world name
     */
    private static final String DEFAULT_WORLD_NAME = "world";

    /**
     * The file visitor which removes all files under the
     * enclosing directory (including the enclosing
     * directory and all subdirectories).
     */
    private static final SimpleFileVisitor<Path> DELETE_FILES = new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * The instance of the world loader
     */
    @Getter
    private static final TridentWorldLoader instance = new TridentWorldLoader();

    /**
     * The collection of all the loaded worlds
     */
    private final Map<String, TridentWorld> worlds = new ConcurrentHashMap<>();

    // Prevent instantiation
    private TridentWorldLoader() {
    }

    /**
     * Initializer method for the registry.
     *
     * <p>Loads all of the worlds on file, or creates a
     * default world if it does not exist already.</p>
     */
    public void loadAll() {
        try {
            // try to walk the file tree and load the worlds
            Files.walkFileTree(Misc.HOME_PATH, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    Path levelDat = dir.resolve("level.dat");
                    if (Files.exists(levelDat)) {
                        TridentWorldLoader.this.load(dir.getFileName().toString(), dir, Dimension.OVERWORLD);
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // if the end up with no worlds or no default world
        // create a new world
        if (this.worlds.isEmpty() || !this.worlds.containsKey(DEFAULT_WORLD_NAME)) {
            this.create(DEFAULT_WORLD_NAME, WorldCreateSpec.getDefaultOptions());
        }
    }

    /**
     * Helper load method for shortcutting NBT decoding.
     *
     * @param name the name of the world to be loaded
     * @param enclosing the enclosing folder
     * @return the world, once it has loaded
     */
    @Nonnull
    private TridentWorld load(String name, Path enclosing, Dimension dimension) {
        Logger.get(this.getClass()).log("Loading world \"" + name + "\"...");
        TridentWorld world = new TridentWorld(name, enclosing, dimension);
        world.loadSpawnChunks();

        this.worlds.put(name, world);
        Logger.get(this.getClass()).log("Finished loading \"" + name + "\".");

        return world;
    }

    @Override
    public Map<String, World> getWorlds() {
        return Collections.unmodifiableMap(this.worlds);
    }

    /**
     * The raw collection of worlds that are currently
     * loaded on the server.
     *
     * @return the collection of worlds
     */
    public Collection<TridentWorld> worlds() {
        return this.worlds.values();
    }

    @Override
    public TridentWorld getDefaultWorld() {
        return this.worlds.get(DEFAULT_WORLD_NAME);
    }

    @Nonnull
    @Override
    public TridentWorld get(String name) {
        TridentWorld world = this.worlds.get(name);
        if (world != null) {
            return world;
        }

        Path enclosing = Misc.HOME_PATH.resolve(name); // TODO incase this is a subdir to nether
        if (Files.isDirectory(enclosing)) {
            Path levelDat = enclosing.resolve("level.dat");
            if (Files.exists(levelDat)) {
                return this.load(name, enclosing, Dimension.OVERWORLD);
            }
        }

        throw new IllegalArgumentException(name + " has no world");
    }

    @Override
    public TridentWorld create(String name, WorldCreateSpec spec) {
        return this.worlds.compute(name, (k, v) -> {
           if (v != null) {
               throw new IllegalArgumentException("World \"" + name + "\" already exists");
           }

            Logger.get(this.getClass()).log("Creating world \"" + name + "\"...");
            TridentWorld world = new TridentWorld(name, Misc.HOME_PATH.resolve(name), spec);
            world.loadSpawnChunks();
            world.save();
            Logger.get(this.getClass()).log("Finished creating \"" + name + "\".");
            return world;
        });
    }

    @Override
    public boolean delete(World world) {
        if (this.worlds.remove(world.getName()) != null) {
            Path path = world.getDirectory();
            try {
                Files.walkFileTree(path, DELETE_FILES);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return false;
    }
}
