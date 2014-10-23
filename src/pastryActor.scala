import akka.actor.Actor
import akka.actor.ActorRef

case class JoinNetwork(nearestPeer: ActorRef)
case class FirstJoin(nodeid: String, b: Int, l: Int, lenUUID: Int)
case class startRouting(message: String)
case class AddMe(destination: String, rTable: Array[Array[ActorRef]], lastHop: Boolean)

class pastryActor(nid: String, numReq: Int, b: Int, l: Int, lenUUID: Int, logBaseB: Int) extends Actor {

  var nodeID: String = nid;
  var rTable: Array[Array[String]] = Array.ofDim[String](b, lenUUID) /*(logBaseB, lenUUID)*/
  var smallL = new Array[String](l / 2)
  var largeL = new Array[String](l / 2)

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  def receive = {

    //each node joins one by one
    case FirstJoin(nodeid: String, b: Int, l: Int, lenUUID: Int) =>
      sender ! Joined(nodeID)

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    case JoinNetwork(nearestPeer: ActorRef) =>
      //println("Entered Join")
      nearestPeer ! AddMe(nodeID, Array[Array[ActorRef]](), false)
      //println("Node " + self.path + " joined")
      sender ! Joined(nodeID)

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////      
    case AddMe(destination: String, rTable: Array[Array[ActorRef]], lastHop: Boolean) =>
      var isLastHop = lastHop
      println("node akka://pastry/user/" + destination + " asked node" + self.path + " for help")
      var level = shl(destination, nodeID)
      println("level:::::" + level)

      if (smallL.isEmpty && largeL.isEmpty && rTable.isEmpty) { //current node is the only node in network
        isLastHop = true
      }

      if (level == 0) { //can only happen for the first neighbor node
        if (!isLastHop) {
          //find next node and ask them to add me
          var next = findNext(destination, level): String
          var nextPeer = context.actorFor("akka://pastry/user/" + next)
          //UpdateSelf
          //UpdateNew
          nextPeer ! AddMe(nodeID, Array[Array[ActorRef]](), false)
        } else { //it will come here if there is only one node in the system and it does not have any matching prefix with new node
          //UpdateSelf
          //UpdateNew
        }

      } else if (level <= nodeID.length()) {
        if (!isLastHop) {

          //find next and ask them to add me
          var next = findNext(destination, level): String
          var nextPeer = context.actorFor("akka://pastry/user/" + next)
          //UpdateSelf
          //UpdateNew
          nextPeer ! AddMe(nodeID, Array[Array[ActorRef]](), false)
        } else {
          //UpdateSelf
          //UpdateNew
        }
      } else {
        println("This cannot be happening!!! What did I do wrong?")
      }
    

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    case startRouting(message: String) =>
      println("Node " + self.path + " started routing")

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    case _ => println("Entering default case of pastryActor")

  }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  def shl(nodeID1: String, nodeID2: String): Int = {
    val maxSize = scala.math.min(nodeID1.length, nodeID2.length)
    var i: Int = 0;
    while (i < maxSize && nodeID1(i) == nodeID2(i)) i += 1;
    i
  }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  def findNext(dest: String, level: Int): String = {
    //case 1
    //check leaf tables
    var found: Boolean = false
    var next: String = "somebody"
    var destDec = Integer.parseInt(dest, 2)
    var currentDec = Integer.parseInt(nodeID, 2)
    if (!largeL.isEmpty) {
      var largestDec = Integer.parseInt(largeL(l / 2 - 1), 2)
      if (destDec >= currentDec && destDec <= largestDec) { //search in smaller table
        next = null //this node is the last hop
        for (i <- 0 to l / 2 - 1) {
          if (Integer.parseInt(largeL(i), 2) <= destDec)
            next = largeL(i)
        }
        found = true
      }
    }
    if (!smallL.isEmpty) {
      var smallestDec = Integer.parseInt(smallL(l / 2 - 1), 2)
      if (destDec >= smallestDec && destDec < currentDec) { //search in smaller table
        for (i <- 0 to l / 2 - 1) {
          if (Integer.parseInt(smallL(i), 2) <= destDec)
            next = smallL(i)

        }
        found = true
      }
    }

    //case 2
    //check routing table for prefix + 1
    if (found == false) {
      var Dl = dest.charAt(level).toInt
      //check correct place in routing table if(correct place contains value) {next = that value; found = true}
      if (rTable(level)(Dl) != null) { //check values again!!!
        next = rTable(level)(Dl)
      } else {
    //case 3
    //check for same length prefix
        //check leaf tables
        /*
         * We can just forward it to the largest node in the leaf table. That should work perfectly
         */
        if (!largeL.isEmpty) {
          for (i <- 0 to l / 2 - 1) {
            if (Integer.parseInt(largeL(i), 2) <= destDec)
              next = largeL(i)
          }
          found = true
        }
      }
    }

    if (found == false) { //next node should be found by now.....code should never come here
      println("There is something wrong with the algorithm")
      sys.exit
    }

    next
  }
  
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    def UpdateSelf = {
      
    }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    def UpdateNew = {
      
    }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
