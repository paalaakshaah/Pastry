import akka.actor.Actor
import akka.actor.ActorRef

case class JoinNetwork(nearestPeer: ActorRef)
case class FirstJoin(nodeid:String, b:Int, l:Int, lenUUID:Int)
case class startRouting(message:String)
case class AddMe(destination:String, rTable: Array[Array[ActorRef]], level:Int, logicalNeighbour: Boolean)

class pastryActor(nid: String, numReq: Int, b:Int, l:Int, lenUUID:Int, logBaseB: Int) extends Actor {
  
  var nodeId: String = nid;
  var rtable : Array[Array[ActorRef]] = Array.ofDim[ActorRef](logBaseB, lenUUID)
  var smallL = new Array[ActorRef](l/2)
  var largeL = new Array[ActorRef](l/2)
  
  def receive = {
    //each node joins one by one  
    case FirstJoin(nodeid:String, b:Int, l:Int, lenUUID:Int) =>
      sender ! Joined(nodeId)
      
  	case JoinNetwork(nearestPeer: ActorRef) =>
      println("Entered Join")
      nearestPeer ! AddMe(nodeId, Array[Array[ActorRef]](), -1, false)
      println("Node " + self.path + " joined")
      sender ! Joined(nodeId)
      
  	case AddMe(destination:String, rTable: Array[Array[ActorRef]], level:Int, logicalNeighbour: Boolean) =>
  	  //println("node akka://pastry/user/"+destination+" asked node"+self.path+ " for help")
  	  

    case startRouting(message:String) =>
      println("Node " + self.path + " started routing")

    //all node join together... to be removed later
    /*case "initTable" =>
      println("Node "+ self.path + " initializing table")
      sender ! "initTableDone"*/

    case _ => println("Entering default case of pastryActor")

  }
}
