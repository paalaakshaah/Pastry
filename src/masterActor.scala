import akka.actor.Actor
import akka.actor.ActorSystem
import scala.util.Random
import akka.actor.Props
import akka.actor.ActorRef
import scala.collection.mutable.HashSet

case class Joined(nodeId: String)

class masterActor(numNodes: Int, numReq: Int, mySys: ActorSystem) extends Actor {

  var b: Int = 3;
  var l: Int = 8;
  var lenUUID: Int = 1 << b //length of random string can be increased to support more actors
  var logBaseB = (Math.log(numNodes) / Math.log(lenUUID)).toInt
  //println("lenUUID::" + lenUUID + "log::" + logBaseB)
  var i: Int = 1
  var idHash = new HashSet[String]
  var peerList: List[ActorRef] = List()
  var prevNID: String = "first"
  var count: Int = 0; //jugaad varible for initial phase of codeing....to be removed later on
  
  def receive = {
  
    //each node joins one by one
    case "init" =>
      if (i == 1) {
        var nodeid: String = getRandomID(i)
        idHash.+=(nodeid)
        //println("Hash Set::" + idHash)
        var peer = mySys.actorOf(Props(new pastryActor(nodeid, numReq, b, l, lenUUID, logBaseB)), name = nodeid)
        peerList = peerList :+ peer
        i += 1
        peer ! FirstJoin(nodeid, b, l, lenUUID)
      } else if ((i > 1) && (i <= numNodes)) {
        //create nodes
        var nodeid: String = getRandomID(i)
        idHash.+=(nodeid)
        var peer = mySys.actorOf(Props(new pastryActor(nodeid, numReq, b, l, lenUUID, logBaseB)), name = nodeid)
        peerList = peerList :+ peer
        i += 1
        //add nodes
        peer ! JoinNetwork(peerList((Math.random * (peerList.length - 1)).toInt))
      } else {
        for (peer <- peerList) {
          var msg: String = "hello"
          peer ! startRouting(msg)
        }
      }
    
    case Joined(nodeId: String) =>
      self ! "init"
     
    case _ => println("Entering default case of masterActor")
  
  } //receive method ends here
  
  def getRandomID(i: Int): String = {
  
    var r = new Random();
    var sb = new StringBuffer();
    var flag = true
    
    while (flag == true) {
    
      while (sb.length() < lenUUID) {
        //sb.append(Integer.toOctalString(r.nextInt()))
        //sb.append(Integer.toString(Math.abs(Integer.parseInt(Integer.toString(r.nextInt, 4)))))
        sb.append(Integer.toBinaryString(r.nextInt))
      }
      
      //println(sb.toString().substring(0, lenUUID))
      
      if (idHash.isEmpty) {
        //println("Randomid for first node")
        flag = false
      } else if (idHash.contains(sb.toString().substring(0, lenUUID))) {
        //println("Randomid already taken")
        flag = true
        sb.setLength(0)
      } else {
        //println("Randomid is free")
        flag = false
      }
    
    }
    
    sb.toString().substring(0, lenUUID)
  } //getRandomID method ends here
}
