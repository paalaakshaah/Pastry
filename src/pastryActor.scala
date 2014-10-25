import akka.actor.Actor
import scala.concurrent.Future
import scala.concurrent.{ ExecutionContext, Promise }
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask
import akka.actor.ActorRef
import akka.util.Timeout

case class JoinNetwork(nearestPeer: ActorRef)
case class FirstJoin(nodeid: String, b: Int, l: Int, lenUUID: Int)
case class startRouting(message: String)
case class AddMe(destination: String, rT: Array[Array[String]])
case class Deliver(rt: Array[Array[String]])
case class NextPeer(nextPeer : ActorRef)

class pastryActor(nid: String, numReq: Int, b: Int, l: Int, lenUUID: Int, logBaseB: Int) extends Actor {

  var nodeID: String = nid;
  var rTable: Array[Array[String]] = Array.ofDim[String](lenUUID, lenUUID) /*(logBaseB, lenUUID)*/
  var largeLeaf: List[Int] = List()
  var smallLeaf: List[Int] = List()
  var master: ActorRef = context.actorFor("akka://pastry/user/master")

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  def receive = {
    
    case NextPeer(nextPeer : ActorRef) =>
      var nextNID: String = nextPeer.path.toString().substring(19)
      var l: Int = shl(nodeID, nextNID)
      UpdateLeafT(l, nextNID)
      nextPeer ! AddMe(nodeID, rTable)

    case Deliver(rt: Array[Array[String]]) =>
      //Thread.sleep(1000)
      rTable = rt.clone
      println("For New Node::::::::" + nodeID)
      printList(smallLeaf)
      printList(largeLeaf)
      printRTable(rTable)

      println(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::")
      println(nodeID + " has joined!!!")
      println(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::")
      master ! Joined(nodeID)

    //each node joins one by one
    case FirstJoin(nodeid: String, b: Int, l: Int, lenUUID: Int) =>
      sender ! Joined(nodeID)

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    case JoinNetwork(nearestPeer: ActorRef) =>
      var nextNID: String = nearestPeer.path.toString().substring(19)
      var l: Int = shl(nodeID, nextNID)
      UpdateLeafT(l, nextNID)
      nearestPeer ! AddMe(nodeID, Array[Array[String]]())

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////      
    case AddMe(destination: String, rT: Array[Array[String]]) =>
      var isLastHop: Boolean = false
      var dRT: Array[Array[String]] = rT.clone
      println("node akka://pastry/user/" + destination + " asked node" + self.path + " for help")
      var level = shl(destination, nodeID)
      println("level:::::" + level)

      if (smallLeaf.isEmpty && largeLeaf.isEmpty) {
        //println("::::::::::::::::::::::ONLY ONE NODE IN NETWORK:::::::::::::::::::::::")
        isLastHop = true
      }

      var next = findNext(destination, level): String
      if (next == null) {
        isLastHop = true
      }
      
      println("isLastHop::::::::" + isLastHop)
      if (level <= nodeID.length()) { //unnecessary precautionary checking
        UpdateLeafT(level, destination)
        UpdateSelfRT(level, destination)
        dRT = UpdateNewRT(destination, level, rT) //update one row in route table of destination node
        if (!isLastHop) {
          var nextPeer = context.actorFor("akka://pastry/user/" + next)
          sender ! NextPeer(nextPeer)
          //nextPeer ! AddMe(nodeID, rT, level)
        } else {
          sender ! Deliver(dRT)
        }
        println("For Node A ::::::::" + nodeID)
        printList(smallLeaf)
        printList(largeLeaf)
        printRTable(rTable)
      }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    case startRouting(message: String) =>
      println("Node " + self.path + " started routing")

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    case _ => println("Entering default case of pastryActor")

  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /*
   * 
   */
  def shl(nodeID1: String, nodeID2: String): Int = {
    val maxSize = scala.math.min(nodeID1.length, nodeID2.length)
    var i: Int = 0;
    while (i < maxSize && nodeID1(i) == nodeID2(i)) i += 1;
    i
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  def findNext(dest: String, level: Int): String = {
    println("<===========In find Next========Level = "+level + " =====================>")
    var found: Boolean = false
    var next: String = "somebody"
    var nextDec: Int = -1 
    var destDec = Integer.parseInt(dest, 2)
    var currentDec = Integer.parseInt(nodeID, 2)
    var mindiff = (destDec - currentDec).abs

    //search in leaf tables first
    if (destDec > currentDec) {
      if (!largeLeaf.isEmpty) {
        if (destDec < largeLeaf.last) {
        	//next hop is in largeLeaf
          for(i <- 0 to largeLeaf .length-1) {
            if(largeLeaf (i) < destDec) {
              nextDec = largeLeaf (i)
            }
          }
          if (nextDec != -1) {
            next = Integer.toBinaryString(nextDec)
            found = true
          }
        }
      }
    } else if(destDec < currentDec) {
      if (!smallLeaf.isEmpty) {
        if (destDec > smallLeaf.head) {
        	//next hop is in largeLeaf
          for(i <- 0 to smallLeaf .length -1) {
            if(smallLeaf (i) < destDec) {
              nextDec = smallLeaf (i)
            }
          }
          if (nextDec != -1) {
            next = Integer.toBinaryString(nextDec)
            //println(nextDec + " converted to binary " + next)
            found = true
          }
        }
      }
    }

    //search in route table
    if (!found) {
      println("Searching Routing table. Level = "+level)
      var Dl = dest.charAt(level).toString.toInt
      //check correct place in routing table if(correct place contains value) {next = that value; found = true}
      if (rTable(level)(Dl) != null) { //check values again!!!
        if(Integer.parseInt(rTable(level)(Dl), 2) < destDec) {
          next = rTable(level)(Dl)
        } else { 
          next = null
        }

        println("found in routing table")
        found = true
      }
    }

    //case three.....try to get closer
    if (!found) {
      println("not found anywhere:::::::::entered case 3")
      var eligibleNodes: Set[Int] = Set() 
      
      for(i <- 0 to largeLeaf .length-1) {
        var largeleafNode = Integer.toBinaryString(largeLeaf (i))
        if(shl(largeleafNode, dest) >= level) {
          eligibleNodes.+(largeLeaf (i))
        }
      }

      for(i <- 0 to smallLeaf  .length-1) {
        var smallleafNode = Integer.toBinaryString(smallLeaf (i))
        if(shl(smallleafNode, dest) >= level) {
          eligibleNodes.+(smallLeaf (i))
        }
      }
      
      for (i <- level to rTable .length-1) {
        for (j <- 0 to rTable (i).length -1) {
          if(rTable (i)(j) != null){
            eligibleNodes.+ (Integer.parseInt(rTable (i)(j),2))
          }
        }
      }
       
      for (iterateSet <- eligibleNodes) {
      if (iterateSet != destDec) {
        var diff: Int = (destDec - iterateSet).abs
        if (diff < mindiff) {
          mindiff = diff
          nextDec = iterateSet
          found = true
        }
      }
    }
      next = Integer.toBinaryString(nextDec)
    }

    if (!found) { //current node is the last node
      next = null
      found = true
    }

    println("next::::::::::::" + next)
    next
  }
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def UpdateLeafT(level: Int, dest: String) = {
    
    largeLeaf.distinct
    smallLeaf.distinct

    var destDec = Integer.parseInt(dest, 2)
    var currentDec = Integer.parseInt(nodeID, 2)

    var isLargeFull: Boolean = false
    var isSmallFull: Boolean = false

    if (largeLeaf.length == l / 2) {
      isLargeFull = true
    }
    
    if (smallLeaf.length == l / 2) {
      isSmallFull = true
    }
    
    if (destDec > currentDec) {
      if (!largeLeaf.contains(destDec)) {
        if (isLargeFull) {
          largeLeaf = largeLeaf :+ destDec
          largeLeaf = largeLeaf.sorted
          largeLeaf = largeLeaf.take(largeLeaf.length - 1)
          println("SIZE OF LARGE LEAF TABLE::::" + largeLeaf.length)
        } else {
          
          largeLeaf = largeLeaf :+ destDec
          largeLeaf = largeLeaf.sorted
        }
      }
    } else if (destDec < currentDec) {
      if (!smallLeaf.contains(destDec)) {
        if (isSmallFull) {
          smallLeaf = smallLeaf :+ destDec
          smallLeaf = smallLeaf.sorted
          smallLeaf = smallLeaf.take(0)
          println("SIZE OF SMALL LEAF TABLE::::" + smallLeaf.length)
        } else {
          smallLeaf = smallLeaf :+ destDec
          smallLeaf = smallLeaf.sorted
        }
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /*
   * Only one node is added to my own RT. I will always take the latest node that contacted me
   */
  def UpdateSelfRT(level: Int, dest: String) = {
    //update routing table
    var DLevel: Int = dest.toCharArray()(level).toString.toInt //digit at index "level" in node to be added
    rTable(level)(DLevel) = dest
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /*
   * Update RT of new node. written slightly differently to avoid problems during initialization.
   */
  def UpdateNewRT(dest: String, level: Int, rt : Array[Array[String]]): Array[Array[String]] = {
    //copy only non-null values from correct rows of RTable of peer. This is necessary for initial phase. or else table will never be full
    var newRT: Array[Array[String]] = Array.ofDim[String](lenUUID, lenUUID)
    rt.copyToArray(newRT)

    for (j <- 0 to rTable(level).length - 1) {
      if (rTable(level)(j) != null) //copy only those values that are not null
        newRT(level)(j) = rTable(level)(j)
    }

    //add nodeid of peer to table - WORKING
    var DLevel: Int = nodeID.toCharArray()(level).toString.toInt
    if (newRT(level)(DLevel) != null) { //if there is a value in that place, compare it with the new value and take the larger one
      newRT(level)(DLevel) = Integer.toBinaryString(Math.max(Integer.parseInt(newRT(level)(DLevel), 2), Integer.parseInt(nodeID, 2)))
    } else {
      newRT(level)(DLevel) = nodeID
    }

    //remove own nodeid from table - NOT WORKING!!! CHECK NOW
    DLevel = dest.toCharArray()(level).toString.toInt
    newRT(level)(DLevel) = null
    newRT
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /*
   * FOR DEBUG ONLY
   */
  def printTable(table: Array[String]) = {
    for (i <- 0 to table.length - 1) {
      if (table(i) != null) {
        print(table(i) + '\t')
      } else {
        print("@\t")
      }
    }
    println()
  }

  def printRTable(Table: Array[Array[String]]) = {
    for (i <- 0 to Table.length - 1) {
      printTable(Table(i))
    }
  }

  def printList(list: List[Int]) {
    print(list.mkString(", "))
    println()
  }
}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
