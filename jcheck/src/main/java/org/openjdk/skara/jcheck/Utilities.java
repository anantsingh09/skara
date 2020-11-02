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
package org.openjdk.skara.jcheck;

import org.openjdk.skara.vcs.Commit;
import org.openjdk.skara.vcs.Hash;

import java.util.HashSet;
import java.util.Set;

class Utilities {
    private final Set<Hash> addsHgTagCache = new HashSet<>();

    boolean addsHgTag(Commit commit) {
        if (addsHgTagCache.contains(commit.hash())) {
            return true;
        }
        for (var diff : commit.parentDiffs()) {
            for (var patch : diff.patches()) {
                if (!patch.target().path().isPresent() || patch.isBinary()) {
                    continue;
                }
                if (patch.target().path().get().endsWith(".hgtags") ||
                    patch.target().path().get().endsWith(".hgtags-top-repo")) {
                    for (var hunk : patch.asTextualPatch().hunks()) {
                        var removed = new HashSet<>(hunk.source().lines());
                        var added = new HashSet<>(hunk.target().lines());
                        added.removeAll(removed);
                        if (added.size() > 0) {
                            addsHgTagCache.add(commit.hash());
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
