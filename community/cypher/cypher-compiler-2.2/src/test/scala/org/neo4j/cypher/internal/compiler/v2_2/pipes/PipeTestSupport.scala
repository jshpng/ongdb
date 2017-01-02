/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v2_2.pipes

import org.neo4j.cypher.internal.commons.CypherTestSupport
import org.neo4j.cypher.internal.compiler.v2_2.ExecutionContext
import org.neo4j.cypher.internal.compiler.v2_2.executionplan.Effects
import org.neo4j.cypher.internal.compiler.v2_2.symbols.SymbolTable
import org.scalatest.mock.MockitoSugar

trait PipeTestSupport extends CypherTestSupport with MockitoSugar {

  val newMonitor = mock[PipeMonitor]

  def pipeWithResults(f: QueryState => Iterator[ExecutionContext]): Pipe = new Pipe {
    protected def internalCreateResults(state: QueryState) = f(state)
    def exists(pred: (Pipe) => Boolean) = ???
    def planDescription = ???
    def symbols: SymbolTable = ???
    def monitor: PipeMonitor = newMonitor
    def dup(sources: List[Pipe]): Pipe = ???
    def sources: Seq[Pipe] = ???
    def localEffects: Effects = ???
  }
}
