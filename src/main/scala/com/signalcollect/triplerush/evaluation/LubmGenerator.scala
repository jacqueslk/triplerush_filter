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

package com.signalcollect.triplerush.evaluation

import java.io.File
import scala.sys.process._
import com.signalcollect.nodeprovisioning.torque.TorqueHost
import com.signalcollect.nodeprovisioning.torque.TorqueJobSubmitter
import com.signalcollect.nodeprovisioning.torque.TorqueNodeProvisioner
import com.signalcollect.nodeprovisioning.torque.TorquePriority
import java.nio.file.{ Files, Path, Paths }

object LubmGenerator extends KrakenExecutable with App {

  override def remoteRun {
    val universities = 10
    import FileOperations._

    // Generate raw LUBM files.
    s"java -cp uba.jar edu.lehigh.swat.bench.uba.Generator -univ $universities -onto http://swat.cse.lehigh.edu/onto/univ-bench.owl" !! (ProcessLogger(println(_)))

    // Create new directory and move files there.
    val targetFolder = createFolder(s"./lubm$universities")
    println("Moving OWL files ...")
    for (owlFile <- filesIn("./").filter(_.getName.endsWith("owl"))) {
      println(s"Moving file ${owlFile.getName} ...")
      move(owlFile, targetFolder)
    }
    move(new File("./log.txt"), targetFolder)
    println("All LUBM files have been copied.")
  }
}

