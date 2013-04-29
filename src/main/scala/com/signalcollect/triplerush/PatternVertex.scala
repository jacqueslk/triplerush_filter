/*
 *  @author Philip Stutz
 *  @author Mihaela Verman
 *  
 *  Copyright 2013 University of Zurich
 *      
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *         http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 */

package com.signalcollect.triplerush

import com.signalcollect.GraphEditor

/**
 * Basic vertex that recursively builds the TripleRush index structure. 
 */
abstract class PatternVertex[Signal, State](
  id: TriplePattern)
  extends BaseVertex[TriplePattern, Signal, State](id) {

  override def afterInitialization(graphEditor: GraphEditor[Any, Any]) {
    // Build the hierarchical index on initialization.
//    println(s"Added vertex $id, will add vertices ${id.parentPatterns}.")
    id.parentPatterns foreach { parentId =>
      graphEditor.addVertex(new IndexVertex(parentId))
      val idDelta = id.parentIdDelta(parentId)
      graphEditor.addEdge(parentId, new PlaceholderEdge(idDelta))
    }
  }
}
