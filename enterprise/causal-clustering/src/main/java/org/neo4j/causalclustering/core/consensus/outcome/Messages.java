/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) as found
 * in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
package org.neo4j.causalclustering.core.consensus.outcome;

import java.util.Iterator;
import java.util.Map;

import org.neo4j.causalclustering.core.consensus.RaftMessages;
import org.neo4j.causalclustering.identity.MemberId;

public class Messages implements Iterable<Map.Entry<MemberId, RaftMessages.RaftMessage>>
{
    private final Map<MemberId, RaftMessages.RaftMessage> map;

    Messages( Map<MemberId, RaftMessages.RaftMessage> map )
    {
        this.map = map;
    }

    public boolean hasMessageFor( MemberId member )
    {
        return map.containsKey( member );
    }

    public RaftMessages.RaftMessage messageFor( MemberId member )
    {
        return map.get( member );
    }

    @Override
    public Iterator<Map.Entry<MemberId, RaftMessages.RaftMessage>> iterator()
    {
        return map.entrySet().iterator();
    }
}
