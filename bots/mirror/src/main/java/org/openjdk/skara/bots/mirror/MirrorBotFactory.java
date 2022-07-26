/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.skara.bots.mirror;

import org.openjdk.skara.bot.*;
import org.openjdk.skara.forge.HostedBranch;
import org.openjdk.skara.vcs.Branch;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.logging.Logger;

public class MirrorBotFactory implements BotFactory {
    private final Logger log = Logger.getLogger("org.openjdk.skara.bots");;

    static final String NAME = "mirror";
    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<Bot> create(BotConfiguration configuration) {
        var storage = configuration.storageFolder();
        try {
            Files.createDirectories(storage);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        var specific = configuration.specific();

        var bots = new ArrayList<Bot>();
        for (var repo : specific.get("repositories").asArray()) {
            var fromName = repo.get("from").asString();
            var fromRepo = configuration.repository(fromName);

            var toName = repo.get("to").asString();
            var toRepo = configuration.repository(toName);

//            var branchNames = repo.contains("branches")?
//                repo.get("branches").asString().split(",") : new String[0];

//            var branches = Arrays.stream(branchNames)
//                                 .map(Branch::new)
//                                 .collect(Collectors.toList());

            var fromBranches = fromRepo.branches();
            var branchRegex = "^" + fromRepo.name().substring(fromRepo.name().lastIndexOf("/"))+ "[0-9][0-9]";
            var toBranches = toRepo.branches();
            var newlyAddedBranches = fromBranches.stream().map(x->x.name().substring(x.name().lastIndexOf("/")))
                                            .filter(x->!toBranches.contains(x) && x.matches(branchRegex)).toArray(String[]::new);
            var branches = Arrays.stream(newlyAddedBranches)
                    .map(Branch::new)
                    .collect(Collectors.toList());


            var includeTags = repo.contains("tags") && repo.get("tags").asBoolean();

            log.info("Setting up mirroring from " + fromRepo.name() + "to " + toRepo.name());
            bots.add(new MirrorBot(storage, fromRepo, toRepo, branches, includeTags));
        }
        return bots;
    }
}
